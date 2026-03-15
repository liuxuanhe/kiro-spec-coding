package com.parking.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 车位使用率报表响应
 */
@Data
public class SpaceUsageResponse {

    /**
     * 使用率数据列表
     */
    private List<UsageItem> items;

    /**
     * 总车位数
     */
    private int totalSpaces;

    @Data
    public static class UsageItem {
        /** 统计日期 */
        private LocalDate date;
        /** 当日入场总数 */
        private int totalEntryCount;
        /** 当日出场总数 */
        private int totalExitCount;
        /** 平均停放时长（分钟） */
        private int avgParkingDuration;
        /** 车位使用率（百分比，保留2位小数） */
        private double usageRate;
    }
}
