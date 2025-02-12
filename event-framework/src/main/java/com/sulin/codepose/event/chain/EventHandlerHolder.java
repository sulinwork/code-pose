package com.sulin.codepose.event.chain;

import com.sulin.codepose.event.handler.EventHandler;

import java.util.*;

public class EventHandlerHolder implements Iterable<EventHandler> {
    private final List<EventHandler> eventHandlers = new LinkedList<>();

    @Override
    public Iterator<EventHandler> iterator() {
        return eventHandlers.iterator();
    }


    @Override
    public Spliterator<EventHandler> spliterator() {
        return Spliterators.spliterator(this.eventHandlers, 0);
    }


    public EventHandlerHolder add(Class<EventHandler> cla) {
        return this;
    }

    public EventHandlerHolder addWithChild(Class<EventHandler> mainHandler, Class<EventHandler>... subHandlers) {
        return this;
    }
}
