package com.parking.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Visitor 权限查询响应
 * Validates: Requirements 11.3
 */
@Data
public class VisitorQueryResponse {
    /** 申请ID */
    private Long applicationId;
    /** 车牌号（脱敏后） */
    private String carNumber;
    /** 申请原因 */
    private String applyReason;
    /** 申请状态 */
    private String applicationStatus;
    /** 驳回原因 */
    private String rejectReason;
    /** 申请时间 */
    private LocalDateTime applyTime;
    /** 授权ID */
    private Long authorizationId;
    /** 授权状态 */
    private String authorizationStatus;
    /** 激活窗口开始时间 */
    private LocalDateTime startTime;
    /** 激活窗口截止时间 */
    private LocalDateTime expireTime;
    /** 激活时间 */
    private LocalDateTime activationTime;
}
