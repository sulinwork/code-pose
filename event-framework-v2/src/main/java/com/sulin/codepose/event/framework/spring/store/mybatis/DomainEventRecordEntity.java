package com.sulin.codepose.event.framework.spring.store.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@TableName("domain_event_record")
public class DomainEventRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String eventKey;

    private String bizCode;

    private String bizId;

    private String eventType;

    //事件维度的上下文
    private String eventContext;

    private String handlerCode;

    private String parentHandlerCode;

    //handler维度的上下文
    private String payload;

    private String status;

    private Integer retryNum;

    private LocalDateTime executeTime;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
