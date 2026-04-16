package com.sulin.codepose.event.framework.api.model;

public enum EventHandleResult {
    FINISHED,
    FAIL,
    ABORT,
    GROUP_MAIN_FINISHED,
    GROUP_MAIN_FINISHED_SUB_ABORT;

    public boolean isFinished() {
        return this == FINISHED || this == ABORT;
    }

    public boolean shouldContinueSubHandlers() {
        return this == GROUP_MAIN_FINISHED;
    }

    public boolean shouldAbortSubHandlers() {
        return this == GROUP_MAIN_FINISHED_SUB_ABORT || this == ABORT;
    }
}
