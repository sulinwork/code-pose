package com.sulin.web.intensify.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;

/**
 * 参考这里
 * org.springframework.web.filter.AbstractRequestLoggingFilter
 */
public class RequestLoggingFilter extends OncePerRequestFilter {
//    @Value("${c2c.web.logging.enable:true}")
    private boolean webLoggingEnabled;

//    @Value("${c2c.web.logging.request-message-max-len:1024}")
    private int requestMessageMaxLen;
//    @Value("${c2c.web.logging.response-message-max-len:1024}")
    private int responseMessageMaxLen;
//    @Value("${c2c.web.logging.ignore-paths:[]}")
    private Set<String> ignorePaths;

//    @Value("${c2c.web.logging.allow-methods:['POST']}")
    private Set<String> allowMethods;


    private boolean hitIgnorePaths(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return ignorePaths.contains(requestURI);
    }

    private boolean allowMethod(HttpServletRequest request) {
        return allowMethods.contains(request.getMethod());
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (hitIgnorePaths(request) || !allowMethod(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestToUse = request;
        HttpServletResponse responseToUse = response;

        if (isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
            requestToUse = new ContentCachingRequestWrapper(request);
        }
        if (isFirstRequest && !(response instanceof ContentCachingResponseWrapper)) {
            responseToUse = new ContentCachingResponseWrapper(response);
        }
        try {
            filterChain.doFilter(requestToUse, responseToUse);
        } finally {
            if (webLoggingEnabled && !isAsyncStarted(requestToUse)) {
                //打印日志
                String requestMessage = getRequestMessage(requestToUse);
                String responseMessage = getResponseMessage(responseToUse);
//                log.info("request:{},response:{}",
//                        StringUtils.left(requestMessage, requestMessageMaxLen),
//                        StringUtils.left(responseMessage, responseMessageMaxLen));
            }
            if (responseToUse instanceof ContentCachingResponseWrapper) {
                ((ContentCachingResponseWrapper) responseToUse).copyBodyToResponse();
            }
        }
    }

    private String getResponseMessage(HttpServletResponse responseToUse) {
        StringBuilder msg = new StringBuilder();
        msg.append("[");
        if (responseToUse instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) responseToUse;
            return new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        }
        msg.append("]");
        return msg.toString();
    }

    private String getRequestMessage(HttpServletRequest request) {
        StringBuilder msg = new StringBuilder();
        msg.append("[");
        msg.append(request.getMethod()).append(' ');
        msg.append(request.getRequestURI());
        //print query params
        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }
        //print header
        HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String header = names.nextElement();
            if (!header.toUpperCase().startsWith("X-XF")) {
                headers.remove(header);
            }
        }

        msg.append(", headers=").append(headers);
        //print payload
        if (request instanceof ContentCachingRequestWrapper) {
            byte[] contentAsByteArray = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
            String payload = new String(contentAsByteArray, StandardCharsets.UTF_8);
            msg.append(", payload=").append(payload);
        }
        msg.append("]");
        return msg.toString();
    }

}
