package com.sulin.codepose.event.framework.api.policy;

import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;

import java.time.LocalDateTime;

public interface RetryPolicy {

    int maxRetryCount(String bizCode, String eventType, String handlerCode);

    LocalDateTime nextExecuteTime(HandlerExecutionRecord record);

    default boolean canRetry(HandlerExecutionRecord record) {
        return record.getRetryNum() < maxRetryCount(
                record.getBizCode(),
                record.getEventType(),
                record.getHandlerCode()
        );
    }
}
