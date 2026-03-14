package com.parking.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应格式
 * Validates: Requirements 26.1, 26.2, 26.3, 26.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 响应码，0 表示成功，非零表示失败 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 请求唯一标识，用于请求追踪 */
    private String requestId;

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(0, "success", data, requestId);
    }

    public static <T> ApiResponse<T> success(String requestId) {
        return new ApiResponse<>(0, "success", null, requestId);
    }

    public static <T> ApiResponse<T> error(int code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String requestId) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, requestId);
    }
}
