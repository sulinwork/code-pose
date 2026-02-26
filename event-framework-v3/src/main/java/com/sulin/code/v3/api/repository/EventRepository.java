package com.sulin.code.v3.api.repository;


import com.sulin.code.v3.api.Event;
import com.sulin.code.v3.api.EventHandlerInfo;

public interface EventRepository {
    /**
     * 创建事件
     */
    void addEvent(Event event);

    void updateEventHandleStatus(Event event, EventHandlerInfo handlerInfo);
}
