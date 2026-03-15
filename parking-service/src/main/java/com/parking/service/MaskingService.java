package com.parking.service;

/**
 * 数据脱敏服务接口
 * 提供手机号、身份证号等敏感数据的脱敏处理
 * Validates: Requirements 17.1, 17.2, 17.8
 */
public interface MaskingService {

    /**
     * 手机号脱敏
     * 保留前3位和后4位，中间用****替换
     * 示例: "13812345678" → "138****5678"
     *
     * @param phoneNumber 原始手机号
     * @return 脱敏后的手机号；输入为 null 或空字符串时返回原值
     */
    String maskPhoneNumber(String phoneNumber);

    /**
     * 身份证号脱敏
     * 仅显示后4位，其余用*替换
     * 示例: "110101199001011234" → "**************1234"
     *
     * @param idCard 原始身份证号
     * @return 脱敏后的身份证号；输入为 null 或空字符串时返回原值
     */
    String maskIdCard(String idCard);

    /**
     * 通用脱敏方法
     * 保留前 prefixLen 位和后 suffixLen 位，中间用*替换
     *
     * @param value     原始字符串
     * @param prefixLen 保留前缀长度
     * @param suffixLen 保留后缀长度
     * @return 脱敏后的字符串；输入为 null 或空字符串时返回原值
     */
    String mask(String value, int prefixLen, int suffixLen);
}
