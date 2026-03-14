package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Visitor 申请实体
 */
@Data
public class VisitorApplication {
    private Long id;
    private Long communityId;
    private String houseNo;
    private Long ownerId;
    private Long carPlateId;
    private String carNumber;
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
