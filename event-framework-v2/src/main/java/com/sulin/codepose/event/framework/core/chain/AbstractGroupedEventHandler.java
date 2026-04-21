package com.sulin.codepose.event.framework.core.chain;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.handler.GroupedEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;

import java.util.List;

public abstract class AbstractGroupedEventHandler<E extends DomainEvent> implements GroupedEventHandler<E> {

    @Override
    public final EventHandleResult handle(E event,HandlerExecutionRecord record, EventExecutionContext context) {
        return handleMain(event, record, context);
    }

    protected abstract EventHandleResult handleMain(E event,  HandlerExecutionRecord record, EventExecutionContext context);

    @Override
    public abstract List<DomainEventHandler<E>> subHandlers();
}
