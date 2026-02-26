package com.sulin.code.v3.api.enums;


import com.sulin.codepose.event.Event;

public interface EventType<Domain> {

    String name();

    Event getEvent(Domain domain);

}
