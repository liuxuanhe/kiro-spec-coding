package com.parking.exception;

import com.parking.common.ApiResponse;
import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_shouldReturnBusinessErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.PARKING_5001);

        ApiResponse<Void> response = handler.handleBusinessException(ex);

        assertEquals(5001, response.getCode());
        assertEquals("车位已满，无法入场", response.getMessage());
    }

    @Test
    void handleMissingParam_shouldReturnParamError() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("communityId", "Long");

        ApiResponse<Void> response = handler.handleMissingParam(ex);

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), response.getCode());
        assertTrue(response.getMessage().contains("communityId"));
    }

    @Test
    void handleException_shouldReturnSystemError() {
        Exception ex = new RuntimeException("unexpected");

        ApiResponse<Void> response = handler.handleException(ex);

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), response.getCode());
        assertEquals(ErrorCode.SYSTEM_ERROR.getMessage(), response.getMessage());
    }
}
