package com.sulin.codepose.sample;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class RunTask implements ApplicationRunner {

    @Value("${a.demo}")
    String a;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("===========" + a);
    }
}
