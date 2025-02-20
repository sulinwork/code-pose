package com.sulin.codepose.event;

import com.sulin.codepose.event.chain.EventHandlerChain;
import com.sulin.codepose.event.chain.EventHandlerChainFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;

/**
 * @author harry
 */
@Component
@Slf4j
public class EventListener {

    @Resource
    private EventHandlerChainFactory eventHandlerChainFactory;


    @org.springframework.context.event.EventListener
    @Async("eventExecutor")
    public void handleOrderEvent(Event event) {
        log.info("事件：{}，监听处理", event.getEventType());
        eventChainHandle(event);
    }


    private void eventChainHandle(Event event) {
        if (Objects.isNull(event)) {
            throw new RuntimeException(this.getClass().getSimpleName() + "事件不能为空");
        }
        EventHandlerChain<Event> handlerChain = eventHandlerChainFactory.getChain(event);
        Optional.ofNullable(handlerChain).ifPresent(c -> {
            log.info("获取EventHandlerChain, bizCode: {},bizId:{}, eventType: {}, chain: {}, handlers: {}", event.getBizCode(), event.getBizId(), event.getEventType(), handlerChain.getClass().getSimpleName(), handlerChain.getAllEventHandlers());
            c.handle(event);
        });
    }
}
