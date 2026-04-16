package com.sulin.codepose.event.framework.api.chain;

import com.sulin.codepose.event.framework.api.model.EventHandleResult;

import java.util.Map;
import java.util.Optional;

public interface EventExecutionContext {

    void putResult(String handlerCode, EventHandleResult result);

    Optional<EventHandleResult> getResult(String handlerCode);

    Map<String, EventHandleResult> results();
}
