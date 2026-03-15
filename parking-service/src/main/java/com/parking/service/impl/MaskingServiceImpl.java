package com.parking.service.impl;

import com.parking.service.MaskingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据脱敏服务实现类
 * 实现手机号、身份证号等敏感数据的脱敏处理
 * - 手机号: 保留前3位和后4位，中间用****替换，如 "13812345678" → "138****5678"
 * - 身份证号: 仅显示后4位，其余用*替换，如 "110101199001011234" → "**************1234"
 * - 输入为 null 或空字符串时返回原值
 * - 输入长度不足时返回全部*替换
 * Validates: Requirements 17.1, 17.2, 17.8
 */
@Slf4j
@Service
public class MaskingServiceImpl implements MaskingService {

    /** 手机号脱敏: 保留前3位 */
    private static final int PHONE_PREFIX_LEN = 3;

    /** 手机号脱敏: 保留后4位 */
    private static final int PHONE_SUFFIX_LEN = 4;

    /** 身份证号脱敏: 保留前0位 */
    private static final int ID_CARD_PREFIX_LEN = 0;

    /** 身份证号脱敏: 保留后4位 */
    private static final int ID_CARD_SUFFIX_LEN = 4;

    @Override
    public String maskPhoneNumber(String phoneNumber) {
        return mask(phoneNumber, PHONE_PREFIX_LEN, PHONE_SUFFIX_LEN);
    }

    @Override
    public String maskIdCard(String idCard) {
        return mask(idCard, ID_CARD_PREFIX_LEN, ID_CARD_SUFFIX_LEN);
    }

    @Override
    public String mask(String value, int prefixLen, int suffixLen) {
        // 输入为 null 或空字符串时返回原值
        if (value == null || value.isEmpty()) {
            return value;
        }

        int length = value.length();

        // 输入长度不足以保留前缀和后缀时，返回全部*替换
        if (length <= prefixLen + suffixLen) {
            return "*".repeat(length);
        }

        int maskLen = length - prefixLen - suffixLen;
        return value.substring(0, prefixLen)
                + "*".repeat(maskLen)
                + value.substring(length - suffixLen);
    }
}
