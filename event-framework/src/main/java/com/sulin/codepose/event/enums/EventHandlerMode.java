package com.sulin.codepose.event.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EventHandlerMode {
    NORMAL(0),FUTURE(1);

    private final int mode;
}
