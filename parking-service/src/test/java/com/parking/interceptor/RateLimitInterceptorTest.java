package com.parking.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitInterceptor 单元测试
 * 验证 IP 级限流功能
 */
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RateLimitInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(redisTemplate, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("普通接口未超限时放行")
    void shouldAllowWhenNotExceedingLimit() throws Exception {
        request.setRequestURI("/api/v1/vehicles");
        request.setRemoteAddr("192.168.1.100");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("普通接口超过每分钟60次限制时拒绝")
    void shouldRejectWhenExceedingDefaultLimit() throws Exception {
        request.setRequestURI("/api/v1/vehicles");
        request.setRemoteAddr("192.168.1.100");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(61L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getContentAsString().contains("19004"));
    }

    @Test
    @DisplayName("注册接口超过每小时10次限制时拒绝")
    void shouldRejectRegisterWhenExceedingLimit() throws Exception {
        request.setRequestURI("/api/v1/owners/register");
        request.setRemoteAddr("10.0.0.1");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(11L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertTrue(response.getContentAsString().contains("19004"));
    }

    @Test
    @DisplayName("注册接口未超限时放行")
    void shouldAllowRegisterWhenWithinLimit() throws Exception {
        request.setRequestURI("/api/v1/owners/register");
        request.setRemoteAddr("10.0.0.1");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(10L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("登录接口超过每小时20次限制时拒绝")
    void shouldRejectLoginWhenExceedingLimit() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("172.16.0.1");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(21L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertTrue(response.getContentAsString().contains("19004"));
    }

    @Test
    @DisplayName("登录接口未超限时放行")
    void shouldAllowLoginWhenWithinLimit() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("172.16.0.1");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(20L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("通过 X-Forwarded-For 获取客户端真实 IP")
    void shouldUseXForwardedForHeader() throws Exception {
        request.setRequestURI("/api/v1/vehicles");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
        request.setRemoteAddr("127.0.0.1");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(contains("203.0.113.50"))).thenReturn(1L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        // 验证使用了 X-Forwarded-For 中的第一个 IP
        verify(valueOperations).increment(contains("203.0.113.50"));
    }

    @Test
    @DisplayName("通过 X-Real-IP 获取客户端真实 IP")
    void shouldUseXRealIpHeader() throws Exception {
        request.setRequestURI("/api/v1/vehicles");
        request.addHeader("X-Real-IP", "198.51.100.25");
        request.setRemoteAddr("127.0.0.1");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(contains("198.51.100.25"))).thenReturn(1L);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(valueOperations).increment(contains("198.51.100.25"));
    }

    @Test
    @DisplayName("Redis 异常时放行请求")
    void shouldAllowWhenRedisException() throws Exception {
        request.setRequestURI("/api/v1/vehicles");
        request.setRemoteAddr("192.168.1.100");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis 连接失败"));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    @DisplayName("首次请求设置过期时间")
    void shouldSetExpireOnFirstRequest() throws Exception {
        request.setRequestURI("/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.1");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        interceptor.preHandle(request, response, new Object());

        // 登录接口窗口为 3600 秒
        verify(redisTemplate).expire(anyString(), eq(3600L), eq(TimeUnit.SECONDS));
    }
}
