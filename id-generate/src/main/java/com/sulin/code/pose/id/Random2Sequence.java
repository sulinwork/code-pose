package com.sulin.code.pose.id;

import com.sulin.code.pose.id.api.Sequence;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 没问题
 */
public class Random2Sequence implements Sequence {

    private final ThreadPoolExecutor executors =
            new ThreadPoolExecutor(
                    2,
                    2,
                    0L,
                    TimeUnit.MINUTES,
                    new ArrayBlockingQueue<>(5),
                    new ThreadPoolExecutor.DiscardPolicy());

    private final int bufferSize;

    private final int maxRandomValue;

    private volatile RandomBuffer randomBuffer;

    private final int factor;

    private final AtomicBoolean switchBufferMark = new AtomicBoolean(false);

    public Random2Sequence(int bufferSize, int factor) {
        this.bufferSize = bufferSize;
        this.factor = factor;
        this.maxRandomValue = (int) Math.pow(10, factor);
        this.randomBuffer = initRandomBuffer();
    }

    @Override
    public synchronized String getSequence() {
        for (; ; ) {
            Integer poll = randomBuffer.poll();
            if (Objects.nonNull(poll)) {
                return randomBuffer.getNodeId() + StringUtils.leftPad(String.valueOf(poll), factor, "0");
            }
            synchronized (this) {
                //double check
                poll = randomBuffer.poll();
                if (Objects.nonNull(poll)) {
                    return randomBuffer.getNodeId() + StringUtils.leftPad(String.valueOf(poll), factor, "0");
                }
                //没了 要切换了
                RandomBuffer old = randomBuffer;
                executors.execute(old::refresh);
                //在这里 old从1 变替换成了2 导致这里的next变成了3
                randomBuffer = old.nextBuffer;
            }
        }
    }

    @Override
    public void close() {
        executors.shutdown();
    }

    private RandomBuffer initRandomBuffer() {
        RandomBuffer headBuffer = new RandomBuffer(maxRandomValue, 1);
        RandomBuffer current = headBuffer;
        int i = 1;
        while (i < this.bufferSize && current.nextBuffer == null) {
            current.nextBuffer = new RandomBuffer(this.maxRandomValue, i + 1);
            current = current.nextBuffer;
            i++;
        }
        current.nextBuffer = headBuffer;
        return headBuffer;
    }


    // 随机buffer 环形队列
    private static class RandomBuffer {
        @Getter
        private final int nodeId;
        private final int[] array;
        private final AtomicInteger index;
        @Getter
        private volatile RandomBuffer nextBuffer;

        public RandomBuffer(int size, int nodeId) {
            this.nodeId = nodeId;
            this.array = new int[size];
            this.index = new AtomicInteger(0);
            init();
            refresh();  // 构造时初始化
        }

        public void init() {
            int n = array.length;
            // 初始化数组
            for (int i = 0; i < n; i++) {
                array[i] = i;
            }
        }

        public void refresh() {
            int n = array.length;
            // 关键点：在这里获取当前线程的ThreadLocalRandom
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            // 先标记为不可用（防止其他线程看到一半的数据）
            index.getAndSet(n);  // 原子操作

            // Fisher-Yates洗牌
            for (int i = n - 1; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
            index.set(0);
        }

        public Integer poll() {
            int idx = index.getAndIncrement();
            return idx < array.length ? array[idx] : null;
        }

        public boolean isEmpty() {
            return index.get() >= array.length;
        }
    }
}
