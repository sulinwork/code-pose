package com.sulin.codepose.event.framework.core.registry;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.*;

public class BasicEventHandlerChainRegistry implements EventHandlerChainRegistry {

    private final Map<String, EventHandlerChain<?>> chains;

    public BasicEventHandlerChainRegistry(String bizCode, Collection<EventHandlerChain<?>> chains) {
        Map<String, EventHandlerChain<?>> registered = new LinkedHashMap<>();
        if (chains != null) {
            for (EventHandlerChain<?> chain : chains) {
                if (!Objects.equals(bizCode, chain.bizCode())) {
                    continue;
                }
                if (registered.containsKey(chain.eventType())) {
                    throw new IllegalStateException("Duplicate event handler chain for key: " + chain.eventType());
                }
                registered.put(chain.eventType(), chain);
            }
        }
        this.chains = Collections.unmodifiableMap(registered);
    }

    @Override
    public <E extends DomainEvent> Optional<EventHandlerChain<E>> getChain(DomainEvent event) {
        return Optional.ofNullable((EventHandlerChain<E>) chains.get(event.getEventType()));
    }
}
