package com.sulin.code.pose.strategy;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

public class StrategyTest {

    public enum Sex {
        MAN, WOMAN
    }

    public interface SexStrategy extends Strategy<Sex> {
        void doIt(String param);
    }

    @Component
    public static class ManStrategy implements SexStrategy {
        @Override
        public Sex type() {
            return Sex.MAN;
        }

        @Override
        public void doIt(String param) {
            System.out.println(" ManStrategy doIt !");
        }
    }


    @Bean
    public SimpleFactory<Sex, SexStrategy> getSimpleFactory() {
        return new SimpleFactory<>(SexStrategy.class);
    }
}
