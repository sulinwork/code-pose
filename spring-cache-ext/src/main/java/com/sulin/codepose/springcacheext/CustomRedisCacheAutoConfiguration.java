package com.sulin.codepose.springcacheext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.HashMap;

@Configuration
public class CustomRedisCacheAutoConfiguration {
    @Bean("customRedisCacheManager")
    @ConditionalOnBean({RedisConnectionFactory.class, RedisCacheManager.class})
    public CustomRedisCacheManager customRedisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return new CustomRedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                new HashMap<>());
    }
}
