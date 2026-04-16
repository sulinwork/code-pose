package com.sulin.codepose.event.framework.api.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

public final class HandlerExecutionRecord {

    private final Long id;
    private final String eventKey;
    private final String bizCode;
    private final Long bizId;
    private final String eventType;
    private final String handlerCode;
    private final String parentHandlerCode;
    private final String payload;
    private final Integer payloadVersion;
    private final ExecutionStatus status;
    private final Integer retryNum;
    private final LocalDateTime executeTime;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public HandlerExecutionRecord(
            Long id,
            String eventKey,
            String bizCode,
            Long bizId,
            String eventType,
            String handlerCode,
            String parentHandlerCode,
            String payload,
            Integer payloadVersion,
            ExecutionStatus status,
            Integer retryNum,
            LocalDateTime executeTime,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.eventKey = Objects.requireNonNull(eventKey, "eventKey must not be null");
        this.bizCode = Objects.requireNonNull(bizCode, "bizCode must not be null");
        this.bizId = bizId;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.handlerCode = Objects.requireNonNull(handlerCode, "handlerCode must not be null");
        this.parentHandlerCode = parentHandlerCode;
        this.payload = payload;
        this.payloadVersion = payloadVersion == null ? 1 : payloadVersion;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.retryNum = retryNum == null ? 0 : retryNum;
        this.executeTime = executeTime;
        this.version = version == null ? 0L : version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long id() {
        return id;
    }

    public String eventKey() {
        return eventKey;
    }

    public String bizCode() {
        return bizCode;
    }

    public Long bizId() {
        return bizId;
    }

    public String eventType() {
        return eventType;
    }

    public String handlerCode() {
        return handlerCode;
    }

    public String parentHandlerCode() {
        return parentHandlerCode;
    }

    public String payload() {
        return payload;
    }

    public Integer payloadVersion() {
        return payloadVersion;
    }

    public ExecutionStatus status() {
        return status;
    }

    public Integer retryNum() {
        return retryNum;
    }

    public LocalDateTime executeTime() {
        return executeTime;
    }

    public Long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

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
                payloadVersion,
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
                payloadVersion,
                status,
                retryNum,
                executeTime,
                version,
                createdAt,
                updatedAt
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HandlerExecutionRecord)) {
            return false;
        }
        HandlerExecutionRecord that = (HandlerExecutionRecord) other;
        return Objects.equals(id, that.id)
                && Objects.equals(eventKey, that.eventKey)
                && Objects.equals(bizCode, that.bizCode)
                && Objects.equals(bizId, that.bizId)
                && Objects.equals(eventType, that.eventType)
                && Objects.equals(handlerCode, that.handlerCode)
                && Objects.equals(parentHandlerCode, that.parentHandlerCode)
                && Objects.equals(payload, that.payload)
                && Objects.equals(payloadVersion, that.payloadVersion)
                && status == that.status
                && Objects.equals(retryNum, that.retryNum)
                && Objects.equals(executeTime, that.executeTime)
                && Objects.equals(version, that.version)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                eventKey,
                bizCode,
                bizId,
                eventType,
                handlerCode,
                parentHandlerCode,
                payload,
                payloadVersion,
                status,
                retryNum,
                executeTime,
                version,
                createdAt,
                updatedAt
        );
    }

    @Override
    public String toString() {
        return "HandlerExecutionRecord{" +
                "id=" + id +
                ", eventKey='" + eventKey + '\'' +
                ", bizCode='" + bizCode + '\'' +
                ", bizId=" + bizId +
                ", eventType='" + eventType + '\'' +
                ", handlerCode='" + handlerCode + '\'' +
                ", parentHandlerCode='" + parentHandlerCode + '\'' +
                ", payloadVersion=" + payloadVersion +
                ", status=" + status +
                ", retryNum=" + retryNum +
                ", executeTime=" + executeTime +
                ", version=" + version +
                '}';
    }
}
