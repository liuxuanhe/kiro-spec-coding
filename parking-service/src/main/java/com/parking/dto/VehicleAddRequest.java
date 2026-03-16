package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 车牌添加请求 DTO
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Data
public class VehicleAddRequest {

    /** 小区ID */
    @NotNull(message = "小区ID不能为空")
    private Long communityId;

    /** 房屋号 */
    @NotBlank(message = "房屋号不能为空")
    private String houseNo;

    /** 业主ID */
    @NotNull(message = "业主ID不能为空")
    private Long ownerId;

    /** 车牌号 */
    @NotBlank(message = "车牌号不能为空")
    private String carNumber;

    /** 车辆品牌（可选） */
    private String carBrand;

    /** 车辆型号（可选） */
    private String carModel;

    /** 车辆颜色（可选） */
    private String carColor;
}
