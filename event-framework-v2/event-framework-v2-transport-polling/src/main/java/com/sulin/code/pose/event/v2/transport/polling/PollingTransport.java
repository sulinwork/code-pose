package com.sulin.code.pose.event.v2.transport.polling;

import com.sulin.code.pose.event.v2.api.EventStore;
import com.sulin.code.pose.event.v2.api.EventHandlerChain;
import com.sulin.code.pose.event.v2.api.IntegrationEvent;
import com.sulin.code.pose.event.v2.api.Transport;
import com.sulin.code.pose.event.v2.transport.polling.config.PollingProperties;
import lombok.Setter;


import java.util.concurrent.*;

public class PollingTransport implements Transport {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "polling-transport");
        t.setDaemon(true);
        return t;
    });

    @Setter
    private ExecutorService eventExecutor;

    private EventHandlerChain handlerInvoker;

    private volatile boolean running = false;
    @Setter
    private PollingProperties pollingProperties;

    @Setter
    private EventStore eventStore;

    @Override
    public void send(IntegrationEvent event) {
        eventStore.store(event);
        //立刻异步执行事件
        eventExecutor.execute(() -> {
            try {
                doEvent(event);
            } catch (Exception e) {
                //pass
            }
        });
    }

    @Override
    public void register(EventHandlerChain invoker) {
        this.handlerInvoker = invoker;
    }

    @Override
    public void start() {
        if (running) return;
        //启动轮训
        running = true;
        scheduler.scheduleWithFixedDelay(this::poll, 0, pollingProperties.getPollIntervalMillis(), TimeUnit.MILLISECONDS);
    }

    private void poll() {
        //数据库循环获取

    }

    private void doEvent(IntegrationEvent event) {
        handlerInvoker.invoke(null);
    }

    @Override
    public void close() {
        //关闭
        if (!running) return;
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ignore) {
            scheduler.shutdownNow();
        }
    }
}
