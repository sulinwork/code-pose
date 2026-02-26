package com.sulin.code.v3.api.chain.strategy;


import com.sulin.code.v3.api.Event;
import com.sulin.code.v3.api.annotation.HandlerChain;
import com.sulin.code.v3.api.chain.EventHandlerChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DefaultSpringEventHandlerChainBuilderStrategy implements EventHandlerChainBuilderStrategy, BeanFactoryPostProcessor, SmartInitializingSingleton {


    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    private final Map<String, Map<String, EventHandlerChain<?>>> eventType2HandlerChainMap = new HashMap<>();


    @Override
    public String eventSource() {
        return "";
    }

    @Override
    public <T extends Event> EventHandlerChain<T> getChain(T event) {
        Map<String, EventHandlerChain<?>> eventHandlerChainMap = eventType2HandlerChainMap.getOrDefault(event.getSource(), new HashMap<>());
        return (EventHandlerChain<T>) eventHandlerChainMap.get(event.getType());
    }


    @Override
    public void afterSingletonsInstantiated() {
        String[] beanNames = configurableListableBeanFactory.getBeanNamesForAnnotation(HandlerChain.class);
        for (String beanName : beanNames) {
            Class<?> type = configurableListableBeanFactory.getType(beanName);
            if (type == null) {
                log.warn("bean name:{} not find bean type", beanName);
                continue;
            }
            HandlerChain annotation = type.getAnnotation(HandlerChain.class);
            //获取了bizCode
            //继续获取eventType和实例
            if (type.isAssignableFrom(EventHandlerChain.class)) {
                EventHandlerChain<?> eventHandlerChain = configurableListableBeanFactory.getBean(beanName, EventHandlerChain.class);
                Map<String, EventHandlerChain<?>> stringEventHandlerChainMap = eventType2HandlerChainMap.computeIfAbsent(annotation.eventSource(), k -> new HashMap<>());
                stringEventHandlerChainMap.put(eventHandlerChain.subscribeEventType(), eventHandlerChain);
            } else {
                //在一个class内做内部类
                Class<?>[] innerClassList = type.getDeclaredClasses();
                for (Class<?> innerClass : innerClassList) {
                    //不是一个抽象类 并且是 基础 EventHandlerChain
                    if (!Modifier.isAbstract(innerClass.getModifiers()) && EventHandlerChain.class.isAssignableFrom(innerClass)) {
                        EventHandlerChain<?> eventHandlerChain = configurableListableBeanFactory.getBean(innerClass.getName(), EventHandlerChain.class);
                        Map<String, EventHandlerChain<?>> stringEventHandlerChainMap = eventType2HandlerChainMap.computeIfAbsent(annotation.eventSource(), k -> new HashMap<>());
                        stringEventHandlerChainMap.put(eventHandlerChain.subscribeEventType(), eventHandlerChain);
                    }
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        this.configurableListableBeanFactory = configurableListableBeanFactory;
    }
}
