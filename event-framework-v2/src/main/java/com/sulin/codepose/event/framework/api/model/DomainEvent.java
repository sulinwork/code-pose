package com.sulin.codepose.event.framework.api.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface DomainEvent {

    String getBizCode();

    Long getBizId();

    String getEventType();

    String getEventKey();

    default Map<String, EventPayload> getPayloadMap() {
        return Collections.emptyMap();
    }

    default List<HandlerExecutionRecord> getRecords() {
        return Collections.emptyList();
    }
}
