package com.sulin.codepose.event.framework.api.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface DomainEvent {

    String getBizCode();

    String getBizId();

    String getEventType();

    String getEventKey();

    List<Payload> getPayloads();

    default Map<String, Object> getEventContextMap() {
        return Collections.emptyMap();
    }

    default void resetEventContext(Map<String, Object> context) {

    }


    default List<HandlerExecutionRecord> getRecords() {
        return Collections.emptyList();
    }
}
