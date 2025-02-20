package com.sulin.codepose.event.repository;


import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.EventHandlerInfo;

public interface EventRepository {
    /**
     * 创建事件
     *
     * @param event
     */
    void addEvent(Event event);

    /**
     * 更新事件处理状态
     *
     * @param event
     * @param handlerInfo
     */
    void updateEventHandleStatus(Event event, EventHandlerInfo handlerInfo);
}
