package com.parking.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RequestIdInterceptor implements HandlerInterceptor {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = generateRequestId();
        request.setAttribute(REQUEST_ID_KEY, requestId);
        MDC.put(REQUEST_ID_KEY, requestId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(REQUEST_ID_KEY);
    }

    private String generateRequestId() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "req_" + timestamp + "_" + uuid;
    }
}
