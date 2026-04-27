package com.sulin.codepose.event.framework.core.chain;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.handler.GroupedEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.core.router.RouterStrategyFactory;
import com.sulin.codepose.event.framework.core.store.EventRecordStateMachine;
import com.sulin.codepose.event.framework.util.AssetUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultEventProcessor {

    private final RouterStrategyFactory routerStrategyFactory;
    private final EventStore eventStore;
    private final RetryPolicy retryPolicy;
    private final EventRecordStateMachine stateMachine;

    public DefaultEventProcessor(RouterStrategyFactory routerStrategyFactory, EventStore eventStore, RetryPolicy retryPolicy, EventRecordStateMachine stateMachine) {
        this.routerStrategyFactory = routerStrategyFactory;
        this.eventStore = eventStore;
        this.retryPolicy = retryPolicy;
        this.stateMachine = stateMachine;
    }

    public void process(DomainEvent event) {
        List<HandlerExecutionRecord> records = event.getRecords();
        if (records == null || records.isEmpty()) {
            records = eventStore.loadByEventKey(event.getEventKey());
        }
        process(event, records);
    }

    public <E extends DomainEvent> void process(E event, List<HandlerExecutionRecord> records) {
        List<HandlerExecutionRecord> safeRecords = records == null ? Collections.emptyList() : records;
        if (safeRecords.isEmpty()) {
            return;
        }
        Optional<EventHandlerChain<E>> chain = routerStrategyFactory.getStrategy(event.getBizCode()).getChain(event);
        if (!chain.isPresent()) {
            return;
        }
        Map<String, HandlerExecutionRecord> recordsByHandlerCode = indexByHandlerCode(safeRecords);
        EventExecutionContext context = new DefaultEventExecutionContext();
        for (DomainEventHandler<E> handler : chain.get().handlers()) {
            processHandler(handler, event, recordsByHandlerCode, context);
        }
    }

    private <E extends DomainEvent> void processHandler(DomainEventHandler<E> handler, E event, Map<String, HandlerExecutionRecord> recordsByHandlerCode, EventExecutionContext context) {
        HandlerExecutionRecord record = recordsByHandlerCode.get(handler.handlerCode());
        if (record == null) {
            return;
        }
        if (handler instanceof GroupedEventHandler) {
            processGroupedHandler((GroupedEventHandler<E>) handler, event, record, recordsByHandlerCode, context);
            return;
        }
        processSingleHandler(handler, event, record, recordsByHandlerCode, context);
    }

    private <E extends DomainEvent> void processSingleHandler(DomainEventHandler<E> handler, E event, HandlerExecutionRecord record, Map<String, HandlerExecutionRecord> recordsByHandlerCode, EventExecutionContext context) {
        executeHandler(handler, event, record, recordsByHandlerCode, context);
    }

    private <E extends DomainEvent> void processGroupedHandler(GroupedEventHandler<E> handler, E event, HandlerExecutionRecord record, Map<String, HandlerExecutionRecord> recordsByHandlerCode, EventExecutionContext context) {
        if (record.getStatus() == ExecutionStatus.GROUP_MAIN_FINISHED) {
            runSubHandlers(handler, event, record, recordsByHandlerCode, context);
            return;
        }
        if (record.getStatus() == ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT || record.getStatus() == ExecutionStatus.FINISHED || record.getStatus() == ExecutionStatus.ABORT) {
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

    private <E extends DomainEvent> void runSubHandlers(GroupedEventHandler<E> handler, E event, HandlerExecutionRecord parentRecord, Map<String, HandlerExecutionRecord> recordsByHandlerCode, EventExecutionContext context) {
        for (DomainEventHandler<E> subHandler : handler.subHandlers()) {
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
                persistTransition(nextParent);
                return;
            }
            if (subResult == EventHandleResult.FAIL) {
                return;
            }
        }
    }

    private <E extends DomainEvent> EventHandleResult executeHandler(DomainEventHandler<E> handler, E event, HandlerExecutionRecord record, Map<String, HandlerExecutionRecord> recordsByHandlerCode, EventExecutionContext context) {
        //是否是可以运行的状态
        if (!stateMachine.isRunnable(record)) {
            return null;
        }
        //时间是否满足
        if (stateMachine.isFutureExecution(record, LocalDateTime.now())) {
            return null;
        }
        //更新为处理中
        moveToProcessing(record);

        try {
            //执行
            EventHandleResult result = invokeHandler(handler, event, record, context);
            //获取下一个状态
            HandlerExecutionRecord nextRecord = stateMachine.afterHandleResult(record, result, retryPolicy);
            persistTransition(nextRecord);
            context.putResult(handler.handlerCode(), result);
            return result;
        } catch (RuntimeException ex) {
            HandlerExecutionRecord nextRecord = stateMachine.afterHandleException(record, retryPolicy);
            persistTransition(nextRecord);
            context.putResult(handler.handlerCode(), EventHandleResult.FAIL);
            return EventHandleResult.FAIL;
        }
    }

    private void moveToProcessing(HandlerExecutionRecord record) {
        if (record.getStatus() != ExecutionStatus.PENDING) {
            return;
        }
        stateMachine.toProcessing(record);
        boolean updated = eventStore.update4VersionCas(record);
        AssetUtil.isTrue(updated, "event move to processing failed!" + record);
    }

    private void persistTransition(HandlerExecutionRecord nextRecord) {
        eventStore.update4VersionCas(nextRecord);
    }

    private <E extends DomainEvent> EventHandleResult invokeHandler(DomainEventHandler<E> handler, E event, HandlerExecutionRecord record, EventExecutionContext context) {
        return handler.handle(event, record, context);
    }

    private Map<String, HandlerExecutionRecord> indexByHandlerCode(List<HandlerExecutionRecord> records) {
        Map<String, HandlerExecutionRecord> indexed = new LinkedHashMap<>();
        for (HandlerExecutionRecord record : records) {
            indexed.put(record.getHandlerCode(), record);
        }
        return indexed;
    }

}
