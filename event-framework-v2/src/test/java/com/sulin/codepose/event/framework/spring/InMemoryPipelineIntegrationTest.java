package com.sulin.codepose.event.framework.spring;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
import com.sulin.codepose.event.framework.api.model.*;
import com.sulin.codepose.event.framework.api.publish.DomainEventPublisher;
import com.sulin.codepose.event.framework.api.router.RouterStrategy;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.core.builder.DefaultHandlerExecutionRecordBuilder;
import com.sulin.codepose.event.framework.core.registry.BasicEventHandlerChainRegistry;
import com.sulin.codepose.event.framework.spring.config.DomainEventFrameworkAutoConfiguration;
import com.sulin.codepose.event.framework.util.MapUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            OrderPaidNotifyHandler handler = context.getBean(OrderPaidNotifyHandler.class);

            OrderPaidEvent event = new OrderPaidEvent();//业务构建
            event.addPayload(new TestHandlerPayload().setValue("demo payload"));

            List<HandlerExecutionRecord> records = recordBuilder.build(event);
            eventStore.append(event, records);

            PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
            TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
            publisher.publishAfterCommit(event);
            transactionManager.commit(transaction);

            List<HandlerExecutionRecord> persistedRecords = eventStore.loadByEventKey(event.getEventKey());
            assertEquals(1, persistedRecords.size());
            assertEquals(ExecutionStatus.FINISHED, persistedRecords.get(0).getStatus());
        } finally {
            context.close();
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    static class OrderPaidEvent extends AbstractDomainEvent {
        //特殊新增参数
        private String orderType;

        @Override
        public Map<String, Object> getEventContextMap() {
            return MapUtil.of("orderType", orderType);
        }

        @Override
        public void resetEventContext(Map<String, Object> context) {
            this.orderType = context.get("orderType").toString();
        }
    }

    @Data
    @Accessors(chain = true)
    static class TestHandlerPayload implements Payload {
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

        @Override
        public Class<? extends Payload> requirePayload() {
            return TestHandlerPayload.class;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        RouterStrategy routerStrategy(Collection<EventHandlerChain<?>> chains){
            return new RouterStrategy() {

                private final EventHandlerChainRegistry registry = new BasicEventHandlerChainRegistry(bizCode(),chains);

                @Override
                public String bizCode() {
                    return "order";
                }

                @Override
                public <E extends DomainEvent> Optional<EventHandlerChain<E>> getChain(DomainEvent event) {
                    return registry.getChain(event);
                }

                @Override
                public DomainEvent buildDomainEvent(HandlerExecutionRecord record) {
                    if(record.getEventType().equals("paid")){
                        return new OrderPaidEvent();
                    }
                    return null;
                }
            };
        }


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
                    return null;
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
