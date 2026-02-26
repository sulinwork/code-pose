package com.sulin.code.v3.api.handler;


import com.sulin.code.v3.api.Event;
import com.sulin.code.v3.api.EventHandlerInfo;
import com.sulin.code.v3.api.chain.EventChainContext;
import com.sulin.code.v3.api.enums.EventHandleResult;

/**
 * 事件处理者
 */
public interface EventHandler<T extends Event> {

    EventHandleResult handle(T event, EventHandlerInfo handlerInfo, EventChainContext context);

    //大部分场景用不到这个
    default String concernEventHandlerContextUniqueCode(){
        return "";
    }

}
