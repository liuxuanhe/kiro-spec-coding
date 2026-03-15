package com.parking.service;

import com.parking.dto.EntryTrendResponse;
import com.parking.dto.PeakHoursResponse;
import com.parking.dto.SpaceUsageResponse;
import com.parking.dto.ZombieVehicleStatResponse;

import java.time.LocalDate;

/**
 * 报表服务接口
 * Validates: Requirements 21.1, 21.2, 21.7
 */
public interface ReportService {

    /**
     * 查询入场趋势报表
     * 从 parking_stat_daily 预聚合表查询，使用 Redis 缓存（1小时过期）
     *
     * @param communityId 小区ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 入场趋势数据
     */
    EntryTrendResponse getEntryTrend(Long communityId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询车位使用率报表
     * 车位使用率 = (入场数 - 出场数) / total_spaces 的日均估算
     *
     * @param communityId 小区ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 车位使用率数据
     */
    SpaceUsageResponse getSpaceUsage(Long communityId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询峰值时段报表
     * 从 parking_stat_daily 预聚合表查询 peak_hour 和 peak_count
     *
     * @param communityId 小区ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 峰值时段数据
     */
    PeakHoursResponse getPeakHours(Long communityId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询僵尸车辆统计报表
     * 从 parking_stat_daily 预聚合表查询 zombie_vehicle_count 和 exception_exit_count
     *
     * @param communityId 小区ID
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 僵尸车辆统计数据
     */
    ZombieVehicleStatResponse getZombieVehicleStat(Long communityId, LocalDate startDate, LocalDate endDate);
}
