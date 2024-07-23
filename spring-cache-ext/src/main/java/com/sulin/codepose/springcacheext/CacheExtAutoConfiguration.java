package com.sulin.codepose.springcacheext;

import com.sulin.codepose.springcacheext.cachemanager.CacheableReturnTypeHolder;
import com.sulin.codepose.springcacheext.cachemanager.CustomCaffeineCacheManager;
import com.sulin.codepose.springcacheext.cachemanager.CustomRedisCacheManager;
import com.sulin.codepose.springcacheext.properties.CaffeineCacheProperties;
import com.sulin.codepose.springcacheext.properties.RedisCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.HashMap;

@EnableCaching
@Configuration
@EnableConfigurationProperties({CaffeineCacheProperties.class, RedisCacheProperties.class})
public class CacheExtAutoConfiguration {


    @Bean
    public CacheableReturnTypeHolder cacheableReturnTypeHolder() {
        return new CacheableReturnTypeHolder();
    }

    /**
     * specs可以通过spring application.yml注入进来
     */
    @Bean("customerCaffeineCacheManager")
    @Primary
    @ConditionalOnProperty(prefix = "cache.caffeine", value = "enabled", havingValue = "true")
    public CacheManager customerCaffeineCacheManager() {
        return new CustomCaffeineCacheManager(cacheableReturnTypeHolder());
    }


    @Bean("customRedisCacheManager")
    @ConditionalOnProperty(prefix = "cache.redis", value = "enabled", havingValue = "true")
    public CustomRedisCacheManager customRedisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return new CustomRedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                new HashMap<>(), cacheableReturnTypeHolder());
    }

}
