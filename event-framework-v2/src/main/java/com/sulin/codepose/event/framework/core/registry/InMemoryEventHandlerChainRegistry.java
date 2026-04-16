package com.sulin.codepose.event.framework.core.registry;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryEventHandlerChainRegistry implements EventHandlerChainRegistry {

    private final Map<String, EventHandlerChain> chains;

    public InMemoryEventHandlerChainRegistry(Collection<EventHandlerChain> chains) {
        Map<String, EventHandlerChain> registered = new LinkedHashMap<String, EventHandlerChain>();
        if (chains != null) {
            for (EventHandlerChain chain : chains) {
                String key = key(chain.bizCode(), chain.eventType());
                if (registered.containsKey(key)) {
                    throw new IllegalStateException("Duplicate event handler chain for key: " + key);
                }
                registered.put(key, chain);
            }
        }
        this.chains = Collections.unmodifiableMap(registered);
    }

    @Override
    public Optional<EventHandlerChain> getChain(String bizCode, String eventType) {
        return Optional.ofNullable(chains.get(key(bizCode, eventType)));
    }

    static String key(String bizCode, String eventType) {
        return Objects.requireNonNull(bizCode, "bizCode must not be null")
                + "::"
                + Objects.requireNonNull(eventType, "eventType must not be null");
    }
}
