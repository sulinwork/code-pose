package com.sulin.code.pose.event.v2.transport.polling;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sulin.code.pose.event.v2.api.Transport;
import com.sulin.code.pose.event.v2.api.TransportFactory;
import com.sulin.code.pose.event.v2.transport.polling.config.PollingProperties;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PollingTransportFactory implements TransportFactory<PollingProperties> {
    @Override
    public String name() {
        return "polling";
    }

    @Override
    public Transport create(PollingProperties config) {
        PollingTransport pollingTransport = new PollingTransport();
        pollingTransport.setPollingProperties(config);
        pollingTransport.setEventExecutor(
                new ThreadPoolExecutor(5, 20,
                        60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("async_event_%d").build(),
                        new ThreadPoolExecutor.CallerRunsPolicy()));
        return pollingTransport;
    }
}
