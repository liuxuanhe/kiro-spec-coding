package com.parking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.common.OperationLogAnnotation;
import com.parking.common.RequestContext;
import com.parking.mapper.OperationLogMapper;
import com.parking.model.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面，拦截标注了 @OperationLogAnnotation 的方法，
 * 记录操作日志到 sys_operation_log 表。
 * 日志不可删除、不可篡改。
 * Validates: Requirements 18.1, 18.2, 18.5, 18.6
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLogAnnotation operationLogAnnotation) throws Throwable {
        OperationLog operationLog = new OperationLog();
        operationLog.setRequestId(RequestContext.getRequestId());
        operationLog.setOperationType(operationLogAnnotation.operationType());
        operationLog.setTargetType(operationLogAnnotation.targetType());
        operationLog.setOperationTime(LocalDateTime.now());

        // 从请求上下文获取 IP
        fillRequestInfo(operationLog);

        // 记录请求参数作为 beforeValue
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                operationLog.setBeforeValue(safeToJson(args));
            }
        } catch (Exception e) {
            log.warn("序列化操作日志请求参数失败", e);
        }

        Object result;
        try {
            result = joinPoint.proceed();
            operationLog.setOperationResult("SUCCESS");
            // 记录返回值作为 afterValue
            try {
                operationLog.setAfterValue(safeToJson(result));
            } catch (Exception e) {
                log.warn("序列化操作日志返回值失败", e);
            }
        } catch (Throwable ex) {
            operationLog.setOperationResult("FAILURE");
            operationLog.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            // 异步写入日志，不影响主流程
            try {
                operationLogMapper.insert(operationLog);
            } catch (Exception e) {
                log.error("写入操作日志失败: requestId={}, operationType={}",
                        operationLog.getRequestId(), operationLog.getOperationType(), e);
            }
        }
        return result;
    }

    /**
     * 从 HTTP 请求中填充操作人信息
     */
    private void fillRequestInfo(OperationLog operationLog) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                operationLog.setOperatorIp(getClientIp(request));
                // TODO: 从认证上下文获取 operatorId/operatorName/operatorRole/communityId
                // 当前使用请求头中的信息作为占位
                String operatorId = request.getHeader("X-Operator-Id");
                if (operatorId != null) {
                    operationLog.setOperatorId(Long.parseLong(operatorId));
                }
                operationLog.setOperatorName(request.getHeader("X-Operator-Name"));
                operationLog.setOperatorRole(request.getHeader("X-Operator-Role"));
                String communityId = request.getHeader("X-Community-Id");
                if (communityId != null) {
                    operationLog.setCommunityId(Long.parseLong(communityId));
                }
            }
        } catch (Exception e) {
            log.warn("获取请求上下文信息失败", e);
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 安全地将对象序列化为 JSON，截断过长内容
     */
    private String safeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            // 截断超过2000字符的内容
            if (json.length() > 2000) {
                return json.substring(0, 2000) + "...[truncated]";
            }
            return json;
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
