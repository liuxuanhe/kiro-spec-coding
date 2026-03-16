package com.parking.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 车位配置查询响应
 * Validates: Requirements 9.5
 */
@Data
public class ParkingConfigResponse {

    /** 配置ID */
    private Long id;

    /** 小区ID */
    private Long communityId;

    /** 总车位数 */
    private Integer totalSpaces;

    /** 预留车位数 */
    private Integer reservedSpaces;

    /** Visitor 月度配额（小时） */
    private Integer visitorQuotaHours;

    /** Visitor 单次时长限制（小时） */
    private Integer visitorSingleDurationHours;

    /** Visitor 激活窗口（小时） */
    private Integer visitorActivationWindowHours;

    /** 僵尸车辆阈值（天） */
    private Integer zombieVehicleThresholdDays;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
