package com.parking.service.scheduler;

import com.parking.mapper.ParkingCarRecordMapper;
import com.parking.mapper.ParkingConfigMapper;
import com.parking.mapper.ZombieVehicleMapper;
import com.parking.model.ParkingCarRecord;
import com.parking.model.ParkingConfig;
import com.parking.model.ZombieVehicle;
import com.parking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Zombie_Vehicle 识别定时任务
 * 每日凌晨3点执行，扫描所有小区中 status='entered' 且连续在场超过7天的车辆，
 * 创建 zombie_vehicle 记录并通知物业管理员
 * Validates: Requirements 22.1, 22.2, 22.3, 22.4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZombieVehicleDetectionScheduler {

    private final ParkingCarRecordMapper parkingCarRecordMapper;
    private final ParkingConfigMapper parkingConfigMapper;
    private final ZombieVehicleMapper zombieVehicleMapper;
    private final NotificationService notificationService;

    /** Zombie_Vehicle 阈值天数 */
    private static final int ZOMBIE_THRESHOLD_DAYS = 7;

    /**
     * 每日凌晨3点执行僵尸车辆扫描
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void detectZombieVehicles() {
        log.info("开始执行 Zombie_Vehicle 识别任务");
        try {
            List<Long> communityIds = parkingConfigMapper.selectAllCommunityIds();
            int totalDetected = 0;

            for (Long communityId : communityIds) {
                int count = detectForCommunity(communityId);
                totalDetected += count;
            }

            log.info("Zombie_Vehicle 识别任务完成，共检测到 {} 辆僵尸车辆", totalDetected);
        } catch (Exception e) {
            log.error("Zombie_Vehicle 识别任务执行异常", e);
        }
    }

    /**
     * 扫描指定小区的僵尸车辆
     */
    public int detectForCommunity(Long communityId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdTime = now.minusDays(ZOMBIE_THRESHOLD_DAYS);

        // 查询当前月份分表中的候选车辆
        String tableName = getTableName(now);
        List<ParkingCarRecord> candidates = parkingCarRecordMapper.selectZombieCandidates(
                tableName, communityId, thresholdTime);

        // 也查询上个月分表（跨月场景）
        String prevTableName = getTableName(now.minusMonths(1));
        if (!prevTableName.equals(tableName)) {
            List<ParkingCarRecord> prevCandidates = parkingCarRecordMapper.selectZombieCandidates(
                    prevTableName, communityId, thresholdTime);
            candidates.addAll(prevCandidates);
        }

        int detected = 0;
        for (ParkingCarRecord record : candidates) {
            // 检查是否已存在僵尸车辆记录
            ZombieVehicle existing = zombieVehicleMapper.selectByEntryRecordId(record.getId());
            if (existing != null) {
                continue;
            }

            // 计算连续在场天数
            int days = (int) ChronoUnit.DAYS.between(record.getEnterTime(), now);

            // 创建僵尸车辆记录
            ZombieVehicle zombie = new ZombieVehicle();
            zombie.setCommunityId(communityId);
            zombie.setHouseNo(record.getHouseNo());
            zombie.setCarNumber(record.getCarNumber());
            zombie.setEntryRecordId(record.getId());
            zombie.setEnterTime(record.getEnterTime());
            zombie.setContinuousDays(days);
            zombie.setStatus("unhandled");
            zombieVehicleMapper.insert(zombie);

            // 通知物业管理员
            notifyAdmin(communityId, record.getCarNumber(), days);
            detected++;
        }

        if (detected > 0) {
            log.info("小区 {} 检测到 {} 辆新增 Zombie_Vehicle", communityId, detected);
        }
        return detected;
    }

    /**
     * 通知物业管理员有新的 Zombie_Vehicle
     */
    private void notifyAdmin(Long communityId, String carNumber, int days) {
        try {
            notificationService.sendSubscriptionMessage(
                    communityId,
                    "zombie_vehicle_alert",
                    Map.of(
                            "carNumber", carNumber,
                            "days", String.valueOf(days),
                            "message", "车牌 " + carNumber + " 已连续在场 " + days + " 天，请及时处理"
                    )
            );
        } catch (Exception e) {
            log.warn("Zombie_Vehicle 通知发送失败: communityId={}, carNumber={}", communityId, carNumber, e);
        }
    }

    /**
     * 根据时间获取分表名称
     */
    private String getTableName(LocalDateTime time) {
        return String.format("parking_car_record_%d%02d", time.getYear(), time.getMonthValue());
    }
}
