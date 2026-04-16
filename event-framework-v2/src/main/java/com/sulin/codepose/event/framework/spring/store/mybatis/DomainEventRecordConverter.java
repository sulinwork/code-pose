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
        entity.setId(record.getId());
        entity.setEventKey(record.getEventKey());
        entity.setBizCode(record.getBizCode());
        entity.setBizId(record.getBizId());
        entity.setEventType(record.getEventType());
        entity.setHandlerCode(record.getHandlerCode());
        entity.setParentHandlerCode(record.getParentHandlerCode());
        entity.setPayload(record.getPayload());
        entity.setStatus(record.getStatus().name());
        entity.setRetryNum(record.getRetryNum());
        entity.setExecuteTime(record.getExecuteTime());
        entity.setVersion(record.getVersion());
        entity.setCreatedAt(toLocalDateTime(record.getCreatedAt()));
        entity.setUpdatedAt(toLocalDateTime(record.getUpdatedAt()));
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
