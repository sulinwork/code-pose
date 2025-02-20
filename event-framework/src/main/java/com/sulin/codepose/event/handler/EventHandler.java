package com.sulin.codepose.event.handler;


import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.EventHandlerInfo;
import com.sulin.codepose.event.chain.EventChainContext;
import com.sulin.codepose.event.enums.EventHandleResult;
import org.apache.commons.lang3.StringUtils;

/**
 * 事件处理者
 */
public interface EventHandler<T extends Event> {

    EventHandleResult handle(T event, EventHandlerInfo handlerInfo, EventChainContext context);


    //大部分场景用不到这个
    default String concernEventHandlerContextUniqueCode(){
        return StringUtils.EMPTY;
    }

    /**
     * 设置分组父处理者，只有子处理者才设置
     */
    void setParentGroupHandler(EventGroupHandler<T> orderEventGroupHandler);

    EventGroupHandler<T> getParentGroupHandler();

}
