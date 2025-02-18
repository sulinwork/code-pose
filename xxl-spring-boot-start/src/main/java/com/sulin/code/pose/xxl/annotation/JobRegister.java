package com.sulin.code.pose.xxl.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JobRegister {
    //job name
    String name();

    String desc() default "default-desc";
}
