package com.sulin.code.v3.api.chain.strategy;


import com.sulin.code.v3.api.Event;
import com.sulin.code.v3.api.chain.EventHandlerChain;

public interface EventHandlerChainBuilderStrategy{
    String eventSource();

    <T extends Event> EventHandlerChain<T> getChain(T event);
}
