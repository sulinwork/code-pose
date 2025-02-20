package com.sulin.codepose.event.enums;

/**
 * 事件处理结果
 */
public enum EventHandleResult {

    /**
     * 处理完成
     *    分组主处理者：分组内所有处理者已完成（FINISHED或ABORT）
     *    非分组主处理者：处理完成
     */
    FINISHED,

    /**
     * 分组主处理者已完成，子处理者待处理
     */
    GROUP_MAIN_FINISHED,

    /**
     * 分组主处理者执行完成，待终止子处理者
     */
    GROUP_MAIN_FINISHED_SUB_ABORT,

    /**
     * 处理失败
     *      分组主处理者：处理失败，子处理者不执行
     *      非分组主处理者：处理失败
     */
    FAIL,

    /**
     * 终止处理
     *      分组主处理者：分组内所有处理者都终止处理
     *      非分组主处理者：终止处理
     */
    ABORT,

    ;

    /**
     * 是否已完成
     */
    public boolean isFinished() {
        return EventHandleResult.FINISHED == this || EventHandleResult.ABORT == this;
    }
}
