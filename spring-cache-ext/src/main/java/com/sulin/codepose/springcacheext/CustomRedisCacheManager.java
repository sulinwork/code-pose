package com.sulin.codepose.springcacheext;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class CustomRedisCacheManager extends RedisCacheManager implements SmartInitializingSingleton, ApplicationContextAware {
    private final Map<String, RedisCacheConfiguration> initialCacheConfigurations;
    private ApplicationContext applicationContext;

    public CustomRedisCacheManager(RedisCacheWriter cacheWriter, Map<String, RedisCacheConfiguration> initialCacheConfigurations) {
        super(cacheWriter, RedisCacheConfiguration.defaultCacheConfig(), initialCacheConfigurations);
        this.initialCacheConfigurations = initialCacheConfigurations;
    }

    //触发initializeCaches方法 会load当前缓存
    @Override
    protected Collection<RedisCache> loadCaches() {
        List<RedisCache> caches = new LinkedList<>();
        for (Map.Entry<String, RedisCacheConfiguration> entry : initialCacheConfigurations.entrySet()) {
            caches.add(createRedisCache(entry.getKey(), entry.getValue()));
        }
        return caches;
    }


    @Override
    public void afterSingletonsInstantiated() {
        //扫描@Cacheable
        Map<String, Object> beansWithAnnotationMap = applicationContext.getBeansWithAnnotation(Component.class);
        beansWithAnnotationMap.forEach((k, v) -> {
            ReflectionUtils.doWithMethods(v.getClass(), method -> {
                if (method.isAnnotationPresent(Cacheable.class)) {
                    refreshRedisCacheConfiguration(method, method.getAnnotation(Cacheable.class));
                }
            });
        });
        initializeCaches();
    }

    private void refreshRedisCacheConfiguration(Method method, Cacheable cacheable) {
        final String[] cacheNames = cacheable.cacheNames();
        final Type genericReturnType = method.getGenericReturnType();
        Collection<String> allCacheNames = getCacheNames();
        for (String cacheName : cacheNames) {
            if (!allCacheNames.contains(cacheName)) continue;
            RedisCacheConfiguration redisCacheConfiguration = initialCacheConfigurations.get(cacheName);
            if (Objects.isNull(redisCacheConfiguration)) {
                redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
            }
            redisCacheConfiguration = redisCacheConfiguration.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer(genericReturnType)));
            initialCacheConfigurations.put(cacheName, redisCacheConfiguration);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private RedisSerializer<Object> redisSerializer(Type type) {
        return new Jackson2JsonRedisSerializer<>(TypeFactory.defaultInstance().constructType(type));
    }

}
