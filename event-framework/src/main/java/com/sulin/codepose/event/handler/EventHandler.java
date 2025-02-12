package com.sulin.codepose.event.handler;

public interface EventHandler {
    //事件本身 Event  + 当前handler执行的上下文参数
    void handler();
}
