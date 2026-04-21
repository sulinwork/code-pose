package com.sulin.codepose.event.framework.api.chain;

import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.List;

public interface EventHandlerChain<E extends DomainEvent> {

    String bizCode();

    String eventType();

    List<DomainEventHandler<E>> handlers();
}
