package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Visitor 授权实体
 */
@Data
public class VisitorAuthorization {
    private Long id;
    private Long communityId;
    private String houseNo;
    private Long applicationId;
    private Long carPlateId;
    private String carNumber;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime expireTime;
    private LocalDateTime activationTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
