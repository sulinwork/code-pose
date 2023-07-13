package com.sulin.code.pose.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
@EnableConfigurationProperties(CaffeineCacheProperties.class)
public class CacheConfig extends CachingConfigurerSupport {

    private CaffeineCacheProperties caffeineCacheProperties;

    public CacheConfig(CaffeineCacheProperties caffeineCacheProperties) {
        this.caffeineCacheProperties = caffeineCacheProperties;
    }


    @Bean("caffeineCacheManager")
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        // 方案一(常用)：定制化缓存Cache
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .initialCapacity(100)
                .maximumSize(200));
        //存在弊端 全局的缓存都是一个设置  无法针对性的设置TTL
        return cacheManager;
    }


    /**
     * specs可以通过spring application.yml注入进来
     */
    @Bean("customerCaffeineCacheManager")
    @ConditionalOnProperty(prefix = "cache.caffeine",value = "enable",havingValue = "true")
    public CacheManager customerCaffeineCacheManager() {
        return new CaffeineCacheManager() {
            @Override
            @NonNull
            protected Cache<Object, Object> createNativeCaffeineCache(@NonNull String name) {
                //根据name 去load特定的TTL配置
                return Caffeine.from(caffeineCacheProperties.getSpecs().get(name)).build();
            }
        };
    }
}
