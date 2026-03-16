package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 硬件设备实体（预留）
 */
@Data
public class HardwareDevice {
    private Long id;
    private Long communityId;
    private String deviceType;
    private String deviceCode;
    private String deviceName;
    private String location;
    private String ipAddress;
    private String secretKey;
    private String status;
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
