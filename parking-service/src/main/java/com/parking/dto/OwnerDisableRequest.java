package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 业主账号注销请求
 * Validates: Requirements 14.6
 */
@Data
public class OwnerDisableRequest {

    /** 注销原因（必填） */
    @NotBlank(message = "注销原因不能为空")
    private String reason;
}
