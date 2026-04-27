package com.sulin.codepose.event.framework.api.router;

import com.sulin.codepose.event.framework.api.chain.EventHandlerChain;
import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.api.model.HandlerExecutionRecord;
import com.sulin.codepose.kit.strategy.Strategy;

import java.util.Optional;

public interface RouterStrategy extends Strategy<String> {

    String bizCode();

    default String type() {
        return bizCode();
    }

    /**
     * 路由到事件链的
     */
    <E extends DomainEvent> Optional<EventHandlerChain<E>> getChain(DomainEvent event);


    /**
     * 通过一条记录构建DomainEvent
     */
    DomainEvent buildDomainEvent(HandlerExecutionRecord record);

}
