package com.parking.service;

import com.parking.common.BusinessException;
import com.parking.dto.InfoModifyApplyRequest;
import com.parking.dto.InfoModifyApplyResponse;
import com.parking.dto.InfoModifyAuditRequest;
import com.parking.mapper.OwnerInfoModifyMapper;
import com.parking.mapper.OwnerMapper;
import com.parking.model.Owner;
import com.parking.model.OwnerInfoModifyApplication;
import com.parking.service.impl.OwnerInfoModifyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OwnerInfoModifyService 单元测试
 * Validates: Requirements 24.1, 24.2, 24.3, 24.4, 24.5, 24.6
 */
@ExtendWith(MockitoExtension.class)
class OwnerInfoModifyServiceTest {

    @Mock
    private OwnerInfoModifyMapper ownerInfoModifyMapper;

    @Mock
    private OwnerMapper ownerMapper;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private NotificationService notificationService;

    private OwnerInfoModifyServiceImpl service;

    private static final Long OWNER_ID = 10001L;
    private static final Long COMMUNITY_ID = 1001L;
    private static final Long ADMIN_ID = 2001L;
    private static final Long APPLY_ID = 5001L;
    private static final String REQUEST_ID = "req_test_001";

    @BeforeEach
    void setUp() {
        service = new OwnerInfoModifyServiceImpl(
                ownerInfoModifyMapper, ownerMapper, idempotencyService, notificationService);
    }

    private Owner createOwner() {
        Owner owner = new Owner();
        owner.setId(OWNER_ID);
        owner.setCommunityId(COMMUNITY_ID);
        owner.setPhoneNumber("13812345678");
        owner.setRealName("张三");
        owner.setAccountStatus("active");
        return owner;
    }

    private InfoModifyApplyRequest createApplyRequest(String modifyType, String newValue) {
        InfoModifyApplyRequest request = new InfoModifyApplyRequest();
        request.setModifyType(modifyType);
        request.setNewValue(newValue);
        request.setApplyReason("信息变更");
        return request;
    }

    private OwnerInfoModifyApplication createPendingApplication() {
        OwnerInfoModifyApplication app = new OwnerInfoModifyApplication();
        app.setId(APPLY_ID);
        app.setCommunityId(COMMUNITY_ID);
        app.setOwnerId(OWNER_ID);
        app.setModifyType("phone_number");
        app.setOldValue("13812345678");
        app.setNewValue("13999998888");
        app.setApplyReason("换号");
        app.setStatus("pending");
        return app;
    }

    @Nested
    @DisplayName("apply - 申请修改敏感信息")
    class ApplyTests {

        @Test
        @DisplayName("申请修改手机号成功，应创建 pending 状态的申请记录")
        void apply_phoneNumber_success() {
            Owner owner = createOwner();
            when(ownerMapper.selectById(OWNER_ID)).thenReturn(owner);
            doAnswer(invocation -> {
                OwnerInfoModifyApplication app = invocation.getArgument(0);
                app.setId(APPLY_ID);
                return null;
            }).when(ownerInfoModifyMapper).insert(any(OwnerInfoModifyApplication.class));

            InfoModifyApplyRequest request = createApplyRequest("phone_number", "13999998888");
            InfoModifyApplyResponse response = service.apply(OWNER_ID, COMMUNITY_ID, request);

            assertNotNull(response);
            assertEquals(APPLY_ID, response.getApplyId());
            assertEquals("pending", response.getStatus());

            // 验证插入的申请记录
            ArgumentCaptor<OwnerInfoModifyApplication> captor =
                    ArgumentCaptor.forClass(OwnerInfoModifyApplication.class);
            verify(ownerInfoModifyMapper).insert(captor.capture());
            OwnerInfoModifyApplication captured = captor.getValue();
            assertEquals(COMMUNITY_ID, captured.getCommunityId());
            assertEquals(OWNER_ID, captured.getOwnerId());
            assertEquals("phone_number", captured.getModifyType());
            assertEquals("13812345678", captured.getOldValue());
            assertEquals("13999998888", captured.getNewValue());
            assertEquals("pending", captured.getStatus());
        }

        @Test
        @DisplayName("申请修改真实姓名成功")
        void apply_realName_success() {
            Owner owner = createOwner();
            when(ownerMapper.selectById(OWNER_ID)).thenReturn(owner);
            doAnswer(invocation -> {
                OwnerInfoModifyApplication app = invocation.getArgument(0);
                app.setId(APPLY_ID);
                return null;
            }).when(ownerInfoModifyMapper).insert(any(OwnerInfoModifyApplication.class));

            InfoModifyApplyRequest request = createApplyRequest("real_name", "李四");
            InfoModifyApplyResponse response = service.apply(OWNER_ID, COMMUNITY_ID, request);

            assertNotNull(response);
            assertEquals("pending", response.getStatus());

            ArgumentCaptor<OwnerInfoModifyApplication> captor =
                    ArgumentCaptor.forClass(OwnerInfoModifyApplication.class);
            verify(ownerInfoModifyMapper).insert(captor.capture());
            assertEquals("张三", captor.getValue().getOldValue());
            assertEquals("李四", captor.getValue().getNewValue());
        }

        @Test
        @DisplayName("业主不存在应抛出异常")
        void apply_ownerNotFound_shouldThrow() {
            when(ownerMapper.selectById(OWNER_ID)).thenReturn(null);

            InfoModifyApplyRequest request = createApplyRequest("phone_number", "13999998888");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.apply(OWNER_ID, COMMUNITY_ID, request));
            assertEquals(10000, ex.getCode());
            assertTrue(ex.getMessage().contains("业主不存在"));
            verify(ownerInfoModifyMapper, never()).insert(any());
        }

        @Test
        @DisplayName("不支持的修改类型应抛出异常")
        void apply_unsupportedModifyType_shouldThrow() {
            Owner owner = createOwner();
            when(ownerMapper.selectById(OWNER_ID)).thenReturn(owner);

            InfoModifyApplyRequest request = createApplyRequest("email", "test@test.com");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.apply(OWNER_ID, COMMUNITY_ID, request));
            assertEquals(10000, ex.getCode());
            assertTrue(ex.getMessage().contains("不支持的修改类型"));
        }
    }

    @Nested
    @DisplayName("audit - 审批敏感信息修改")
    class AuditTests {

        @Test
        @DisplayName("审批通过应更新业主信息（Requirements 24.2）")
        void audit_approved_shouldUpdateOwnerInfo() {
            OwnerInfoModifyApplication application = createPendingApplication();
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(application);
            when(idempotencyService.generateKey(anyString(), anyLong(), anyLong(), anyString()))
                    .thenReturn("test_key");
            when(idempotencyService.checkAndSet(anyString(), anyString(), anyInt())).thenReturn(true);
            when(ownerInfoModifyMapper.selectByIdForUpdate(APPLY_ID)).thenReturn(application);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(true);

            service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID);

            // 验证更新申请状态为 approved
            verify(ownerInfoModifyMapper).updateStatus(APPLY_ID, "approved", null, ADMIN_ID);
            // 验证更新业主手机号
            verify(ownerMapper).updatePhoneNumber(OWNER_ID, "13999998888");
        }

        @Test
        @DisplayName("审批驳回应记录驳回原因（Requirements 24.3）")
        void audit_rejected_shouldRecordRejectReason() {
            OwnerInfoModifyApplication application = createPendingApplication();
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(application);
            when(idempotencyService.generateKey(anyString(), anyLong(), anyLong(), anyString()))
                    .thenReturn("test_key");
            when(idempotencyService.checkAndSet(anyString(), anyString(), anyInt())).thenReturn(true);
            when(ownerInfoModifyMapper.selectByIdForUpdate(APPLY_ID)).thenReturn(application);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(false);
            request.setRejectReason("信息不符");

            service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID);

            // 验证更新申请状态为 rejected
            verify(ownerInfoModifyMapper).updateStatus(APPLY_ID, "rejected", "信息不符", ADMIN_ID);
            // 验证不更新业主信息
            verify(ownerMapper, never()).updatePhoneNumber(anyLong(), anyString());
            verify(ownerMapper, never()).updateRealName(anyLong(), anyString());
        }

        @Test
        @DisplayName("非 pending 状态应拒绝审批（PARKING_2001）")
        void audit_notPending_shouldReject() {
            OwnerInfoModifyApplication application = createPendingApplication();
            application.setStatus("approved");
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(application);
            when(idempotencyService.generateKey(anyString(), anyLong(), anyLong(), anyString()))
                    .thenReturn("test_key");
            when(idempotencyService.checkAndSet(anyString(), anyString(), anyInt())).thenReturn(true);
            when(ownerInfoModifyMapper.selectByIdForUpdate(APPLY_ID)).thenReturn(application);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID));
            assertEquals(2001, ex.getCode());
            verify(ownerInfoModifyMapper, never()).updateStatus(anyLong(), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("重复审批请求应被幂等处理")
        void audit_duplicateRequest_shouldBeIdempotent() {
            OwnerInfoModifyApplication application = createPendingApplication();
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(application);
            when(idempotencyService.generateKey(anyString(), anyLong(), anyLong(), anyString()))
                    .thenReturn("test_key");
            // 模拟重复请求：checkAndSet 返回 false
            when(idempotencyService.checkAndSet(anyString(), anyString(), anyInt())).thenReturn(false);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(true);

            // 不应抛出异常，直接返回
            service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID);

            // 验证不执行任何审批操作
            verify(ownerInfoModifyMapper, never()).selectByIdForUpdate(anyLong());
            verify(ownerInfoModifyMapper, never()).updateStatus(anyLong(), anyString(), anyString(), anyLong());
            verify(ownerMapper, never()).updatePhoneNumber(anyLong(), anyString());
        }

        @Test
        @DisplayName("审批通过后应发送通知（Requirements 24.5）")
        void audit_approved_shouldSendNotification() {
            OwnerInfoModifyApplication application = createPendingApplication();
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(application);
            when(idempotencyService.generateKey(anyString(), anyLong(), anyLong(), anyString()))
                    .thenReturn("test_key");
            when(idempotencyService.checkAndSet(anyString(), anyString(), anyInt())).thenReturn(true);
            when(ownerInfoModifyMapper.selectByIdForUpdate(APPLY_ID)).thenReturn(application);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(true);

            service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID);

            // 验证发送通知
            verify(notificationService).sendSubscriptionMessage(
                    eq(OWNER_ID), eq("info_modify_result"), anyMap());
        }

        @Test
        @DisplayName("审批驳回后也应发送通知")
        void audit_rejected_shouldSendNotification() {
            OwnerInfoModifyApplication application = createPendingApplication();
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(application);
            when(idempotencyService.generateKey(anyString(), anyLong(), anyLong(), anyString()))
                    .thenReturn("test_key");
            when(idempotencyService.checkAndSet(anyString(), anyString(), anyInt())).thenReturn(true);
            when(ownerInfoModifyMapper.selectByIdForUpdate(APPLY_ID)).thenReturn(application);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(false);
            request.setRejectReason("信息不符");

            service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID);

            verify(notificationService).sendSubscriptionMessage(
                    eq(OWNER_ID), eq("info_modify_result"), anyMap());
        }

        @Test
        @DisplayName("申请不存在应抛出异常")
        void audit_applicationNotFound_shouldThrow() {
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(null);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID));
            assertEquals(10000, ex.getCode());
            assertTrue(ex.getMessage().contains("修改申请不存在"));
        }

        @Test
        @DisplayName("驳回时未填写原因应抛出异常")
        void audit_rejectedWithoutReason_shouldThrow() {
            OwnerInfoModifyApplication application = createPendingApplication();
            when(ownerInfoModifyMapper.selectById(APPLY_ID)).thenReturn(application);
            when(idempotencyService.generateKey(anyString(), anyLong(), anyLong(), anyString()))
                    .thenReturn("test_key");
            when(idempotencyService.checkAndSet(anyString(), anyString(), anyInt())).thenReturn(true);
            when(ownerInfoModifyMapper.selectByIdForUpdate(APPLY_ID)).thenReturn(application);

            InfoModifyAuditRequest request = new InfoModifyAuditRequest();
            request.setApproved(false);
            // 不设置 rejectReason

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.audit(APPLY_ID, request, ADMIN_ID, REQUEST_ID));
            assertEquals(10000, ex.getCode());
            assertTrue(ex.getMessage().contains("驳回时必须填写驳回原因"));
        }
    }
}
