package com.sulin.codepose.event.chain;



import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.EventHandlerInfo;
import com.sulin.codepose.event.enums.EventHandleResult;
import com.sulin.codepose.event.handler.EventGroupHandler;
import com.sulin.codepose.event.handler.EventHandler;
import com.sulin.codepose.event.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 事件处理链抽象类
 */
@Slf4j
@Component
public abstract class AbstractEventHandlerChain<T extends Event> implements EventHandlerChain<T>, InitializingBean {

    @Resource
    private EventRepository eventRepository;

    // 事件处理者（不包括分组子处理者）
    protected List<EventHandler<T>> eventHandlers = new ArrayList<>();

    // 事件处理者（包括分组子处理者）
    protected List<EventHandler<T>> allEventHandlers = new ArrayList<>();

    @Override
    public void handle(T event) {
        final EventChainContext context = buildContext();

        // 遍历所有事件处理类（不包括分组子处理者）并进行处理
        for (EventHandler<T> eventHandler : getEventHandlers()) {
            EventHandlerInfo handlerInfo;
            // 获取当前事件处理类信息
            handlerInfo = event.getEventHandlerInfo(eventHandler);
            if (handlerInfo == null) {
                // 事件重试时会出现该情况（因为只把没处理的处理者查出来执行）
                continue;
            }
            try {
                // 根据处理状态决定是否执行事件处理
                if (handlerInfo.isNeedHandle() && handlerInfo.arrayExecuteTime()) {
                    log.info("Handler {} handling, bizId: {}", eventHandler.getClass().getSimpleName(), event.getBizId());
                    // 执行事件处理
                    EventHandleResult result = eventHandler.handle(event, handlerInfo, context);
                    context.addHandleResult(handlerInfo.getEventHandler(), result);
                    handlerInfo.updateByHandleResult(result);
                    // 更新本地事件状态
                    eventRepository.updateEventHandleStatus(event, handlerInfo);
                }
            } catch (Exception e) {
                log.error("Handler {} exception, bizId: {}", eventHandler.getClass().getSimpleName(), event.getBizId(), e);
                // 更新本地事件异常状态
                handlerInfo.updateByHandleException();
                eventRepository.updateEventHandleStatus(event, handlerInfo);
            }
        }
    }


    private String retryNumToMaxKey(Event event, EventHandlerInfo handlerInfo) {
        return event.getBizId() + "-" + event.getEventType() + "-" + handlerInfo.getEventHandler();
    }

    /**
     * 系统启动时执行，初始化事件处理者
     */
    @Override
    public void afterPropertiesSet() {
        EventHandlerHolder<T> holder = new EventHandlerHolder<>();
//        if (TemplateInitFlag.USE_TEMPLATE == templateInitFlag()) {
//            // 通过模板初始化处理链
//            holder = initHandlersByTemplate();
//        } else {
//            holder = new OrderEventHandlerHolder<>();
//        }
        // 增加自定义处理者
        appendHandlers(holder);
        // 设置allOrderEventHandlerList
        setAllHandlers(holder);
    }


    /**
     * 为处理链增加处理者，用于子类增加自定义处理者
     */
    protected void appendHandlers(EventHandlerHolder<T> sources) {
    }

    private void setAllHandlers(EventHandlerHolder<T> sources) {
        eventHandlers.addAll(sources.getEventHandlers());
        allEventHandlers.addAll(sources.getEventHandlers());
        setGroupSubHandlerToAllHandlers(sources.getEventHandlers());
    }

    private void setGroupSubHandlerToAllHandlers(List<EventHandler<T>> handlers) {
        for (EventHandler<T> eventHandler : handlers) {
            if (eventHandler instanceof EventGroupHandler) {
                List<EventHandler<T>> subHandlers = ((EventGroupHandler<T>) eventHandler).getSubEventHandlers();
                if (!CollectionUtils.isEmpty(subHandlers)) {
                    allEventHandlers.addAll(subHandlers);
                    setGroupSubHandlerToAllHandlers(subHandlers);
                }
            }
        }
    }

    protected EventChainContext buildContext() {
        return new EventChainContext();
    }

    @Override
    public List<EventHandler<T>> getEventHandlers() {
        return eventHandlers;
    }

    @Override
    public List<EventHandler<T>> getAllEventHandlers() {
        return allEventHandlers;
    }

}
