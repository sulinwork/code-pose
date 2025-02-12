package com.sulin.codepose.event.chain;

import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.handler.EventHandler;

import java.util.*;

public class EventHandlerHolder<T extends Event> implements Iterable<EventHandler<T>> {
    private final List<EventHandler<T>> eventHandlers = new LinkedList<>();

    @Override
    public Iterator<EventHandler<T>> iterator() {
        return eventHandlers.iterator();
    }


    @Override
    public Spliterator<EventHandler<T>> spliterator() {
        return Spliterators.spliterator(this.eventHandlers, 0);
    }


    public <P extends EventHandler<T>> EventHandlerHolder<T> add(Class<P> cla) {
        return this;
    }

    public <P extends EventHandler<T>> EventHandlerHolder<T> addWithChild(Class<P> mainHandler, Class<P> ... subHandlers) {
        return this;
    }
}
