package com.sulin.codepose.event.framework.spring.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.spring.store.mybatis.DomainEventRecordMapper;
import com.sulin.codepose.event.framework.spring.store.mybatis.MybatisPlusEventStore;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnClass(BaseMapper.class)
@ConditionalOnBean({DataSource.class, SqlSessionFactory.class})
@MapperScan(basePackageClasses = DomainEventRecordMapper.class)
public class DomainEventMybatisPlusStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventStore.class)
    public EventStore mybatisPlusEventStore(DomainEventRecordMapper mapper) {
        return new MybatisPlusEventStore(mapper);
    }
}
