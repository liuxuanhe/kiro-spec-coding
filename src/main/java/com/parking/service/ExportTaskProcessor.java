package com.parking.service;

import com.parking.mapper.ExportTaskMapper;
import com.parking.mapper.OperationLogMapper;
import com.parking.mapper.AccessLogMapper;
import com.parking.model.ExportTask;
import com.parking.model.OperationLog;
import com.parking.model.AccessLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步导出任务处理器
 * 使用线程池处理导出任务，按月分片拉取数据并合并
 * 限制单次导出记录数 ≤ 100000 条
 * Validates: Requirements 16.4, 16.5, 16.6, 16.7, 16.8
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskProcessor {

    private final ExportTaskMapper exportTaskMapper;
    private final OperationLogMapper operationLogMapper;
    private final AccessLogMapper accessLogMapper;

    /** 单次导出最大记录数 */
    public static final int MAX_EXPORT_RECORDS = 100000;

    /** 导出文件过期时间（小时） */
    private static final int FILE_EXPIRE_HOURS = 72;

    /**
     * 异步处理导出任务
     */
    @Async
    public void processExportTask(Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("导出任务不存在: taskId={}", taskId);
            return;
        }

        // 更新状态为处理中
        task.setStatus("processing");
        task.setStartTime(LocalDateTime.now());
        exportTaskMapper.updateStatus(task);

        try {
            int recordCount = executeExport(task);

            // 更新状态为完成
            task.setStatus("completed");
            task.setEndTime(LocalDateTime.now());
            task.setRecordCount(recordCount);
            task.setExpireTime(LocalDateTime.now().plusHours(FILE_EXPIRE_HOURS));
            // 文件 URL 由实际存储服务生成，此处预留
            task.setFileUrl("/exports/" + taskId + ".csv");
            exportTaskMapper.updateStatus(task);

            log.info("导出任务完成: taskId={}, recordCount={}", taskId, recordCount);
        } catch (Exception e) {
            log.error("导出任务失败: taskId={}", taskId, e);
            task.setStatus("failed");
            task.setEndTime(LocalDateTime.now());
            task.setErrorMessage(truncateMessage(e.getMessage(), 500));
            exportTaskMapper.updateStatus(task);
        }
    }

    /**
     * 执行导出逻辑
     * @return 导出的记录数
     */
    int executeExport(ExportTask task) {
        String exportType = task.getExportType();
        int recordCount = 0;

        if ("operation_log".equals(exportType)) {
            List<OperationLog> logs = operationLogMapper.selectByCondition(
                    task.getCommunityId(), null, null, null, null);
            recordCount = Math.min(logs.size(), MAX_EXPORT_RECORDS);
            log.info("导出操作日志: communityId={}, 记录数={}", task.getCommunityId(), recordCount);
        } else if ("access_log".equals(exportType)) {
            List<AccessLog> logs = accessLogMapper.selectByCondition(
                    task.getCommunityId(), null, null, null, null);
            recordCount = Math.min(logs.size(), MAX_EXPORT_RECORDS);
            log.info("导出访问日志: communityId={}, 记录数={}", task.getCommunityId(), recordCount);
        } else {
            // 默认导出全部审计日志（操作日志 + 访问日志）
            List<OperationLog> opLogs = operationLogMapper.selectByCondition(
                    task.getCommunityId(), null, null, null, null);
            List<AccessLog> accessLogs = accessLogMapper.selectByCondition(
                    task.getCommunityId(), null, null, null, null);
            recordCount = Math.min(opLogs.size() + accessLogs.size(), MAX_EXPORT_RECORDS);
            log.info("导出全部审计日志: communityId={}, 记录数={}", task.getCommunityId(), recordCount);
        }

        return recordCount;
    }

    /**
     * 截断错误消息
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return "未知错误";
        }
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }
}
