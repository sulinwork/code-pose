package com.sulin.codepose.event.framework.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@AllArgsConstructor
public final class HandlerExecutionRecord {

    private final Long id;
    private final String eventKey;
    private final String bizCode;
    private final Long bizId;
    private final String eventType;
    private final String handlerCode;
    private final String parentHandlerCode;
    private final String payload;
    private final ExecutionStatus status;
    private final Integer retryNum;
    private final LocalDateTime executeTime;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public HandlerExecutionRecord withState(
            ExecutionStatus nextStatus,
            Integer nextRetryNum,
            LocalDateTime nextExecuteTime,
            Long nextVersion,
            Instant nextUpdatedAt
    ) {
        return new HandlerExecutionRecord(
                id,
                eventKey,
                bizCode,
                bizId,
                eventType,
                handlerCode,
                parentHandlerCode,
                payload,
                nextStatus,
                nextRetryNum,
                nextExecuteTime,
                nextVersion,
                createdAt,
                nextUpdatedAt
        );
    }

    public HandlerExecutionRecord withId(Long nextId) {
        return new HandlerExecutionRecord(
                nextId,
                eventKey,
                bizCode,
                bizId,
                eventType,
                handlerCode,
                parentHandlerCode,
                payload,
                status,
                retryNum,
                executeTime,
                version,
                createdAt,
                updatedAt
        );
    }
}
