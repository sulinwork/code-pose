package com.sulin.codepose.event.framework.core.store;

import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;

import java.time.Instant;
import java.time.LocalDateTime;

public class EventRecordStateMachine {

    public boolean isRunnable(HandlerExecutionRecord record) {
        return record.getStatus() == ExecutionStatus.PENDING || record.getStatus() == ExecutionStatus.PROCESSING;
    }

    public boolean isFutureExecution(HandlerExecutionRecord record, LocalDateTime now) {
        return record.getExecuteTime() != null && record.getExecuteTime().isAfter(now);
    }

    public void toProcessing(HandlerExecutionRecord record) {
        if (record.getStatus() != ExecutionStatus.PENDING) {
            return;
        }
        record.setStatus(ExecutionStatus.PROCESSING);
    }

    public HandlerExecutionRecord afterHandleResult(
            HandlerExecutionRecord record,
            EventHandleResult result,
            RetryPolicy retryPolicy) {
        switch (result) {
            case FINISHED:
                record.setStatus(ExecutionStatus.FINISHED);
                return record;
            case GROUP_MAIN_FINISHED:
                record.setStatus(ExecutionStatus.GROUP_MAIN_FINISHED);
                return record;
            case GROUP_MAIN_FINISHED_SUB_ABORT:
                record.setStatus(ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT);
                return record;
            case ABORT:
                record.setStatus(ExecutionStatus.ABORT);
                return record;
            case FAIL:
            default:
                onFailure(record, retryPolicy);
                return record;
        }
    }

    public HandlerExecutionRecord afterHandleException(HandlerExecutionRecord record, RetryPolicy retryPolicy) {
        onFailure(record, retryPolicy);
        return record;
    }

    public HandlerExecutionRecord afterGroupedSubHandlerAbort(HandlerExecutionRecord record) {
        record.setStatus(ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT);
        return record;
    }

    private void onFailure(HandlerExecutionRecord record, RetryPolicy retryPolicy) {
        int nextRetryNum = record.getRetryNum() + 1;
        if (retryPolicy.canRetry(record)) {
            record.setStatus(ExecutionStatus.PENDING);
            record.setRetryNum(nextRetryNum);
            record.setExecuteTime(retryPolicy.nextExecuteTime(record));
            return;
        }
        record.setStatus(ExecutionStatus.ABORT);
    }
}
