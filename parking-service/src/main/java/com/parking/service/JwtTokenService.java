package com.parking.service;

import io.jsonwebtoken.Claims;

/**
 * JWT Token 管理服务接口
 * 负责 Access Token 和 Refresh Token 的生成、验证、撤销
 * Validates: Requirements 13.1, 13.2, 13.3
 */
public interface JwtTokenService {

    /**
     * 生成 Access Token（有效期2小时）
     * 包含 userId, role, communityId, houseNo
     */
    String generateAccessToken(Long userId, String role, Long communityId, String houseNo);

    /**
     * 生成 Refresh Token（有效期7天）
     */
    String generateRefreshToken(Long userId);

    /**
     * 验证 Token 有效性，返回解析后的 Claims
     * 验证失败时抛出 BusinessException
     */
    Claims validateToken(String token);

    /**
     * 撤销 Token，将其加入黑名单
     */
    void revokeToken(String token);
}
