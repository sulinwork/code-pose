package com.sulin.codepose.event.utils;

import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * 获取Bean工具类
 */
@Component
public class SpringContextUtils implements BeanFactoryPostProcessor {

    private static ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        SpringContextUtils.beanFactory = configurableListableBeanFactory;
    }

    public static <T> T getBean(String name) throws BeansException {
        return (T) beanFactory.getBean(name);
    }

    public static <T> T getBean(Class<T> clz) throws BeansException {
        return beanFactory.getBean(clz);
    }

    public static <T> List<T> getBeansOfType(Class<T> type) {
        return Lists.newArrayList(beanFactory.getBeansOfType(type).values());
    }

    public static List<Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        return Lists.newArrayList(beanFactory.getBeansWithAnnotation(annotationType).values());
    }
}
