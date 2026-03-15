package com.parking.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.ApiResponse;
import com.parking.common.ErrorCode;
import com.parking.common.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * IP 级限流拦截器
 * 使用 Redis 计数器实现滑动窗口限流
 * 注册接口：每个 IP 每小时最多 10 次
 * 登录接口：每个 IP 每小时最多 20 次
 * 其他接口：每个 IP 每分钟最多 60 次
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:ip:";

    /** 限流规则：路径前缀 -> {limit, windowSeconds} */
    private static final Map<String, int[]> RATE_LIMIT_RULES = Map.of(
        "/api/v1/owners/register", new int[]{10, 3600},
        "/api/v1/verification-code", new int[]{10, 3600},
        "/api/v1/auth/login", new int[]{20, 3600}
    );

    /** 默认限流：每个 IP 每分钟 60 次 */
    private static final int DEFAULT_LIMIT = 60;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RedisTemplate<String, Object> redisTemplate,
                                ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String ip = getClientIp(request);
        String path = request.getRequestURI();

        // 匹配限流规则
        int[] rule = matchRule(path);
        int limit = rule[0];
        int windowSeconds = rule[1];

        String cacheKey = buildCacheKey(ip, path, windowSeconds);

        if (isRateLimited(cacheKey, limit, windowSeconds)) {
            log.warn("IP 级限流触发: ip={}, path={}, limit={}/{}s", ip, path, limit, windowSeconds);
            writeErrorResponse(response);
            return false;
        }

        return true;
    }

    /**
     * 匹配限流规则，返回 {limit, windowSeconds}
     */
    private int[] matchRule(String path) {
        for (Map.Entry<String, int[]> entry : RATE_LIMIT_RULES.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new int[]{DEFAULT_LIMIT, DEFAULT_WINDOW_SECONDS};
    }

    /**
     * 构建限流缓存键
     * 格式: rate_limit:ip:{ip}:{ruleKey}:{windowId}
     */
    private String buildCacheKey(String ip, String path, int windowSeconds) {
        String ruleKey = RATE_LIMIT_RULES.containsKey(path) ? path : "default";
        long windowId = System.currentTimeMillis() / (windowSeconds * 1000L);
        return RATE_LIMIT_PREFIX + ip + ":" + ruleKey + ":" + windowId;
    }

    /**
     * 判断是否触发限流
     * 使用 Redis INCR + EXPIRE 实现计数器
     */
    private boolean isRateLimited(String cacheKey, int limit, int windowSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(cacheKey);
            if (count != null && count == 1) {
                // 首次计数，设置过期时间
                redisTemplate.expire(cacheKey, windowSeconds, TimeUnit.SECONDS);
            }
            return count != null && count > limit;
        } catch (Exception e) {
            log.error("限流计数器异常，放行请求: cacheKey={}", cacheKey, e);
            // Redis 异常时放行，避免影响正常业务
            return false;
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // 取第一个 IP（客户端真实 IP）
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 写入限流错误响应
     */
    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(
                ErrorCode.PARKING_19004, RequestContext.getRequestId());

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
