package com.sulin.codepose.event.framework.support.store;

import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryEventStore implements EventStore {

    private final AtomicLong recordIdSequence = new AtomicLong(1L);
    private final Map<Long, HandlerExecutionRecord> recordsById = new LinkedHashMap<Long, HandlerExecutionRecord>();
    private final Map<String, List<Long>> recordIdsByEventKey = new LinkedHashMap<String, List<Long>>();
    private final Map<String, Set<String>> handlerCodesByEventKey = new LinkedHashMap<String, Set<String>>();

    @Override
    public synchronized void append(DomainEvent event, List<HandlerExecutionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> eventRecordIds = recordIdsByEventKey.computeIfAbsent(event.eventKey(), key -> new ArrayList<Long>());
        Set<String> handlerCodes = handlerCodesByEventKey.computeIfAbsent(event.eventKey(), key -> new LinkedHashSet<String>());
        for (HandlerExecutionRecord record : records) {
            if (!Objects.equals(event.eventKey(), record.eventKey())) {
                throw new IllegalArgumentException("Record eventKey does not match appended event");
            }
            if (!handlerCodes.add(record.handlerCode())) {
                throw new IllegalStateException("Duplicate handler record for eventKey="
                        + event.eventKey()
                        + ", handlerCode="
                        + record.handlerCode());
            }
            HandlerExecutionRecord persistedRecord = record.id() == null
                    ? record.withId(recordIdSequence.getAndIncrement())
                    : record;
            recordsById.put(persistedRecord.id(), persistedRecord);
            eventRecordIds.add(persistedRecord.id());
        }
    }

    @Override
    public synchronized boolean compareAndSet(
            Long recordId,
            Long expectedVersion,
            ExecutionStatus expectedStatus,
            HandlerExecutionRecord nextRecord
    ) {
        if (recordId == null || nextRecord == null) {
            return false;
        }
        HandlerExecutionRecord currentRecord = recordsById.get(recordId);
        if (currentRecord == null) {
            return false;
        }
        if (!Objects.equals(currentRecord.version(), expectedVersion) || currentRecord.status() != expectedStatus) {
            return false;
        }
        HandlerExecutionRecord persistedRecord = nextRecord.id() == null ? nextRecord.withId(recordId) : nextRecord;
        recordsById.put(recordId, persistedRecord);
        return true;
    }

    @Override
    public synchronized List<HandlerExecutionRecord> scanRetryable(ReplayScanRequest request) {
        List<HandlerExecutionRecord> matched = new ArrayList<HandlerExecutionRecord>();
        for (HandlerExecutionRecord record : recordsById.values()) {
            if (!isRetryableStatus(record.status())) {
                continue;
            }
            if (!matchesBizCodes(record, request.bizCodes())) {
                continue;
            }
            if (request.lastId() != null && record.id() != null && record.id() <= request.lastId()) {
                continue;
            }
            if (request.maxRetryNum() != null && record.retryNum() != null && record.retryNum() > request.maxRetryNum()) {
                continue;
            }
            if (!matchesCreatedBefore(record, request.createdBefore())) {
                continue;
            }
            if (!matchesExecuteBefore(record, request.executeBefore())) {
                continue;
            }
            matched.add(record);
            if (request.limit() != null && request.limit() > 0 && matched.size() >= request.limit()) {
                break;
            }
        }
        return Collections.unmodifiableList(matched);
    }

    @Override
    public synchronized List<HandlerExecutionRecord> loadByEventKey(String eventKey) {
        List<Long> recordIds = recordIdsByEventKey.get(eventKey);
        if (recordIds == null || recordIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<HandlerExecutionRecord> records = new ArrayList<HandlerExecutionRecord>(recordIds.size());
        for (Long recordId : recordIds) {
            HandlerExecutionRecord record = recordsById.get(recordId);
            if (record != null) {
                records.add(record);
            }
        }
        return Collections.unmodifiableList(records);
    }

    private boolean isRetryableStatus(ExecutionStatus status) {
        return status == ExecutionStatus.PENDING
                || status == ExecutionStatus.PROCESSING
                || status == ExecutionStatus.GROUP_MAIN_FINISHED
                || status == ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT;
    }

    private boolean matchesBizCodes(HandlerExecutionRecord record, List<String> bizCodes) {
        return bizCodes == null || bizCodes.isEmpty() || bizCodes.contains(record.bizCode());
    }

    private boolean matchesCreatedBefore(HandlerExecutionRecord record, Instant createdBefore) {
        return createdBefore == null
                || record.createdAt() == null
                || !record.createdAt().isAfter(createdBefore);
    }

    private boolean matchesExecuteBefore(HandlerExecutionRecord record, Instant executeBefore) {
        if (executeBefore == null || record.executeTime() == null) {
            return true;
        }
        LocalDateTime threshold = LocalDateTime.ofInstant(executeBefore, ZoneId.systemDefault());
        return !record.executeTime().isAfter(threshold);
    }
}
