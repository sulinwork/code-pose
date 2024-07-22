package com.sulin.codepose.springcacheext.cachemanager;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CacheableReturnTypeHolder implements SmartInitializingSingleton, ApplicationContextAware {
    @Setter
    private ApplicationContext applicationContext;

    @Getter
    private final Map<String, Type> cacheReturnTypeMap = new ConcurrentHashMap<>();
    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beansWithAnnotationMap = applicationContext.getBeansWithAnnotation(Component.class);
        beansWithAnnotationMap.forEach((k, v) -> {
            ReflectionUtils.doWithMethods(v.getClass(), method -> {
                if (method.isAnnotationPresent(Cacheable.class)) {
                    refreshRedisCacheConfiguration(method, method.getAnnotation(Cacheable.class));
                }
            });
        });
    }

    private void refreshRedisCacheConfiguration(Method method, Cacheable cacheable) {
        final String[] cacheNames = cacheable.cacheNames();
        final Type genericReturnType = method.getGenericReturnType();
        for (String cacheName : cacheNames) {
            cacheReturnTypeMap.put(cacheName, genericReturnType);
        }
    }
}
