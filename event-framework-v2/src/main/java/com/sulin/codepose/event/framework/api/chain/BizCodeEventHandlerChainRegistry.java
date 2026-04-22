package com.sulin.codepose.event.framework.api.chain;


/**
 * 给自定义实现获取链留一个业务的特殊入口
 */
public interface BizCodeEventHandlerChainRegistry extends EventHandlerChainRegistry{
    String getBizCode();
}
