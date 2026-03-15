package com.parking.scheduler;

import com.parking.mapper.AccessLogMapper;
import com.parking.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 审计日志归档定时任务
 * 每月1日凌晨4点执行，将6个月以上的日志迁移到归档表
 * Validates: Requirements 18.9, 18.10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogArchiveScheduler {

    private final OperationLogMapper operationLogMapper;
    private final AccessLogMapper accessLogMapper;

    /** 每批处理的记录数 */
    private static final int BATCH_SIZE = 5000;

    /** 归档阈值：6个月 */
    private static final int ARCHIVE_MONTHS = 6;

    /**
     * 每月1日凌晨4点执行归档
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void archiveAuditLogs() {
        String beforeDate = calculateArchiveDate();
        log.info("开始执行审计日志归档任务，归档 {} 之前的日志", beforeDate);

        try {
            int opArchived = archiveOperationLogs(beforeDate);
            int accessArchived = archiveAccessLogs(beforeDate);
            log.info("审计日志归档完成: 操作日志归档 {} 条，访问日志归档 {} 条", opArchived, accessArchived);
        } catch (Exception e) {
            log.error("审计日志归档任务执行失败", e);
        }
    }

    /**
     * 归档操作日志
     * @return 归档的记录总数
     */
    public int archiveOperationLogs(String beforeDate) {
        int totalCount = operationLogMapper.countBeforeDate(beforeDate);
        if (totalCount == 0) {
            log.info("无需归档的操作日志");
            return 0;
        }

        log.info("待归档操作日志: {} 条", totalCount);
        int totalArchived = 0;

        // 分批迁移到归档表并删除原表数据
        while (totalArchived < totalCount) {
            // 插入归档表
            operationLogMapper.insertToArchive(beforeDate, BATCH_SIZE);
            // 删除原表数据
            int deleted = operationLogMapper.deleteBeforeDate(beforeDate, BATCH_SIZE);
            totalArchived += deleted;
            log.info("操作日志归档进度: {}/{}", totalArchived, totalCount);

            if (deleted == 0) {
                break;
            }
        }

        // 验证归档数据完整性
        int remainingCount = operationLogMapper.countBeforeDate(beforeDate);
        if (remainingCount > 0) {
            log.warn("操作日志归档后仍有 {} 条未归档记录", remainingCount);
        }

        return totalArchived;
    }

    /**
     * 归档访问日志
     * @return 归档的记录总数
     */
    public int archiveAccessLogs(String beforeDate) {
        int totalCount = accessLogMapper.countBeforeDate(beforeDate);
        if (totalCount == 0) {
            log.info("无需归档的访问日志");
            return 0;
        }

        log.info("待归档访问日志: {} 条", totalCount);
        int totalArchived = 0;

        while (totalArchived < totalCount) {
            accessLogMapper.insertToArchive(beforeDate, BATCH_SIZE);
            int deleted = accessLogMapper.deleteBeforeDate(beforeDate, BATCH_SIZE);
            totalArchived += deleted;
            log.info("访问日志归档进度: {}/{}", totalArchived, totalCount);

            if (deleted == 0) {
                break;
            }
        }

        int remainingCount = accessLogMapper.countBeforeDate(beforeDate);
        if (remainingCount > 0) {
            log.warn("访问日志归档后仍有 {} 条未归档记录", remainingCount);
        }

        return totalArchived;
    }

    /**
     * 计算归档截止日期（当前日期 - 6个月）
     */
    String calculateArchiveDate() {
        return LocalDate.now()
                .minusMonths(ARCHIVE_MONTHS)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
