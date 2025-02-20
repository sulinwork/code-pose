package com.sulin.codepose.event.chain;


import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.chain.strategy.EventHandlerChainBuilderStrategy;
import com.sulin.codepose.event.utils.SpringContextUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class EventHandlerChainFactory implements SmartInitializingSingleton {

    private final Map<String, EventHandlerChainBuilderStrategy> strategyMap = new HashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        SpringContextUtils.getBeansOfType(EventHandlerChainBuilderStrategy.class)
                .forEach(strategy -> {
                    strategyMap.put(strategy.bizCode(), strategy);
                });
    }

    public <T extends Event> EventHandlerChain<T> getChain(T event) {
        String bizCode = event.getBizCode();
        return Optional.ofNullable(strategyMap.get(bizCode)).map(s -> s.getChain(event))
                .orElseThrow(() -> new IllegalArgumentException("event handler chain not exists"));
    }

}
