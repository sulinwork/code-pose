package com.sulin.codepose.event.framework.core.chain;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.handler.GroupedEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;
import com.sulin.codepose.event.framework.api.serialize.EventPayloadSerializer;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.core.store.EventRecordStateMachine;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultEventProcessor {

    private final EventHandlerChainRegistry chainRegistry;
    private final EventPayloadSerializer payloadSerializer;
    private final EventStore eventStore;
    private final RetryPolicy retryPolicy;
    private final EventRecordStateMachine stateMachine;

    public DefaultEventProcessor(
            EventHandlerChainRegistry chainRegistry,
            EventPayloadSerializer payloadSerializer,
            EventStore eventStore,
            RetryPolicy retryPolicy,
            EventRecordStateMachine stateMachine
    ) {
        this.chainRegistry = chainRegistry;
        this.payloadSerializer = payloadSerializer;
        this.eventStore = eventStore;
        this.retryPolicy = retryPolicy;
        this.stateMachine = stateMachine;
    }

    public void process(DomainEvent event) {
        List<HandlerExecutionRecord> records = event.records();
        if (records == null || records.isEmpty()) {
            records = eventStore.loadByEventKey(event.eventKey());
        }
        process(event, records);
    }

    public void process(DomainEvent event, List<HandlerExecutionRecord> records) {
        List<HandlerExecutionRecord> safeRecords = records == null ? Collections.emptyList() : records;
        if (safeRecords.isEmpty()) {
            return;
        }
        Optional<EventHandlerChain> chain = chainRegistry.getChain(event);
        if (!chain.isPresent()) {
            return;
        }
        Map<String, HandlerExecutionRecord> recordsByHandlerCode = indexByHandlerCode(safeRecords);
        EventExecutionContext context = new DefaultEventExecutionContext();
        for (DomainEventHandler<?> handler : chain.get().handlers()) {
            processHandler(handler, event, recordsByHandlerCode, context);
        }
    }

    private void processHandler(
            DomainEventHandler<?> handler,
            DomainEvent event,
            Map<String, HandlerExecutionRecord> recordsByHandlerCode,
            EventExecutionContext context
    ) {
        HandlerExecutionRecord record = recordsByHandlerCode.get(handler.handlerCode());
        if (record == null) {
            return;
        }
        if (handler instanceof GroupedEventHandler) {
            processGroupedHandler((GroupedEventHandler<?>) handler, event, record, recordsByHandlerCode, context);
            return;
        }
        processSingleHandler(handler, event, record, recordsByHandlerCode, context);
    }

    private void processSingleHandler(
            DomainEventHandler<?> handler,
            DomainEvent event,
            HandlerExecutionRecord record,
            Map<String, HandlerExecutionRecord> recordsByHandlerCode,
            EventExecutionContext context
    ) {
        executeHandler(handler, event, record, recordsByHandlerCode, context);
    }

    private void processGroupedHandler(
            GroupedEventHandler<?> handler,
            DomainEvent event,
            HandlerExecutionRecord record,
            Map<String, HandlerExecutionRecord> recordsByHandlerCode,
            EventExecutionContext context
    ) {
        if (record.status() == ExecutionStatus.GROUP_MAIN_FINISHED) {
            runSubHandlers(handler, event, record, recordsByHandlerCode, context);
            return;
        }
        if (record.status() == ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT
                || record.status() == ExecutionStatus.FINISHED
                || record.status() == ExecutionStatus.ABORT) {
            return;
        }
        EventHandleResult result = executeHandler(handler, event, record, recordsByHandlerCode, context);
        if (result != null && result.shouldContinueSubHandlers()) {
            HandlerExecutionRecord nextRecord = recordsByHandlerCode.get(handler.handlerCode());
            if (nextRecord != null) {
                runSubHandlers(handler, event, nextRecord, recordsByHandlerCode, context);
            }
        }
    }

    private void runSubHandlers(
            GroupedEventHandler<?> handler,
            DomainEvent event,
            HandlerExecutionRecord parentRecord,
            Map<String, HandlerExecutionRecord> recordsByHandlerCode,
            EventExecutionContext context
    ) {
        for (DomainEventHandler<?> subHandler : handler.subHandlers()) {
            HandlerExecutionRecord subRecord = recordsByHandlerCode.get(subHandler.handlerCode());
            if (subRecord == null) {
                continue;
            }
            EventHandleResult subResult = executeHandler(subHandler, event, subRecord, recordsByHandlerCode, context);
            if (subResult == null) {
                continue;
            }
            if (subResult == EventHandleResult.ABORT || subResult == EventHandleResult.GROUP_MAIN_FINISHED_SUB_ABORT) {
                HandlerExecutionRecord nextParent = stateMachine.afterGroupedSubHandlerAbort(parentRecord);
                persistTransition(parentRecord, nextParent);
                recordsByHandlerCode.put(parentRecord.handlerCode(), nextParent);
                return;
            }
            if (subResult == EventHandleResult.FAIL) {
                return;
            }
        }
    }

    private EventHandleResult executeHandler(
            DomainEventHandler<?> handler,
            DomainEvent event,
            HandlerExecutionRecord record,
            Map<String, HandlerExecutionRecord> recordsByHandlerCode,
            EventExecutionContext context
    ) {
        if (!stateMachine.isRunnable(record)) {
            return null;
        }
        if (stateMachine.isFutureExecution(record, LocalDateTime.now())) {
            return null;
        }
        HandlerExecutionRecord currentRecord = moveToProcessing(record);
        if (currentRecord == null) {
            return null;
        }
        recordsByHandlerCode.put(handler.handlerCode(), currentRecord);
        try {
            EventHandleResult result = invokeHandler(handler, event, currentRecord, context);
            HandlerExecutionRecord nextRecord = stateMachine.afterHandleResult(currentRecord, result, retryPolicy);
            persistTransition(currentRecord, nextRecord);
            recordsByHandlerCode.put(handler.handlerCode(), nextRecord);
            context.putResult(handler.handlerCode(), result);
            return result;
        } catch (RuntimeException ex) {
            HandlerExecutionRecord nextRecord = stateMachine.afterHandleException(currentRecord, retryPolicy);
            persistTransition(currentRecord, nextRecord);
            recordsByHandlerCode.put(handler.handlerCode(), nextRecord);
            context.putResult(handler.handlerCode(), EventHandleResult.FAIL);
            return EventHandleResult.FAIL;
        }
    }

    private HandlerExecutionRecord moveToProcessing(HandlerExecutionRecord record) {
        if (record.status() != ExecutionStatus.PENDING) {
            return record;
        }
        HandlerExecutionRecord processingRecord = stateMachine.toProcessing(record);
        boolean updated = eventStore.compareAndSet(
                record.id(),
                record.version(),
                record.status(),
                processingRecord
        );
        return updated ? processingRecord : null;
    }

    private void persistTransition(HandlerExecutionRecord currentRecord, HandlerExecutionRecord nextRecord) {
        eventStore.compareAndSet(
                currentRecord.id(),
                currentRecord.version(),
                currentRecord.status(),
                nextRecord
        );
    }

    @SuppressWarnings("unchecked")
    private <P> EventHandleResult invokeHandler(
            DomainEventHandler<?> handler,
            DomainEvent event,
            HandlerExecutionRecord record,
            EventExecutionContext context
    ) {
        DomainEventHandler<P> typedHandler = (DomainEventHandler<P>) handler;
        P payload = payloadSerializer.deserialize(record.payload(), typedHandler.payloadClass(), record.payloadVersion());
        return typedHandler.handle(event, payload, record, context);
    }

    private Map<String, HandlerExecutionRecord> indexByHandlerCode(List<HandlerExecutionRecord> records) {
        Map<String, HandlerExecutionRecord> indexed = new LinkedHashMap<String, HandlerExecutionRecord>();
        for (HandlerExecutionRecord record : records) {
            indexed.put(record.handlerCode(), record);
        }
        return indexed;
    }

}
