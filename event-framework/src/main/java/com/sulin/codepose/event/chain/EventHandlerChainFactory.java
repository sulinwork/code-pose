package com.sulin.codepose.event.chain;


import com.sulin.codepose.event.Event;
import com.sulin.codepose.event.chain.strategy.DefaultSpringEventHandlerChainBuilderStrategy;
import com.sulin.codepose.event.chain.strategy.EventHandlerChainBuilderStrategy;
import com.sulin.codepose.event.utils.SpringContextUtils;
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
                    if (StringUtils.isNoneBlank(strategy.bizCode())) {
                        strategyMap.put(strategy.bizCode(), strategy);
                    }
                });
    }

    public <T extends Event> EventHandlerChain<T> getChain(T event) {
        String bizCode = event.getBizCode();
        return Optional.ofNullable(strategyMap.get(bizCode))
                .map(s -> s.getChain(event))
                .orElseGet(() -> defaultSpringEventHandlerChainBuilderStrategy.getChain(event));
    }

}
