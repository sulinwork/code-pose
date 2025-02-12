package com.sulin.codepose.event.handler;

import com.sulin.codepose.event.Event;

public interface EventHandler<T extends Event> {
    //事件本身 Event  + 当前handler执行的上下文参数
    void handler(T event);
}
