package com.sulin.codepose.event.enums;

/**
 * 分组主处理者处理结果
 */
public enum EventGroupMainHandleResult {

    /**
     * 分组主处理者已完成，子处理者待处理
     */
    GROUP_MAIN_FINISHED {
        @Override
        public EventHandleResult toEventHandleResult() {
            return EventHandleResult.GROUP_MAIN_FINISHED;
        }
    },

    /**
     * 分组主处理者执行完成，待终止子处理者
     */
    GROUP_MAIN_FINISHED_SUB_ABORT {
        @Override
        public EventHandleResult toEventHandleResult() {
            return EventHandleResult.GROUP_MAIN_FINISHED_SUB_ABORT;
        }
    },

    /**
     * 分组主处理者处理失败，子处理者不执行
     */
    FAIL {
        @Override
        public EventHandleResult toEventHandleResult() {
            return EventHandleResult.FAIL;
        }
    },

    /**
     * 分组主处理者终止处理，子处理者也终止处理
     */
    ABORT {
        @Override
        public EventHandleResult toEventHandleResult() {
            return EventHandleResult.ABORT;
        }
    },

    ;

    public EventHandleResult toEventHandleResult() {
        throw new UnsupportedOperationException("只支持子类方法");
    }
}
