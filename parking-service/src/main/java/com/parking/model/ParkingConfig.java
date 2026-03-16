package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 停车场配置实体
 */
@Data
public class ParkingConfig {
    private Long id;
    private Long communityId;
    private Integer totalSpaces;
    private Integer reservedSpaces;
    private Integer visitorQuotaHours;
    private Integer visitorSingleDurationHours;
    private Integer visitorActivationWindowHours;
    private Integer zombieVehicleThresholdDays;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
