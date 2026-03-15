package com.parking.service;

import com.parking.dto.VisitorApplyRequest;
import com.parking.dto.VisitorApplyResponse;
import com.parking.dto.VisitorAuditRequest;

/**
 * Visitor 权限服务接口
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 9.2
 */
public interface VisitorService {

    /**
     * 申请 Visitor 权限
     * 验证车牌绑定 → 检查月度配额 → 检查 Visitor 可开放车位 → 创建申请记录
     *
     * @param request     申请请求
     * @param ownerId     业主ID
     * @param communityId 小区ID
     * @param houseNo     房屋号
     * @return 申请响应
     */
    VisitorApplyResponse apply(VisitorApplyRequest request, Long ownerId, Long communityId, String houseNo);

    /**
     * 审批 Visitor 申请
     * 幂等键检查 → 行级锁 → 更新状态 → 创建授权记录 → 通知业主
     *
     * @param visitorId   申请ID
     * @param request     审批请求
     * @param adminId     管理员ID
     * @param communityId 小区ID
     */
    void audit(Long visitorId, VisitorAuditRequest request, Long adminId, Long communityId);
}
