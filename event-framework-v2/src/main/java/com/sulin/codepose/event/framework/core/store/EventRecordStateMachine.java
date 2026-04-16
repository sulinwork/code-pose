package com.sulin.codepose.event.framework.core.store;

import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;

import java.time.Instant;
import java.time.LocalDateTime;

public class EventRecordStateMachine {

    public boolean isRunnable(HandlerExecutionRecord record) {
        return record.status() == ExecutionStatus.PENDING || record.status() == ExecutionStatus.PROCESSING;
    }

    public boolean isFutureExecution(HandlerExecutionRecord record, LocalDateTime now) {
        return record.executeTime() != null && record.executeTime().isAfter(now);
    }

    public HandlerExecutionRecord toProcessing(HandlerExecutionRecord record) {
        if (record.status() != ExecutionStatus.PENDING) {
            return record;
        }
        return record.withState(
                ExecutionStatus.PROCESSING,
                record.retryNum(),
                record.executeTime(),
                record.version() + 1,
                Instant.now()
        );
    }

    public HandlerExecutionRecord afterHandleResult(
            HandlerExecutionRecord record,
            EventHandleResult result,
            RetryPolicy retryPolicy
    ) {
        switch (result) {
            case FINISHED:
                return record.withState(
                        ExecutionStatus.FINISHED,
                        record.retryNum(),
                        record.executeTime(),
                        record.version() + 1,
                        Instant.now()
                );
            case GROUP_MAIN_FINISHED:
                return record.withState(
                        ExecutionStatus.GROUP_MAIN_FINISHED,
                        record.retryNum(),
                        record.executeTime(),
                        record.version() + 1,
                        Instant.now()
                );
            case GROUP_MAIN_FINISHED_SUB_ABORT:
                return record.withState(
                        ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT,
                        record.retryNum(),
                        record.executeTime(),
                        record.version() + 1,
                        Instant.now()
                );
            case ABORT:
                return record.withState(
                        ExecutionStatus.ABORT,
                        record.retryNum(),
                        record.executeTime(),
                        record.version() + 1,
                        Instant.now()
                );
            case FAIL:
            default:
                return onFailure(record, retryPolicy);
        }
    }

    public HandlerExecutionRecord afterHandleException(HandlerExecutionRecord record, RetryPolicy retryPolicy) {
        return onFailure(record, retryPolicy);
    }

    public HandlerExecutionRecord afterGroupedSubHandlerAbort(HandlerExecutionRecord record) {
        return record.withState(
                ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT,
                record.retryNum(),
                record.executeTime(),
                record.version() + 1,
                Instant.now()
        );
    }

    private HandlerExecutionRecord onFailure(HandlerExecutionRecord record, RetryPolicy retryPolicy) {
        int nextRetryNum = record.retryNum() + 1;
        if (retryPolicy.canRetry(record)) {
            return record.withState(
                    ExecutionStatus.PENDING,
                    nextRetryNum,
                    retryPolicy.nextExecuteTime(record),
                    record.version() + 1,
                    Instant.now()
            );
        }
        return record.withState(
                ExecutionStatus.ABORT,
                nextRetryNum,
                record.executeTime(),
                record.version() + 1,
                Instant.now()
        );
    }
}
