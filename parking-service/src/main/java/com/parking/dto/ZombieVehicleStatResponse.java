package com.parking.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 僵尸车辆统计报表响应
 */
@Data
public class ZombieVehicleStatResponse {

    /**
     * 僵尸车辆统计数据列表
     */
    private List<ZombieStatItem> items;

    @Data
    public static class ZombieStatItem {
        /** 统计日期 */
        private LocalDate date;
        /** 僵尸车辆数量 */
        private int zombieVehicleCount;
        /** 异常出场数量 */
        private int exceptionExitCount;
    }
}
