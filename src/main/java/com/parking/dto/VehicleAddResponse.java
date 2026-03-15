package com.parking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车牌添加响应 DTO
 */
@Data
public class VehicleAddResponse {

    /** 车牌记录ID */
    private Long vehicleId;

    /** 车牌号 */
    private String carNumber;

    /** 车牌状态 */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
