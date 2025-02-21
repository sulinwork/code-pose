package com.sulin.codepose.event.chain;



import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.enums.EventType;
import com.sulin.codepose.event.handler.EventGroupHandler;
import com.sulin.codepose.event.handler.EventHandler;

import java.util.List;
import java.util.Map;

/**
 * 事件处理链，组装事件Handler处理事件信息
 */
public interface EventHandlerChain<T extends Event> {

    EventType<?> subscribeEventType();

    /**
     * 遍历订单事件Handler处理事件，处理完成更新本地事件表处理状态
     */
    void handle(T orderEvent);

    /**
     * 获取该处理链所有事件处理者（不包括分组子处理者）
     */
    List<EventHandler<T>> getEventHandlers();

    /**
     * 获取该处理链所有事件处理者（包括分组子处理者）
     */
    List<EventHandler<T>> getAllEventHandlers();


    Map<EventHandler<T>, EventGroupHandler<T>> getGroupRefMap();
}
