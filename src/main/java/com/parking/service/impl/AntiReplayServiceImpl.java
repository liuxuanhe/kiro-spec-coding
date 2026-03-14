package com.parking.service.impl;

import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import com.parking.service.AntiReplayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 防重放服务实现类
 * 通过验证 timestamp 时间窗口和 nonce 唯一性来防止重放攻击
 * - timestamp 验证：请求时间戳必须在当前时间的5分钟窗口内
 * - nonce 验证：使用 Redis 存储已使用的 nonce，5分钟过期
 * Validates: Requirements 19.5, 19.6, 19.7, 19.8
 */
@Slf4j
@Service
public class AntiReplayServiceImpl implements AntiReplayService {

    /** 时间窗口：5分钟（毫秒） */
    private static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;

    /** nonce 在 Redis 中的键前缀 */
    private static final String NONCE_KEY_PREFIX = "anti_replay:nonce:";

    /** nonce 过期时间：5分钟 */
    private static final Duration NONCE_EXPIRE_DURATION = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;

    public AntiReplayServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void validateTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            log.warn("请求时间戳为空");
            throw new BusinessException(ErrorCode.PARKING_19001);
        }

        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("请求时间戳格式无效: {}", timestamp);
            throw new BusinessException(ErrorCode.PARKING_19001);
        }

        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - requestTime);

        if (diff > TIMESTAMP_WINDOW_MS) {
            log.warn("请求时间戳超出有效窗口, timestamp: {}, currentTime: {}, diff: {}ms",
                    timestamp, currentTime, diff);
            throw new BusinessException(ErrorCode.PARKING_19001);
        }

        log.debug("时间戳验证通过, timestamp: {}, diff: {}ms", timestamp, diff);
    }

    @Override
    public void validateNonce(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            log.warn("nonce 为空");
            throw new BusinessException(ErrorCode.PARKING_19002);
        }

        String key = NONCE_KEY_PREFIX + nonce;

        // 使用 setIfAbsent 原子操作：如果 key 不存在则设置并返回 true，否则返回 false
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", NONCE_EXPIRE_DURATION);

        if (Boolean.FALSE.equals(success)) {
            log.warn("nonce 已被使用: {}", nonce);
            throw new BusinessException(ErrorCode.PARKING_19002);
        }

        log.debug("nonce 验证通过: {}", nonce);
    }

    @Override
    public void validate(String timestamp, String nonce) {
        validateTimestamp(timestamp);
        validateNonce(nonce);
    }
}
