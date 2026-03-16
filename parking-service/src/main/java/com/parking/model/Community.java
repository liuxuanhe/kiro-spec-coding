package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 小区实体
 */
@Data
public class Community {
    private Long id;
    private String communityName;
    private String communityCode;
    private String province;
    private String city;
    private String district;
    private String address;
    private String contactPerson;
    private String contactPhone;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
