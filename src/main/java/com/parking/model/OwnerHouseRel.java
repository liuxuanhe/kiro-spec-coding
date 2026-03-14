package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 业主房屋号关联实体
 */
@Data
public class OwnerHouseRel {
    private Long id;
    private Long communityId;
    private Long ownerId;
    private String houseNo;
    private String relationType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
