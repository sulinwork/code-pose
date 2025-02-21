package com.sulin.codepose.event.chain;


import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.handler.AbstractEventGroupHandler;
import com.sulin.codepose.event.handler.EventGroupHandler;
import com.sulin.codepose.event.handler.EventHandler;
import com.sulin.codepose.event.utils.SpringContextUtils;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 事件处理者管理器
 */
@Getter
public class EventHandlerHolder<T extends Event> implements Iterable<EventHandler<T>> {

    private final List<EventHandler<T>> eventHandlers = new ArrayList<>();

    protected final Map<EventHandler<T>, EventGroupHandler<T>> groupRefMap = new HashMap<>();


    public EventHandlerHolder() {
    }

    public EventHandlerHolder(List<Class<? extends EventHandler<T>>> eventHandlers) {
        for (Class<? extends EventHandler<T>> eventHandler : eventHandlers) {
            addLast(eventHandler);
        }
    }

    public synchronized EventHandlerHolder<T> add(List<Class<? extends EventHandler<T>>> eventHandlers) {
        for (Class<? extends EventHandler<T>> eventHandler : eventHandlers) {
            addLast(eventHandler);
        }
        return this;
    }

    public synchronized EventHandlerHolder<T> addFirst(Class<? extends EventHandler<T>> eventHandler) {
        removeIfPresent(eventHandler);
        this.eventHandlers.add(0, SpringContextUtils.getBean(eventHandler));
        return this;
    }

    public synchronized EventHandlerHolder<T> addLast(Class<? extends EventHandler<T>> eventHandler) {
        removeIfPresent(eventHandler);
        this.eventHandlers.add(SpringContextUtils.getBean(eventHandler));
        return this;
    }

    @SafeVarargs
    public final synchronized EventHandlerHolder<T> addWithChildren(Class<? extends AbstractEventGroupHandler<T>> mainEventHandler,
                                                                    Class<? extends EventHandler<T>>... subeventHandler) {
        AbstractEventGroupHandler<T> orderEventGroupHandler = (AbstractEventGroupHandler<T>) createIfAbsentAtClass(mainEventHandler);
        List<EventHandler<T>> subHandlers = Arrays.stream(subeventHandler).map(SpringContextUtils::getBean).collect(Collectors.toList());
        orderEventGroupHandler.addSubHandlers(subHandlers);
        appendGroupHandlerRef(orderEventGroupHandler, subHandlers);
        return this;
    }

    public final synchronized EventHandlerHolder<T> addWithChildren(Class<? extends AbstractEventGroupHandler<T>> maineventHandler,
                                                                    List<Class<? extends EventHandler<T>>> subeventHandler) {
        AbstractEventGroupHandler<T> orderEventGroupHandler = (AbstractEventGroupHandler<T>) createIfAbsentAtClass(maineventHandler);
        List<EventHandler<T>> subEventHandlers = subeventHandler.stream().map(SpringContextUtils::getBean).collect(Collectors.toList());
        orderEventGroupHandler.addSubHandlers(subEventHandlers);
        appendGroupHandlerRef(orderEventGroupHandler, subEventHandlers);
        return this;
    }

    public final synchronized EventHandlerHolder<T> addWithChildren(Class<? extends AbstractEventGroupHandler<T>> maineventHandler,
                                                                    EventHandlerHolder<T> holder) {
        AbstractEventGroupHandler<T> orderEventGroupHandler = (AbstractEventGroupHandler<T>) createIfAbsentAtClass(maineventHandler);
        orderEventGroupHandler.addSubHandlers(holder.getEventHandlers());
        appendGroupHandlerRef(orderEventGroupHandler, holder.getEventHandlers());
        return this;
    }

    //层级太深 有需要在说吧
//    public final synchronized EventHandlerHolder<T> addWithChildren(Class<? extends AbstractEventGroupHandler<T>> mainEventHandler,
//                                                                    Class<? extends AbstractEventGroupHandler<T>> secondaryEventHandler,
//                                                                    List<Class<? extends EventHandler<T>>> subEventHandler) {
//        List<EventHandler<T>> subEventHandlers = createIfAbsentAtClass(mainEventHandler, secondaryEventHandler);
//
//        for (EventHandler<T> eventHandler : subEventHandlers) {
//            if (eventHandler.getClass().equals(secondaryEventHandler)) {
//                ((AbstractEventGroupHandler<T>) eventHandler).addSubHandlers(subEventHandler.stream().map(SpringContextUtils::getBean).collect(Collectors.toList()));
//                return this;
//            }
//        }
//        return this;
//    }
//
//    public final synchronized EventHandlerHolder<T> addWithChildren(Class<? extends AbstractEventGroupHandler<T>> mainEventHandler,
//                                                                    Class<? extends AbstractEventGroupHandler<T>> secondaryEventHandler,
//                                                                    EventHandlerHolder<T> holder) {
//        List<EventHandler<T>> subEventHandlers = createIfAbsentAtClass(mainEventHandler, secondaryEventHandler);
//
//        for (EventHandler<T> eventHandler : subEventHandlers) {
//            if (eventHandler.getClass().equals(secondaryEventHandler)) {
//                ((AbstractEventGroupHandler<T>) eventHandler).addSubHandlers(holder.getEventHandlers());
//                return this;
//            }
//        }
//        return this;
//    }


    public synchronized EventHandlerHolder<T> addBefore(Class<? extends EventHandler<T>> eventHandler,
                                                        Class<? extends EventHandler<T>> targeteventHandler) {
        assertSameClass(eventHandler, targeteventHandler);
        removeIfPresent(eventHandler);
        int index = getIndex(targeteventHandler);
        addAtIndex(index, eventHandler);
        return this;
    }

    public synchronized EventHandlerHolder<T> addAfter(Class<? extends EventHandler<T>> eventHandler,
                                                       Class<? extends EventHandler<T>> targeteventHandler) {
        assertSameClass(eventHandler, targeteventHandler);
        removeIfPresent(eventHandler);
        int index = getIndex(targeteventHandler);
        addAtIndex(index + 1, eventHandler);
        return this;
    }

    public synchronized EventHandlerHolder<T> remove(Class<? extends EventHandler<T>> eventHandler) {
        int index = getIndex(eventHandler);
        this.eventHandlers.remove(index);
        return this;
    }

    public synchronized EventHandlerHolder<T> replace(Class<? extends EventHandler<T>> oldeventHandler,
                                                      Class<? extends EventHandler<T>> neweventHandler) {
        assertSameClass(oldeventHandler, neweventHandler);
        int index = getIndex(oldeventHandler);
        this.eventHandlers.set(index, SpringContextUtils.getBean(neweventHandler));
        return this;
    }

    public synchronized EventHandlerHolder<T> clear() {
        this.eventHandlers.clear();
        return this;
    }


    private void removeIfPresent(Class<? extends EventHandler<T>> eventHandler) {
        int index = -1;
        for (EventHandler<T> handler : this.eventHandlers) {
            if (handler.getClass().equals(eventHandler)) {
                index = this.eventHandlers.indexOf(handler);
            }
        }
        if (index == -1) {
            return;
        }
        this.eventHandlers.remove(index);
    }

    private void addAtIndex(int index, Class<? extends EventHandler<T>> eventHandler) {
        removeIfPresent(eventHandler);
        this.eventHandlers.add(index, SpringContextUtils.getBean(eventHandler));
    }

    private int getIndex(Class<? extends EventHandler<T>> eventHandler) {
        for (EventHandler<T> handler : this.eventHandlers) {
            if (handler.getClass().equals(eventHandler)) {
                return this.eventHandlers.indexOf(handler);
            }
        }
        throw new IllegalArgumentException(eventHandler.getSimpleName() + " not found");
    }

    private EventHandler<T> createIfAbsentAtClass(Class<? extends EventHandler<T>> eventHandler) {
        for (EventHandler<T> handler : this.eventHandlers) {
            if (handler.getClass().equals(eventHandler)) {
                return handler;
            }
        }
        // 不存在就新建并加入队列
        EventHandler<T> handler = SpringContextUtils.getBean(eventHandler);
        this.eventHandlers.add(handler);
        return handler;
    }

    private List<EventHandler<T>> createIfAbsentAtClass(Class<? extends AbstractEventGroupHandler<T>> mainEventHandler, Class<? extends AbstractEventGroupHandler<T>> secondaryeventHandler) {
        AbstractEventGroupHandler<T> eventGroupHandler = (AbstractEventGroupHandler<T>) createIfAbsentAtClass(mainEventHandler);
        List<EventHandler<T>> subEventHandlers = eventGroupHandler.getSubEventHandlers();
        boolean secondaryExist = subEventHandlers.stream().anyMatch(h -> h.getClass().equals(secondaryeventHandler));
        if (!secondaryExist) {
            eventGroupHandler.addSubHandlers(Collections.singletonList(SpringContextUtils.getBean(secondaryeventHandler)));
        }
        return subEventHandlers;
    }

    public int size() {
        return this.eventHandlers.size();
    }

    private void assertSameClass(Class<?> oldType, Class<?> newType) {
        if (Objects.equals(oldType, newType)) {
            throw new IllegalArgumentException("[" + oldType.getName() + "] old Bean and new Bean must not be the same class");
        }
    }

    public Stream<EventHandler<T>> stream() {
        return this.eventHandlers.stream();
    }

    @Override
    public Iterator<EventHandler<T>> iterator() {
        return this.eventHandlers.iterator();
    }

    @Override
    public Spliterator<EventHandler<T>> spliterator() {
        return Spliterators.spliterator(this.eventHandlers, 0);
    }

    @Override
    public String toString() {
        return this.eventHandlers.toString();
    }

    protected void appendGroupHandlerRef(EventGroupHandler<T> mainHandler, List<EventHandler<T>> subHandlers) {
        if (CollectionUtils.isEmpty(subHandlers)) return;
        for (EventHandler<T> subHandler : subHandlers) {
            groupRefMap.putIfAbsent(subHandler, mainHandler);
        }
    }

}
