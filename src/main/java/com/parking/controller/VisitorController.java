package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.VisitorApplyRequest;
import com.parking.dto.VisitorApplyResponse;
import com.parking.dto.VisitorAuditRequest;
import com.parking.service.VisitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Visitor 权限控制器
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 9.2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    /**
     * 申请 Visitor 权限
     * POST /api/v1/visitors/apply
     */
    @PostMapping("/apply")
    public ApiResponse<VisitorApplyResponse> apply(@Valid @RequestBody VisitorApplyRequest request,
                                                    @RequestParam Long ownerId,
                                                    @RequestParam Long communityId,
                                                    @RequestParam String houseNo) {
        log.info("Visitor 申请请求: carNumber={}, communityId={}, houseNo={}, ownerId={}",
                request.getCarNumber(), communityId, houseNo, ownerId);
        VisitorApplyResponse response = visitorService.apply(request, ownerId, communityId, houseNo);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }

    /**
     * 审批 Visitor 申请
     * POST /api/v1/visitors/{visitorId}/audit
     */
    @PostMapping("/{visitorId}/audit")
    public ApiResponse<Void> audit(@PathVariable Long visitorId,
                                    @Valid @RequestBody VisitorAuditRequest request,
                                    @RequestParam Long adminId,
                                    @RequestParam Long communityId) {
        log.info("Visitor 审批请求: visitorId={}, action={}, adminId={}, communityId={}",
                visitorId, request.getAction(), adminId, communityId);
        visitorService.audit(visitorId, request, adminId, communityId);
        return ApiResponse.success(RequestContext.getRequestId());
    }
}
