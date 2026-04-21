//package com.sulin.codepose.event.framework.spring.config;
//
//import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
//import com.sulin.codepose.event.framework.api.store.EventStore;
//import com.sulin.codepose.event.framework.core.chain.DefaultEventProcessor;
//import com.sulin.codepose.event.framework.spring.store.mybatis.DomainEventRecordMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.autoconfigure.AutoConfigurations;
//import org.springframework.boot.test.context.runner.ApplicationContextRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//import org.springframework.util.StreamUtils;
//
//import javax.sql.DataSource;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//class DomainEventMybatisPlusStoreAutoConfigurationTest {
//
//    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
//            .withConfiguration(AutoConfigurations.of(
//                    MybatisPlusAutoConfiguration.class,
//                    DomainEventMybatisPlusStoreAutoConfiguration.class,
//                    DomainEventFrameworkAutoConfiguration.class
//            ));
//
//    @Test
//    void shouldCreateDefaultEventStoreWhenMybatisPlusAndDataSourcePresent() {
//        contextRunner
//                .withUserConfiguration(DataSourceConfiguration.class)
//                .run(context -> {
//                    assertTrue(context.containsBean("mybatisPlusEventStore"));
//                    assertFalse(context.getBeansOfType(DomainEventRecordMapper.class).isEmpty());
//                    assertTrue(context.getBean(EventStore.class) != null);
//                    assertTrue(context.getBean(DefaultEventProcessor.class) != null);
//                });
//    }
//
//    @Test
//    void shouldBackOffWhenCustomEventStoreProvided() {
//        contextRunner
//                .withUserConfiguration(DataSourceConfiguration.class, CustomEventStoreConfiguration.class)
//                .run(context -> {
//                    assertFalse(context.containsBean("mybatisPlusEventStore"));
//                    assertTrue(context.getBean(EventStore.class) != null);
//                });
//    }
//
//    @Test
//    void shouldNotCreateProcessorWithoutEventStore() {
//        new ApplicationContextRunner()
//                .withConfiguration(AutoConfigurations.of(DomainEventFrameworkAutoConfiguration.class))
//                .run(context -> {
//                    assertFalse(context.containsBean("defaultEventProcessor"));
//                    assertFalse(context.containsBean("defaultReplayScanner"));
//                });
//    }
//
//    @Configuration(proxyBeanMethods = false)
//    static class DataSourceConfiguration {
//
//        @Bean
//        DataSource dataSource() throws Exception {
//            DataSource dataSource = new DriverManagerDataSource(
//                    "jdbc:h2:mem:auto-config;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
//                    "sa",
//                    ""
//            );
//            initializeSchema(dataSource);
//            return dataSource;
//        }
//
//        private void initializeSchema(DataSource dataSource) throws Exception {
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//            jdbcTemplate.execute("drop table if exists domain_event_record");
//            ClassPathResource resource = new ClassPathResource("sql/domain_event_record_h2.sql");
//            String sql;
//            InputStream inputStream = resource.getInputStream();
//            try {
//                sql = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
//            } finally {
//                inputStream.close();
//            }
//            for (String statement : sql.split(";")) {
//                String trimmed = statement.trim();
//                if (!trimmed.isEmpty()) {
//                    jdbcTemplate.execute(trimmed);
//                }
//            }
//        }
//    }
//
//    @Configuration(proxyBeanMethods = false)
//    static class CustomEventStoreConfiguration {
//
//        @Bean
//        EventStore customEventStore() {
//            return new EventStore() {
//                @Override
//                public void append(com.sulin.codepose.event.framework.api.model.DomainEvent event, java.util.List<com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord> records) {
//                }
//
//                @Override
//                public boolean compareAndSet(Long recordId, Long expectedVersion, com.sulin.codepose.event.framework.api.model.ExecutionStatus expectedStatus, com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord nextRecord) {
//                    return false;
//                }
//
//                @Override
//                public java.util.List<com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord> scanRetryable(com.sulin.codepose.event.framework.api.store.ReplayScanRequest request) {
//                    return java.util.Collections.emptyList();
//                }
//
//                @Override
//                public java.util.List<com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord> loadByEventKey(String eventKey) {
//                    return java.util.Collections.emptyList();
//                }
//            };
//        }
//    }
//}
