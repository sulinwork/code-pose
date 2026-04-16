package com.sulin.codepose.event.framework.api.handler;

import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.time.LocalDateTime;

public interface DelayableEventHandler<P> extends DomainEventHandler<P> {

    LocalDateTime executeTime(DomainEvent event, P payload);
}
