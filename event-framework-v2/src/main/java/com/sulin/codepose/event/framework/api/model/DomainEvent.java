package com.sulin.codepose.event.framework.api.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public interface DomainEvent {

    String bizCode();

    Long bizId();

    String eventType();

    String eventKey();

    Instant occurredAt();

    List<EventPayload> payloads();

    default List<HandlerExecutionRecord> records() {
        return Collections.emptyList();
    }
}
