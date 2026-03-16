package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 车牌实体
 */
@Data
public class CarPlate {
    private Long id;
    private Long communityId;
    private String houseNo;
    private Long ownerId;
    private String carNumber;
    private String carBrand;
    private String carModel;
    private String carColor;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
