package com.parking.service;

import com.parking.service.impl.MaskingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MaskingService 单元测试
 * 覆盖手机号脱敏、身份证号脱敏、通用脱敏方法及边界条件
 * Validates: Requirements 17.1, 17.2, 17.8
 */
class MaskingServiceTest {

    private MaskingService maskingService;

    @BeforeEach
    void setUp() {
        maskingService = new MaskingServiceImpl();
    }

    // ========== maskPhoneNumber 测试 ==========

    @Test
    @DisplayName("标准11位手机号脱敏: 保留前3位和后4位")
    void maskPhoneNumber_standard() {
        assertEquals("138****5678", maskingService.maskPhoneNumber("13812345678"));
    }

    @Test
    @DisplayName("不同手机号脱敏格式正确")
    void maskPhoneNumber_differentNumbers() {
        assertEquals("159****9012", maskingService.maskPhoneNumber("15987659012"));
        assertEquals("186****3456", maskingService.maskPhoneNumber("18600003456"));
    }

    @Test
    @DisplayName("手机号为 null 时返回 null")
    void maskPhoneNumber_null() {
        assertNull(maskingService.maskPhoneNumber(null));
    }

    @Test
    @DisplayName("手机号为空字符串时返回空字符串")
    void maskPhoneNumber_empty() {
        assertEquals("", maskingService.maskPhoneNumber(""));
    }

    @Test
    @DisplayName("手机号长度不足时返回全部*替换")
    void maskPhoneNumber_shortInput() {
        // 长度 <= 3+4=7 时，全部替换为*
        assertEquals("*******", maskingService.maskPhoneNumber("1234567"));
        assertEquals("***", maskingService.maskPhoneNumber("123"));
        assertEquals("*", maskingService.maskPhoneNumber("1"));
    }

    // ========== maskIdCard 测试 ==========

    @Test
    @DisplayName("标准18位身份证号脱敏: 仅显示后4位")
    void maskIdCard_standard18() {
        assertEquals("**************1234", maskingService.maskIdCard("110101199001011234"));
    }

    @Test
    @DisplayName("15位身份证号脱敏: 仅显示后4位")
    void maskIdCard_standard15() {
        assertEquals("***********1567", maskingService.maskIdCard("110101900101567"));
    }

    @Test
    @DisplayName("身份证号为 null 时返回 null")
    void maskIdCard_null() {
        assertNull(maskingService.maskIdCard(null));
    }

    @Test
    @DisplayName("身份证号为空字符串时返回空字符串")
    void maskIdCard_empty() {
        assertEquals("", maskingService.maskIdCard(""));
    }

    @Test
    @DisplayName("身份证号长度不足4位时返回全部*替换")
    void maskIdCard_shortInput() {
        assertEquals("****", maskingService.maskIdCard("1234"));
        assertEquals("**", maskingService.maskIdCard("12"));
        assertEquals("*", maskingService.maskIdCard("1"));
    }

    // ========== mask 通用方法测试 ==========

    @Test
    @DisplayName("通用脱敏: 自定义前缀和后缀长度")
    void mask_customPrefixSuffix() {
        assertEquals("ab***fg", maskingService.mask("abcdefg", 2, 2));
        assertEquals("a***e", maskingService.mask("abcde", 1, 1));
    }

    @Test
    @DisplayName("通用脱敏: 前缀和后缀长度之和等于字符串长度时全部替换")
    void mask_prefixSuffixEqualsLength() {
        assertEquals("*****", maskingService.mask("abcde", 3, 2));
    }

    @Test
    @DisplayName("通用脱敏: null 输入返回 null")
    void mask_null() {
        assertNull(maskingService.mask(null, 3, 4));
    }

    @Test
    @DisplayName("通用脱敏: 空字符串输入返回空字符串")
    void mask_empty() {
        assertEquals("", maskingService.mask("", 3, 4));
    }
}
