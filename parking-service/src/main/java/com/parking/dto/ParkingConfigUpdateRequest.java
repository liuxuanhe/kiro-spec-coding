package com.parking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 车位配置修改请求
 * Validates: Requirements 9.5, 9.6
 */
@Data
public class ParkingConfigUpdateRequest {

    /** 小区ID */
    @NotNull(message = "小区ID不能为空")
    private Long communityId;

    /** 总车位数 */
    @NotNull(message = "总车位数不能为空")
    @Min(value = 0, message = "总车位数不能为负数")
    private Integer totalSpaces;

    /** 预留车位数 */
    @Min(value = 0, message = "预留车位数不能为负数")
    private Integer reservedSpaces;

    /** Visitor 月度配额（小时） */
    @Min(value = 0, message = "Visitor 月度配额不能为负数")
    private Integer visitorQuotaHours;

    /** Visitor 单次时长限制（小时） */
    @Min(value = 0, message = "Visitor 单次时长限制不能为负数")
    private Integer visitorSingleDurationHours;

    /** Visitor 激活窗口（小时） */
    @Min(value = 0, message = "Visitor 激活窗口不能为负数")
    private Integer visitorActivationWindowHours;

    /** 僵尸车辆阈值（天） */
    @Min(value = 1, message = "僵尸车辆阈值至少为1天")
    private Integer zombieVehicleThresholdDays;

    /** 乐观锁版本号，用于并发冲突检测 */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
