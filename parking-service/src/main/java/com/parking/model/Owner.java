package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 业主实体
 */
@Data
public class Owner {
    private Long id;
    private Long communityId;
    private String houseNo;
    private String phoneNumber;
    private String idCardLast4;
    private String realName;
    private String status;
    private String rejectReason;
    private Long auditAdminId;
    private LocalDateTime auditTime;
    private String accountStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
