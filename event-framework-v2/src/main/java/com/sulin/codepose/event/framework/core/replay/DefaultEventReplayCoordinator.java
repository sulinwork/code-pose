package com.sulin.codepose.event.framework.core.replay;

import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.EventPayload;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;
import com.sulin.codepose.event.framework.core.chain.DefaultEventProcessor;
import com.sulin.codepose.event.framework.core.scheduler.DefaultReplayScanner;

import java.time.Instant;
import java.util.*;

public class DefaultEventReplayCoordinator {

    private final DefaultReplayScanner replayScanner;
    private final EventStore eventStore;
    private final DefaultEventProcessor eventProcessor;

    public DefaultEventReplayCoordinator(
            DefaultReplayScanner replayScanner,
            EventStore eventStore,
            DefaultEventProcessor eventProcessor) {
        this.replayScanner = replayScanner;
        this.eventStore = eventStore;
        this.eventProcessor = eventProcessor;
    }

    public void replay(ReplayScanRequest request) {
        List<HandlerExecutionRecord> scannedRecords = replayScanner.scan(request);
        if (scannedRecords == null || scannedRecords.isEmpty()) {
            return;
        }
        Set<String> eventKeys = new LinkedHashSet<String>();
        for (HandlerExecutionRecord scannedRecord : scannedRecords) {
            eventKeys.add(scannedRecord.getEventKey());
        }
        for (String eventKey : eventKeys) {
            List<HandlerExecutionRecord> records = eventStore.loadByEventKey(eventKey);
            if (records == null || records.isEmpty()) {
                continue;
            }
            HandlerExecutionRecord first = records.get(0);
            DomainEvent replayEvent = new ReplayDomainEvent(
                    first.getBizCode(),
                    first.getBizId(),
                    first.getEventType(),
                    first.getEventKey(),
                    records
            );
            eventProcessor.process(replayEvent, records);
        }
    }

    private static final class ReplayDomainEvent implements DomainEvent {

        private final String bizCode;
        private final Long bizId;
        private final String eventType;
        private final String eventKey;
        private final List<HandlerExecutionRecord> records;

        private ReplayDomainEvent(
                String bizCode,
                Long bizId,
                String eventType,
                String eventKey,
                List<HandlerExecutionRecord> records
        ) {
            this.bizCode = bizCode;
            this.bizId = bizId;
            this.eventType = eventType;
            this.eventKey = eventKey;
            this.records = Collections.unmodifiableList(new ArrayList<>(records));
        }

        @Override
        public String getBizCode() {
            return bizCode;
        }

        @Override
        public Long getBizId() {
            return bizId;
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public String getEventKey() {
            return eventKey;
        }

        @Override
        public Map<String, EventPayload> getPayloadMap() {
            return Collections.emptyMap();
        }

        @Override
        public void pushPayload(String name, EventPayload payload) {

        }


        @Override
        public List<HandlerExecutionRecord> getRecords() {
            return records;
        }
    }
}
