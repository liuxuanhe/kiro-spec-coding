package com.parking.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void successWithData_shouldReturnCodeZero() {
        ApiResponse<String> response = ApiResponse.success("test-data", "req_123");

        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("test-data", response.getData());
        assertEquals("req_123", response.getRequestId());
    }

    @Test
    void successWithoutData_shouldReturnCodeZero() {
        ApiResponse<Void> response = ApiResponse.success("req_456");

        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertNull(response.getData());
        assertEquals("req_456", response.getRequestId());
    }

    @Test
    void errorWithCodeAndMessage_shouldReturnNonZeroCode() {
        ApiResponse<Void> response = ApiResponse.error(1001, "error message", "req_789");

        assertEquals(1001, response.getCode());
        assertEquals("error message", response.getMessage());
        assertNull(response.getData());
        assertEquals("req_789", response.getRequestId());
    }

    @Test
    void errorWithErrorCode_shouldReturnCorrectCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.PARKING_5001, "req_abc");

        assertEquals(5001, response.getCode());
        assertEquals("车位已满，无法入场", response.getMessage());
        assertNull(response.getData());
        assertEquals("req_abc", response.getRequestId());
    }
}
