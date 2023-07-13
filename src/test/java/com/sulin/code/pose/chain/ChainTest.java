package com.sulin.code.pose.chain;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChainTest {


    //普通用法
    @Test
    public void chainTest() {
        RiskChainContext riskChainContext = new RiskChainContext();
        riskChainContext.setRequests("param");
        String execute = new Chain<RiskChainContext, String>(
                Arrays.asList(
                        context -> ChainResult.keepRunning(),
                        context -> ChainResult.keepRunning())
        ).execute(riskChainContext);

        System.out.println("chain result:" + execute);
    }

    //spring用法
    @Bean("requestChain")
    public Chain<RiskChainContext, String> requestChain(ApplicationContext applicationContext) {
        //构建chain 将需要的chainHandler注册到chain上
        List<ChainHandler<RiskChainContext, String>> chainHandlers = new ArrayList<>();
        chainHandlers.add(applicationContext.getBean(RiskChainHandler.class));
        return new Chain<>(chainHandlers);
    }



    @Component
    public static class RiskChainHandler implements ChainHandler<RiskChainContext,String>{

        @Override
        public ChainResult<String> doChain(RiskChainContext context) {
            return ChainResult.finish("finish");
        }
    }
}
