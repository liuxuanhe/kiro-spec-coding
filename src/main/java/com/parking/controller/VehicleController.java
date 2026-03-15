package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.VehicleAddRequest;
import com.parking.dto.VehicleAddResponse;
import com.parking.service.VehicleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆管理控制器
 * 处理车牌添加等接口
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.9
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
}
