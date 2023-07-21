package com.sulin.codepose.kit.chain;

/**
 * 链的每个handler
 * @param <P>
 * @param <R>
 */
public interface ChainHandler<P extends ChainContext,R>{
    ChainResult<R> doChain(P context);
}
