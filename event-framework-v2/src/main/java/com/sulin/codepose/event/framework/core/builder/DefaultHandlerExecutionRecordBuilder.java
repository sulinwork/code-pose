package com.sulin.codepose.event.framework.core.builder;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.handler.DelayableEventHandler;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.handler.GroupedEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.model.Payload;
import com.sulin.codepose.event.framework.api.serialize.EventPayloadSerializer;
import com.sulin.codepose.event.framework.core.router.RouterStrategyFactory;
import com.sulin.codepose.event.framework.core.serialize.JacksonEventPayloadSerializer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultHandlerExecutionRecordBuilder {

    private final RouterStrategyFactory routerStrategyFactory;

    private final EventPayloadSerializer eventPayloadSerializer = new JacksonEventPayloadSerializer();


    public DefaultHandlerExecutionRecordBuilder(RouterStrategyFactory routerStrategyFactory) {
        this.routerStrategyFactory = routerStrategyFactory;
    }

    public <E extends DomainEvent> List<HandlerExecutionRecord> build(E event) {
        Optional<EventHandlerChain<E>> chain = routerStrategyFactory.getStrategy(event.getBizCode()).getChain(event);
        if (!chain.isPresent()) {
            return Collections.emptyList();
        }
        List<HandlerExecutionRecord> records = new ArrayList<>();
        for (DomainEventHandler<E> handler : chain.get().handlers()) {
            appendRecord(event, handler, null, records);
        }
        return Collections.unmodifiableList(records);
    }

    private <E extends DomainEvent> void appendRecord(E event, DomainEventHandler<E> handler, String parentHandlerCode, List<HandlerExecutionRecord> records) {
        LocalDateTime executeTime = LocalDateTime.now();
        if (handler instanceof DelayableEventHandler) {
            executeTime = resolveExecuteTime(event, handler);
        }

        Map<String, Payload> payloadMap = Optional.ofNullable(event.getPayloads()).orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(e -> e.getClass().getName(), Function.identity()));

        Optional<Payload> payloadOpt = Optional.ofNullable(handler.requirePayload())
                .map(e -> payloadMap.get(e.getName()));

        records.add(
                new HandlerExecutionRecord()
                        .setEventKey(event.getEventKey())
                        .setBizCode(event.getBizCode())
                        .setBizId(event.getBizId())
                        .setEventType(event.getEventType())
                        .setEventContext(Optional.ofNullable(event.getEventContextMap()).map(eventPayloadSerializer::serialize).orElse(null))
                        .setHandlerCode(handler.handlerCode())
                        .setParentHandlerCode(parentHandlerCode)
                        .setPayload(payloadOpt.map(eventPayloadSerializer::serialize).orElse(null))
                        .setStatus(ExecutionStatus.PENDING)
                        .setRetryNum(0)
                        .setVersion(1L)
                        .setExecuteTime(executeTime)
        );
        if (handler instanceof GroupedEventHandler) {
            for (DomainEventHandler<E> subHandler : ((GroupedEventHandler<E>) handler).subHandlers()) {
                appendRecord(event, subHandler, handler.handlerCode(), records);
            }
        }
    }

    private <E extends DomainEvent> LocalDateTime resolveExecuteTime(E event, DomainEventHandler<E> handler) {
        return ((DelayableEventHandler<E>) handler).executeTime(event);
    }
}
