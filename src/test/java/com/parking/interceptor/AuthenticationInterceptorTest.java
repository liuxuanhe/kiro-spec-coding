package com.parking.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import com.parking.service.AntiReplayService;
import com.parking.service.JwtTokenService;
import com.parking.service.SignatureService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthenticationInterceptor 单元测试
 * Validates: Requirements 19.4, 19.5, 18.3, 18.4
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private SignatureService signatureService;

    @Mock
    private AntiReplayService antiReplayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuthenticationInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new AuthenticationInterceptor(
                jwtTokenService, signatureService, antiReplayService, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    /**
     * 构建包含用户信息的 Claims
     */
    private Claims buildClaims(Long userId, String role, Long communityId, String houseNo) {
        Map<String, Object> map = new HashMap<>();
        map.put("sub", String.valueOf(userId));
        map.put("role", role);
        map.put("communityId", communityId);
        map.put("houseNo", houseNo);
        return new DefaultClaims(map);
    }

    /**
     * 设置完整的认证请求头
     */
    private void setUpValidHeaders() {
        request.addHeader("Authorization", "Bearer valid_token");
        request.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        request.addHeader("X-Nonce", "test-nonce-123");
        request.addHeader("X-Signature", "valid-signature");
    }

    @Test
    @DisplayName("认证通过 - 所有验证均成功")
    void preHandle_allValidationsPass_returnsTrue() throws Exception {
        setUpValidHeaders();
        Claims claims = buildClaims(1001L, "owner", 1L, "1-101");
        when(jwtTokenService.validateToken("valid_token")).thenReturn(claims);
        doNothing().when(antiReplayService).validate(anyString(), anyString());
        doNothing().when(signatureService).verifySignature(anyString(), anyString(), anyString(), anyString());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(1001L, request.getAttribute(AuthenticationInterceptor.ATTR_USER_ID));
        assertEquals("owner", request.getAttribute(AuthenticationInterceptor.ATTR_USER_ROLE));
        assertEquals(1L, request.getAttribute(AuthenticationInterceptor.ATTR_COMMUNITY_ID));
        assertEquals("1-101", request.getAttribute(AuthenticationInterceptor.ATTR_HOUSE_NO));
    }

    @Test
    @DisplayName("认证失败 - 缺少 Authorization 请求头")
    void preHandle_missingAuthHeader_returnsFalse() throws Exception {
        request.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        request.addHeader("X-Nonce", "nonce");
        request.addHeader("X-Signature", "sig");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.UNAUTHORIZED.getCode())));
    }

    @Test
    @DisplayName("认证失败 - Authorization 格式错误（无 Bearer 前缀）")
    void preHandle_invalidAuthFormat_returnsFalse() throws Exception {
        request.addHeader("Authorization", "InvalidToken");
        request.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        request.addHeader("X-Nonce", "nonce");
        request.addHeader("X-Signature", "sig");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.UNAUTHORIZED.getCode())));
    }

    @Test
    @DisplayName("认证失败 - Bearer 后 Token 为空")
    void preHandle_emptyBearerToken_returnsFalse() throws Exception {
        request.addHeader("Authorization", "Bearer ");
        request.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        request.addHeader("X-Nonce", "nonce");
        request.addHeader("X-Signature", "sig");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.UNAUTHORIZED.getCode())));
    }

    @Test
    @DisplayName("认证失败 - JWT Token 已过期")
    void preHandle_expiredToken_returnsFalse() throws Exception {
        setUpValidHeaders();
        when(jwtTokenService.validateToken("valid_token"))
                .thenThrow(new BusinessException(ErrorCode.PARKING_13001));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.PARKING_13001.getCode())));
    }

    @Test
    @DisplayName("认证失败 - JWT Token 无效")
    void preHandle_invalidToken_returnsFalse() throws Exception {
        setUpValidHeaders();
        when(jwtTokenService.validateToken("valid_token"))
                .thenThrow(new BusinessException(ErrorCode.PARKING_13002));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.PARKING_13002.getCode())));
    }

    @Test
    @DisplayName("认证失败 - JWT Token 已被撤销")
    void preHandle_revokedToken_returnsFalse() throws Exception {
        setUpValidHeaders();
        when(jwtTokenService.validateToken("valid_token"))
                .thenThrow(new BusinessException(ErrorCode.PARKING_13003));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.PARKING_13003.getCode())));
    }

    @Test
    @DisplayName("认证失败 - timestamp 超出有效窗口")
    void preHandle_invalidTimestamp_returnsFalse() throws Exception {
        setUpValidHeaders();
        Claims claims = buildClaims(1001L, "owner", 1L, "1-101");
        when(jwtTokenService.validateToken("valid_token")).thenReturn(claims);
        doThrow(new BusinessException(ErrorCode.PARKING_19001))
                .when(antiReplayService).validate(anyString(), anyString());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.PARKING_19001.getCode())));
    }

    @Test
    @DisplayName("认证失败 - nonce 已被使用")
    void preHandle_duplicateNonce_returnsFalse() throws Exception {
        setUpValidHeaders();
        Claims claims = buildClaims(1001L, "owner", 1L, "1-101");
        when(jwtTokenService.validateToken("valid_token")).thenReturn(claims);
        doThrow(new BusinessException(ErrorCode.PARKING_19002))
                .when(antiReplayService).validate(anyString(), anyString());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.PARKING_19002.getCode())));
    }

    @Test
    @DisplayName("认证失败 - 签名验证失败")
    void preHandle_invalidSignature_returnsFalse() throws Exception {
        setUpValidHeaders();
        Claims claims = buildClaims(1001L, "owner", 1L, "1-101");
        when(jwtTokenService.validateToken("valid_token")).thenReturn(claims);
        doNothing().when(antiReplayService).validate(anyString(), anyString());
        doThrow(new BusinessException(ErrorCode.PARKING_19003))
                .when(signatureService).verifySignature(anyString(), anyString(), anyString(), anyString());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        String body = response.getContentAsString();
        assertTrue(body.contains(String.valueOf(ErrorCode.PARKING_19003.getCode())));
    }

    @Test
    @DisplayName("验证顺序 - JWT 验证在防重放之前执行")
    void preHandle_validationOrder_jwtBeforeAntiReplay() throws Exception {
        setUpValidHeaders();
        when(jwtTokenService.validateToken("valid_token"))
                .thenThrow(new BusinessException(ErrorCode.PARKING_13002));

        interceptor.preHandle(request, response, new Object());

        verify(jwtTokenService).validateToken("valid_token");
        verifyNoInteractions(antiReplayService);
        verifyNoInteractions(signatureService);
    }

    @Test
    @DisplayName("验证顺序 - 防重放验证在签名验证之前执行")
    void preHandle_validationOrder_antiReplayBeforeSignature() throws Exception {
        setUpValidHeaders();
        Claims claims = buildClaims(1001L, "owner", 1L, "1-101");
        when(jwtTokenService.validateToken("valid_token")).thenReturn(claims);
        doThrow(new BusinessException(ErrorCode.PARKING_19001))
                .when(antiReplayService).validate(anyString(), anyString());

        interceptor.preHandle(request, response, new Object());

        verify(jwtTokenService).validateToken("valid_token");
        verify(antiReplayService).validate(anyString(), anyString());
        verifyNoInteractions(signatureService);
    }

    @Test
    @DisplayName("afterCompletion - 正常记录访问日志")
    void afterCompletion_logsAccessInfo() {
        request.setAttribute(AuthenticationInterceptor.ATTR_ACCESS_START_TIME, System.currentTimeMillis() - 100);
        request.setAttribute(AuthenticationInterceptor.ATTR_USER_ID, 1001L);
        request.setAttribute(AuthenticationInterceptor.ATTR_USER_ROLE, "owner");

        // 不应抛出异常
        assertDoesNotThrow(() ->
                interceptor.afterCompletion(request, response, new Object(), null));
    }

    @Test
    @DisplayName("afterCompletion - 无开始时间也不抛异常")
    void afterCompletion_noStartTime_doesNotThrow() {
        assertDoesNotThrow(() ->
                interceptor.afterCompletion(request, response, new Object(), null));
    }

    @Test
    @DisplayName("错误响应格式 - 返回标准 JSON 格式")
    void preHandle_errorResponse_isValidJson() throws Exception {
        // 不设置 Authorization 头
        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());

        String body = response.getContentAsString();
        assertNotNull(body);
        assertFalse(body.isEmpty());

        // 验证可以被解析为 JSON
        var jsonNode = objectMapper.readTree(body);
        assertTrue(jsonNode.has("code"));
        assertTrue(jsonNode.has("message"));
    }

    @Test
    @DisplayName("认证通过 - communityId 为 null 时正常处理")
    void preHandle_nullCommunityId_handlesGracefully() throws Exception {
        setUpValidHeaders();
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", "1001");
        claimsMap.put("role", "super_admin");
        claimsMap.put("houseNo", "");
        DefaultClaims claims = new DefaultClaims(claimsMap);
        when(jwtTokenService.validateToken("valid_token")).thenReturn(claims);
        doNothing().when(antiReplayService).validate(anyString(), anyString());
        doNothing().when(signatureService).verifySignature(anyString(), anyString(), anyString(), anyString());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(1001L, request.getAttribute(AuthenticationInterceptor.ATTR_USER_ID));
        assertEquals("super_admin", request.getAttribute(AuthenticationInterceptor.ATTR_USER_ROLE));
        assertNull(request.getAttribute(AuthenticationInterceptor.ATTR_COMMUNITY_ID));
    }
}
