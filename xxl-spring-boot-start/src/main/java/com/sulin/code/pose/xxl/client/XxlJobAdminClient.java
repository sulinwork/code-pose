package com.sulin.code.pose.xxl.client;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.lang.Validator;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.sulin.code.pose.xxl.config.XxlConfig;
import com.sulin.code.pose.xxl.executor.XxlJobAutoRegisterSpringExecutor;

import java.net.HttpCookie;
import java.util.List;
import java.util.Optional;

/**
 * 和xxl admin交互的类
 */
public class XxlJobAdminClient {

    private final XxlConfig xxlConfig;

    private final String COOKIE_KEY = "XXL_JOB_LOGIN_IDENTITY";

    public XxlJobAdminClient(XxlConfig xxlConfig) {
        this.xxlConfig = xxlConfig;
    }

    private String cookie;

    public boolean login() {
        String url = xxlConfig.getAdminAddress() + "/login";
        HttpRequest httpRequest = HttpRequest.post(url)
                .form("userName", xxlConfig.getUsername())
                .form("password", xxlConfig.getPassword())
                .timeout(3000);
        try (HttpResponse response = httpRequest.execute()) {
            Validator.validateTrue(response.isOk(), "xxl-job管理后台登录失败");
            List<HttpCookie> cookies = response.getCookies();
            Optional<HttpCookie> cookieOpt = cookies.stream()
                    .filter(cookie -> cookie.getName().equals(COOKIE_KEY)).findFirst();
            this.cookie = COOKIE_KEY + "=" + cookieOpt.orElseThrow(() -> new ValidateException("xxl-job管理后台cookie获取失败")).getValue();
        }
        return true;
    }

    public Long getJobGroupIfPresent() {
        return null;
    }

    public Long getJobGroupId() {
        String url =  xxlConfig.getAdminAddress() + "/jobgroup/pageList";
        HttpRequest httpRequest = HttpRequest.post(url)
                .form("appname", xxlConfig.getAppName())
                .timeout(3000)
                .cookie(this.cookie);
        try (HttpResponse response = httpRequest.execute()) {
            Validator.validateTrue(response.isOk(), "获取当前应用job分组失败");
            String body = response.body();
//            JSONArray array = JSONUtil.parse(body).getByPath("data", JSONArray.class);
//            return array.stream()
//                    .map(o -> JSONUtil.toBean((JSONObject) o, XxlJobGroup.class))
//                    .filter(xxlJobGroup -> xxlJobGroup != null && Objects.equals(xxlJobGroup.getAppname(), appName))
//                    .findFirst().orElse(null);
        }
        return null;
    }

    public void createJobIfAbsent(Long groupId, XxlJobAutoRegisterSpringExecutor.JobInfo jobInfo) {

    }
}
