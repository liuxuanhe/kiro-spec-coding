package com.parking.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructWithErrorCode_shouldSetCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.PARKING_3001);

        assertEquals(3001, ex.getCode());
        assertEquals("车牌数量已达上限（5个），无法继续添加", ex.getMessage());
    }

    @Test
    void constructWithCodeAndMessage_shouldSetBoth() {
        BusinessException ex = new BusinessException(9999, "custom error");

        assertEquals(9999, ex.getCode());
        assertEquals("custom error", ex.getMessage());
    }
}
