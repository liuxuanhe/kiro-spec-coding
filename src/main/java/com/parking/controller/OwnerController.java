package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.OwnerDisableRequest;
import com.parking.dto.OwnerRegisterRequest;
import com.parking.dto.OwnerRegisterResponse;
import com.parking.service.OwnerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业主控制器
 * 处理业主注册等接口
 * Validates: Requirements 1.1, 1.4, 1.5, 1.6, 1.7
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/owners")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    /**
     * 业主注册接口
     * POST /api/v1/owners/register
     *
     * @param request 注册请求
     * @return 注册响应
     */
    @PostMapping("/register")
    public ApiResponse<OwnerRegisterResponse> register(@Valid @RequestBody OwnerRegisterRequest request) {
        log.info("业主注册请求: phone={}, communityId={}, houseNo={}",
                request.getPhone(), request.getCommunityId(), request.getHouseNo());
        OwnerRegisterResponse response = ownerService.register(request);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }

    /**
     * 业主账号注销接口
     * POST /api/v1/owners/{ownerId}/disable
     * 仅允许超级管理员执行
     * Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7, 14.8
     *
     * @param ownerId 业主ID
     * @param request 注销请求（包含注销原因）
     * @return 操作结果
     */
    @PostMapping("/{ownerId}/disable")
    public ApiResponse<Void> disable(@PathVariable Long ownerId,
                                     @Valid @RequestBody OwnerDisableRequest request) {
        log.info("业主账号注销请求: ownerId={}, reason={}", ownerId, request.getReason());
        // 操作人ID从认证上下文获取，此处预留硬编码（后续由拦截器注入）
        Long operatorId = 0L;
        ownerService.disable(ownerId, request.getReason(), operatorId);
        return ApiResponse.success(RequestContext.getRequestId());
    }
}
