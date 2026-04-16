package com.sulin.codepose.event.framework.api.chain;

import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;

import java.util.List;

public interface EventHandlerChain {

    String bizCode();

    String eventType();

    List<DomainEventHandler<?>> handlers();
}
