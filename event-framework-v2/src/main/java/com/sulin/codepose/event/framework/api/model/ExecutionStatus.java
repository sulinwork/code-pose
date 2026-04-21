package com.sulin.codepose.event.framework.api.model;

public enum ExecutionStatus {
    //待处理
    PENDING,
    //处理中
    PROCESSING,
    //结束
    FINISHED,
    //中断
    ABORT,

    GROUP_MAIN_FINISHED,
    GROUP_MAIN_FINISHED_SUB_ABORT
}
