package com.sulin.codepose.kit.spel;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

public class SpElUtils {

    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 解析SpEL
     *
     * @param method 方法
     * @param args   参数
     * @param spEl   spEL表达式
     * @return 解析的结果
     */
    public static String parseSpEl(Method method, Object[] args, String spEl) {
        //获取方法上的形参名称
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames == null) return null;
        EvaluationContext context = new StandardEvaluationContext();//el解析需要的上下文对象
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        Expression expression = parser.parseExpression(spEl);
        return expression.getValue(context, String.class);
    }
}
