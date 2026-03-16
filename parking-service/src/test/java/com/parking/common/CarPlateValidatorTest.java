package com.parking.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CarPlateValidator 单元测试
 * Validates: Requirements 3.2
 */
class CarPlateValidatorTest {

    // ========== 有效普通车牌（7位）测试 ==========

    @ParameterizedTest(name = "有效普通车牌: {0}")
    @ValueSource(strings = {
            "京A12345",
            "沪B6C789",
            "粤C0D1E2",
            "川D99999",
            "浙E1ABCD",
            "苏FA1234",
            "鲁G00001",
            "豫H1A2B3"
    })
    void shouldAcceptValidStandardPlates(String plate) {
        assertTrue(CarPlateValidator.isValid(plate));
    }

    // ========== 有效新能源车牌（8位）测试 ==========

    @ParameterizedTest(name = "有效新能源车牌: {0}")
    @ValueSource(strings = {
            "京AD12345",
            "沪BF00001",
            "粤CD12345",
            "川DF99999",
            "浙EA12345",
            "京AABCDEF"
    })
    void shouldAcceptValidNewEnergyPlates(String plate) {
        assertTrue(CarPlateValidator.isValid(plate));
    }

    // ========== 无效车牌测试：空值和 null ==========

    @ParameterizedTest(name = "空值或null应被拒绝")
    @NullAndEmptySource
    void shouldRejectNullAndEmpty(String plate) {
        assertFalse(CarPlateValidator.isValid(plate));
    }

    // ========== 无效车牌测试：长度不对 ==========

    @ParameterizedTest(name = "长度不对的车牌: {0}")
    @ValueSource(strings = {
            "京A1234",       // 6位，太短
            "京A123456789"   // 11位，太长
    })
    void shouldRejectInvalidLength(String plate) {
        assertFalse(CarPlateValidator.isValid(plate));
    }

    // ========== 无效车牌测试：非法字符 ==========

    @ParameterizedTest(name = "含非法字符的车牌: {0}")
    @ValueSource(strings = {
            "京a12345",      // 小写字母
            "京A1234!",      // 特殊字符
            "京A 1234",      // 空格
            "京A1234五"      // 中文数字
    })
    void shouldRejectInvalidCharacters(String plate) {
        assertFalse(CarPlateValidator.isValid(plate));
    }

    // ========== 无效车牌测试：省份简称错误 ==========

    @ParameterizedTest(name = "省份简称错误的车牌: {0}")
    @ValueSource(strings = {
            "中A12345",      // 非省份简称
            "AA12345",       // 字母开头
            "1A12345"        // 数字开头
    })
    void shouldRejectInvalidProvince(String plate) {
        assertFalse(CarPlateValidator.isValid(plate));
    }

    // ========== 无效车牌测试：第2位非字母 ==========

    @Test
    void shouldRejectSecondCharNotLetter() {
        assertFalse(CarPlateValidator.isValid("京112345"));
    }

    // ========== 无效车牌测试：普通车牌后5位全字母（无数字） ==========

    @Test
    void shouldRejectStandardPlateWithAllLetters() {
        // 普通车牌后5位必须至少包含1个数字
        assertFalse(CarPlateValidator.isValid("京AABCDE"));
    }

    // ========== validate 方法测试 ==========

    @Test
    void validateShouldPassForValidPlate() {
        assertDoesNotThrow(() -> CarPlateValidator.validate("京A12345"));
    }

    @Test
    void validateShouldThrowForInvalidPlate() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CarPlateValidator.validate("invalid"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getMessage(), ex.getMessage());
    }

    @Test
    void validateShouldThrowForNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CarPlateValidator.validate(null));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void validateShouldThrowForEmpty() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> CarPlateValidator.validate(""));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }
}
