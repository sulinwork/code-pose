package com.sulin.web.intensify.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

public class PathMatchUtil {
    private static final PathMatcher pathMatcher = new AntPathMatcher();

    public static boolean match(String pattern,String requestPath){
        return pathMatcher.match(pattern, requestPath);
    }
}
