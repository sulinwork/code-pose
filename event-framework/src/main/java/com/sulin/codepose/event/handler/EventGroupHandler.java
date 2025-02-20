package com.sulin.codepose.event.handler;




import com.sulin.codepose.event.Event;

import java.util.List;

/**
 * 事件处理者
 */
public interface EventGroupHandler<T extends Event> extends EventHandler<T> {

    /**
     * 初始化子事件处理者
     */
    EventGroupHandler<T> addSubHandlers(List<EventHandler<T>> subOrderEventHandlers);

    /**
     * 获取子处理者
     */
    List<EventHandler<T>> getSubEventHandlers();
}
