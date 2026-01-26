package com.sulin.code.pose.event.v2.api;

public interface EventStore {
    void store(IntegrationEvent event);
}
