
package com.sulin.code.pose.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 由于spring boot cache starter 自带的Caffeine cache不支持单个cache自定义配置，增加此配置
 *
 * @author chenchiwei
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cache.caffeine")
public class CaffeineCacheProperties {

    private boolean enabled;

    /**
     * The spec to use to create caches. See CaffeineSpec for more details on the spec format.
     */
    private Map<String, String> specs;

}