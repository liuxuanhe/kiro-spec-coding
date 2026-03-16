package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.SetPrimaryRequest;
import com.parking.dto.VehicleAddRequest;
import com.parking.dto.VehicleAddResponse;
import com.parking.dto.VehicleQueryResponse;
import com.parking.service.VehicleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆管理控制器
 * 处理车牌添加、删除等接口
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * 添加车牌接口
     * POST /api/v1/vehicles
     *
     * @param request 添加车牌请求
     * @return 添加车牌响应
     */
    @PostMapping
    public ApiResponse<VehicleAddResponse> addVehicle(@Valid @RequestBody VehicleAddRequest request) {
        log.info("车牌添加请求: carNumber={}, communityId={}, houseNo={}, ownerId={}",
                request.getCarNumber(), request.getCommunityId(),
                request.getHouseNo(), request.getOwnerId());
        VehicleAddResponse response = vehicleService.addVehicle(request);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }

    /**
     * 删除车牌接口（逻辑删除）
     * DELETE /api/v1/vehicles/{vehicleId}
     * Validates: Requirements 3.6, 3.7, 3.8, 3.9
     *
     * @param vehicleId 车牌记录ID
     * @return 删除结果
     */
    @DeleteMapping("/{vehicleId}")
    public ApiResponse<Void> deleteVehicle(@PathVariable Long vehicleId) {
        log.info("车牌删除请求: vehicleId={}", vehicleId);
        vehicleService.deleteVehicle(vehicleId);
        return ApiResponse.success(RequestContext.getRequestId());
    }

    /**
     * 查询车牌列表接口
     * GET /api/v1/vehicles?communityId={communityId}&houseNo={houseNo}&carNumber={carNumber}&page=1&pageSize=10
     * 物业管理员按 Community 维度查询，houseNo / carNumber 为可选筛选条件
     * 业主端按 Data_Domain 维度查询时需传 houseNo
     * 使用 Redis 缓存（30分钟过期），对敏感信息执行脱敏处理
     * Validates: Requirements 11.1, 11.5
     *
     * @param communityId 小区ID
     * @param houseNo 房屋号（可选筛选）
     * @param carNumber 车牌号（可选筛选）
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 车牌查询响应
     */
    @GetMapping
    public ApiResponse<VehicleQueryResponse> listVehicles(
            @RequestParam Long communityId,
            @RequestParam(required = false) String houseNo,
            @RequestParam(required = false) String carNumber,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        log.info("车牌查询请求: communityId={}, houseNo={}, carNumber={}, page={}, pageSize={}",
                communityId, houseNo, carNumber, page, pageSize);
        VehicleQueryResponse response = vehicleService.listVehicles(communityId, houseNo, carNumber, page, pageSize);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }

    /**
     * 设置 Primary 车辆接口
     * PUT /api/v1/vehicles/{vehicleId}/primary
     * 获取分布式锁 → 行级锁查询 → 验证所有车辆不在场 → 切换 Primary → 失效缓存
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10
     *
     * @param vehicleId 目标车牌记录ID
     * @param request   设置 Primary 请求
     * @return 操作结果
     */
    @PutMapping("/{vehicleId}/primary")
    public ApiResponse<Void> setPrimary(@PathVariable Long vehicleId,
                                        @Valid @RequestBody SetPrimaryRequest request) {
        log.info("设置 Primary 车辆请求: vehicleId={}, communityId={}, houseNo={}",
                vehicleId, request.getCommunityId(), request.getHouseNo());
        vehicleService.setPrimaryVehicle(vehicleId, request.getCommunityId(), request.getHouseNo());
        return ApiResponse.success(RequestContext.getRequestId());
    }
}
