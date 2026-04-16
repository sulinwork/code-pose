package com.sulin.codepose.event.framework.core.registry;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryEventHandlerChainRegistryTest {

    @Test
    void shouldReturnRegisteredChain() {
        InMemoryEventHandlerChainRegistry registry = new InMemoryEventHandlerChainRegistry(
                Collections.<EventHandlerChain>singletonList(new SimpleChain("biz", "created"))
        );

        assertTrue(registry.getChain("biz", "created").isPresent());
        assertFalse(registry.getChain("biz", "missing").isPresent());
    }

    @Test
    void shouldRejectDuplicateChainKey() {
        assertThrows(IllegalStateException.class, () -> new InMemoryEventHandlerChainRegistry(Arrays.<EventHandlerChain>asList(
                new SimpleChain("biz", "created"),
                new SimpleChain("biz", "created")
        )));
    }

    private static final class SimpleChain implements EventHandlerChain {

        private final String bizCode;
        private final String eventType;

        private SimpleChain(String bizCode, String eventType) {
            this.bizCode = bizCode;
            this.eventType = eventType;
        }

        @Override
        public String bizCode() {
            return bizCode;
        }

        @Override
        public String eventType() {
            return eventType;
        }

        @Override
        public List<DomainEventHandler<?>> handlers() {
            return Collections.emptyList();
        }
    }
}
