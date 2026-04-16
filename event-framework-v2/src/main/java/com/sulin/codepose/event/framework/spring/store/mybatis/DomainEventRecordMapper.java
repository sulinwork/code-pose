package com.sulin.codepose.event.framework.spring.store.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DomainEventRecordMapper extends BaseMapper<DomainEventRecordEntity> {
}
