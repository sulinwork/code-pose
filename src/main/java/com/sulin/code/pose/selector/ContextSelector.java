package com.sulin.code.pose.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 选择器
 * <pre>
 *    int param = 3;
 *    // if() return if() return
 *    String result = ContextSelector.init(param, o -> "4")
 *             .register(() -> new ContextSelector.Context<>(o -> o == 1, o -> "1"))
 *             .register(() -> new ContextSelector.Context<>(o -> o == 2, o -> "2"))
 *             .register(() -> new ContextSelector.Context<>(o -> o == 3, o -> "3"))
 *             .execute();
 *    // if()...if()...
 *    String result = ContextSelector.init(param, o -> "4")
 *          .register(() -> new ContextSelector.Context<>(o -> o == 1, o -> "1"))
 *          .register(() -> new ContextSelector.Context<>(o -> o == 2, o -> "2"))
 *          .register(() -> new ContextSelector.Context<>(o -> o == 3, o -> "3"))
 *          .pipeline();
 * </pre>
 */
public class ContextSelector<C, R> {

    /**
     * 参数
     */
    private C param;

    /**
     * 选择的上下文结果集
     */
    private List<Context<C, R>> contexts;

    /**
     * 默认结果
     */
    private Function<C, R> result;

    private ContextSelector() {
    }

    /**
     * 初始化,无默认返回结果
     *
     * @param param 参数
     * @param <C>   参数类型
     * @param <R>   结果类型
     * @return 选择器
     */
    public static <C, R> ContextSelector<C, R> init(final C param) {
        ContextSelector<C, R> converter = new ContextSelector<>();
        converter.param = param;
        return converter;
    }

    /**
     * 初始化
     *
     * @param param         参数
     * @param defaultResult 默认结果
     * @param <C>           参数类型
     * @param <R>           结果类型
     * @return 选择器
     */
    public static <C, R> ContextSelector<C, R> init(final C param, final Function<C, R> defaultResult) {
        ContextSelector<C, R> converter = new ContextSelector<>();
        converter.param = param;
        converter.result = defaultResult;
        return converter;
    }

    /**
     * 注册选择的上下文
     *
     * @param context 上下文
     * @return 选择器
     */
    public synchronized ContextSelector<C, R> register(final Predicate<C> condition, final Function<C, R> result) {
        if (contexts == null) {
            contexts = new ArrayList<>();
        }
        contexts.add(new Context<>(condition,result));
        return this;
    }

    /**
     * 执行选择器
     *
     * @return 执行结果
     */
    public R execute() {
        List<Context<C, R>> contexts = this.contexts;
        for (final Context<C, R> context : contexts) {
            if (context.condition.test(this.param)) {
                return context.result.apply(this.param);
            }
        }
        return Optional.ofNullable(result).map(result -> result.apply(this.param)).orElse(null);
    }

    /**
     * 执行管道
     *
     * @return 执行结果
     */
    public List<R> pipeline() {
        List<R> result = new ArrayList<>();
        List<Context<C, R>> contexts = this.contexts;
        for (final Context<C, R> context : contexts) {
            if (context.condition.test(this.param)) {
                result.add(context.result.apply(this.param));
            }
        }
        Optional.ofNullable(this.result).map(o -> o.apply(this.param)).ifPresent(result::add);
        return result;
    }


    public static class Context<C, R> {

        /**
         * 条件
         */
        Predicate<C> condition;

        /**
         * 结果
         */
        Function<C, R> result;

        private Context() {
        }

        public Context(final Predicate<C> condition, final Function<C, R> result) {
            this.condition = condition;
            this.result = result;
        }
    }

    private static <C, R> Context<C, R> nullSafeGet(Supplier<Context<C, R>> messageSupplier) {
        return messageSupplier != null ? messageSupplier.get() : null;
    }

}
