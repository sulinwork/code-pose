package com.sulin.codepose.event.framework.core.chain;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.List;

public abstract class AbstractEventHandlerChain<E extends DomainEvent> implements EventHandlerChain<E> {

    private final EventHandlerHolder<E> holder;

    public AbstractEventHandlerChain() {
        holder = new EventHandlerHolder<>();
    }

    public List<DomainEventHandler<E>> handlers() {
        return holder.getAllHandlers();
    }
}
