package com.parking.service;

import com.parking.dto.InfoModifyApplyRequest;
import com.parking.dto.InfoModifyApplyResponse;
import com.parking.dto.InfoModifyAuditRequest;

/**
 * 敏感信息修改服务接口
 * Validates: Requirements 24.1, 24.2, 24.3, 24.4, 24.5, 24.6
 */
public interface OwnerInfoModifyService {

    /**
     * 业主申请修改敏感信息
     * 查询业主信息 → 获取当前字段值作为 oldValue → 创建申请记录(status=pending) → 返回 applyId
     *
     * @param ownerId     业主ID
     * @param communityId 小区ID
     * @param request     申请请求
     * @return 申请响应
     */
    InfoModifyApplyResponse apply(Long ownerId, Long communityId, InfoModifyApplyRequest request);

    /**
     * 物业审批敏感信息修改申请
     * 幂等检查 → 行级锁查询 → 验证状态 → 更新状态/业主信息 → 通知业主
     *
     * @param applyId   申请ID
     * @param request   审批请求
     * @param adminId   审批管理员ID
     * @param requestId 请求ID（用于幂等键）
     */
    void audit(Long applyId, InfoModifyAuditRequest request, Long adminId, String requestId);
}
