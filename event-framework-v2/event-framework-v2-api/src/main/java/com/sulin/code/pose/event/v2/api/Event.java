package com.sulin.code.pose.event.v2.api;

/**
 * 事件类
 */
public interface Event {
    /**
     * 业务事件的唯一标识
     */
    String aggregateId();

    /**
     * 事件归属业务源 比如：ORDER AFTER_SALE 等等
     */
    String eventSource();

    /**
     * 事件类型
     */
    String eventType();
}
