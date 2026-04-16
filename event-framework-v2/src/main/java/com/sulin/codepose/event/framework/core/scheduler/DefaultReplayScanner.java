package com.sulin.codepose.event.framework.core.scheduler;

import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.event.framework.api.store.EventStore;
import com.sulin.codepose.event.framework.api.store.ReplayScanRequest;

import java.util.List;

public class DefaultReplayScanner {

    private final EventStore eventStore;

    public DefaultReplayScanner(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public List<HandlerExecutionRecord> scan(ReplayScanRequest request) {
        return eventStore.scanRetryable(request);
    }
}
