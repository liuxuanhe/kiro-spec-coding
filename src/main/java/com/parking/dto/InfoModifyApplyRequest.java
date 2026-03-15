package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 敏感信息修改申请请求
 * Validates: Requirements 24.1
 */
@Data
public class InfoModifyApplyRequest {

    /** 修改类型：phone_number / real_name */
    @NotBlank(message = "修改类型不能为空")
    private String modifyType;

    /** 新值 */
    @NotBlank(message = "新值不能为空")
    private String newValue;

    /** 申请原因 */
    @NotBlank(message = "申请原因不能为空")
    private String applyReason;
}
