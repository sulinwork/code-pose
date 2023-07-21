package com.sulin.codepose.kit.chain;

import java.util.List;

public class Chain<C extends ChainContext, R> {

    private final List<? extends ChainHandler<C, R>> handlers;

    public Chain(List<? extends ChainHandler<C, R>> handlers) {
        this.handlers = handlers;
    }

    public R execute(C context) {
        for (ChainHandler<C, R> handler : handlers) {
            final ChainResult<R> result = handler.doChain(context);

            if (!result.isExecuteNextHandlerMark()) {
                return result.getResult();
            }
        }
        throw new NoChainHandlerProcessException();
    }
}
