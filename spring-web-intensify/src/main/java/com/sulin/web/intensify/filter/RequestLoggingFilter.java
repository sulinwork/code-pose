package com.sulin.web.intensify.filter;

import com.sulin.web.intensify.properties.RequestLoggingProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 参考这里
 * org.springframework.web.filter.AbstractRequestLoggingFilter
 */
@AllArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {


    private RequestLoggingProperties requestLoggingProperties;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestMethod = request.getMethod();
        String requestURI = request.getRequestURI();
        if (!requestLoggingProperties.isEnable() && requestLoggingProperties.hitExcludeRequestMethod(requestMethod) || requestLoggingProperties.hitExcludeRequestPath(requestMethod, requestURI)) {
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
            if (!isAsyncStarted(requestToUse)) {
                //打印日志
                String requestMessage = getRequestMessage(requestToUse);
                String responseMessage = getResponseMessage(responseToUse);
                log.info("request:{},response:{}",
                        StringUtils.left(requestMessage, requestLoggingProperties.getRequestMaxLen()),
                        StringUtils.left(responseMessage, requestLoggingProperties.getResponseMaxLen()));
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
//        //print header
//        HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
//        Enumeration<String> names = request.getHeaderNames();
//        while (names.hasMoreElements()) {
//            String header = names.nextElement();
//            if (!header.toUpperCase().startsWith("X-XF")) {
//                headers.remove(header);
//            }
//        }
//
//        msg.append(", headers=").append(headers);
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
