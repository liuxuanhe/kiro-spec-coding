package com.parking.service;

import com.parking.dto.ParkingConfigResponse;
import com.parking.dto.ParkingConfigUpdateRequest;

/**
 * 车位配置管理服务接口
 * Validates: Requirements 9.5, 9.6, 9.7, 9.8
 */
public interface ParkingConfigService {

    /**
     * 查询指定小区的停车场配置
     *
     * @param communityId 小区ID
     * @return 停车场配置响应
     */
    ParkingConfigResponse getConfig(Long communityId);

    /**
     * 修改停车场配置
     * 验证 total_spaces ≥ 当前在场车辆数 → 乐观锁更新 → 重算 Visitor_Available_Spaces →
     * 更新不可用 Visitor 授权 → 失效缓存 → 记录操作日志
     *
     * @param request 修改请求
     * @return 更新后的停车场配置响应
     */
    ParkingConfigResponse updateConfig(ParkingConfigUpdateRequest request);
}
