package com.parking.service.impl;

import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import com.parking.dto.InfoModifyApplyRequest;
import com.parking.dto.InfoModifyApplyResponse;
import com.parking.dto.InfoModifyAuditRequest;
import com.parking.mapper.OwnerInfoModifyMapper;
import com.parking.mapper.OwnerMapper;
import com.parking.model.Owner;
import com.parking.model.OwnerInfoModifyApplication;
import com.parking.service.IdempotencyService;
import com.parking.service.NotificationService;
import com.parking.service.OwnerInfoModifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 敏感信息修改服务实现类
 * 实现业主敏感信息修改申请与物业审批流程
 * Validates: Requirements 24.1, 24.2, 24.3, 24.4, 24.5, 24.6
 */
@Slf4j
@Service
public class OwnerInfoModifyServiceImpl implements OwnerInfoModifyService {

    /** 幂等键过期时间（秒） */
    private static final int IDEMPOTENCY_EXPIRE_SECONDS = 300;

    /** 审批结果通知模板ID */
    private static final String NOTIFY_TEMPLATE_INFO_MODIFY = "info_modify_result";

    private final OwnerInfoModifyMapper ownerInfoModifyMapper;
    private final OwnerMapper ownerMapper;
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;

    public OwnerInfoModifyServiceImpl(OwnerInfoModifyMapper ownerInfoModifyMapper,
                                      OwnerMapper ownerMapper,
                                      IdempotencyService idempotencyService,
                                      NotificationService notificationService) {
        this.ownerInfoModifyMapper = ownerInfoModifyMapper;
        this.ownerMapper = ownerMapper;
        this.idempotencyService = idempotencyService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InfoModifyApplyResponse apply(Long ownerId, Long communityId, InfoModifyApplyRequest request) {
        // 1. 查询业主信息
        Owner owner = ownerMapper.selectById(ownerId);
        if (owner == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "业主不存在");
        }

        // 2. 获取当前字段值作为 oldValue
        String oldValue = getOldValue(owner, request.getModifyType());

        // 3. 创建申请记录，状态为 pending（Requirements 24.1）
        OwnerInfoModifyApplication application = new OwnerInfoModifyApplication();
        application.setCommunityId(communityId);
        application.setOwnerId(ownerId);
        application.setModifyType(request.getModifyType());
        application.setOldValue(oldValue);
        application.setNewValue(request.getNewValue());
        application.setApplyReason(request.getApplyReason());
        application.setStatus("pending");
        ownerInfoModifyMapper.insert(application);

        log.info("敏感信息修改申请创建成功: applyId={}, ownerId={}, modifyType={}",
                application.getId(), ownerId, request.getModifyType());

        // 4. 构建响应
        InfoModifyApplyResponse response = new InfoModifyApplyResponse();
        response.setApplyId(application.getId());
        response.setStatus(application.getStatus());
        response.setCreateTime(application.getCreateTime());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long applyId, InfoModifyAuditRequest request, Long adminId, String requestId) {
        // 1. 使用幂等键防止重复审批（Requirements 24.4）
        // 先查询申请记录获取 communityId（不加锁，仅用于生成幂等键）
        OwnerInfoModifyApplication preCheck = ownerInfoModifyMapper.selectById(applyId);
        if (preCheck == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "修改申请不存在");
        }

        String idempotencyKey = idempotencyService.generateKey(
                "info_modify_audit", preCheck.getCommunityId(), applyId, requestId);

        boolean isFirstRequest = idempotencyService.checkAndSet(
                idempotencyKey, "", IDEMPOTENCY_EXPIRE_SECONDS);

        if (!isFirstRequest) {
            // 重复请求，直接返回
            log.info("重复审批请求，已忽略: applyId={}, requestId={}", applyId, requestId);
            return;
        }

        // 2. 使用行级锁查询申请记录（SELECT FOR UPDATE）
        OwnerInfoModifyApplication application = ownerInfoModifyMapper.selectByIdForUpdate(applyId);
        if (application == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "修改申请不存在");
        }

        // 3. 验证状态为 pending
        if (!"pending".equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.PARKING_2001);
        }

        // 4. 审批驳回时必须填写驳回原因
        if (Boolean.FALSE.equals(request.getApproved())
                && (request.getRejectReason() == null || request.getRejectReason().isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "驳回时必须填写驳回原因");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            // 5. 审批通过：更新申请状态为 approved，更新业主对应字段（Requirements 24.2）
            ownerInfoModifyMapper.updateStatus(applyId, "approved", null, adminId);
            updateOwnerField(application.getOwnerId(), application.getModifyType(), application.getNewValue());

            log.info("敏感信息修改审批通过: applyId={}, ownerId={}, modifyType={}, oldValue={}, newValue={}",
                    applyId, application.getOwnerId(), application.getModifyType(),
                    application.getOldValue(), application.getNewValue());
        } else {
            // 6. 审批驳回：更新申请状态为 rejected，记录驳回原因（Requirements 24.3）
            ownerInfoModifyMapper.updateStatus(applyId, "rejected", request.getRejectReason(), adminId);

            log.info("敏感信息修改审批驳回: applyId={}, ownerId={}, reason={}",
                    applyId, application.getOwnerId(), request.getRejectReason());
        }

        // 7. 发送订阅消息通知业主（Requirements 24.5）
        sendNotification(application, request.getApproved());

        // 8. 记录操作日志预留（Requirements 24.4）
        log.info("操作日志预留: 敏感信息修改审批, applyId={}, before={}, after={}, adminId={}",
                applyId, application.getOldValue(), application.getNewValue(), adminId);
    }

    /**
     * 获取业主当前字段值
     *
     * @param owner      业主实体
     * @param modifyType 修改类型
     * @return 当前字段值
     */
    private String getOldValue(Owner owner, String modifyType) {
        return switch (modifyType) {
            case "phone_number" -> owner.getPhoneNumber();
            case "real_name" -> owner.getRealName();
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "不支持的修改类型: " + modifyType);
        };
    }

    /**
     * 更新业主对应字段
     *
     * @param ownerId    业主ID
     * @param modifyType 修改类型
     * @param newValue   新值
     */
    private void updateOwnerField(Long ownerId, String modifyType, String newValue) {
        switch (modifyType) {
            case "phone_number" -> ownerMapper.updatePhoneNumber(ownerId, newValue);
            case "real_name" -> ownerMapper.updateRealName(ownerId, newValue);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "不支持的修改类型: " + modifyType);
        }
    }

    /**
     * 发送审批结果通知
     *
     * @param application 申请记录
     * @param approved    是否通过
     */
    private void sendNotification(OwnerInfoModifyApplication application, boolean approved) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("modifyType", application.getModifyType());
            data.put("result", approved ? "approved" : "rejected");
            data.put("applyId", String.valueOf(application.getId()));
            notificationService.sendSubscriptionMessage(
                    application.getOwnerId(), NOTIFY_TEMPLATE_INFO_MODIFY, data);
        } catch (Exception e) {
            // 通知发送失败不影响主流程，由 NotificationService 内部重试机制处理
            log.warn("发送敏感信息修改审批通知失败: applyId={}, ownerId={}, error={}",
                    application.getId(), application.getOwnerId(), e.getMessage());
        }
    }
}
