package com.sulin.codepose.event.framework.api.handler;

import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.List;

public interface GroupedEventHandler<E extends DomainEvent> extends DomainEventHandler<E> {

    List<DomainEventHandler<E>> subHandlers();
}
