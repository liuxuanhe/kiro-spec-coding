package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 入场记录实体（对应分表 parking_car_record_yyyymm）
 */
@Data
public class ParkingCarRecord {
    private Long id;
    private Long communityId;
    private String houseNo;
    private String carNumber;
    private String vehicleType;
    private LocalDateTime enterTime;
    private LocalDateTime exitTime;
    private Integer duration;
    private String status;
    private String exceptionReason;
    private Long handlerAdminId;
    private LocalDateTime handleTime;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
