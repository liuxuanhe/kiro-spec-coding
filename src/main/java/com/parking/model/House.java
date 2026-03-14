package com.parking.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋号实体
 */
@Data
public class House {
    private Long id;
    private Long communityId;
    private String houseNo;
    private String building;
    private String unit;
    private String floor;
    private String room;
    private BigDecimal area;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
