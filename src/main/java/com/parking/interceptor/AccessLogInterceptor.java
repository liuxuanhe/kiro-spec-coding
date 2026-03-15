package com.parking.interceptor;

import com.parking.common.RequestContext;
import com.parking.mapper.AccessLogMapper;
import com.parking.model.AccessLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * 访问日志拦截器，记录所有接口访问到 sys_access_log 表。
 * Validates: Requirements 18.3, 18.4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogInterceptor implements HandlerInterceptor {

    private final AccessLogMapper accessLogMapper;

    private static final String ATTR_START_TIME = "accessLog_startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        try {
            AccessLog accessLog = new AccessLog();
            accessLog.setRequestId(RequestContext.getRequestId());
            accessLog.setAccessTime(LocalDateTime.now());
            accessLog.setApiPath(request.getRequestURI());
            accessLog.setHttpMethod(request.getMethod());
            accessLog.setQueryParams(request.getQueryString());
            accessLog.setUserIp(getClientIp(request));
            accessLog.setUserAgent(request.getHeader("User-Agent"));
            accessLog.setResponseCode(response.getStatus());

            // 计算响应时间（毫秒）
            Long startTime = (Long) request.getAttribute(ATTR_START_TIME);
            if (startTime != null) {
                accessLog.setResponseTime((int) (System.currentTimeMillis() - startTime));
            }

            // TODO: 从认证上下文获取用户信息
            String userId = request.getHeader("X-Operator-Id");
            if (userId != null) {
                accessLog.setUserId(Long.parseLong(userId));
            }
            accessLog.setUserName(request.getHeader("X-Operator-Name"));
            accessLog.setUserRole(request.getHeader("X-Operator-Role"));
            String communityId = request.getHeader("X-Community-Id");
            if (communityId != null) {
                accessLog.setCommunityId(Long.parseLong(communityId));
            }

            accessLogMapper.insert(accessLog);
        } catch (Exception e) {
            // 访问日志写入失败不影响主流程
            log.error("写入访问日志失败: requestId={}, path={}",
                    RequestContext.getRequestId(), request.getRequestURI(), e);
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
