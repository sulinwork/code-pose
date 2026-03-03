package com.sulin.code.pose.id;

import com.sulin.code.pose.id.api.WorkerIdHolder;

public class RedisWorkerIdHolder implements WorkerIdHolder {
    @Override
    public long getWorkerId() {
        return 1L;
    }

    @Override
    public long getMaxWorkerId() {
        return 1024L;
    }
}
