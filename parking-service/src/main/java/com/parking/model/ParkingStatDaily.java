package com.parking.model;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日统计预聚合实体
 */
@Data
public class ParkingStatDaily {
    private Long id;
    private Long communityId;
    private LocalDate statDate;
    private Integer totalEntryCount;
    private Integer totalExitCount;
    private Integer primaryEntryCount;
    private Integer visitorEntryCount;
    private Integer peakHour;
    private Integer peakCount;
    private Integer avgParkingDuration;
    private Integer zombieVehicleCount;
    private Integer exceptionExitCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
