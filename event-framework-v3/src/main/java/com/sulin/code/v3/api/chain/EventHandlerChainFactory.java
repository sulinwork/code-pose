package com.sulin.code.v3.api.chain;


import com.sulin.code.v3.api.Event;
import com.sulin.code.v3.api.chain.strategy.DefaultSpringEventHandlerChainBuilderStrategy;
import com.sulin.code.v3.api.chain.strategy.EventHandlerChainBuilderStrategy;
import com.sulin.code.v3.utils.SpringContextUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class EventHandlerChainFactory implements SmartInitializingSingleton {

    @Resource
    private DefaultSpringEventHandlerChainBuilderStrategy defaultSpringEventHandlerChainBuilderStrategy;

    private final Map<String, EventHandlerChainBuilderStrategy> strategyMap = new HashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        SpringContextUtils.getBeansOfType(EventHandlerChainBuilderStrategy.class)
                .forEach(strategy -> {
                    if (StringUtils.isNoneBlank(strategy.eventSource())) {
                        strategyMap.put(strategy.eventSource(), strategy);
                    }
                });
    }

    public <T extends Event> EventHandlerChain<T> getChain(T event) {
        String bizCode = event.getSource();
        return Optional.ofNullable(strategyMap.get(bizCode))
                .map(s -> s.getChain(event))
                .orElseGet(() -> defaultSpringEventHandlerChainBuilderStrategy.getChain(event));
    }

}
