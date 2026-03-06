package com.sulin.code.pose.id;


import com.sulin.code.pose.id.api.IdGenerate;
import com.sulin.code.pose.id.api.WorkerIdHolder;

public class RedisWorkerIdHolderTest {
    public static void main(String[] args) {


//        new RedisWorkerIdHolder("mall-order", "demo-2", "10.2.0.86", 6379);
        WorkerIdHolder redisWorkerIdHolder = new RedisWorkerIdHolder("mall-order", "demo-1", "10.2.0.86", 6379);
//        new RedisWorkerIdHolder("mall-order", "demo-3", "10.2.0.86", 6379);


        RandomSequence sequence = new RandomSequence(10, 5);
        IdGenerate idGenerate = new SnowflakeIdGenerate(redisWorkerIdHolder, sequence);
        for (int i = 0; i < 50; i++) {
            System.out.println(idGenerate.getId("CO"));
        }

        redisWorkerIdHolder.close();
        sequence.close();
    }
}
