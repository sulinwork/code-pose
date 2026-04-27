package com.sulin.codepose.event.framework.api.model;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public abstract class AbstractDomainEvent implements DomainEvent{
    private String bizCode;
    private String bizId;
    private String eventType;
    private String eventKey;

    private List<Payload> payloads = new ArrayList<>();

    public void addPayload(Payload payload){
        payloads.add(payload);
    }
}
