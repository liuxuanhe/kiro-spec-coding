package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * IP 白名单实体
 */
@Data
public class IpWhitelist {
    private Long id;
    private Long communityId;
    private String ipAddress;
    private String ipRange;
    private String operationType;
    private String description;
    private String status;
    private Long createAdminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
