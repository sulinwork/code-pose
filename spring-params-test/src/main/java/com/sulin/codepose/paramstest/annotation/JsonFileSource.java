package com.sulin.codepose.paramstest.annotation;

import com.sulin.codepose.paramstest.provider.JsonArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(JsonArgumentsProvider.class)
public @interface JsonFileSource {
    String value();

    //映射到多个对象内
    boolean multiMapping() default false;
}
