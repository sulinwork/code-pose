//package com.sulin.code.pose.id;
//
//import com.google.common.collect.Lists;
//import com.sulin.code.pose.id.api.Sequence;
//import lombok.Getter;
//import lombok.SneakyThrows;
//import org.apache.commons.lang3.StringUtils;
//import sun.misc.Unsafe;
//
//import java.lang.reflect.Field;
//import java.util.Collections;
//import java.util.List;
//import java.util.Objects;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * 子龙版本
// */
//public class Random3Sequence implements Sequence {
//    private static final int NORMAL = 0;
//    private static final int CHANGE_NEXT_BUFFER = 1;
//    private volatile int state;
//    private static int stateOffset;
//    private volatile RandomBuffer buffer;
//    private final int bufferSize;
//
//    private static Unsafe UNSAFE;
//
//
//    private static final ThreadPoolExecutor Async = new ThreadPoolExecutor(
//            10,
//            10,
//            0,
//            TimeUnit.SECONDS,
//            new LinkedBlockingQueue<>(),
//            new ThreadPoolExecutor.CallerRunsPolicy());
//
//    static {
//        try {
//            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
//            unsafe.setAccessible(true);
//            UNSAFE = (Unsafe) unsafe.get(null);
//            stateOffset = (int) UNSAFE.objectFieldOffset(Random3Sequence.class.getDeclaredField("state"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public Random3Sequence(final int factor, final int size) {
//        this.buffer = new RandomBuffer(factor, size);
//        this.bufferSize = size;
//        this.state = NORMAL;
//    }
//
//
//    public String getRandomValue() throws InterruptedException {
//        check();
//        Integer randomValue;
//        RandomBuffer oldBuffer;
//        for (; ; ) {
//            //A挂起
//            oldBuffer = buffer;
//            randomValue = oldBuffer.poll(); //poll 上一个环buffer 第一个
//            if (randomValue == null) {
//                check();
//                while (state != NORMAL) {
//                    synchronized (this){
//                        this.wait(500);
//                    }
////                    Thread.yield();
//                }
//            } else {
//                if (Objects.equals(oldBuffer, this.buffer)) {
//                    break;
//                } else {
//                    continue;
//                }
//            }
//        }
//        return oldBuffer.nodeId + StringUtils.leftPad(String.valueOf(randomValue), bufferSize, "0");
//    }
//
//    @SneakyThrows
//    private void check() {
//        if (this.buffer.isEmpty() && compareAndSwapState(NORMAL, CHANGE_NEXT_BUFFER)) {
//            System.out.printf("change %s[%s]->%s \n", buffer.nodeId, buffer.queue.size(), buffer.nextBuffer.nodeId);
//            if (!this.buffer.isEmpty()) {
//
//                this.state = NORMAL;
//                this.notifyAll();
//                return;
//            }
//
//            refreshBuffer(this.buffer);
//
//            // 应对极端情况，所有buffer用完一轮还未完成刷新，则等待100ms 手动再刷新一次
//            if (buffer.nextBuffer.isEmpty()) {
////                System.out.println("sleep:" + buffer.nodeId + ":" + buffer.nextBuffer.nodeId);
//                TimeUnit.MILLISECONDS.sleep(100);
//                this.buffer.nextBuffer.refresh();
//            }
//
//            synchronized (this) {
//                int cur = this.buffer.nodeId;
//                int size = this.buffer.queue.size();
//                // 修改当前buffer指向下一个buffer
//                this.buffer = this.buffer.nextBuffer;
//                this.state = NORMAL;
////                System.out.printf("change cur %s-%s to %s \n", cur,size, this.buffer.nodeId);
//                this.notifyAll();
//            }
//        }
//    }
//
//    private boolean compareAndSwapState(int curState, int newState) {
//        return UNSAFE.compareAndSwapInt(this, stateOffset, curState, newState);
//    }
//
//
//    private void refreshBuffer(RandomBuffer randomBuffer) {
//        Async.submit(randomBuffer::refresh);
//    }
//
//    @Override
//    public void close() {
//        Async.shutdown();
//    }
//
//    @SneakyThrows
//    @Override
//    public String getSequence() {
//        return getRandomValue();
//    }
//
//
//    // 随机buffer 环形队列
//    private static class RandomBuffer {
//        private int nodeId;
//        private final int size;
//        private volatile RandomBuffer nextBuffer;
//        private volatile ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
//
//        public RandomBuffer(int size) {
//            this.size = size;
//            this.refresh();
//        }
//
//        public RandomBuffer(int factor, int size) {
//            this(size);
//            int i = 1;
//            RandomBuffer current = this;
//            while (i < factor && current.nextBuffer == null) {
//                RandomBuffer randomBuffer = new RandomBuffer(size);
//                current.nodeId = i;
//                current.nextBuffer = randomBuffer;
//                current = current.nextBuffer;
//                i++;
//            }
//            current.nodeId = i;
//            current.nextBuffer = this;
//        }
//
//
//        public Integer poll() {
//            return queue.poll();
//        }
//
//        public void refresh() {
//            queue.clear();
//            final List<Integer> list = Lists.newArrayList();
//            final int maximum = (int) Math.pow(10, size);
//            for (int i = 0; i < maximum; i++) {
//                list.add(i);
//            }
//            Collections.shuffle(list);
//            queue = new ConcurrentLinkedQueue<>(list);
//        }
//
//        public boolean isEmpty() {
//            return queue.isEmpty();
//        }
//    }
//}
