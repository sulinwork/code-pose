package com.sulin.codepose.event.handler;

import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.EventContext;
import com.sulin.codepose.event.enums.EnumEventContextType;


public abstract class EventContextWarpHandler<T extends Event<?>, C extends EventContext> implements EventHandler<T> {
    //事件本身 Event  + 当前handler执行的上下文参数
    public void handler(T event) {
//        通过event拿到handler info 在转成bean

    }


    protected abstract void handler(T event, C context);


    protected abstract EnumEventContextType getContextType();
}
