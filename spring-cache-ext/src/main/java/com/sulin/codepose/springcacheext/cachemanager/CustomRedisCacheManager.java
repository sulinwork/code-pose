package com.sulin.codepose.springcacheext.cachemanager;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.sulin.codepose.springcacheext.properties.RedisCacheProperties;
import org.springframework.cache.Cache;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

public class CustomRedisCacheManager extends RedisCacheManager {
    private final Map<String, RedisCacheConfiguration> initialCacheConfigurations;

    private final CacheableReturnTypeHolder cacheableReturnTypeHolder;

    @Resource
    private RedisCacheProperties redisCacheProperties;

    public CustomRedisCacheManager(RedisCacheWriter cacheWriter, Map<String, RedisCacheConfiguration> initialCacheConfigurations, CacheableReturnTypeHolder cacheableReturnTypeHolder) {
        super(cacheWriter, RedisCacheConfiguration.defaultCacheConfig(), initialCacheConfigurations);
        this.initialCacheConfigurations = initialCacheConfigurations;
        this.cacheableReturnTypeHolder = cacheableReturnTypeHolder;
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
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        if (Objects.nonNull(cacheConfig) && cacheableReturnTypeHolder.getCacheReturnTypeMap().containsKey(name)) {
            Type genericReturnType = cacheableReturnTypeHolder.getCacheReturnTypeMap().get(name);
            if (Objects.nonNull(genericReturnType)) {
                //替换默认配置序列化改成自定义Type对象
                cacheConfig = cacheConfig.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer(genericReturnType)));
                Long settleTtl = Optional.ofNullable(redisCacheProperties.getTtl()).orElse(Collections.emptyMap()).get(name);
                if (Objects.nonNull(settleTtl)) {
                    cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(settleTtl));
                }
            }
        }
        return super.createRedisCache(name, cacheConfig);
    }


    private RedisSerializer<Object> redisSerializer(Type type) {
        return new Jackson2JsonRedisSerializer<>(TypeFactory.defaultInstance().constructType(type));
    }
}
