package com.sulin.codepose.event.chain;

import com.sulin.codepose.event.handler.EventHandler;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractEventHandlerChain implements EventHandlerChain, InitializingBean {

    private final EventHandlerHolder eventHandlerHolder;

    public AbstractEventHandlerChain() {
        this.eventHandlerHolder = new EventHandlerHolder();
    }

    public abstract void appendHandler(EventHandlerHolder holder);


    @Override
    public void afterPropertiesSet() throws Exception {
        appendHandler(this.eventHandlerHolder);
    }

    @Override
    public void handler() {
        for (EventHandler eventHandler : this.eventHandlerHolder) {
            eventHandler.handler();
        }
    }
}
