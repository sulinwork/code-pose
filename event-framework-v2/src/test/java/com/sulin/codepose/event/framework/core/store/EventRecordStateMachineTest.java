package com.sulin.codepose.event.framework.core.store;

import com.sulin.codepose.event.framework.api.model.EventHandleResult;
import com.sulin.codepose.event.framework.api.model.ExecutionStatus;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.policy.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventRecordStateMachineTest {

    private final EventRecordStateMachine stateMachine = new EventRecordStateMachine();

    @Test
    void shouldTransitionToFinished() {
        HandlerExecutionRecord next = stateMachine.afterHandleResult(baseRecord(), EventHandleResult.FINISHED, retryPolicy(3));

        assertEquals(ExecutionStatus.FINISHED, next.getStatus());
        assertEquals(Integer.valueOf(0), next.getRetryNum());
    }

    @Test
    void shouldRetryPendingFailure() {
        HandlerExecutionRecord next = stateMachine.afterHandleResult(baseRecord(), EventHandleResult.FAIL, retryPolicy(3));

        assertEquals(ExecutionStatus.PENDING, next.getStatus());
        assertEquals(Integer.valueOf(1), next.getRetryNum());
    }

    @Test
    void shouldAbortWhenRetryExceeded() {
        HandlerExecutionRecord next = stateMachine.afterHandleResult(baseRecord(3), EventHandleResult.FAIL, retryPolicy(3));

        assertEquals(ExecutionStatus.ABORT, next.getStatus());
        assertEquals(Integer.valueOf(4), next.getRetryNum());
    }

    @Test
    void shouldTransitionGroupedAbortStatus() {
        HandlerExecutionRecord next = stateMachine.afterGroupedSubHandlerAbort(baseRecord());

        assertEquals(ExecutionStatus.GROUP_MAIN_FINISHED_SUB_ABORT, next.getStatus());
    }

    private HandlerExecutionRecord baseRecord() {
        return baseRecord(0);
    }

    private HandlerExecutionRecord baseRecord(int getRetryNum) {
        return new HandlerExecutionRecord(
                1L,
                "biz_1_created_1",
                "biz",
                1L,
                "created",
                "handler",
                null,
                "{}",
                ExecutionStatus.PENDING,
                getRetryNum,
                LocalDateTime.now(),
                0L,
                Instant.now(),
                Instant.now()
        );
    }

    private RetryPolicy retryPolicy(final int maxRetryCount) {
        return new RetryPolicy() {
            @Override
            public int maxRetryCount(String bizCode, String eventType, String handlerCode) {
                return maxRetryCount;
            }

            @Override
            public LocalDateTime nextExecuteTime(HandlerExecutionRecord record) {
                return LocalDateTime.now().plusMinutes(1);
            }
        };
    }
}
