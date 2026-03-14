package com.parking.service.impl;

import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import com.parking.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token 管理服务实现类
 * 使用 JJWT 库生成和验证 Token，使用 Redis 存储撤销的 Token（黑名单机制）
 * Validates: Requirements 13.1, 13.2, 13.3
 */
@Slf4j
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    /** Redis 中 Token 黑名单的键前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    @Value("${jwt.secret-key}")
    private String secretKeyStr;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RedisTemplate<String, Object> redisTemplate;

    private SecretKey secretKey;

    public JwtTokenServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyStr.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(Long userId, String role, Long communityId, String houseNo) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("communityId", communityId)
                .claim("houseNo", houseNo)
                .claim("tokenType", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("tokenType", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Claims validateToken(String token) {
        // 先检查 Token 是否已被撤销
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            throw new BusinessException(ErrorCode.PARKING_13001);
        } catch (JwtException e) {
            log.warn("Token 无效: {}", e.getMessage());
            throw new BusinessException(ErrorCode.PARKING_13002);
        }

        // 检查黑名单
        String jti = claims.getId();
        if (jti != null && isTokenRevoked(jti)) {
            log.warn("Token 已被撤销, jti: {}", jti);
            throw new BusinessException(ErrorCode.PARKING_13003);
        }

        return claims;
    }

    @Override
    public void revokeToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (jti == null) {
                log.warn("Token 缺少 jti，无法撤销");
                return;
            }

            // 计算剩余有效时间，将 Token 加入黑名单直到其自然过期
            long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMillis > 0) {
                String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
                redisTemplate.opsForValue().set(blacklistKey, "revoked", remainingMillis, TimeUnit.MILLISECONDS);
                log.info("Token 已撤销, jti: {}, 剩余有效时间: {}ms", jti, remainingMillis);
            }
        } catch (ExpiredJwtException e) {
            // Token 已过期，无需撤销
            log.info("Token 已过期，无需撤销");
        } catch (JwtException e) {
            log.warn("撤销 Token 时解析失败: {}", e.getMessage());
        }
    }

    /**
     * 检查 Token 是否已被撤销（在黑名单中）
     */
    private boolean isTokenRevoked(String jti) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
}
