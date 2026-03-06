package com.sulin.code.pose.id.api;

public interface WorkerIdHolder {
    String getWorkerId();

    default void close(){}
}
