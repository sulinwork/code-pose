package com.sulin.codepose.event.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class EventThreadPoolConfiguration {

    @Bean("eventExecutor")
    public ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(5, 20,
                60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("async_event_%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
