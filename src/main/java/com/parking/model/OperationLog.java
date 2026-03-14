package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
public class OperationLog {
    private Long id;
    private String requestId;
    private Long communityId;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String operatorIp;
    private String operationType;
    private LocalDateTime operationTime;
    private String targetType;
    private Long targetId;
    private String beforeValue;
    private String afterValue;
    private String operationResult;
    private String errorMessage;
    private LocalDateTime createTime;
}
