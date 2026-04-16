package com.sulin.codepose.event.framework.api.handler;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;

import java.util.Optional;

public interface DomainEventHandler<P> {

    String handlerCode();

    Class<P> payloadClass();

    Optional<P> buildPayload(DomainEvent event);

    EventHandleResult handle(
            DomainEvent event,
            P payload,
            HandlerExecutionRecord record,
            EventExecutionContext context
    );

    default String parentHandlerCode() {
        return null;
    }
}
