package com.sulin.codepose.event.chain;

import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.handler.EventHandler;
import org.springframework.beans.factory.InitializingBean;

/**
 * 定义事件链的执行流程 + 事件处理器的编排
 * @param <T>
 */
public abstract class AbstractEventHandlerChain<T extends Event<?>> implements EventHandlerChain<T>, InitializingBean {

    private final EventHandlerHolder<T> eventHandlerHolder;

    public AbstractEventHandlerChain() {
        this.eventHandlerHolder = new EventHandlerHolder<T>();
    }

    public abstract void appendHandler(EventHandlerHolder<T> holder);


    @Override
    public void afterPropertiesSet() throws Exception {
        appendHandler(this.eventHandlerHolder);
    }

    @Override
    public void handler(T event) {
        for (EventHandler<T> eventHandler : this.eventHandlerHolder) {
            eventHandler.handler(event);
        }
    }
}
