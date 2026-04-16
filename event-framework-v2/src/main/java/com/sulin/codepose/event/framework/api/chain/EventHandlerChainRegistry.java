package com.sulin.codepose.event.framework.api.chain;

import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.Optional;

public interface EventHandlerChainRegistry {

    Optional<EventHandlerChain> getChain(String bizCode, String eventType);

    default Optional<EventHandlerChain> getChain(DomainEvent event) {
        return getChain(event.bizCode(), event.eventType());
    }
}
