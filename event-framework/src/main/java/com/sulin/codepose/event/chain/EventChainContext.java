package com.sulin.codepose.event.chain;

import com.sulin.codepose.event.enums.EventHandleResult;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地事件处理链上下文
 */
@Data
@Accessors(chain = true)
public class EventChainContext {

    protected Map<String, EventHandleResult> handleResults = new HashMap<>();

    /**
     * 记录事件处理结果
     * @param eventHandler
     * @param handlerInfo
     */
    public void addHandleResult(String eventHandler, EventHandleResult handlerInfo) {
        handleResults.put(eventHandler, handlerInfo);
    }

    /**
     * 是否所有事件处理已完成
     */
    public boolean isAllHandleFinished() {
        return handleResults.values().stream().allMatch(EventHandleResult::isFinished);
    }
}
