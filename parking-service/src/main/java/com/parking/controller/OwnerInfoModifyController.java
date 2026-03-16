package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.InfoModifyApplyRequest;
import com.parking.dto.InfoModifyApplyResponse;
import com.parking.dto.InfoModifyAuditRequest;
import com.parking.service.OwnerInfoModifyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 敏感信息修改控制器
 * 处理业主敏感信息修改申请和物业审批接口
 * Validates: Requirements 24.1, 24.2, 24.3, 24.4, 24.5, 24.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/owners/info-modify")
public class OwnerInfoModifyController {

    private final OwnerInfoModifyService ownerInfoModifyService;

    public OwnerInfoModifyController(OwnerInfoModifyService ownerInfoModifyService) {
        this.ownerInfoModifyService = ownerInfoModifyService;
    }

    /**
     * 业主申请修改敏感信息
     * POST /api/v1/owners/info-modify/apply
     *
     * @param request 申请请求
     * @return 申请响应
     */
    @PostMapping("/apply")
    public ApiResponse<InfoModifyApplyResponse> apply(@Valid @RequestBody InfoModifyApplyRequest request) {
        // 业主ID和小区ID从认证上下文获取，此处预留硬编码（后续由拦截器注入）
        Long ownerId = 0L;
        Long communityId = 0L;
        log.info("敏感信息修改申请: ownerId={}, modifyType={}", ownerId, request.getModifyType());
        InfoModifyApplyResponse response = ownerInfoModifyService.apply(ownerId, communityId, request);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }

    /**
     * 物业审批敏感信息修改申请
     * POST /api/v1/owners/info-modify/{applyId}/audit
     *
     * @param applyId 申请ID
     * @param request 审批请求
     * @return 操作结果
     */
    @PostMapping("/{applyId}/audit")
    public ApiResponse<Void> audit(@PathVariable Long applyId,
                                   @Valid @RequestBody InfoModifyAuditRequest request) {
        // 管理员ID从认证上下文获取，此处预留硬编码（后续由拦截器注入）
        Long adminId = 0L;
        String requestId = RequestContext.getRequestId();
        log.info("敏感信息修改审批: applyId={}, approved={}", applyId, request.getApproved());
        ownerInfoModifyService.audit(applyId, request, adminId, requestId);
        return ApiResponse.success(requestId);
    }
}
