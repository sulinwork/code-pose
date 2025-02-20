package com.sulin.codepose.event.enums;


import com.sulin.codepose.event.Event;

public interface EventType<Domain> {

    String name();

    Event getEvent(Domain domain);

}
