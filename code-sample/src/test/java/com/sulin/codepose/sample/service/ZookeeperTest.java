package com.sulin.codepose.sample.service;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ZookeeperTest {

    CuratorFramework client;

    @BeforeEach
    public void init() {
        //1 重试策略：初试时间为1s 重试10次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        //2 通过工厂创建连接
        client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181").connectionTimeoutMs(3000)
                .sessionTimeoutMs(3000)
                .retryPolicy(retryPolicy)
                .build();
        client.start();
    }

    @Test
    public void test() throws Exception {

        String s = client.create()
                .creatingParentContainersIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(String.format("/sn-v1/%s-", UUID.randomUUID()), "context".getBytes(StandardCharsets.UTF_8));
        System.out.println("s:" + s);

        Thread.sleep(2 * 10000);
    }

}
