package com.parking.service;

import com.parking.dto.ExitExceptionHandleRequest;
import com.parking.dto.ExitRequest;
import com.parking.dto.ExitResponse;

/**
 * 车辆出场服务接口
 * 处理车辆出场逻辑：查找入场记录 → 正常出场/异常出场 → 更新车位 → 失效缓存 → 记录日志
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8
 */
public interface ExitService {

    /**
     * 车辆出场
     * 查找入场记录 → 正常出场更新状态/异常出场创建记录 → 分布式锁更新车位 → 失效缓存 → 记录日志
     *
     * @param request 出场请求
     * @return 出场响应
     */
    ExitResponse vehicleExit(ExitRequest request);

    /**
     * 处理异常出场记录
     * 物业管理员填写处理原因，更新异常出场记录状态为 exception_handled
     *
     * @param request 异常出场处理请求
     * @param adminId 当前操作管理员ID
     */
    void handleExitException(ExitExceptionHandleRequest request, Long adminId);
}
