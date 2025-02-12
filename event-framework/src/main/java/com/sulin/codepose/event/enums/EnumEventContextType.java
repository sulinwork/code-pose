package com.sulin.codepose.event.enums;

import com.sulin.codepose.event.EventContext;

/**
 * 事件上下文的类型
 */
public interface EnumEventContextType {

    String getEventContextType();


    <T extends EventContext> T decodeContext(String context);
}
