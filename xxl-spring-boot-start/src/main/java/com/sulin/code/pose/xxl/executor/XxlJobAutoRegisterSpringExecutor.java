package com.sulin.code.pose.xxl.executor;

import com.sulin.code.pose.xxl.annotation.JobRegister;
import com.sulin.code.pose.xxl.client.XxlJobAdminClient;
import com.sulin.code.pose.xxl.config.XxlConfig;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class XxlJobAutoRegisterSpringExecutor extends XxlJobSpringExecutor {

    private final List<JobInfo> autoRegisterJobInfos = new ArrayList<>();

    @Resource
    private XxlConfig xxlConfig;

    @Override
    protected void registJobHandler(XxlJob xxlJob, Object bean, Method executeMethod) {
        super.registJobHandler(xxlJob, bean, executeMethod);
        try {
            JobRegister annotation = AnnotationUtils.findAnnotation(executeMethod, JobRegister.class);
            if (Objects.nonNull(annotation)) {
                autoRegisterJobInfos.add(new JobInfo(xxlJob, annotation, bean, executeMethod));
            }
        } catch (Exception e) {
            log.error("scan JobRegister.class Annotation error", e);
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        super.afterSingletonsInstantiated();
        //开始自动注册
        try {
            autoRegister();
        } catch (Exception e) {
            log.error("xxl job auto register exception", e);
        }
    }


    protected void autoRegister() {
        if (!StringUtils.hasText(xxlConfig.getUsername()) || !StringUtils.hasText(xxlConfig.getPassword())) {
            return;
        }
        XxlJobAdminClient xxlJobAdminClient = getApplicationContext().getBean(XxlJobAdminClient.class);
        boolean login = xxlJobAdminClient.login();
        if (!login) {
            log.error("xxl job admin login fail");
            return;
        }
        Long groupId = xxlJobAdminClient.getJobGroupIfPresent();
        for (JobInfo jobInfo : autoRegisterJobInfos) {
            try {
                xxlJobAdminClient.createJobIfAbsent(groupId, jobInfo);
            } catch (Exception e) {
                log.error("job name:{} auto register fail", jobInfo.getXxlJob().value());
            }
        }
    }


    @Data
    @AllArgsConstructor
    public static class JobInfo {
        private XxlJob xxlJob;
        private JobRegister jobRegister;
        private Object bean;
        private Method method;
    }
}
