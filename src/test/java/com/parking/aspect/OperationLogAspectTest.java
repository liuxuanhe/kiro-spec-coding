package com.parking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.BusinessException;
import com.parking.common.ErrorCode;
import com.parking.common.OperationLogAnnotation;
import com.parking.mapper.OperationLogMapper;
import com.parking.model.OperationLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OperationLogAspect 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OperationLogAspectTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private OperationLogAnnotation annotation;

    private OperationLogAspect aspect;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        aspect = new OperationLogAspect(operationLogMapper, objectMapper);
    }

    @Test
    @DisplayName("操作成功 - 记录 SUCCESS 日志")
    void around_success() throws Throwable {
        when(annotation.operationType()).thenReturn("CREATE");
        when(annotation.targetType()).thenReturn("owner");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"testArg"});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.around(joinPoint, annotation);

        assertEquals("result", result);
        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("CREATE", log.getOperationType());
        assertEquals("owner", log.getTargetType());
        assertEquals("SUCCESS", log.getOperationResult());
        assertNotNull(log.getOperationTime());
        assertNotNull(log.getAfterValue());
    }

    @Test
    @DisplayName("操作失败 - 记录 FAILURE 日志并重新抛出异常")
    void around_failure() throws Throwable {
        when(annotation.operationType()).thenReturn("UPDATE");
        when(annotation.targetType()).thenReturn("vehicle");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenThrow(new BusinessException(ErrorCode.PARAM_ERROR));

        assertThrows(BusinessException.class, () -> aspect.around(joinPoint, annotation));

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("FAILURE", log.getOperationResult());
        assertNotNull(log.getErrorMessage());
    }

    @Test
    @DisplayName("日志写入失败 - 不影响主流程")
    void around_logInsertFails() throws Throwable {
        when(annotation.operationType()).thenReturn("DELETE");
        when(annotation.targetType()).thenReturn("zombie_vehicle");
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("ok");
        doThrow(new RuntimeException("数据库异常")).when(operationLogMapper).insert(any());

        // 日志写入失败不应影响主流程
        Object result = aspect.around(joinPoint, annotation);
        assertEquals("ok", result);
    }

    @Test
    @DisplayName("无请求参数 - beforeValue 为空")
    void around_noArgs() throws Throwable {
        when(annotation.operationType()).thenReturn("AUDIT");
        when(annotation.targetType()).thenReturn("visitor");
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn(null);

        aspect.around(joinPoint, annotation);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        OperationLog log = captor.getValue();
        assertNull(log.getBeforeValue());
        assertEquals("SUCCESS", log.getOperationResult());
    }

    @Test
    @DisplayName("超长参数 - 截断为2000字符")
    void around_longArgs() throws Throwable {
        when(annotation.operationType()).thenReturn("CREATE");
        when(annotation.targetType()).thenReturn("owner");
        // 构造超长字符串
        String longStr = "x".repeat(3000);
        when(joinPoint.getArgs()).thenReturn(new Object[]{longStr});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.around(joinPoint, annotation);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        OperationLog log = captor.getValue();
        assertTrue(log.getBeforeValue().length() <= 2020); // 2000 + "[truncated]"
        assertTrue(log.getBeforeValue().endsWith("...[truncated]"));
    }
}
