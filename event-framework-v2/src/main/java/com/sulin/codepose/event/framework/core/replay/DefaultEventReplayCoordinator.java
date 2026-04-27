package com.sulin.codepose.event.framework.core.replay;

import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.Payload;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;
import com.sulin.codepose.event.framework.core.chain.DefaultEventProcessor;
import com.sulin.codepose.event.framework.core.router.RouterStrategyFactory;
import com.sulin.codepose.event.framework.core.scheduler.DefaultReplayScanner;

import java.util.*;

public class DefaultEventReplayCoordinator {

    private final DefaultReplayScanner replayScanner;
    private final EventStore eventStore;
    private final DefaultEventProcessor eventProcessor;
    private final RouterStrategyFactory routerStrategyFactory;

    public DefaultEventReplayCoordinator(
            DefaultReplayScanner replayScanner,
            EventStore eventStore,
            DefaultEventProcessor eventProcessor,
            RouterStrategyFactory routerStrategyFactory) {
        this.replayScanner = replayScanner;
        this.eventStore = eventStore;
        this.eventProcessor = eventProcessor;
        this.routerStrategyFactory = routerStrategyFactory;
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
            DomainEvent domainEvent = routerStrategyFactory.getStrategy(first.getBizCode()).buildDomainEvent(first);
            eventProcessor.process(domainEvent, records);
        }
    }
}
