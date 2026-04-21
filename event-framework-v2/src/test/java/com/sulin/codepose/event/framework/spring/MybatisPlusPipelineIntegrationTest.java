//package com.sulin.codepose.event.framework.spring;
//
//import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
//import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
//import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
//import com.sulin.codepose.event.framework.api.handler.DomainEventHandler;
//import com.sulin.codepose.event.framework.api.model.DomainEvent;
//import com.sulin.codepose.event.framework.api.model.EventHandleResult;
//import com.sulin.codepose.event.framework.api.model.EventPayload;
//import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
//import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
//import com.sulin.codepose.event.framework.api.publish.DomainEventPublisher;
//import com.sulin.codepose.event.framework.api.store.EventStore;
//import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;
//import com.sulin.codepose.event.framework.core.builder.DefaultHandlerExecutionRecordBuilder;
//import com.sulin.codepose.event.framework.core.replay.DefaultEventReplayCoordinator;
//import com.sulin.codepose.event.framework.spring.config.DomainEventFrameworkAutoConfiguration;
//import com.sulin.codepose.event.framework.spring.config.DomainEventMybatisPlusStoreAutoConfiguration;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.context.annotation.AnnotationConfigApplicationContext;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//import org.springframework.transaction.support.TransactionSynchronization;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
//import org.springframework.util.StreamUtils;
//
//import javax.sql.DataSource;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.time.Instant;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//class MybatisPlusPipelineIntegrationTest {
//
//    @BeforeEach
//    void resetCounter() {
//        TestConfiguration.COUNT.set(0);
//    }
//
//    @Test
//    void shouldAppendAndProcessEventThroughMybatisPlusStore() throws Exception {
//        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//        context.register(TestConfiguration.class, MybatisPlusAutoConfiguration.class, DomainEventMybatisPlusStoreAutoConfiguration.class, DomainEventFrameworkAutoConfiguration.class);
//        context.refresh();
//        try {
//            executeSchema(context.getBean(DataSource.class));
//            DefaultHandlerExecutionRecordBuilder recordBuilder = context.getBean(DefaultHandlerExecutionRecordBuilder.class);
//            EventStore eventStore = context.getBean(EventStore.class);
//            DomainEventPublisher publisher = context.getBean(DomainEventPublisher.class);
//
//            OrderPaidEvent event = new OrderPaidEvent("order_1_paid");
//            List<HandlerExecutionRecord> records = recordBuilder.build(event);
//            eventStore.append(event, records);
//
//            publishAfterCommit(publisher, event);
//
//            assertEquals(1, TestConfiguration.COUNT.get());
//            List<HandlerExecutionRecord> persistedRecords = eventStore.loadByEventKey(event.eventKey());
//            assertEquals(1, persistedRecords.size());
//            assertEquals(ExecutionStatus.FINISHED, persistedRecords.get(0).getStatus());
//        } finally {
//            context.close();
//        }
//    }
//
//    @Test
//    void shouldReplayPendingRecordFromMybatisPlusStore() throws Exception {
//        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
//        context.register(TestConfiguration.class, MybatisPlusAutoConfiguration.class, DomainEventMybatisPlusStoreAutoConfiguration.class, DomainEventFrameworkAutoConfiguration.class);
//        context.refresh();
//        try {
//            executeSchema(context.getBean(DataSource.class));
//            DefaultHandlerExecutionRecordBuilder recordBuilder = context.getBean(DefaultHandlerExecutionRecordBuilder.class);
//            EventStore eventStore = context.getBean(EventStore.class);
//            DefaultEventReplayCoordinator coordinator = context.getBean(DefaultEventReplayCoordinator.class);
//
//            OrderPaidEvent event = new OrderPaidEvent("order_1_paid_replay");
//            List<HandlerExecutionRecord> records = recordBuilder.build(event);
//            eventStore.append(event, records);
//
//            coordinator.replay(new ReplayScanRequest(
//                    Collections.singletonList("order"),
//                    null,
//                    10,
//                    3,
//                    Instant.parse("2026-04-16T11:00:00Z"),
//                    Instant.parse("2026-04-16T11:00:00Z")
//            ));
//
//            assertEquals(1, TestConfiguration.COUNT.get());
//            assertEquals(ExecutionStatus.FINISHED, eventStore.loadByEventKey(event.eventKey()).get(0).getStatus());
//        } finally {
//            context.close();
//        }
//    }
//
//    private void publishAfterCommit(DomainEventPublisher publisher, DomainEvent event) {
//        TransactionSynchronizationManager.initSynchronization();
//        TransactionSynchronizationManager.setActualTransactionActive(true);
//        try {
//            publisher.publishAfterCommit(event);
//            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
//                synchronization.afterCommit();
//            }
//        } finally {
//            TransactionSynchronizationManager.clearSynchronization();
//            TransactionSynchronizationManager.setActualTransactionActive(false);
//        }
//    }
//
//    private void executeSchema(DataSource dataSource) throws Exception {
//        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
//        jdbcTemplate.execute("drop table if exists domain_event_record");
//        ClassPathResource resource = new ClassPathResource("sql/domain_event_record_h2.sql");
//        String sql;
//        InputStream inputStream = resource.getInputStream();
//        try {
//            sql = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
//        } finally {
//            inputStream.close();
//        }
//        for (String statement : sql.split(";")) {
//            String trimmed = statement.trim();
//            if (!trimmed.isEmpty()) {
//                jdbcTemplate.execute(trimmed);
//            }
//        }
//    }
//
//    @Configuration(proxyBeanMethods = false)
//    static class TestConfiguration {
//
//        private static final AtomicInteger COUNT = new AtomicInteger();
//
//        @Bean
//        DataSource dataSource() {
//            return new DriverManagerDataSource(
//                    "jdbc:h2:mem:mybatis-plus-pipeline;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
//                    "sa",
//                    ""
//            );
//        }
//
//        @Bean
//        EventHandlerChain countingChain(CountingHandler handler) {
//            return new EventHandlerChain() {
//                @Override
//                public String bizCode() {
//                    return "order";
//                }
//
//                @Override
//                public String eventType() {
//                    return "paid";
//                }
//
//                @Override
//                public List<DomainEventHandler<DomainEvent,?>> handlers() {
//                    return Collections.singletonList(handler);
//                }
//            };
//        }
//
//        @Bean
//        CountingHandler countingHandler() {
//            return new CountingHandler();
//        }
//    }
//
//    static class CountingHandler implements DomainEventHandler<DomainEvent, TestPayload> {
//
//        @Override
//        public String handlerCode() {
//            return "counting";
//        }
//
//        @Override
//        public Class<TestPayload> payloadClass() {
//            return TestPayload.class;
//        }
//
//        @Override
//        public Optional<TestPayload> buildPayload(DomainEvent event) {
//            return Optional.of(new TestPayload("payload"));
//        }
//
//        @Override
//        public EventHandleResult handle(
//                DomainEvent event,
//                TestPayload payload,
//                HandlerExecutionRecord record,
//                EventExecutionContext context) {
//            TestConfiguration.COUNT.incrementAndGet();
//            return EventHandleResult.FINISHED;
//        }
//    }
//
//    static class OrderPaidEvent implements DomainEvent {
//
//        private final String eventKey;
//
//        OrderPaidEvent(String eventKey) {
//            this.eventKey = eventKey;
//        }
//
//        @Override
//        public String bizCode() {
//            return "order";
//        }
//
//        @Override
//        public Long bizId() {
//            return 1L;
//        }
//
//        @Override
//        public String eventType() {
//            return "paid";
//        }
//
//        @Override
//        public String eventKey() {
//            return eventKey;
//        }
//
//        @Override
//        public Instant occurredAt() {
//            return Instant.parse("2026-04-16T10:00:00Z");
//        }
//
//        @Override
//        public List<EventPayload> payloads() {
//            return Collections.singletonList(new TestPayload("payload"));
//        }
//    }
//
//    static class TestPayload implements EventPayload {
//
//        private String value;
//
//        public TestPayload() {
//        }
//
//        TestPayload(String value) {
//            this.value = value;
//        }
//
//        public String getValue() {
//            return value;
//        }
//    }
//}
