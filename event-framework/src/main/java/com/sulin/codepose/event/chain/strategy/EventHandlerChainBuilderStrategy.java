package com.sulin.codepose.event.chain.strategy;


import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.chain.EventHandlerChain;

public interface EventHandlerChainBuilderStrategy{
    String bizCode();

    <T extends Event> EventHandlerChain<T> getChain(T event);
}
