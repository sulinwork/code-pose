package com.sulin.codepose.event.framework.spring.store.mybatis;

import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class DomainEventRecordConverter {

    private static final ZoneOffset STORAGE_OFFSET = ZoneOffset.UTC;

    private DomainEventRecordConverter() {
    }

    static DomainEventRecordEntity toEntity(HandlerExecutionRecord record) {
        DomainEventRecordEntity entity = new DomainEventRecordEntity();
        entity.setId(record.id());
        entity.setEventKey(record.eventKey());
        entity.setBizCode(record.bizCode());
        entity.setBizId(record.bizId());
        entity.setEventType(record.eventType());
        entity.setHandlerCode(record.handlerCode());
        entity.setParentHandlerCode(record.parentHandlerCode());
        entity.setPayload(record.payload());
        entity.setPayloadVersion(record.payloadVersion());
        entity.setStatus(record.status().name());
        entity.setRetryNum(record.retryNum());
        entity.setExecuteTime(record.executeTime());
        entity.setVersion(record.version());
        entity.setCreatedAt(toLocalDateTime(record.createdAt()));
        entity.setUpdatedAt(toLocalDateTime(record.updatedAt()));
        return entity;
    }

    static HandlerExecutionRecord toRecord(DomainEventRecordEntity entity) {
        return new HandlerExecutionRecord(
                entity.getId(),
                entity.getEventKey(),
                entity.getBizCode(),
                entity.getBizId(),
                entity.getEventType(),
                entity.getHandlerCode(),
                entity.getParentHandlerCode(),
                entity.getPayload(),
                entity.getPayloadVersion(),
                ExecutionStatus.valueOf(entity.getStatus()),
                entity.getRetryNum(),
                entity.getExecuteTime(),
                entity.getVersion(),
                toInstant(entity.getCreatedAt()),
                toInstant(entity.getUpdatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, STORAGE_OFFSET);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(STORAGE_OFFSET);
    }
}
