package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 僵尸车辆实体
 */
@Data
public class ZombieVehicle {
    private Long id;
    private Long communityId;
    private String houseNo;
    private String carNumber;
    private Long entryRecordId;
    private LocalDateTime enterTime;
    private Integer continuousDays;
    private String status;
    private String contactRecord;
    private String solution;
    private String ignoreReason;
    private Long handlerAdminId;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
