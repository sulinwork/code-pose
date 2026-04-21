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
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

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
            List<HandlerExecutionRecord> persistedRecords = eventStore.loadByEventKey(event.getEventKey());
            assertEquals(1, persistedRecords.size());
            assertEquals(ExecutionStatus.FINISHED, persistedRecords.get(0).getStatus());
        } finally {
            context.close();
        }
    }

    @Data
    static class OrderPaidEvent implements DomainEvent {
        private String bizCode;
        private Long bizId;
        private String eventType;
        private String eventKey;
    }

    @Data
    static class TestPayload implements EventPayload {
        private String value;

    }


    static class OrderPaidNotifyHandler implements DomainEventHandler<OrderPaidEvent> {
        @Override
        public String handlerCode() {
            return "counting";
        }

        @Override
        public EventHandleResult handle(OrderPaidEvent event, HandlerExecutionRecord record, EventExecutionContext context) {
            return EventHandleResult.FINISHED;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        EventStore eventStore() {
            return null;
        }

        @Bean
        OrderPaidNotifyHandler countingHandler() {
            return new OrderPaidNotifyHandler();
        }

        @Bean
        EventHandlerChain<OrderPaidEvent> countingChain() {
            return new EventHandlerChain<OrderPaidEvent>() {
                @Override
                public String bizCode() {
                    return "order";
                }

                @Override
                public String eventType() {
                    return "paid";
                }

                @Override
                public List<DomainEventHandler<OrderPaidEvent>> handlers() {

                }
            };
        }



        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return new NoOpTransactionManager();
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
