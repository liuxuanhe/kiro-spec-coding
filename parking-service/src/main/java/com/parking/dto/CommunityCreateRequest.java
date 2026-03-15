package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建小区请求 DTO
 */
@Data
public class CommunityCreateRequest {

    /** 小区名称 */
    @NotBlank(message = "小区名称不能为空")
    private String communityName;

    /** 小区编码 */
    @NotBlank(message = "小区编码不能为空")
    private String communityCode;

    /** 省份 */
    @NotBlank(message = "省份不能为空")
    private String province;

    /** 城市 */
    @NotBlank(message = "城市不能为空")
    private String city;

    /** 区县 */
    @NotBlank(message = "区县不能为空")
    private String district;

    /** 详细地址 */
    @NotBlank(message = "详细地址不能为空")
    private String address;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;
}
