package com.sulin.code.pose.event.v2.api;


public interface TransportFactory<T> {

    String name();

    Transport create(T config);
}
