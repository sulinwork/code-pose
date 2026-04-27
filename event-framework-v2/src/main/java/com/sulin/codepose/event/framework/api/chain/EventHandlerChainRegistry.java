package com.sulin.codepose.event.framework.api.chain;

import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.Optional;

public interface EventHandlerChainRegistry {

    <E extends DomainEvent> Optional<EventHandlerChain<E>> getChain(DomainEvent event);
}
