
package com.sulin.codepose.springcacheext.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;


@Data
@Configuration
@ConfigurationProperties(prefix = "cache.redis")
public class RedisCacheProperties {

    private boolean enabled;

    private Map<String, Long> ttl;

}