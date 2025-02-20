package com.sulin.codepose.event.handler;


import com.sulin.codepose.event.Event;

public abstract class AbstractEventHandler<T extends Event> implements EventHandler<T> {

    /**
     * 分组父处理者，只有子处理者才有值
     */
    protected EventGroupHandler<T> parentGroupHandler;

    @Override
    public void setParentGroupHandler(EventGroupHandler<T> parentGroupHandler) {
        this.parentGroupHandler = parentGroupHandler;
    }

    @Override
    public EventGroupHandler<T> getParentGroupHandler() {
        return parentGroupHandler;
    }
}
