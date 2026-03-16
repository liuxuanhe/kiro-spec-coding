package com.parking.service;

/**
 * 验证码服务接口
 * 提供验证码发送和校验功能，支持失败次数统计和锁定机制
 * Validates: Requirements 1.2, 1.3
 */
public interface VerificationCodeService {

    /**
     * 发送验证码
     * 生成6位随机数字验证码，存入 Redis（key: verification_code:{phone}，5分钟过期）
     * 实际短信发送预留接口（当前仅 log 记录）
     *
     * @param phone 手机号
     */
    void send(String phone);

    /**
     * 验证码校验
     * 从 Redis 获取验证码并比对，验证成功后删除验证码
     * 失败次数达到3次后锁定10分钟，返回 PARKING_1001
     * 验证码过期返回 PARKING_1002
     *
     * @param phone 手机号
     * @param code  验证码
     * @return true 表示验证成功，false 表示验证失败
     */
    boolean verify(String phone, String code);
}
