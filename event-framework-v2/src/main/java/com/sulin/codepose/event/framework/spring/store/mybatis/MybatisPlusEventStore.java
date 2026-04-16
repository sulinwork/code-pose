package com.sulin.codepose.event.framework.spring.store.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MybatisPlusEventStore implements EventStore {

    private static final List<ExecutionStatus> RETRYABLE_STATUSES = Arrays.asList(
            ExecutionStatus.PENDING,
            ExecutionStatus.PROCESSING,
            ExecutionStatus.GROUP_MAIN_FINISHED,
            ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT
    );

    private final DomainEventRecordMapper mapper;

    public MybatisPlusEventStore(DomainEventRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void append(DomainEvent event, List<HandlerExecutionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (HandlerExecutionRecord record : records) {
            if (!Objects.equals(event.eventKey(), record.eventKey())) {
                throw new IllegalArgumentException("Record eventKey does not match appended event");
            }
            insertRecord(record);
        }
    }

    @Override
    public boolean compareAndSet(
            Long recordId,
            Long expectedVersion,
            ExecutionStatus expectedStatus,
            HandlerExecutionRecord nextRecord
    ) {
        if (recordId == null || nextRecord == null) {
            return false;
        }
        UpdateWrapper<DomainEventRecordEntity> update = new UpdateWrapper<DomainEventRecordEntity>()
                .eq("id", recordId)
                .eq("version", expectedVersion)
                .eq("status", expectedStatus.name())
                .set("status", nextRecord.status().name())
                .set("retry_num", nextRecord.retryNum())
                .set("execute_time", nextRecord.executeTime())
                .set("version", nextRecord.version())
                .set("updated_at", toStorageTime(nextRecord.updatedAt()));
        return mapper.update(null, update) == 1;
    }

    @Override
    public List<HandlerExecutionRecord> scanRetryable(ReplayScanRequest request) {
        QueryWrapper<DomainEventRecordEntity> query = new QueryWrapper<DomainEventRecordEntity>()
                .in("status", retryableStatusNames())
                .orderByAsc("id");
        if (!request.bizCodes().isEmpty()) {
            query.in("biz_code", request.bizCodes());
        }
        if (request.lastId() != null) {
            query.gt("id", request.lastId());
        }
        if (request.maxRetryNum() != null) {
            query.le("retry_num", request.maxRetryNum());
        }
        if (request.createdBefore() != null) {
            query.le("created_at", toStorageTime(request.createdBefore()));
        }
        if (request.executeBefore() != null) {
            query.and(wrapper -> wrapper.isNull("execute_time").or().le("execute_time", toStorageTime(request.executeBefore())));
        }
        if (request.limit() != null && request.limit() > 0) {
            query.last("limit " + request.limit());
        }
        return toRecords(mapper.selectList(query));
    }

    @Override
    public List<HandlerExecutionRecord> loadByEventKey(String eventKey) {
        QueryWrapper<DomainEventRecordEntity> query = new QueryWrapper<DomainEventRecordEntity>()
                .eq("event_key", eventKey)
                .orderByAsc("id");
        return toRecords(mapper.selectList(query));
    }

    private void insertRecord(HandlerExecutionRecord record) {
        DomainEventRecordEntity entity = DomainEventRecordConverter.toEntity(record);
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw duplicateRecordException(record, ex);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateRecordException(record, ex);
        } catch (RuntimeException ex) {
            if (isDuplicateKey(ex)) {
                throw duplicateRecordException(record, ex);
            }
            throw ex;
        }
        if (entity.getId() == null) {
            throw new IllegalStateException("Failed to generate record id for eventKey=" + record.eventKey());
        }
    }

    private IllegalStateException duplicateRecordException(HandlerExecutionRecord record, Exception ex) {
        return new IllegalStateException(
                "Duplicate handler record for eventKey=" + record.eventKey() + ", handlerCode=" + record.handlerCode(),
                ex
        );
    }

    private List<String> retryableStatusNames() {
        List<String> statusNames = new ArrayList<String>(RETRYABLE_STATUSES.size());
        for (ExecutionStatus status : RETRYABLE_STATUSES) {
            statusNames.add(status.name());
        }
        return statusNames;
    }

    private List<HandlerExecutionRecord> toRecords(List<DomainEventRecordEntity> entities) {
        List<HandlerExecutionRecord> records = new ArrayList<HandlerExecutionRecord>(entities.size());
        for (DomainEventRecordEntity entity : entities) {
            records.add(DomainEventRecordConverter.toRecord(entity));
        }
        return records;
    }

    private boolean isDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (
                    message.contains("uk_domain_event_record_event_handler")
                            || message.contains("Unique index or primary key violation")
                            || message.contains("Duplicate entry")
            )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private LocalDateTime toStorageTime(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
