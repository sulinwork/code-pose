package com.sulin.code.pose.xxl;


import com.sulin.code.pose.xxl.client.XxlJobAdminClient;
import com.sulin.code.pose.xxl.config.XxlConfig;
import com.sulin.code.pose.xxl.executor.XxlJobAutoRegisterSpringExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spring boot starter 自动装配入库
 */
@Configuration
@ConditionalOnProperty(prefix = XxlConfig.PREFIX, name = "admin-address")
@EnableConfigurationProperties({XxlConfig.class})
@Slf4j
public class XxlAutoConfiguration {

    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlConfig xxlConfig) {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobAutoRegisterSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(xxlConfig.getAdminAddress());
        xxlJobSpringExecutor.setAppname(xxlConfig.getAppName());
        xxlJobSpringExecutor.setAccessToken(xxlConfig.getAccessToken());
        xxlJobSpringExecutor.setLogRetentionDays(xxlConfig.getLogRetentionDays());
        log.info("...... xxl job spring bean init success ......");
        return xxlJobSpringExecutor;
    }

    @Bean
    @ConditionalOnProperty(prefix = XxlConfig.PREFIX, name = {"username", "password"})
    public XxlJobAdminClient xxlJobAdminClient(XxlConfig xxlConfig) {
        return new XxlJobAdminClient(xxlConfig);
    }

}
