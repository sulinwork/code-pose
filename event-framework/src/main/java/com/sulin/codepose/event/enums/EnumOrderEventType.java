package com.sulin.codepose.event.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 事件类型
 */
@AllArgsConstructor
@Getter
public enum EnumOrderEventType implements EnumEventType{
    PAID("paid");

    private final String eventType;
}
