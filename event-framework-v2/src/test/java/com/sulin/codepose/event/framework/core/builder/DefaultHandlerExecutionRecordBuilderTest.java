package com.sulin.codepose.event.framework.core.builder;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.handler.DelayableEventHandler;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.handler.GroupedEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.EventPayload;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.core.serialize.JacksonEventPayloadSerializer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultHandlerExecutionRecordBuilderTest {

    @Test
    void shouldBuildNormalDelayedAndGroupedRecords() {
        EventHandlerChain chain = new EventHandlerChain() {
            @Override
            public String bizCode() {
                return "biz";
            }

            @Override
            public String eventType() {
                return "created";
            }

            @Override
            public List<DomainEventHandler<?>> handlers() {
                return Arrays.<DomainEventHandler<?>>asList(
                        new SimpleHandler(),
                        new DelayedHandler(),
                        new GroupHandler()
                );
            }
        };
        DefaultHandlerExecutionRecordBuilder builder = new DefaultHandlerExecutionRecordBuilder(
                new SingleChainRegistry(chain),
                new JacksonEventPayloadSerializer()
        );

        List<HandlerExecutionRecord> records = builder.build(new TestEvent());

        assertEquals(4, records.size());
        assertEquals(ExecutionStatus.PENDING, records.get(0).status());
        assertNull(records.get(0).parentHandlerCode());
        assertEquals("group", records.get(3).parentHandlerCode());
        assertEquals(LocalDateTime.of(2026, 4, 15, 18, 0), records.get(1).executeTime());
    }

    private static final class SingleChainRegistry implements EventHandlerChainRegistry {

        private final EventHandlerChain chain;

        private SingleChainRegistry(EventHandlerChain chain) {
            this.chain = chain;
        }

        @Override
        public Optional<EventHandlerChain> getChain(String bizCode, String eventType) {
            return Optional.of(chain);
        }
    }

    private static final class TestEvent implements DomainEvent {

        @Override
        public String bizCode() {
            return "biz";
        }

        @Override
        public Long bizId() {
            return 1L;
        }

        @Override
        public String eventType() {
            return "created";
        }

        @Override
        public String eventKey() {
            return "biz_1_created_1";
        }

        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-04-15T10:00:00Z");
        }

        @Override
        public List<EventPayload> payloads() {
            return Collections.<EventPayload>singletonList(new TestPayload("payload"));
        }
    }

    private static final class TestPayload implements EventPayload {

        private String value;

        public TestPayload() {
        }

        private TestPayload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static class SimpleHandler implements DomainEventHandler<TestPayload> {

        @Override
        public String handlerCode() {
            return "simple";
        }

        @Override
        public Class<TestPayload> payloadClass() {
            return TestPayload.class;
        }

        @Override
        public Optional<TestPayload> buildPayload(DomainEvent event) {
            return Optional.of(new TestPayload("simple"));
        }

        @Override
        public EventHandleResult handle(DomainEvent event, TestPayload payload, HandlerExecutionRecord record, EventExecutionContext context) {
            return EventHandleResult.FINISHED;
        }
    }

    private static final class DelayedHandler extends SimpleHandler implements DelayableEventHandler<TestPayload> {

        @Override
        public String handlerCode() {
            return "delay";
        }

        @Override
        public LocalDateTime executeTime(DomainEvent event, TestPayload payload) {
            return LocalDateTime.of(2026, 4, 15, 18, 0);
        }
    }

    private static final class GroupHandler extends SimpleHandler implements GroupedEventHandler<TestPayload> {

        @Override
        public String handlerCode() {
            return "group";
        }

        @Override
        public List<DomainEventHandler<?>> subHandlers() {
            return Collections.<DomainEventHandler<?>>singletonList(new GroupSubHandler());
        }
    }

    private static final class GroupSubHandler extends SimpleHandler {

        @Override
        public String handlerCode() {
            return "group-sub";
        }
    }
}
