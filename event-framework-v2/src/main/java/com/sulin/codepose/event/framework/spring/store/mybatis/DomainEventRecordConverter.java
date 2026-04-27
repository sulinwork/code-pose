package com.sulin.codepose.event.framework.spring.store.mybatis;

import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.serialize.EventPayloadSerializer;
import com.sulin.codepose.event.framework.core.serialize.JacksonEventPayloadSerializer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

final class DomainEventRecordConverter {

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
        entity.setCreatedAt(Objects.nonNull(record.getId()) ? null : LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setEventContext(record.getEventContext());
        return entity;
    }

    static HandlerExecutionRecord toRecord(DomainEventRecordEntity entity) {

        return new HandlerExecutionRecord()
                .setId(entity.getId())
                .setEventKey(entity.getEventKey())
                .setBizCode(entity.getBizCode())
                .setBizId(entity.getBizId())
                .setEventType(entity.getEventType())
                .setHandlerCode(entity.getHandlerCode())
                .setParentHandlerCode(entity.getParentHandlerCode())
                .setStatus(ExecutionStatus.valueOf(entity.getStatus()))
                .setRetryNum(entity.getRetryNum())
                .setExecuteTime(entity.getExecuteTime())
                .setVersion(entity.getVersion())
                .setPayload(entity.getPayload())
                .setEventContext(entity.getEventContext());


    }

}
