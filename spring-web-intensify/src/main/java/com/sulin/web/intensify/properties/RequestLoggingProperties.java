package com.sulin.web.intensify.properties;

import com.sulin.web.intensify.utils.PathMatchUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
public class RequestLoggingProperties {
    private static final String PATH_SPILT = "#";

    private boolean enable = Boolean.FALSE;

    private int requestMaxLen = 100;
    private int responseMaxLen = 100;

    private List<String> excludeRequestMethods;

    private List<String> excludeRequestPaths;


    public boolean hitExcludeRequestMethod(String requestMethod) {
        return !CollectionUtils.isEmpty(excludeRequestMethods) && excludeRequestMethods.stream().anyMatch(e -> StringUtils.equalsIgnoreCase(e, requestMethod));
    }

    public boolean hitExcludeRequestPath(String requestMethod, String path) {
        if (CollectionUtils.isEmpty(excludeRequestPaths)) return false;
        for (String excludeRequestPath : excludeRequestPaths) {
            String[] split = excludeRequestPath.split(PATH_SPILT);
            String excludeMethod = split[0];
            String excludePath = split[2];
            if (!StringUtils.equalsIgnoreCase(excludeMethod, requestMethod)) {
                continue;
            }
            if (!PathMatchUtil.match(excludePath, path)) {
                continue;
            }
            return true;
        }
        return false;
    }
}
