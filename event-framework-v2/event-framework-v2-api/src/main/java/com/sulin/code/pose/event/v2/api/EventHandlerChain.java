package com.sulin.code.pose.event.v2.api;

public interface EventHandlerChain {

    String name();

    void invoke(IntegrationEvent event);
}
