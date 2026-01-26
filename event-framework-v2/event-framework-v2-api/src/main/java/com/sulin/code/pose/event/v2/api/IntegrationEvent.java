package com.sulin.code.pose.event.v2.api;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IntegrationEvent {
    private Long eventId;

    /**
     * 业务的唯一标识（聚合根id）
     */
    private String aggregateId;

    /**
     * 事件归属业务源 比如：ORDER AFTER_SALE 等等
     */
    private String eventSource;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 期望的执行时间
     */
    private LocalDateTime expectedTime;

    /**
     * json上下文信息
     */
    private String context;
}
