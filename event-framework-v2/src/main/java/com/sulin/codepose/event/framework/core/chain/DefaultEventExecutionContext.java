package com.sulin.codepose.event.framework.core.chain;

import com.sulin.codepose.event.framework.api.chain.EventExecutionContext;
import com.sulin.codepose.event.framework.api.model.EventHandleResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultEventExecutionContext implements EventExecutionContext {

    private final Map<String, EventHandleResult> results = new LinkedHashMap<String, EventHandleResult>();

    @Override
    public void putResult(String handlerCode, EventHandleResult result) {
        results.put(handlerCode, result);
    }

    @Override
    public Optional<EventHandleResult> getResult(String handlerCode) {
        return Optional.ofNullable(results.get(handlerCode));
    }

    @Override
    public Map<String, EventHandleResult> results() {
        return Collections.unmodifiableMap(results);
    }
}
