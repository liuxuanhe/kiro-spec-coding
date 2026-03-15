package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 异常出场处理请求 DTO
 * 物业管理员填写处理原因，更新异常出场记录
 * Validates: Requirements 6.5, 6.6
 */
@Data
public class ExitExceptionHandleRequest {

    /** 入场记录ID */
    @NotNull(message = "记录ID不能为空")
    private Long recordId;

    /** 小区ID */
    @NotNull(message = "小区ID不能为空")
    private Long communityId;

    /** 处理备注（物业管理员填写的处理原因） */
    @NotBlank(message = "处理备注不能为空")
    private String handleRemark;

    /** 分表名称（可选，不传则自动计算当前月份分表） */
    private String tableName;
}
