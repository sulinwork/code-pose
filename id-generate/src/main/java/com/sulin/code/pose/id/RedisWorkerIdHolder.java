package com.sulin.code.pose.id;

import com.sulin.code.pose.id.api.WorkerIdHolder;
import com.sulin.codepose.kit.json.Gsons;
import com.sun.corba.se.impl.orbutil.graph.NodeData;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RedisWorkerIdHolder implements WorkerIdHolder {

    private static final String WORK_ID_KEY = "IdWork:%s";
    private static final String LOCK_KEY = "IdWork:lock:%s";

    private final StatefulRedisConnection<String, String> connect;
    private final RedisClient redisClient;

    private static final long OFFLINE_TIME_DIFFERENCE = 1000L * 60 * 30;

    private final String serverName;

    private final String serverIp;

    private final ScheduledExecutorService HEARTBEAT_TASK = Executors.newSingleThreadScheduledExecutor();

    private Integer workerId;


    public RedisWorkerIdHolder(String serverName, String serverIp, String host, Integer port) {
        this.serverName = serverName;
        this.serverIp = serverIp;
        RedisURI redisUri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        redisClient = RedisClient.create(redisUri);
        connect = redisClient.connect();
        //初始化
        initWorkerId();
        //心跳任务
        heartbeat();
    }

    private String getWorkIdKey() {
        return String.format(WORK_ID_KEY, this.serverName);
    }

    private String getLockKey() {
        return String.format(LOCK_KEY, this.serverName);
    }

    @SneakyThrows
    public void initWorkerId() {
        for (int i = 0; i < 10; i++) {
            String set = connect.sync().set(getLockKey(), "", SetArgs.Builder.nx().ex(30));
            if (StringUtils.equals(set, "OK")) {
                try {
                    doInitWorkerId();
                } finally {
                    connect.sync().del(getLockKey());
                }
                return;
            }
            TimeUnit.SECONDS.sleep(RandomUtils.nextInt(1, 5));
        }

        throw new RuntimeException("initWorkerId acquire lock error");
    }

    public void doInitWorkerId() {
        Map<String, String> allKeys = connect.sync().hgetall(getWorkIdKey());
        //看看有木有自己
        Optional<Map.Entry<String, String>> matchHistoryOpt = allKeys.entrySet()
                .stream()
                .filter(entry -> {
                    NodeData nodeData = Gsons.GSON.fromJson(entry.getValue(), NodeData.class);
                    return Objects.nonNull(nodeData) && StringUtils.equals(nodeData.getOwner(), this.serverIp);
                }).findAny();
        if (matchHistoryOpt.isPresent()) {
            this.workerId = Integer.parseInt(matchHistoryOpt.get().getKey());
            occupyWorkId();
            return;
        }
        Set<String> effectiveWordIds = allKeys.entrySet()
                .stream()
                .filter(entry -> {
                    NodeData nodeData = Gsons.GSON.fromJson(entry.getValue(), NodeData.class);
                    return !nodeData.isOffline();
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        //开始寻找
        for (int i = 1; i < 100; i++) {
            if (!effectiveWordIds.contains(String.valueOf(i))) {
                this.workerId = i;
                occupyWorkId();
                return;
            }
        }
        throw new RuntimeException("初始化workId异常,可能已经超出最大上限");
    }

    /**
     * 占用workId
     */
    private void occupyWorkId() {
        String value = connect.sync().hget(getWorkIdKey(), String.valueOf(this.workerId));
        if (StringUtils.isEmpty(value)) {
            doOccupyWorkId();
            return;
        }
        NodeData nodeData = Gsons.GSON.fromJson(value, NodeData.class);
        //看看是不是自己
        if (StringUtils.equals(nodeData.getOwner(), serverIp) || nodeData.isOffline()) {
            doOccupyWorkId();
            return;
        }

        throw new RuntimeException("非法抢占workId");
    }

    private void doOccupyWorkId() {
        NodeData nodeData = new NodeData()
                .setOwner(serverIp)
                .setUpdateTime(System.currentTimeMillis());
        Boolean re = connect.sync().hset(getWorkIdKey(), String.valueOf(this.workerId), Gsons.GSON.toJson(nodeData));
        if (Objects.isNull(re)) {
            throw new RuntimeException("占用workId失败");
        }
    }

    private void heartbeat() {
        HEARTBEAT_TASK.scheduleAtFixedRate(this::doOccupyWorkId, 60, 60, TimeUnit.SECONDS);
    }


    @Override
    public void close() {
        connect.sync().hdel(getWorkIdKey(), String.valueOf(workerId));
        connect.close();
        redisClient.close();
        HEARTBEAT_TASK.shutdown();
    }

    @Override
    public String getWorkerId() {
        return StringUtils.leftPad(String.valueOf(workerId), 2, "0");
    }


    @Data
    @Accessors(chain = true)
    public static class NodeData {
        private String owner;
        private Long updateTime;


        public boolean isOffline() {
            return Objects.isNull(updateTime) || (System.currentTimeMillis() - updateTime) > OFFLINE_TIME_DIFFERENCE;
        }
    }
}
