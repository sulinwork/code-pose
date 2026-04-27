package com.sulin.codepose.event.framework.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.chain.EventHandlerChainRegistry;
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;
import com.sulin.codepose.event.framework.api.serialize.EventPayloadSerializer;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.core.builder.DefaultHandlerExecutionRecordBuilder;
import com.sulin.codepose.event.framework.core.chain.DefaultEventProcessor;
import com.sulin.codepose.event.framework.core.policy.DefaultRetryPolicy;
import com.sulin.codepose.event.framework.core.registry.BasicEventHandlerChainRegistry;
import com.sulin.codepose.event.framework.core.replay.DefaultEventReplayCoordinator;
import com.sulin.codepose.event.framework.core.router.RouterStrategyFactory;
import com.sulin.codepose.event.framework.core.scheduler.DefaultReplayScanner;
import com.sulin.codepose.event.framework.core.serialize.JacksonEventPayloadSerializer;
import com.sulin.codepose.event.framework.core.store.EventRecordStateMachine;
import com.sulin.codepose.event.framework.spring.listener.SpringDomainEventListener;
import com.sulin.codepose.event.framework.spring.publish.SpringTransactionAwareEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(DomainEventMybatisPlusStoreAutoConfiguration.class)
public class DomainEventFrameworkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringTransactionAwareEventPublisher domainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringTransactionAwareEventPublisher(applicationEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventPayloadSerializer eventPayloadSerializer(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        if (objectMapper != null) {
            return new JacksonEventPayloadSerializer(objectMapper);
        }
        return new JacksonEventPayloadSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryPolicy retryPolicy() {
        return new DefaultRetryPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventRecordStateMachine eventRecordStateMachine() {
        return new EventRecordStateMachine();
    }


    @Bean
    @ConditionalOnMissingBean
    public DefaultHandlerExecutionRecordBuilder defaultHandlerExecutionRecordBuilder(RouterStrategyFactory routerStrategyFactory) {
        return new DefaultHandlerExecutionRecordBuilder(routerStrategyFactory);
    }

    @Bean
    @ConditionalOnBean(EventStore.class)
    @ConditionalOnMissingBean
    public DefaultReplayScanner defaultReplayScanner(EventStore eventStore) {
        return new DefaultReplayScanner(eventStore);
    }

    @Bean
    @ConditionalOnBean(EventStore.class)
    @ConditionalOnMissingBean
    public DefaultEventProcessor defaultEventProcessor(RouterStrategyFactory routerStrategyFactory, EventStore eventStore, RetryPolicy retryPolicy, EventRecordStateMachine stateMachine) {
        return new DefaultEventProcessor(routerStrategyFactory, eventStore, retryPolicy, stateMachine);
    }

    @Bean
    @ConditionalOnBean({EventStore.class, DefaultReplayScanner.class, DefaultEventProcessor.class})
    @ConditionalOnMissingBean
    public DefaultEventReplayCoordinator defaultEventReplayCoordinator(DefaultReplayScanner replayScanner,
                                                                       EventStore eventStore,
                                                                       DefaultEventProcessor eventProcessor,
                                                                       RouterStrategyFactory routerStrategyFactory) {
        return new DefaultEventReplayCoordinator(replayScanner, eventStore, eventProcessor,routerStrategyFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public RouterStrategyFactory routerStrategyFactory(){
        return new RouterStrategyFactory();
    }

    @Bean
    @ConditionalOnBean(DefaultEventProcessor.class)
    @ConditionalOnMissingBean
    public SpringDomainEventListener springDomainEventListener(DefaultEventProcessor eventProcessor, ObjectProvider<TaskExecutor> taskExecutorProvider) {
        return new SpringDomainEventListener(eventProcessor, taskExecutorProvider.getIfAvailable());
    }
}
