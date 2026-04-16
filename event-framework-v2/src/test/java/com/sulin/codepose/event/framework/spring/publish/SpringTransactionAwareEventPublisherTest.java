package com.sulin.codepose.event.framework.spring.publish;

import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventPayload;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringTransactionAwareEventPublisherTest {

    @Test
    void shouldPublishImmediatelyWithoutTransaction() {
        RecordingPublisher publisher = new RecordingPublisher();
        SpringTransactionAwareEventPublisher eventPublisher = new SpringTransactionAwareEventPublisher(publisher);

        eventPublisher.publishAfterCommit(new TestEvent());

        assertEquals(1, publisher.publishCount);
    }

    @Test
    void shouldPublishAfterCommitWhenTransactionActive() {
        RecordingPublisher publisher = new RecordingPublisher();
        SpringTransactionAwareEventPublisher eventPublisher = new SpringTransactionAwareEventPublisher(publisher);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            eventPublisher.publishAfterCommit(new TestEvent());
            assertEquals(0, publisher.publishCount);

            TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
            assertEquals(1, publisher.publishCount);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private static final class RecordingPublisher implements ApplicationEventPublisher {

        private int publishCount;

        @Override
        public void publishEvent(Object event) {
            publishCount++;
        }
    }

    private static final class TestEvent implements DomainEvent {

        @Override
        public String bizCode() {
            return "biz";
        }

        @Override
        public Long bizId() {
            return 1L;
        }

        @Override
        public String eventType() {
            return "created";
        }

        @Override
        public String eventKey() {
            return "biz_1_created_1";
        }

        @Override
        public Instant occurredAt() {
            return Instant.now();
        }

        @Override
        public List<EventPayload> payloads() {
            return Collections.emptyList();
        }
    }
}
