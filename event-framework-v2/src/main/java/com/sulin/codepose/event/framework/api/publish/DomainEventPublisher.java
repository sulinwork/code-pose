package com.sulin.codepose.event.framework.api.publish;

import com.sulin.codepose.event.framework.api.model.DomainEvent;

public interface DomainEventPublisher {

    void publishNow(DomainEvent event);

    void publishAfterCommit(DomainEvent event);
}
