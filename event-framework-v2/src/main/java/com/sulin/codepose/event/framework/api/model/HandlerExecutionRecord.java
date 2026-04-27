package com.sulin.codepose.event.framework.api.model;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public final class HandlerExecutionRecord {

    private Long id;
    private String eventKey;
    private String bizCode;
    private String bizId;
    private String eventType;
    //事件维度的上下文
    private String eventContext;

    private String handlerCode;
    private String parentHandlerCode;
    private String payload;
    private ExecutionStatus status;
    private Integer retryNum;
    private LocalDateTime executeTime;
    private Long version;

}
