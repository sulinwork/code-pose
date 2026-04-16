package com.sulin.codepose.event.framework.api.serialize;

public interface EventPayloadSerializer {

    <T> String serialize(T payload, Integer version);

    <T> T deserialize(String content, Class<T> payloadClass, Integer version);
}
