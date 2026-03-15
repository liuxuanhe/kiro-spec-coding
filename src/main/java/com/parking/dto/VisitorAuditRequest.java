package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Visitor 审批请求 DTO
 */
@Data
public class VisitorAuditRequest {

    @NotBlank(message = "审批结果不能为空")
    private String action; // "approve" 或 "reject"

    private String rejectReason; // 驳回原因（驳回时必填）

    private String requestId; // 用于幂等键
}
