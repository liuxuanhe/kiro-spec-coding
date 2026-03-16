package com.parking.service;

import com.parking.common.BusinessException;
import com.parking.service.impl.JwtTokenServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JwtTokenService 单元测试
 * Validates: Requirements 13.1, 13.2, 13.3
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    private static final String SECRET_KEY = "YTJiM2M0ZDVlNmY3ZzhoOWkwajFrMmwzbTRuNW82cDdxOHI5czB0MXUydjN3";
    private static final long ACCESS_TOKEN_EXPIRATION = 7200000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private JwtTokenServiceImpl jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(jwtTokenService, "secretKeyStr", SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
        jwtTokenService.init();
    }

    @Test
    @DisplayName("生成 Access Token 应包含正确的 Claims")
    void generateAccessToken_shouldContainCorrectClaims() {
        Long userId = 10001L;
        String role = "owner";
        Long communityId = 1001L;
        String houseNo = "1-101";

        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        String token = jwtTokenService.generateAccessToken(userId, role, communityId, houseNo);
        assertNotNull(token);

        Claims claims = jwtTokenService.validateToken(token);
        assertEquals(String.valueOf(userId), claims.getSubject());
        assertEquals(role, claims.get("role", String.class));
        assertEquals(communityId.intValue(), claims.get("communityId", Integer.class));
        assertEquals(houseNo, claims.get("houseNo", String.class));
        assertEquals("access", claims.get("tokenType", String.class));
        assertNotNull(claims.getId());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("Access Token 有效期应为2小时")
    void generateAccessToken_shouldHaveCorrectExpiration() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        String token = jwtTokenService.generateAccessToken(1L, "owner", 1L, "1-101");
        Claims claims = jwtTokenService.validateToken(token);

        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(ACCESS_TOKEN_EXPIRATION, diff);
    }

    @Test
    @DisplayName("生成 Refresh Token 应包含正确的 Claims")
    void generateRefreshToken_shouldContainCorrectClaims() {
        Long userId = 10001L;

        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        String token = jwtTokenService.generateRefreshToken(userId);
        assertNotNull(token);

        Claims claims = jwtTokenService.validateToken(token);
        assertEquals(String.valueOf(userId), claims.getSubject());
        assertEquals("refresh", claims.get("tokenType", String.class));
        assertNotNull(claims.getId());
    }

    @Test
    @DisplayName("Refresh Token 有效期应为7天")
    void generateRefreshToken_shouldHaveCorrectExpiration() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        String token = jwtTokenService.generateRefreshToken(1L);
        Claims claims = jwtTokenService.validateToken(token);

        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(REFRESH_TOKEN_EXPIRATION, diff);
    }

    @Test
    @DisplayName("验证过期 Token 应抛出 BusinessException（PARKING_13001）")
    void validateToken_expired_shouldThrowException() {
        // 构造一个已过期的 Token
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("1")
                .issuedAt(new Date(now.getTime() - 10000))
                .expiration(new Date(now.getTime() - 5000))
                .signWith(key)
                .compact();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtTokenService.validateToken(expiredToken));
        assertEquals(13001, ex.getCode());
    }

    @Test
    @DisplayName("验证无效 Token 应抛出 BusinessException（PARKING_13002）")
    void validateToken_invalid_shouldThrowException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtTokenService.validateToken("invalid.token.here"));
        assertEquals(13002, ex.getCode());
    }

    @Test
    @DisplayName("验证已撤销的 Token 应抛出 BusinessException（PARKING_13003）")
    void validateToken_revoked_shouldThrowException() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        String token = jwtTokenService.generateAccessToken(1L, "owner", 1L, "1-101");
        Claims claims = jwtTokenService.validateToken(token);
        String jti = claims.getId();

        // 模拟 Token 已在黑名单中
        when(redisTemplate.hasKey("jwt:blacklist:" + jti)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtTokenService.validateToken(token));
        assertEquals(13003, ex.getCode());
    }

    @Test
    @DisplayName("撤销 Token 应将其加入 Redis 黑名单")
    void revokeToken_shouldAddToBlacklist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = jwtTokenService.generateAccessToken(1L, "owner", 1L, "1-101");
        jwtTokenService.revokeToken(token);

        verify(valueOperations).set(
                argThat(key -> key.startsWith("jwt:blacklist:")),
                eq("revoked"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("撤销已过期的 Token 不应报错")
    void revokeToken_expired_shouldNotThrow() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("1")
                .issuedAt(new Date(now.getTime() - 10000))
                .expiration(new Date(now.getTime() - 5000))
                .signWith(key)
                .compact();

        assertDoesNotThrow(() -> jwtTokenService.revokeToken(expiredToken));
    }

    @Test
    @DisplayName("每次生成的 Token 应具有唯一的 jti")
    void generateToken_shouldHaveUniqueJti() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        String token1 = jwtTokenService.generateAccessToken(1L, "owner", 1L, "1-101");
        String token2 = jwtTokenService.generateAccessToken(1L, "owner", 1L, "1-101");

        Claims claims1 = jwtTokenService.validateToken(token1);
        Claims claims2 = jwtTokenService.validateToken(token2);

        assertNotEquals(claims1.getId(), claims2.getId());
    }
}
