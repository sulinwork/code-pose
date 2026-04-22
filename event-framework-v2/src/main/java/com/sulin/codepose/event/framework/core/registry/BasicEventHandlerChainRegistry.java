package com.sulin.codepose.event.framework.core.registry;

import com.sulin.codepose.event.framework.api.chain.BizCodeEventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.model.DomainEvent;

import java.util.*;

public class BasicEventHandlerChainRegistry implements EventHandlerChainRegistry {

    private final Map<String, EventHandlerChain<?>> chains;

    private final Map<String, EventHandlerChainRegistry> registryRouter = new HashMap<>();

    public BasicEventHandlerChainRegistry(Collection<EventHandlerChain<?>> chains) {
        Map<String, EventHandlerChain<?>> registered = new LinkedHashMap<>();
        if (chains != null) {
            for (EventHandlerChain<?> chain : chains) {
                if (chain instanceof BizCodeEventHandlerChainRegistry) {
                    BizCodeEventHandlerChainRegistry registry = (BizCodeEventHandlerChainRegistry) chain;
                    registryRouter.put(registry.getBizCode(), registry);
                    continue;
                }
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
    public <E extends DomainEvent> Optional<EventHandlerChain<E>> getChain(String bizCode, String eventType) {
        if(registryRouter.containsKey(bizCode)){
            return registryRouter.get(bizCode).getChain(bizCode,eventType);
        }
        return Optional.ofNullable((EventHandlerChain<E>) chains.get(key(bizCode, eventType)));
    }

    static String key(String bizCode, String eventType) {
        return Objects.requireNonNull(bizCode, "bizCode must not be null")
                + "::"
                + Objects.requireNonNull(eventType, "eventType must not be null");
    }
}
