package com.sulin.codepose.event.framework.core.policy;

import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;

import java.time.Duration;
import java.time.LocalDateTime;

public class DefaultRetryPolicy implements RetryPolicy {

    private final int maxRetryCount;
    private final Duration retryDelay;

    public DefaultRetryPolicy() {
        this(16, Duration.ofMinutes(5));
    }

    public DefaultRetryPolicy(int maxRetryCount, Duration retryDelay) {
        this.maxRetryCount = maxRetryCount;
        this.retryDelay = retryDelay;
    }

    @Override
    public int maxRetryCount(String bizCode, String eventType, String handlerCode) {
        return maxRetryCount;
    }

    @Override
    public LocalDateTime nextExecuteTime(HandlerExecutionRecord record) {
        return LocalDateTime.now().plus(retryDelay);
    }
}
