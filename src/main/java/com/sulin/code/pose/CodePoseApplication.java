package com.sulin.code.pose;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class CodePoseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodePoseApplication.class, args);
    }

}
