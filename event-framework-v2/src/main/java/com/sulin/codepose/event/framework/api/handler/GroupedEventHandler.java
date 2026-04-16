package com.sulin.codepose.event.framework.api.handler;

import java.util.List;

public interface GroupedEventHandler<P> extends DomainEventHandler<P> {

    List<DomainEventHandler<?>> subHandlers();
}
