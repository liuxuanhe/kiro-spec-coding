package com.parking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车辆入场响应 DTO
 * Validates: Requirements 5.8
 */
@Data
public class EntryResponse {

    /** 入场记录ID */
    private Long recordId;

    /** 车牌号 */
    private String carNumber;

    /** 车辆类型（primary / visitor） */
    private String vehicleType;

    /** 入场时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enterTime;

    /** 入场记录状态 */
    private String status;
}
