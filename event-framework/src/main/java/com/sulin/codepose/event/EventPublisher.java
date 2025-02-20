package com.sulin.codepose.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author harry
 * @since 2024/7/23 17:13
 */
@Slf4j
@Component
public class EventPublisher implements ApplicationEventPublisher {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishEvent(Object event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishEvent(Event event) {
       publishEvent(event);
    }
}
