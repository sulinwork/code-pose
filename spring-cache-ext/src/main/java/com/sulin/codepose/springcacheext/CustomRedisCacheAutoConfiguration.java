package com.sulin.codepose.springcacheext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.HashMap;

@Configuration
@ComponentScan("com.sulin.codepose.springcacheext")
public class CustomRedisCacheAutoConfiguration {
    @Bean("customRedisCacheManager")
    public CustomRedisCacheManager customRedisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return new CustomRedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                new HashMap<>());
    }
}
