package com.sulin.codepose.event.framework.core.builder;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.handler.DelayableEventHandler;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.handler.GroupedEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.serialize.EventPayloadSerializer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DefaultHandlerExecutionRecordBuilder {

    private final EventHandlerChainRegistry chainRegistry;
    private final EventPayloadSerializer payloadSerializer;

    public DefaultHandlerExecutionRecordBuilder(
            EventHandlerChainRegistry chainRegistry,
            EventPayloadSerializer payloadSerializer) {
        this.chainRegistry = chainRegistry;
        this.payloadSerializer = payloadSerializer;
    }

    public List<HandlerExecutionRecord> build(DomainEvent event) {
        Optional<EventHandlerChain> chain = chainRegistry.getChain(event);
        if (!chain.isPresent()) {
            return Collections.emptyList();
        }
        List<HandlerExecutionRecord> records = new ArrayList<>();
        Instant createdAt = event.occurredAt() == null ? Instant.now() : event.occurredAt();
        for (DomainEventHandler<?> handler : chain.get().handlers()) {
            appendRecord(event, handler, null, createdAt, records);
        }
        return Collections.unmodifiableList(records);
    }

    private void appendRecord(
            DomainEvent event,
            DomainEventHandler<?> handler,
            String parentHandlerCode,
            Instant createdAt,
            List<HandlerExecutionRecord> records
    ) {
        Optional<?> payload = handler.buildPayload(event);
        if (!payload.isPresent()) {
            return;
        }
        LocalDateTime executeTime = null;
        if (handler instanceof DelayableEventHandler) {
            executeTime = resolveExecuteTime(event, handler, payload.get());
        }
        records.add(new HandlerExecutionRecord(
                null,
                event.eventKey(),
                event.bizCode(),
                event.bizId(),
                event.eventType(),
                handler.handlerCode(),
                parentHandlerCode,
                payloadSerializer.serialize(payload.get()),
                ExecutionStatus.PENDING,
                0,
                executeTime,
                0L,
                createdAt,
                createdAt
        ));
        if (handler instanceof GroupedEventHandler) {
            for (DomainEventHandler<?> subHandler : ((GroupedEventHandler<?>) handler).subHandlers()) {
                appendRecord(event, subHandler, handler.handlerCode(), createdAt, records);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <P> LocalDateTime resolveExecuteTime(DomainEvent event, DomainEventHandler<?> handler, Object payload) {
        return ((DelayableEventHandler<P>) handler).executeTime(event, (P) payload);
    }
}
