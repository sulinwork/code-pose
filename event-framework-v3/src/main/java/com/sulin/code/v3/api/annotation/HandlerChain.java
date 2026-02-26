package com.sulin.code.v3.api.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Inherited
@Documented
@Component
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface HandlerChain {

    String eventSource();
}
