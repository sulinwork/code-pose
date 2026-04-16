package com.sulin.codepose.event.framework.spring;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.EventPayload;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.publish.DomainEventPublisher;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.core.builder.DefaultHandlerExecutionRecordBuilder;
import com.sulin.codepose.event.framework.spring.config.DomainEventFrameworkAutoConfiguration;
import com.sulin.codepose.event.framework.support.store.InMemoryEventStore;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryPipelineIntegrationTest {

    @Test
    void shouldAppendAndProcessEventThroughSpringListener() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TestConfiguration.class, DomainEventFrameworkAutoConfiguration.class);
        context.refresh();
        try {
            DefaultHandlerExecutionRecordBuilder recordBuilder = context.getBean(DefaultHandlerExecutionRecordBuilder.class);
            EventStore eventStore = context.getBean(EventStore.class);
            DomainEventPublisher publisher = context.getBean(DomainEventPublisher.class);
            CountingHandler handler = context.getBean(CountingHandler.class);

            OrderPaidEvent event = new OrderPaidEvent();
            List<HandlerExecutionRecord> records = recordBuilder.build(event);
            eventStore.append(event, records);

            PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
            TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
            publisher.publishAfterCommit(event);
            transactionManager.commit(transaction);

            assertEquals(1, handler.invocationCount.get());
            List<HandlerExecutionRecord> persistedRecords = eventStore.loadByEventKey(event.eventKey());
            assertEquals(1, persistedRecords.size());
            assertEquals(ExecutionStatus.FINISHED, persistedRecords.get(0).status());
        } finally {
            context.close();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        EventStore eventStore() {
            return new InMemoryEventStore();
        }

        @Bean
        EventHandlerChain countingChain(CountingHandler handler) {
            return new EventHandlerChain() {
                @Override
                public String bizCode() {
                    return "order";
                }

                @Override
                public String eventType() {
                    return "paid";
                }

                @Override
                public List<DomainEventHandler<?>> handlers() {
                    return Collections.<DomainEventHandler<?>>singletonList(handler);
                }
            };
        }

        @Bean
        CountingHandler countingHandler() {
            return new CountingHandler();
        }

        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return new NoOpTransactionManager();
        }
    }

    static class CountingHandler implements DomainEventHandler<TestPayload> {

        private final AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public String handlerCode() {
            return "counting";
        }

        @Override
        public Class<TestPayload> payloadClass() {
            return TestPayload.class;
        }

        @Override
        public Optional<TestPayload> buildPayload(DomainEvent event) {
            return Optional.of(new TestPayload("payload"));
        }

        @Override
        public EventHandleResult handle(
                DomainEvent event,
                TestPayload payload,
                HandlerExecutionRecord record,
                EventExecutionContext context) {
            invocationCount.incrementAndGet();
            return EventHandleResult.FINISHED;
        }
    }

    static class OrderPaidEvent implements DomainEvent {

        @Override
        public String bizCode() {
            return "order";
        }

        @Override
        public Long bizId() {
            return 1L;
        }

        @Override
        public String eventType() {
            return "paid";
        }

        @Override
        public String eventKey() {
            return "order_1_paid";
        }

        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-04-15T10:00:00Z");
        }

        @Override
        public List<EventPayload> payloads() {
            return Collections.singletonList(new TestPayload("payload"));
        }
    }

    static class TestPayload implements EventPayload {

        private String value;

        public TestPayload() {
        }

        TestPayload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    static class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
