package com.sulin.code.v3.api;

import com.sulin.code.v3.api.enums.EventHandleResult;
import com.sulin.code.v3.api.enums.EventHandleStatus;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class EventHandlerInfo {
    private Long id;

    private String name;
    /**
     * 分组父处理类，只有子处理类才有值
     */
    private String parentName;

    /**
     * 处理状态，0:待处理、1:已处理、2:处理中、3:终止处理、4.分组主处理者已处理
     */
    private EventHandleStatus status;

    /**
     * 重试次数
     */
    private Integer retryNum;

    /**
     * 0:普通  1:未来任务
     */
    private Integer model;

    private String context;

    /**
     * 期望执行时间
     */
    private LocalDateTime expectExecuteTime;

    /**
     * 实际执行时间
     */
    private LocalDateTime actualExecuteTime;

    public boolean isNeedHandle() {
        //todo
        return false;
    }

    public boolean arrayExecuteTime() {
        //todo
        return false;
    }

    /**
     * 通过事件处理结果更新处理状态
     */
    public void updateByHandleResult(EventHandleResult result) {
        if (EventHandleResult.FINISHED == result) {
            status = EventHandleStatus.FINISHED_STATUS;
        } else if (EventHandleResult.GROUP_MAIN_FINISHED == result) {
            status = EventHandleStatus.GROUP_MAIN_FINISHED_STATUS;
        } else if (EventHandleResult.GROUP_MAIN_FINISHED_SUB_ABORT == result) {
            status = EventHandleStatus.GROUP_MAIN_FINISHED_SUB_ABORT_STATUS;
        } else if (EventHandleResult.FAIL == result) {
            status = EventHandleStatus.PENDING_STATUS;
            this.retryNum++;
        } else if (EventHandleResult.ABORT == result) {
            status = EventHandleStatus.ABORT_STATUS;
        } else {
            throw new IllegalArgumentException("illegal state, EventHandleResult: " + result);
        }
    }

    /**
     * 事件处理异常时更新处理状态
     */
    public void updateByHandleException() {
        status = EventHandleStatus.PENDING_STATUS;
        this.retryNum++;
    }
}
