package com.sulin.code.pose;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sulin.code.pose.id.MillisecondIncrSequence;
import com.sulin.code.pose.id.RandomSequence;
import com.sulin.code.pose.id.RedisWorkerIdHolder;
import com.sulin.code.pose.id.SnowflakeIdGenerate;
import com.sulin.code.pose.id.api.IdGenerate;
import com.sulin.code.pose.id.api.Sequence;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Unit test for simple App.
 */
public class AppTest {

    public static void main(String[] args) throws InterruptedException {
//        IdGenerate idGenerate = new SnowflakeIdGenerate(new RedisWorkerIdHolder(), new RandomSequence(6, 4));
//
////        Sequence sequence = new MillisecondIncrSequence();
//        for (int i = 0; i < 100; i++) {
//            System.out.println(idGenerate.getId());
//            if (i == 30) {
//                Thread.sleep(500);
//            }
//            if (i == 50) {
//                Thread.sleep(500);
//            }
//        }

        Map<String, String> set = new ConcurrentHashMap<>();


        ThreadPoolExecutor executors =
                new ThreadPoolExecutor(
                        10,
                        10,
                        0L,
                        TimeUnit.MINUTES,
                        new LinkedBlockingQueue<>(),
                        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("async_test_%d").build());

        RandomSequence randomSequence = new RandomSequence(10, 1);

        for (int i = 0; i < 10; i++) {
            executors.execute(() -> {
                for (int j = 0; j < 100; j++) {
                    String sequence = randomSequence.getSequence();
//                    System.out.println(sequence);
                    try {
                        set.put(sequence, sequence);
                    } catch (Exception e) {
                        System.out.println("error:" + sequence);
                    }
                }
            });
        }

        while (executors.getActiveCount() > 0) {
            //pass

        }

        System.out.println("======" + set.size());
        executors.shutdown();
        randomSequence.close();
    }
}
