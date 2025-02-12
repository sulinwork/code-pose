package com.sulin.codepose.event;

import com.sulin.codepose.event.enums.EnumEventType;

public interface Event<T extends Enum<T> & EnumEventType> {
    T eventType();
}
