package com.sulin.codepose.event.chain;

import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.enums.EnumEventType;

public interface EventHandlerChain<T extends Event> {

    EnumEventType getEventType();

    //Event本身 订单基本信息 + 自己会触发的handler(有状态的)
    void handler(T event);
}
