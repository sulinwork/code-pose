package com.sulin.code.pose.id;

import com.google.common.collect.Lists;
import com.sulin.code.pose.id.api.Sequence;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 子龙版本
 */
public class Random5Sequence implements Sequence {
    private static final int NORMAL = 0;
    private static final int CHANGE_NEXT_BUFFER = 1;
    private volatile int state;
    private static int stateOffset;
    private RandomBuffer buffer;
    private final int bufferSize;

    private static Unsafe UNSAFE;


    private static final ThreadPoolExecutor Async = new ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            UNSAFE = (Unsafe) unsafe.get(null);
            stateOffset = (int) UNSAFE.objectFieldOffset(Random5Sequence.class.getDeclaredField("state"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Random5Sequence(final int factor, final int size) {
        this.buffer = new RandomBuffer(factor, size);
        this.bufferSize = size;
        this.state = NORMAL;
    }


    public String getRandomValue() throws InterruptedException {
        check();
        Integer randomValue;
        Integer nodeId;
        for (; ; ) {
            synchronized (buffer) {
                nodeId = buffer.nodeId;
                randomValue = buffer.poll();
            }
            if (randomValue == null) {
                check();
                while (state != NORMAL) {
                    synchronized (this) {
                        // 增加300ms超时，以免全部等待在这里无法唤醒
                        this.wait(500);
                    }
                }
            } else {
                break;
            }
        }
        return nodeId + StringUtils.leftPad(String.valueOf(randomValue), bufferSize, "0");
    }

    private void check() {
        if (this.buffer.isEmpty() && compareAndSwapState(NORMAL, CHANGE_NEXT_BUFFER)) {
            refreshBuffer(this.buffer);
            changeBuffer();
        }
    }

    private boolean compareAndSwapState(int curState, int newState) {
        return UNSAFE.compareAndSwapInt(this, stateOffset, curState, newState);
    }


    @SneakyThrows
    private void changeBuffer() {
        // 应对极端情况，所有buffer用完一轮还未完成刷新，则等待100ms 手动再刷新一次
        if (buffer.nextBuffer.isEmpty()) {
            TimeUnit.MILLISECONDS.sleep(100);
            this.buffer.nextBuffer.refresh();
        }
        synchronized (buffer) {
            // 修改当前buffer指向下一个buffer
            this.buffer = buffer.nextBuffer;
        }
        this.state = NORMAL;
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void refreshBuffer(RandomBuffer randomBuffer) {
        Async.submit(randomBuffer::refresh);
    }

    @SneakyThrows
    @Override
    public String getSequence() {
        return getRandomValue();
    }

    @Override
    public void close() {
        Async.shutdown();
    }


    // 随机buffer 环形队列
    private static class RandomBuffer {
        private int nodeId;
        private final int size;
        private RandomBuffer nextBuffer;
        private ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();

        public RandomBuffer(int size) {
            this.size = size;
            this.refresh();
        }

        public RandomBuffer(int factor, int size) {
            this(size);
            int i = 1;
            RandomBuffer current = this;
            while (i < factor && current.nextBuffer == null) {
                RandomBuffer randomBuffer = new RandomBuffer(size);
                current.nodeId = i;
                current.nextBuffer = randomBuffer;
                current = current.nextBuffer;
                i++;
            }
            current.nodeId = i;
            current.nextBuffer = this;
        }


        public Integer poll() {
            return queue.poll();
        }

        public void refresh() {
            queue.clear();
            final List<Integer> list = Lists.newArrayList();
            final int maximum = (int) Math.pow(10, size);
            for (int i = 0; i < maximum; i++) {
                list.add(i);
            }
            Collections.shuffle(list);
            queue = new ConcurrentLinkedQueue<>(list);
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }
}
