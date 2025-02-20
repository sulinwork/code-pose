package com.sulin.codepose.event;


import com.sulin.codepose.event.chain.EventHandlerChain;
import com.sulin.codepose.event.chain.EventHandlerChainFactory;
import com.sulin.codepose.event.enums.EventType;
import com.sulin.codepose.event.handler.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 事件工厂
 */
@Component
public class EventFactory {
    @Resource
    private EventHandlerChainFactory eventHandlerChainFactory;

    public <Domain> Event buildEvent(Domain domain, EventType<Domain> eventType) {
        Event event = eventType.getEvent(domain);
        // 获取事件处理链，获取对应事件的处理者
        EventHandlerChain<?> handlerChain = eventHandlerChainFactory.getChain(event);
        if (handlerChain != null) {
            // 获取所有订单事件处理者（包括分组子处理者）
            List<? extends EventHandler<?>> allOrderEventHandlers = handlerChain.getAllEventHandlers();
            if (CollectionUtils.isEmpty(allOrderEventHandlers)) {
                // 设置订单事件的处理者信息
                event.setEventHandlerInfoList(initOrderEventHandlerInfos(event, allOrderEventHandlers));
                return event;
            }
        }
        return event;
    }


    private List<EventHandlerInfo> initOrderEventHandlerInfos(Event event, List<? extends EventHandler<?>> orderEventHandlerList) {
        List<EventHandlerInfo> handlerInfoList = new ArrayList<>();
        for (EventHandler<?> eventHandler : orderEventHandlerList) {
            EventHandlerInfo handlerInfo = EventHandlerInfo.init(event, eventHandler, Collections.emptyList());
            if (handlerInfo != null) {
                handlerInfoList.add(handlerInfo);
            }
        }
        return handlerInfoList;
    }
}
