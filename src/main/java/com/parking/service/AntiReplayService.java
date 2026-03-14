package com.parking.service;

/**
 * 防重放服务接口
 * 负责验证请求的 timestamp 和 nonce，防止重放攻击
 * Validates: Requirements 19.5, 19.6, 19.7, 19.8
 */
public interface AntiReplayService {

    /**
     * 验证请求时间戳是否在有效窗口内（5分钟）
     * 超出窗口抛出 BusinessException（PARKING_19001）
     *
     * @param timestamp 请求时间戳（毫秒级）
     */
    void validateTimestamp(String timestamp);

    /**
     * 验证 nonce 是否已被使用，并将新 nonce 存入 Redis（5分钟过期）
     * 重复 nonce 抛出 BusinessException（PARKING_19002）
     *
     * @param nonce 防重放随机数
     */
    void validateNonce(String nonce);

    /**
     * 执行完整的防重放验证（timestamp + nonce）
     * 依次验证 timestamp 和 nonce，任一失败则抛出对应 BusinessException
     *
     * @param timestamp 请求时间戳（毫秒级）
     * @param nonce     防重放随机数
     */
    void validate(String timestamp, String nonce);
}
