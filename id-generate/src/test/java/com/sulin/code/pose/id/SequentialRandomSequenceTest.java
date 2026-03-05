package com.sulin.code.pose.id;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;
import java.util.Set;

public class SequentialRandomSequenceTest {

    public static void main(String[] args) throws Exception {
        int threadCount = 30;
        int idsPerThread = 10000;
        int bufferCount = 10;
        int factor = 3;

        System.out.println("Testing: " + threadCount + " threads, " + idsPerThread + " ids each");
        System.out.println("Buffer count: " + bufferCount + ", factor: " + factor);
        System.out.println("Expected unique IDs: " + (threadCount * idsPerThread));

        SequentialRandomSequence sequence = new SequentialRandomSequence(bufferCount, factor);
        Set<String> idSet = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        String id = sequence.getSequence();
                        if (!idSet.add(id)) {
                            duplicateCount.incrementAndGet();
                            System.out.println("DUPLICATE: " + id);
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

        System.out.println("\n=== Results ===");
        System.out.println("Success: " + successCount.get());
        System.out.println("Duplicates: " + duplicateCount.get());
        System.out.println("Total unique: " + idSet.size());
        
        if (duplicateCount.get() == 0) {
            System.out.println("\n✓ PASS: No duplicates found!");
        } else {
            System.out.println("\n✗ FAIL: Found " + duplicateCount.get() + " duplicates!");
        }
    }
}
