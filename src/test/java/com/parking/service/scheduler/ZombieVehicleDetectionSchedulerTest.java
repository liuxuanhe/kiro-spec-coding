package com.parking.service.scheduler;

import com.parking.mapper.ParkingCarRecordMapper;
import com.parking.mapper.ParkingConfigMapper;
import com.parking.mapper.ZombieVehicleMapper;
import com.parking.model.ParkingCarRecord;
import com.parking.model.ZombieVehicle;
import com.parking.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ZombieVehicleDetectionScheduler 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ZombieVehicleDetectionSchedulerTest {

    @Mock
    private ParkingCarRecordMapper parkingCarRecordMapper;

    @Mock
    private ParkingConfigMapper parkingConfigMapper;

    @Mock
    private ZombieVehicleMapper zombieVehicleMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ZombieVehicleDetectionScheduler scheduler;

    private static final Long COMMUNITY_ID = 1001L;

    @Test
    @DisplayName("检测到新的 Zombie_Vehicle 并创建记录")
    void detectForCommunity_newZombie() {
        ParkingCarRecord record = buildRecord(1L, "京A12345", "101",
                LocalDateTime.now().minusDays(10));
        // 第一次调用（当前月分表）返回候选车辆，第二次调用（上月分表）返回空
        when(parkingCarRecordMapper.selectZombieCandidates(anyString(), eq(COMMUNITY_ID), any()))
                .thenReturn(new ArrayList<>(List.of(record)))
                .thenReturn(new ArrayList<>());
        when(zombieVehicleMapper.selectByEntryRecordId(1L)).thenReturn(null);

        int count = scheduler.detectForCommunity(COMMUNITY_ID);

        assertEquals(1, count);
        verify(zombieVehicleMapper).insert(any(ZombieVehicle.class));
        verify(notificationService).sendSubscriptionMessage(eq(COMMUNITY_ID), eq("zombie_vehicle_alert"), anyMap());
    }

    @Test
    @DisplayName("已存在僵尸记录的车辆不重复创建")
    void detectForCommunity_existingZombie() {
        ParkingCarRecord record = buildRecord(2L, "京B67890", "102",
                LocalDateTime.now().minusDays(8));
        when(parkingCarRecordMapper.selectZombieCandidates(anyString(), eq(COMMUNITY_ID), any()))
                .thenReturn(new ArrayList<>(List.of(record)))
                .thenReturn(new ArrayList<>());
        when(zombieVehicleMapper.selectByEntryRecordId(2L)).thenReturn(new ZombieVehicle());

        int count = scheduler.detectForCommunity(COMMUNITY_ID);

        assertEquals(0, count);
        verify(zombieVehicleMapper, never()).insert(any());
        verify(notificationService, never()).sendSubscriptionMessage(any(), any(), any());
    }

    @Test
    @DisplayName("无候选车辆时返回0")
    void detectForCommunity_noCandidates() {
        when(parkingCarRecordMapper.selectZombieCandidates(anyString(), eq(COMMUNITY_ID), any()))
                .thenReturn(new ArrayList<>());

        int count = scheduler.detectForCommunity(COMMUNITY_ID);

        assertEquals(0, count);
        verify(zombieVehicleMapper, never()).insert(any());
    }

    @Test
    @DisplayName("通知发送失败不影响后续处理")
    void detectForCommunity_notificationFails() {
        ParkingCarRecord record = buildRecord(3L, "京C11111", "103",
                LocalDateTime.now().minusDays(9));
        when(parkingCarRecordMapper.selectZombieCandidates(anyString(), eq(COMMUNITY_ID), any()))
                .thenReturn(new ArrayList<>(List.of(record)))
                .thenReturn(new ArrayList<>());
        when(zombieVehicleMapper.selectByEntryRecordId(3L)).thenReturn(null);
        doThrow(new RuntimeException("通知失败"))
                .when(notificationService).sendSubscriptionMessage(any(), any(), any());

        int count = scheduler.detectForCommunity(COMMUNITY_ID);

        assertEquals(1, count);
        verify(zombieVehicleMapper).insert(any(ZombieVehicle.class));
    }

    @Test
    @DisplayName("定时任务遍历所有小区")
    void detectZombieVehicles_allCommunities() {
        when(parkingConfigMapper.selectAllCommunityIds()).thenReturn(List.of(1001L, 1002L));
        when(parkingCarRecordMapper.selectZombieCandidates(anyString(), anyLong(), any()))
                .thenReturn(new ArrayList<>());

        scheduler.detectZombieVehicles();

        verify(parkingCarRecordMapper, atLeast(2))
                .selectZombieCandidates(anyString(), anyLong(), any());
    }

    private ParkingCarRecord buildRecord(Long id, String carNumber, String houseNo,
                                          LocalDateTime enterTime) {
        ParkingCarRecord record = new ParkingCarRecord();
        record.setId(id);
        record.setCarNumber(carNumber);
        record.setHouseNo(houseNo);
        record.setEnterTime(enterTime);
        record.setStatus("entered");
        return record;
    }
}
