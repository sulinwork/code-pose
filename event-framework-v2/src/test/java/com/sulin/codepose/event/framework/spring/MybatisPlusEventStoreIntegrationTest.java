package com.sulin.codepose.event.framework.spring;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;
import com.sulin.codepose.event.framework.spring.store.mybatis.DomainEventRecordMapper;
import com.sulin.codepose.event.framework.spring.store.mybatis.MybatisPlusEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MybatisPlusEventStoreIntegrationTest {

    private JdbcTemplate jdbcTemplate;
    private MybatisPlusEventStore eventStore;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:mybatis-plus-event-store;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("drop table if exists domain_event_record");
        executeSchema(jdbcTemplate);
        org.apache.ibatis.session.SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(dataSource);
        eventStore = new MybatisPlusEventStore(sqlSessionFactory.openSession(true).getMapper(DomainEventRecordMapper.class));
    }

    @Test
    void shouldAppendAndLoadByEventKey() {
        TestEvent event = new TestEvent("order_1_paid");
        List<HandlerExecutionRecord> records = Arrays.asList(
                record(null, event.eventKey(), "main", ExecutionStatus.PENDING, 0, null, 0L),
                record(null, event.eventKey(), "sub", ExecutionStatus.PENDING, 0, null, 0L)
        );

        eventStore.append(event, records);

        List<HandlerExecutionRecord> loaded = eventStore.loadByEventKey(event.eventKey());
        assertEquals(2, loaded.size());
        assertTrue(loaded.get(0).id() < loaded.get(1).id());
        assertEquals("main", loaded.get(0).handlerCode());
        assertEquals("sub", loaded.get(1).handlerCode());
    }

    @Test
    void shouldRejectDuplicateHandlerPerEventKey() {
        TestEvent event = new TestEvent("order_1_paid_dup");
        List<HandlerExecutionRecord> records = Arrays.asList(
                record(null, event.eventKey(), "main", ExecutionStatus.PENDING, 0, null, 0L),
                record(null, event.eventKey(), "main", ExecutionStatus.PENDING, 0, null, 0L)
        );

        assertThrows(IllegalStateException.class, () -> eventStore.append(event, records));
    }

    @Test
    void shouldCompareAndSetByIdVersionAndStatus() {
        TestEvent event = new TestEvent("order_1_paid_cas");
        eventStore.append(event, Collections.singletonList(record(null, event.eventKey(), "main", ExecutionStatus.PENDING, 0, null, 0L)));
        HandlerExecutionRecord current = eventStore.loadByEventKey(event.eventKey()).get(0);
        HandlerExecutionRecord processing = current.withState(
                ExecutionStatus.PROCESSING,
                current.retryNum(),
                current.executeTime(),
                current.version() + 1,
                Instant.parse("2026-04-16T10:10:00Z")
        );

        assertTrue(eventStore.compareAndSet(current.id(), current.version(), current.status(), processing));
        assertFalse(eventStore.compareAndSet(current.id(), current.version(), current.status(), processing));
        assertEquals(ExecutionStatus.PROCESSING, eventStore.loadByEventKey(event.eventKey()).get(0).status());
    }

    @Test
    void shouldScanRetryableWithFilters() {
        Instant now = Instant.parse("2026-04-16T10:00:00Z");
        eventStore.append(new TestEvent("event_1"), Collections.singletonList(record(null, "event_1", "main", ExecutionStatus.PENDING, 0, null, 0L)));
        eventStore.append(new TestEvent("event_2"), Collections.singletonList(record(null, "event_2", "main", ExecutionStatus.PROCESSING, 1, LocalDateTime.of(2026, 4, 16, 9, 0), 0L)));
        eventStore.append(new TestEvent("event_3"), Collections.singletonList(record(null, "event_3", "main", ExecutionStatus.ABORT, 0, null, 0L)));
        eventStore.append(new TestEvent("event_4"), Collections.singletonList(record(null, "event_4", "main", ExecutionStatus.PENDING, 5, null, 0L)));

        List<HandlerExecutionRecord> scanned = eventStore.scanRetryable(new ReplayScanRequest(
                Collections.singletonList("order"),
                null,
                10,
                2,
                now.plusSeconds(1),
                now.plusSeconds(1)
        ));

        assertEquals(2, scanned.size());
        assertEquals("event_1", scanned.get(0).eventKey());
        assertEquals("event_2", scanned.get(1).eventKey());
    }

    private org.apache.ibatis.session.SqlSessionFactory buildSqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(DomainEventRecordMapper.class);
        com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean factoryBean = new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    private void executeSchema(JdbcTemplate template) throws Exception {
        ClassPathResource resource = new ClassPathResource("sql/domain_event_record_h2.sql");
        String sql;
        InputStream inputStream = resource.getInputStream();
        try {
            sql = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } finally {
            inputStream.close();
        }
        for (String statement : sql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                template.execute(trimmed);
            }
        }
    }

    private HandlerExecutionRecord record(
            Long id,
            String eventKey,
            String handlerCode,
            ExecutionStatus status,
            int retryNum,
            LocalDateTime executeTime,
            long version
    ) {
        Instant createdAt = Instant.parse("2026-04-16T10:00:00Z");
        return new HandlerExecutionRecord(
                id,
                eventKey,
                "order",
                1L,
                "paid",
                handlerCode,
                null,
                "{}",
                1,
                status,
                retryNum,
                executeTime,
                version,
                createdAt,
                createdAt
        );
    }

    static class TestEvent implements DomainEvent {

        private final String eventKey;

        TestEvent(String eventKey) {
            this.eventKey = eventKey;
        }

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
            return eventKey;
        }

        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-04-16T10:00:00Z");
        }

        @Override
        public List<com.sulin.codepose.event.framework.api.model.EventPayload> payloads() {
            return Collections.emptyList();
        }
    }
}
