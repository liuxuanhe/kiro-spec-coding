package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.EntryRequest;
import com.parking.dto.EntryResponse;
import com.parking.service.EntryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆入场控制器
 * 处理 POST /api/v1/parking/entry 接口
 * Validates: Requirements 5.1, 5.5, 5.8
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parking")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    /**
     * 车辆入场接口
     * POST /api/v1/parking/entry
     *
     * @param request 入场请求（包含 communityId 和 carNumber）
     * @return 入场响应
     */
    @PostMapping("/entry")
    public ApiResponse<EntryResponse> vehicleEntry(@Valid @RequestBody EntryRequest request) {
        log.info("车辆入场请求: communityId={}, carNumber={}",
                request.getCommunityId(), request.getCarNumber());
        EntryResponse response = entryService.vehicleEntry(request);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }
}
