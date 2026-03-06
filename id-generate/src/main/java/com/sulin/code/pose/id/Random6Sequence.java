//package com.sulin.code.pose.id;
//
//import com.sulin.code.pose.id.api.Sequence;
//import lombok.Getter;
//import org.apache.commons.lang3.StringUtils;
//
//
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class RandomSequence implements Sequence {
//
//    private final ThreadPoolExecutor executors =
//            new ThreadPoolExecutor(
//                    2,
//                    2,
//                    0L,
//                    TimeUnit.MINUTES,
//                    new ArrayBlockingQueue<>(5),
//                    new ThreadPoolExecutor.DiscardPolicy());
//
//    private final int bufferSize;
//
//    private final int maxRandomValue;
//
//    private volatile RandomBuffer randomBuffer;
//
//    private final int factor;
//
//    private final AtomicBoolean switchBufferMark = new AtomicBoolean(false);
//
//    public RandomSequence(int bufferSize, int factor) {
//        this.bufferSize = bufferSize;
//        this.factor = factor;
//        this.maxRandomValue = (int) Math.pow(10, factor);
//        this.randomBuffer = initRandomBuffer();
//    }
//
//    @Override
//    public String getSequence() {
//        for (; ; ) {
//            // 如果正在切换，快速失败重试
//            if (switchBufferMark.get()) {
//                Thread.yield();
//                continue;
//            }
//            RandomBuffer buffer = randomBuffer;
//            Integer element = buffer.poll();
//            if (Objects.nonNull(element)) {
//                return buffer.getNodeId() + StringUtils.leftPad(String.valueOf(element), factor, "0");
//            }
//            //切换buffer
//            trySwitchBuffer();
//        }
//    }
//
//    private void waitSwitchBuffer() {
//        //自旋等待
//        while (switchBufferMark.get()) {
//            Thread.yield();
//        }
//    }
//
//    private void trySwitchBuffer() {
//        if (switchBufferMark.get()) {
//            waitSwitchBuffer();
//            return;
//        }
//        //cas 修改状态
//        if (!switchBufferMark.compareAndSet(false, true)) {
//            waitSwitchBuffer();
//        } else {
//            RandomBuffer oldBuffer = randomBuffer;
//            randomBuffer = oldBuffer.nextBuffer;
//            executors.execute(oldBuffer::refresh);
//            if (randomBuffer.isEmpty()) {
//                //切换到下一个buffer还是空 同步刷新吧
//                randomBuffer.refresh();
//            }
//            switchBufferMark.set(false);
//        }
//    }
//
//
//    private RandomBuffer initRandomBuffer() {
//        RandomBuffer headBuffer = new RandomBuffer(maxRandomValue, 1);
//        RandomBuffer current = headBuffer;
//        int i = 1;
//        while (i < this.bufferSize && current.nextBuffer == null) {
//            current.nextBuffer = new RandomBuffer(this.maxRandomValue, i + 1);
//            current = current.nextBuffer;
//            i++;
//        }
//        current.nextBuffer = headBuffer;
//        return headBuffer;
//    }
//
//    public void close(){
//        executors.shutdown();
//    }
//
//
//    // 随机buffer 环形队列
//    private static class RandomBuffer {
//        @Getter
//        private final int nodeId;
//        private final int[] array;
//        private final AtomicInteger index;
//        @Getter
//        private volatile RandomBuffer nextBuffer;
//
//        public RandomBuffer(int size, int nodeId) {
//            this.nodeId = nodeId;
//            this.array = new int[size];
//            this.index = new AtomicInteger(0);
//            init();
//            refresh();  // 构造时初始化
//        }
//
//        public void init() {
//            int n = array.length;
//
//            // 初始化数组
//            for (int i = 0; i < n; i++) {
//                array[i] = i;
//            }
//
//            index.set(n);
//        }
//
//        public void refresh() {
//            if (!isEmpty()) {
//                //不用完不能刷新 洗牌后会出现相同的序号被使用
//                return;
//            }
//
//            int n = array.length;
//            // 关键点：在这里获取当前线程的ThreadLocalRandom
//            ThreadLocalRandom rnd = ThreadLocalRandom.current();
//
//            // 先标记为不可用（防止其他线程看到一半的数据）
//            index.getAndSet(n);  // 原子操作
//
//            // Fisher-Yates洗牌
//            for (int i = n - 1; i > 0; i--) {
//                int j = rnd.nextInt(i + 1);
//                int temp = array[i];
//                array[i] = array[j];
//                array[j] = temp;
//            }
//            index.set(0);
//        }
//
//        public Integer poll() {
//            int idx = index.getAndIncrement();
//            return idx < array.length ? array[idx] : null;
//        }
//
//        public boolean isEmpty() {
//            return index.get() >= array.length;
//        }
//    }
//}
