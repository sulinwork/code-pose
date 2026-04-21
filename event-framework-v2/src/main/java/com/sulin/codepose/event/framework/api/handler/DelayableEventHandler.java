package com.sulin.codepose.event.framework.api.handler;

import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.time.LocalDateTime;

public interface DelayableEventHandler<E extends DomainEvent> extends DomainEventHandler<E> {

    LocalDateTime executeTime(E event);
}
