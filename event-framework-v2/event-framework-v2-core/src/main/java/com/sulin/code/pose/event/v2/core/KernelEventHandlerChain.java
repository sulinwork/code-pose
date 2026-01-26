package com.sulin.code.pose.event.v2.core;

import com.sulin.code.pose.event.v2.api.EventHandlerChain;
import com.sulin.code.pose.event.v2.api.IntegrationEvent;

public class KernelEventHandlerChain implements EventHandlerChain {

    @Override
    public String name() {
        return "";
    }

    @Override
    public void invoke(IntegrationEvent event) {
        //更新状态为执行中

        //执行当前事件的全部handler

        //更新状态为成功 or 失败
    }
}
