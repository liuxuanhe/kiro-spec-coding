package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感信息修改申请实体
 */
@Data
public class OwnerInfoModifyApplication {
    private Long id;
    private Long communityId;
    private Long ownerId;
    private String modifyType;
    private String oldValue;
    private String newValue;
    private String applyReason;
    private String status;
    private String rejectReason;
    private Long auditAdminId;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
