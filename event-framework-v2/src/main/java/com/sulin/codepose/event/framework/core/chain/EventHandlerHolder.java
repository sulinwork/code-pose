package com.sulin.codepose.event.framework.core.chain;

import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.List;

public class EventHandlerHolder<E extends DomainEvent> {


    public List<DomainEventHandler<E>> getAllHandlers() {
        return null;
    }
}
