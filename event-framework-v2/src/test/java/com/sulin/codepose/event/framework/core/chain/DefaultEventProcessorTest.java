package com.sulin.codepose.event.framework.core.chain;

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
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.core.store.EventRecordStateMachine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultEventProcessorTest {

    @Test
    void shouldProcessSuccessAndDelayedSkip() {
        RecordingStore store = new RecordingStore();
        DefaultEventProcessor processor = new DefaultEventProcessor(
                new SingleChainRegistry(new TestChain(new SuccessHandler(), new DelayedHandler())),
                new SimplePayloadSerializer(),
                store,
                retryPolicy(3),
                new EventRecordStateMachine()
        );

        processor.process(new TestEvent(records(
                record(1L, "success", ExecutionStatus.PENDING, 0, null),
                record(2L, "delay", ExecutionStatus.PENDING, 0, LocalDateTime.now().plusHours(1))
        )));

        assertEquals(2, store.transitions.size());
        assertEquals(ExecutionStatus.FINISHED, store.transitions.get(1).getStatus());
    }

    @Test
    void shouldRetryOnFailure() {
        RecordingStore store = new RecordingStore();
        DefaultEventProcessor processor = new DefaultEventProcessor(
                new SingleChainRegistry(new TestChain(new FailHandler())),
                new SimplePayloadSerializer(),
                store,
                retryPolicy(3),
                new EventRecordStateMachine()
        );

        processor.process(new TestEvent(records(record(1L, "fail", ExecutionStatus.PENDING, 0, null))));

        assertEquals(2, store.transitions.size());
        assertEquals(ExecutionStatus.PENDING, store.transitions.get(1).getStatus());
        assertEquals(Integer.valueOf(1), store.transitions.get(1).getRetryNum());
    }

    @Test
    void shouldAbortGroupedParentWhenSubHandlerAborts() {
        RecordingStore store = new RecordingStore();
        DefaultEventProcessor processor = new DefaultEventProcessor(
                new SingleChainRegistry(new TestChain(new GroupHandler())),
                new SimplePayloadSerializer(),
                store,
                retryPolicy(3),
                new EventRecordStateMachine()
        );

        processor.process(new TestEvent(records(
                record(1L, "group", ExecutionStatus.PENDING, 0, null),
                record(2L, "group-sub", ExecutionStatus.PENDING, 0, null)
        )));

        assertEquals(5, store.transitions.size());
        assertEquals(ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT, store.transitions.get(4).getStatus());
    }

    private RetryPolicy retryPolicy(final int maxRetryCount) {
        return new RetryPolicy() {
            @Override
            public int maxRetryCount(String bizCode, String eventType, String handlerCode) {
                return maxRetryCount;
            }

            @Override
            public LocalDateTime nextExecuteTime(HandlerExecutionRecord record) {
                return LocalDateTime.now().plusMinutes(1);
            }
        };
    }

    private List<HandlerExecutionRecord> records(HandlerExecutionRecord... records) {
        return Arrays.asList(records);
    }

    private HandlerExecutionRecord record(Long id, String handlerCode, ExecutionStatus status, int retryNum, LocalDateTime executeTime) {
        return new HandlerExecutionRecord(
                id,
                "biz_1_created_1",
                "biz",
                1L,
                "created",
                handlerCode,
                handlerCode.equals("group-sub") ? "group" : null,
                "{}",
                status,
                retryNum,
                executeTime,
                0L,
                Instant.now(),
                Instant.now()
        );
    }

    private static final class TestEvent implements DomainEvent {

        private final List<HandlerExecutionRecord> records;

        private TestEvent(List<HandlerExecutionRecord> records) {
            this.records = records;
        }

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
            return Instant.now();
        }

        @Override
        public List<EventPayload> payloads() {
            return Collections.<EventPayload>emptyList();
        }

        @Override
        public List<HandlerExecutionRecord> records() {
            return records;
        }
    }

    private static final class TestPayload {

        public TestPayload() {
        }
    }

    private static final class TestChain implements EventHandlerChain {

        private final List<DomainEventHandler<?>> handlers;

        private TestChain(DomainEventHandler<?>... handlers) {
            this.handlers = Arrays.<DomainEventHandler<?>>asList(handlers);
        }

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
            return handlers;
        }
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

    private static final class RecordingStore implements EventStore {

        private final List<HandlerExecutionRecord> transitions = new ArrayList<HandlerExecutionRecord>();

        @Override
        public void append(DomainEvent event, List<HandlerExecutionRecord> records) {
        }

        @Override
        public boolean compareAndSet(Long recordId, Long expectedVersion, ExecutionStatus expectedStatus, HandlerExecutionRecord nextRecord) {
            transitions.add(nextRecord);
            return true;
        }

        @Override
        public List<HandlerExecutionRecord> scanRetryable(com.sulin.codepose.event.framework.api.store.ReplayScanRequest request) {
            return Collections.emptyList();
        }

        @Override
        public List<HandlerExecutionRecord> loadByEventKey(String eventKey) {
            return Collections.emptyList();
        }
    }

    private static final class SimplePayloadSerializer implements com.sulin.codepose.event.framework.api.serialize.EventPayloadSerializer {

        @Override
        public <T> String serialize(T payload) {
            return "{}";
        }

        @Override
        public <T> T deserialize(String content, Class<T> payloadClass) {
            try {
                return payloadClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private abstract static class BaseHandler implements DomainEventHandler<TestPayload> {

        private final String handlerCode;

        private BaseHandler(String handlerCode) {
            this.handlerCode = handlerCode;
        }

        @Override
        public String handlerCode() {
            return handlerCode;
        }

        @Override
        public Class<TestPayload> payloadClass() {
            return TestPayload.class;
        }

        @Override
        public Optional<TestPayload> buildPayload(DomainEvent event) {
            return Optional.of(new TestPayload());
        }
    }

    private static final class SuccessHandler extends BaseHandler {

        private SuccessHandler() {
            super("success");
        }

        @Override
        public EventHandleResult handle(DomainEvent event, TestPayload payload, HandlerExecutionRecord record, EventExecutionContext context) {
            return EventHandleResult.FINISHED;
        }
    }

    private static final class FailHandler extends BaseHandler {

        private FailHandler() {
            super("fail");
        }

        @Override
        public EventHandleResult handle(DomainEvent event, TestPayload payload, HandlerExecutionRecord record, EventExecutionContext context) {
            return EventHandleResult.FAIL;
        }
    }

    private static final class DelayedHandler extends BaseHandler implements DelayableEventHandler<TestPayload> {

        private DelayedHandler() {
            super("delay");
        }

        @Override
        public EventHandleResult handle(DomainEvent event, TestPayload payload, HandlerExecutionRecord record, EventExecutionContext context) {
            return EventHandleResult.FINISHED;
        }

        @Override
        public LocalDateTime executeTime(DomainEvent event, TestPayload payload) {
            return LocalDateTime.now().plusHours(1);
        }
    }

    private static final class GroupHandler extends BaseHandler implements GroupedEventHandler<TestPayload> {

        private GroupHandler() {
            super("group");
        }

        @Override
        public EventHandleResult handle(DomainEvent event, TestPayload payload, HandlerExecutionRecord record, EventExecutionContext context) {
            return EventHandleResult.GROUP_MAIN_FINISHED;
        }

        @Override
        public List<DomainEventHandler<?>> subHandlers() {
            return Collections.<DomainEventHandler<?>>singletonList(new GroupSubHandler());
        }
    }

    private static final class GroupSubHandler extends BaseHandler {

        private GroupSubHandler() {
            super("group-sub");
        }

        @Override
        public EventHandleResult handle(DomainEvent event, TestPayload payload, HandlerExecutionRecord record, EventExecutionContext context) {
            return EventHandleResult.ABORT;
        }
    }
}
