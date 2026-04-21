package com.sulin.codepose.event.framework.spring.listener;

import com.sulin.codepose.event.framework.api.model.DomainEvent;
import com.sulin.codepose.event.framework.core.chain.DefaultEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;

public class SpringDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventListener.class);

    private final DefaultEventProcessor eventProcessor;
    private final TaskExecutor taskExecutor;

    public SpringDomainEventListener(DefaultEventProcessor eventProcessor, TaskExecutor taskExecutor) {
        this.eventProcessor = eventProcessor;
        this.taskExecutor = taskExecutor;
    }

    @EventListener
    public void onDomainEvent(final DomainEvent event) {
        if (taskExecutor != null) {
            taskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    process(event);
                }
            });
            return;
        }
        process(event);
    }

    private void process(DomainEvent event) {
        try {
            eventProcessor.process(event);
        } catch (RuntimeException ex) {
            log.error("Failed to process domain event, bizCode={}, bizId={}, eventType={}, eventKey={}",
                    event.getBizCode(),
                    event.getBizId(),
                    event.getEventType(),
                    event.getEventKey(),
                    ex);
        }
    }
}
