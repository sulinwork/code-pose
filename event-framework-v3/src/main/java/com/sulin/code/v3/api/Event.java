package com.sulin.code.v3.api;


import com.sulin.code.v3.api.chain.EventHandlerChain;
import com.sulin.code.v3.api.chain.EventHandlerChainFactory;
import com.sulin.code.v3.api.handler.EventHandler;
import com.sulin.code.v3.api.repository.EventRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Data
@Slf4j
public class Event {
    private Long id;

    /**
     * 业务事件的唯一标识
     */
    private String aggregateId;

    /**
     * 事件归属业务源 比如：ORDER AFTER_SALE 等等
     */
    private String source;

    /**
     * 事件类型
     */
    private String type;

    /**
     * 事件上下文
     */
    private String context;


    private List<EventHandlerInfo> handlers;

    @Resource
    private EventHandlerChainFactory eventHandlerChainFactory;
    @Resource
    private EventRepository eventRepository;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    public <T extends Event> EventHandlerInfo getEventHandlerInfo(EventHandler<T> eventHandler) {
        if (this.handlers == null) {
            return null;
        }
        return handlers.stream().filter(e -> Objects.equals(e.getName(), eventHandler.getClass().getName())).findFirst().orElse(null);
    }

    public void publish(List<String> handlerContexts) {
        EventHandlerChain<Event> chain = eventHandlerChainFactory.getChain(this);
        List<EventHandler<Event>> allEventHandlers = chain.getAllEventHandlers();
        //todo build to List<EventHandlerInfo> and handler context
        //this.handlers = xxxx
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("Synchronization is not active");
        }
        eventRepository.addEvent(this);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applicationEventPublisher.publishEvent(this);
                }
            });
        } else {
            applicationEventPublisher.publishEvent(this);
        }

    }
}
