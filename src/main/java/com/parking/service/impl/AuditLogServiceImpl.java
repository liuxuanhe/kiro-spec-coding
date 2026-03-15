package com.parking.service.impl;

import com.parking.mapper.AccessLogMapper;
import com.parking.mapper.OperationLogMapper;
import com.parking.model.AccessLog;
import com.parking.model.OperationLog;
import com.parking.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计日志服务实现
 * 默认查询最近30天日志
 * Validates: Requirements 18.8
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final OperationLogMapper operationLogMapper;
    private final AccessLogMapper accessLogMapper;

    @Override
    public List<OperationLog> queryOperationLogs(Long communityId, Long operatorId,
                                                  String operationType, String startTime, String endTime) {
        log.info("查询操作日志: communityId={}, operatorId={}, operationType={}", communityId, operatorId, operationType);
        return operationLogMapper.selectByCondition(communityId, operatorId, operationType, startTime, endTime);
    }

    @Override
    public List<AccessLog> queryAccessLogs(Long communityId, Long userId,
                                            String apiPath, String startTime, String endTime) {
        log.info("查询访问日志: communityId={}, userId={}, apiPath={}", communityId, userId, apiPath);
        return accessLogMapper.selectByCondition(communityId, userId, apiPath, startTime, endTime);
    }
}
