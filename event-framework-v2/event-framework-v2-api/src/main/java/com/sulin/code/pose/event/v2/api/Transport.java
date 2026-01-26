package com.sulin.code.pose.event.v2.api;

/**
 * 屏蔽底层的转化层
 */
public interface Transport {

    /**
     * 发送一个事件
     */
    void send(IntegrationEvent event);

    /**
     * 注册一个事件handler执行器
     */
    void register(EventHandlerChain invoker);


    void start();

    void close();
}
