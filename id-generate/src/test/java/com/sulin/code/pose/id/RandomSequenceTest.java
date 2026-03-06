package com.sulin.code.pose.id;

import com.sulin.code.pose.id.api.Sequence;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;

public class RandomSequenceTest {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100; i++) {
            boolean b = runOnce();
            if(!b){
                System.err.println("出现重复");
            }
            Thread.sleep(200);
        }
    }

    public static boolean runOnce() throws InterruptedException {
        int threadCount = 30;
        int idsPerThread = 10000;
        int bufferSize = 4;
        int factor = 5;

//        System.out.println("Testing RandomSequence: " + threadCount + " threads, " + idsPerThread + " ids each");
//        System.out.println("Buffer size: " + bufferSize + ", factor: " + factor);
//        System.out.println("Expected unique IDs: " + (threadCount * idsPerThread));

        Sequence sequence = new RandomSequence(bufferSize, factor);
        Set<String> idSet = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        long start = System.currentTimeMillis();
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        String id = sequence.getSequence();
                        if (!idSet.add(id)) {
                            duplicateCount.incrementAndGet();
//                            System.out.println("DUPLICATE: " + id);
                        } else {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        sequence.close();

//        System.out.println("\n=== Results ===");
//        System.out.println("Success: " + successCount.get());
//        System.out.println("Duplicates: " + duplicateCount.get());
//        System.out.println("Total unique: " + idSet.size());
        System.out.println("耗时：" + (System.currentTimeMillis() - start));

//        if (duplicateCount.get() == 0) {
//            System.out.println("\n✓ PASS: No duplicates found!");
//        } else {
//            System.out.println("\n✗ FAIL: Found " + duplicateCount.get() + " duplicates!");
//        }

        return duplicateCount.get() == 0;
    }
}
