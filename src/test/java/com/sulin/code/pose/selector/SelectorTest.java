package com.sulin.code.pose.selector;

import org.junit.jupiter.api.Test;

import java.util.List;

public class SelectorTest {

    @Test
    public void test() {
        int age = 18;
        ContextSelector<Integer, String> contextSelector = ContextSelector.init(age);
        String re = contextSelector
                .register(o -> o >= 18, o -> "成年")
                .register(o -> o < 18, o -> "未成年")
                .execute();
        System.out.println("result:" + re);

        ContextSelector<Integer, String> pContextSelector = ContextSelector.init(age);
        List<String> a = pContextSelector
                .register(o -> o >= 18, o -> "成年")
                .register(o -> o > 8, o -> "未成年")
                .pipeline();
        System.out.println("pipeline re:"+a);
    }
}
