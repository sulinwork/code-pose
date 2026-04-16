package com.sulin.codepose.event.framework.core.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sulin.codepose.event.framework.api.serialize.EventPayloadSerializer;

import java.io.IOException;

public class JacksonEventPayloadSerializer implements EventPayloadSerializer {

    private final ObjectMapper objectMapper;

    public JacksonEventPayloadSerializer() {
        this(new ObjectMapper());
    }

    public JacksonEventPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> String serialize(T payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize event payload", ex);
        }
    }

    @Override
    public <T> T deserialize(String content, Class<T> payloadClass) {
        if (content == null) {
            return null;
        }
        try {
            return objectMapper.readValue(content, payloadClass);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to deserialize event payload", ex);
        }
    }
}
