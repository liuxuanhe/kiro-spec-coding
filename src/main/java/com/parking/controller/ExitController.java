package com.parking.controller;

import com.parking.common.ApiResponse;
import com.parking.common.RequestContext;
import com.parking.dto.ExitExceptionHandleRequest;
import com.parking.dto.ExitRequest;
import com.parking.dto.ExitResponse;
import com.parking.service.ExitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆出场控制器
 * 处理 POST /api/v1/parking/exit 和 POST /api/v1/parking/exit-exception/handle 接口
 * Validates: Requirements 6.1, 6.2, 6.3, 6.5, 6.6
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parking")
public class ExitController {

    private final ExitService exitService;

    public ExitController(ExitService exitService) {
        this.exitService = exitService;
    }

    /**
     * 车辆出场接口
     * POST /api/v1/parking/exit
     *
     * @param request 出场请求（包含 communityId 和 carNumber）
     * @return 出场响应
     */
    @PostMapping("/exit")
    public ApiResponse<ExitResponse> vehicleExit(@Valid @RequestBody ExitRequest request) {
        log.info("车辆出场请求: communityId={}, carNumber={}",
                request.getCommunityId(), request.getCarNumber());
        ExitResponse response = exitService.vehicleExit(request);
        return ApiResponse.success(response, RequestContext.getRequestId());
    }

    /**
     * 异常出场处理接口
     * POST /api/v1/parking/exit-exception/handle
     * 物业管理员填写处理原因，更新异常出场记录
     *
     * @param request        异常出场处理请求
     * @param servletRequest HTTP 请求（用于获取当前管理员ID）
     * @return 处理结果
     */
    @PostMapping("/exit-exception/handle")
    public ApiResponse<Void> handleExitException(@Valid @RequestBody ExitExceptionHandleRequest request,
                                                  HttpServletRequest servletRequest) {
        log.info("异常出场处理请求: recordId={}, communityId={}",
                request.getRecordId(), request.getCommunityId());

        // 从请求属性中获取当前管理员ID（由认证拦截器设置）
        Long adminId = (Long) servletRequest.getAttribute("userId");
        if (adminId == null) {
            adminId = 0L; // 默认值，实际生产环境由认证拦截器保证
        }

        exitService.handleExitException(request, adminId);
        return ApiResponse.success(RequestContext.getRequestId());
    }
}
