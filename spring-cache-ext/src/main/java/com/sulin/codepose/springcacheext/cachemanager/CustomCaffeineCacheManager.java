package com.sulin.codepose.springcacheext.cachemanager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sulin.codepose.springcacheext.properties.CaffeineCacheProperties;
import com.sulin.codepose.springcacheext.cache.CustomCaffeineCache;
import lombok.NonNull;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Optional;

public class CustomCaffeineCacheManager extends CaffeineCacheManager {
    @Resource
    private CaffeineCacheProperties caffeineCacheProperties;

    private final CacheableReturnTypeHolder cacheableReturnTypeHolder;

    public CustomCaffeineCacheManager(CacheableReturnTypeHolder cacheableReturnTypeHolder) {
        this.cacheableReturnTypeHolder = cacheableReturnTypeHolder;
    }

    @Override
    @NonNull
    protected Cache<Object, Object> createNativeCaffeineCache(@NonNull String name) {
        //根据name 去load特定的TTL配置
        String specs = Optional.ofNullable(caffeineCacheProperties.getSpecs()).orElse(Collections.emptyMap()).getOrDefault(name, "");
        return Caffeine.from(specs).build();
    }

    @Override
    protected org.springframework.cache.Cache adaptCaffeineCache(String name, Cache<Object, Object> cache) {
        return new CustomCaffeineCache(name, cache, isAllowNullValues(), caffeineCacheProperties.getValueSerializer(), cacheableReturnTypeHolder.getCacheReturnTypeMap().get(name));
    }
}
