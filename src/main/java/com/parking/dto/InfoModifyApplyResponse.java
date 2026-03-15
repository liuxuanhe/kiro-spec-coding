package com.parking.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感信息修改申请响应
 * Validates: Requirements 24.1
 */
@Data
public class InfoModifyApplyResponse {

    /** 申请ID */
    private Long applyId;

    /** 申请状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
