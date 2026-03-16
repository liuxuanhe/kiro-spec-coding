package com.parking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 敏感信息修改审批请求
 * Validates: Requirements 24.2, 24.3
 */
@Data
public class InfoModifyAuditRequest {

    /** 是否通过 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 驳回原因（审批驳回时必填） */
    private String rejectReason;
}
