package com.parking.interceptor;

import com.parking.interceptor.AuthenticationInterceptor;
import com.parking.mapper.AccessLogMapper;
import com.parking.model.AccessLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AccessLogInterceptor 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AccessLogInterceptorTest {

    @Mock
    private AccessLogMapper accessLogMapper;

    @InjectMocks
    private AccessLogInterceptor interceptor;

    @Test
    @DisplayName("preHandle - 记录请求开始时间")
    void preHandle_setsStartTime() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertNotNull(request.getAttribute("accessLog_startTime"));
    }

    @Test
    @DisplayName("afterCompletion - 正常记录访问日志")
    void afterCompletion_recordsLog() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/vehicles");
        request.setQueryString("communityId=1001");
        request.setAttribute("accessLog_startTime", System.currentTimeMillis() - 50);
        request.addHeader("User-Agent", "TestAgent/1.0");
        // 模拟认证拦截器设置的请求属性
        request.setAttribute(AuthenticationInterceptor.ATTR_USER_ID, 100L);
        request.setAttribute(AuthenticationInterceptor.ATTR_USER_ROLE, "property_admin");
        request.setAttribute(AuthenticationInterceptor.ATTR_COMMUNITY_ID, 1001L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(accessLogMapper).insert(captor.capture());
        AccessLog log = captor.getValue();
        assertEquals("/api/v1/vehicles", log.getApiPath());
        assertEquals("GET", log.getHttpMethod());
        assertEquals("communityId=1001", log.getQueryParams());
        assertEquals(200, log.getResponseCode());
        assertEquals("TestAgent/1.0", log.getUserAgent());
        assertEquals(100L, log.getUserId());
        assertEquals("property_admin", log.getUserRole());
        assertEquals(1001L, log.getCommunityId());
        assertNotNull(log.getResponseTime());
        assertTrue(log.getResponseTime() >= 0);
    }

    @Test
    @DisplayName("afterCompletion - X-Forwarded-For 获取真实 IP")
    void afterCompletion_xForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/parking/entry");
        request.setAttribute("accessLog_startTime", System.currentTimeMillis());
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(accessLogMapper).insert(captor.capture());
        assertEquals("192.168.1.100", captor.getValue().getUserIp());
    }

    @Test
    @DisplayName("afterCompletion - 日志写入失败不影响主流程")
    void afterCompletion_insertFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reports");
        request.setAttribute("accessLog_startTime", System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new RuntimeException("数据库异常")).when(accessLogMapper).insert(any());

        // 不应抛出异常
        assertDoesNotThrow(() ->
                interceptor.afterCompletion(request, response, new Object(), null));
    }

    @Test
    @DisplayName("afterCompletion - 无用户信息时字段为空")
    void afterCompletion_noUserInfo() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        request.setAttribute("accessLog_startTime", System.currentTimeMillis());
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<AccessLog> captor = ArgumentCaptor.forClass(AccessLog.class);
        verify(accessLogMapper).insert(captor.capture());
        AccessLog log = captor.getValue();
        assertNull(log.getUserId());
        assertNull(log.getUserName());
        assertNull(log.getCommunityId());
    }
}
