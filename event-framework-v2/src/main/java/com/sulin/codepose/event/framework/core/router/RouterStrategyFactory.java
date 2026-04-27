package com.sulin.codepose.event.framework.core.router;

import com.sulin.codepose.event.framework.api.router.RouterStrategy;
import com.sulin.codepose.kit.strategy.SimpleFactory;


public class RouterStrategyFactory extends SimpleFactory<String, RouterStrategy> {

    public RouterStrategyFactory() {
        super(RouterStrategy.class);
    }
}
