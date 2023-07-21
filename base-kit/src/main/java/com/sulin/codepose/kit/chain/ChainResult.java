package com.sulin.codepose.kit.chain;

import lombok.Data;

/**
 * 链的返回
 * @param <T>
 */
@Data
public class ChainResult<T> {
    /**
     * 是否执行下一个链
     */
    private boolean executeNextHandlerMark = true;

    /**
     * 结果
     */
    private T result;

    private ChainResult(T result) {
        this.executeNextHandlerMark = false;
        this.result = result;
    }

    private ChainResult() {
    }

    public static <T> ChainResult<T> finish(T result) {
        return new ChainResult<>(result);
    }
    public static <T> ChainResult<T> keepRunning() {
        return new ChainResult<>();
    }
}
