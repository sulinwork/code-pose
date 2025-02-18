package com.sulin.code.pose.xxl.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = XxlConfig.PREFIX)
public class XxlConfig {

    public static final String PREFIX = "xxl.job";

    private String appName;
    private String adminAddress;
    private String accessToken;
    private Integer logRetentionDays;
    private String username;
    private String password;
}
