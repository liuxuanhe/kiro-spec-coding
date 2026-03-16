package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.ParkingConfigResponse;
import com.parking.dto.ParkingConfigUpdateRequest;
import com.parking.service.ParkingConfigService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车位配置管理控制器
 * 处理停车场配置的查询和修改接口
 * Validates: Requirements 9.5, 9.6, 9.7, 9.8
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parking/config")
public class ParkingConfigController {

    private final ParkingConfigService parkingConfigService;

    public ParkingConfigController(ParkingConfigService parkingConfigService) {
        this.parkingConfigService = parkingConfigService;
    }

    /**
     * 查询停车场配置
     * GET /api/v1/parking/config?communityId={communityId}
     *
     * @param communityId 小区ID
     * @return 停车场配置响应
     */
    @GetMapping
    public ApiResponse<ParkingConfigResponse> getConfig(@RequestParam Long communityId) {
        log.info("查询停车场配置: communityId={}", communityId);
        ParkingConfigResponse response = parkingConfigService.getConfig(communityId);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }

    /**
     * 修改停车场配置
     * PUT /api/v1/parking/config
     * 验证 total_spaces ≥ 当前在场车辆数 → 乐观锁更新 → 重算 Visitor_Available_Spaces →
     * 更新不可用 Visitor 授权 → 失效缓存 → 记录操作日志
     *
     * @param request 修改请求
     * @return 更新后的停车场配置响应
     */
    @PutMapping
    public ApiResponse<ParkingConfigResponse> updateConfig(
            @Valid @RequestBody ParkingConfigUpdateRequest request) {
        log.info("修改停车场配置: communityId={}, totalSpaces={}, version={}",
                request.getCommunityId(), request.getTotalSpaces(), request.getVersion());
        ParkingConfigResponse response = parkingConfigService.updateConfig(request);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }
}
