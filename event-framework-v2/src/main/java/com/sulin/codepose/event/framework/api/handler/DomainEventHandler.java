package com.sulin.codepose.event.framework.api.handler;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;

import java.util.Optional;

public interface DomainEventHandler<E extends DomainEvent> {

    String handlerCode();

    EventHandleResult handle(E event, HandlerExecutionRecord record, EventExecutionContext context);

    default String parentHandlerCode() {
        return null;
    }
}
