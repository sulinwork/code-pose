package com.sulin.codepose.event.framework.api.serialize;

public interface EventPayloadSerializer {

    <T> String serialize(T payload);

    <T> T deserialize(String content, Class<T> payloadClass);
}
