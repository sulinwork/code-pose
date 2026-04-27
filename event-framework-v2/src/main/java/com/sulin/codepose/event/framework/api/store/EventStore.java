package com.sulin.codepose.event.framework.api.store;

import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;

import java.util.List;

public interface EventStore {

    void append(DomainEvent event, List<HandlerExecutionRecord> records);


    boolean update4VersionCas(HandlerExecutionRecord record);

    List<HandlerExecutionRecord> scanRetryable(ReplayScanRequest request);

    List<HandlerExecutionRecord> loadByEventKey(String eventKey);
}
