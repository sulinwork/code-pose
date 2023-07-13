package com.sulin.code.pose.strategy;

import com.sun.istack.internal.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 简单工厂模板类
 *
 * @param <T> 作为区分对象的类型
 * @param <S> 策略
 */
public class SimpleFactory<T, S extends Strategy<T>> implements BeanFactoryPostProcessor, SmartInitializingSingleton {

    private final Class<S> type;

    private Map<T, S> route;

    private ConfigurableListableBeanFactory beanFactory;

    public SimpleFactory(final Class<S> type) {
        this.type = type;
    }


    @Override
    public void afterSingletonsInstantiated() {
        this.route = this.beanFactory.getBeanProvider(this.type).stream().collect(Collectors.toMap(Strategy::type, Function.identity()));
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public S getStrategy(T type) {
        return this.route.get(type);
    }
}
