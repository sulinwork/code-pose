package com.sulin.codepose.event.framework.core.builder;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.handler.DelayableEventHandler;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.handler.GroupedEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DefaultHandlerExecutionRecordBuilder {

    private final EventHandlerChainRegistry chainRegistry;

    public DefaultHandlerExecutionRecordBuilder(EventHandlerChainRegistry chainRegistry ) {
        this.chainRegistry = chainRegistry;
    }

    public <E extends DomainEvent> List<HandlerExecutionRecord> build(E event) {
        Optional<EventHandlerChain<E>> chain = chainRegistry.getChain(event);
        if (!chain.isPresent()) {
            return Collections.emptyList();
        }
        List<HandlerExecutionRecord> records = new ArrayList<>();
        Instant createdAt =  Instant.now();
        for (DomainEventHandler<E> handler : chain.get().handlers()) {
            appendRecord(event, handler, null, createdAt, records);
        }
        return Collections.unmodifiableList(records);
    }

    private <E extends DomainEvent> void appendRecord(E event, DomainEventHandler<E> handler, String parentHandlerCode, Instant createdAt, List<HandlerExecutionRecord> records) {
        LocalDateTime executeTime = null;
        if (handler instanceof DelayableEventHandler) {
            executeTime = resolveExecuteTime(event, handler);
        }
        //todo
        records.add(new HandlerExecutionRecord(null, event.getEventKey(), event.getBizCode(), event.getBizId(), event.getEventType(), handler.handlerCode(), parentHandlerCode, null, ExecutionStatus.PENDING, 0, executeTime, 0L, createdAt, createdAt));
        if (handler instanceof GroupedEventHandler) {
            for (DomainEventHandler<E> subHandler : ((GroupedEventHandler<E>) handler).subHandlers()) {
                appendRecord(event, subHandler, handler.handlerCode(), createdAt, records);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends DomainEvent> LocalDateTime resolveExecuteTime(E event, DomainEventHandler<E> handler) {
        return ((DelayableEventHandler<E>) handler).executeTime(event);
    }
}
