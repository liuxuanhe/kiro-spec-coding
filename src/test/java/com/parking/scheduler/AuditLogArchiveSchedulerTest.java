package com.parking.scheduler;

import com.parking.mapper.AccessLogMapper;
import com.parking.mapper.OperationLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuditLogArchiveScheduler 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuditLogArchiveSchedulerTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @Mock
    private AccessLogMapper accessLogMapper;

    @InjectMocks
    private AuditLogArchiveScheduler scheduler;

    @Test
    @DisplayName("计算归档截止日期 - 当前日期减6个月")
    void calculateArchiveDate_sixMonthsAgo() {
        String result = scheduler.calculateArchiveDate();
        String expected = LocalDate.now().minusMonths(6)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("归档操作日志 - 无数据时返回0")
    void archiveOperationLogs_noData() {
        when(operationLogMapper.countBeforeDate(anyString())).thenReturn(0);

        int result = scheduler.archiveOperationLogs("2025-09-15");

        assertEquals(0, result);
        verify(operationLogMapper, never()).insertToArchive(anyString(), anyInt());
        verify(operationLogMapper, never()).deleteBeforeDate(anyString(), anyInt());
    }

    @Test
    @DisplayName("归档操作日志 - 有数据时分批归档")
    void archiveOperationLogs_withData() {
        when(operationLogMapper.countBeforeDate("2025-09-15"))
                .thenReturn(100)
                .thenReturn(0);
        when(operationLogMapper.deleteBeforeDate("2025-09-15", 5000)).thenReturn(100);

        int result = scheduler.archiveOperationLogs("2025-09-15");

        assertEquals(100, result);
        verify(operationLogMapper).insertToArchive("2025-09-15", 5000);
        verify(operationLogMapper).deleteBeforeDate("2025-09-15", 5000);
    }

    @Test
    @DisplayName("归档访问日志 - 无数据时返回0")
    void archiveAccessLogs_noData() {
        when(accessLogMapper.countBeforeDate(anyString())).thenReturn(0);

        int result = scheduler.archiveAccessLogs("2025-09-15");

        assertEquals(0, result);
        verify(accessLogMapper, never()).insertToArchive(anyString(), anyInt());
    }

    @Test
    @DisplayName("归档访问日志 - 有数据时分批归档")
    void archiveAccessLogs_withData() {
        when(accessLogMapper.countBeforeDate("2025-09-15"))
                .thenReturn(200)
                .thenReturn(0);
        when(accessLogMapper.deleteBeforeDate("2025-09-15", 5000)).thenReturn(200);

        int result = scheduler.archiveAccessLogs("2025-09-15");

        assertEquals(200, result);
        verify(accessLogMapper).insertToArchive("2025-09-15", 5000);
        verify(accessLogMapper).deleteBeforeDate("2025-09-15", 5000);
    }

    @Test
    @DisplayName("归档完整流程 - 操作日志和访问日志均归档")
    void archiveAuditLogs_fullFlow() {
        // 操作日志
        when(operationLogMapper.countBeforeDate(anyString()))
                .thenReturn(50)
                .thenReturn(0);
        when(operationLogMapper.deleteBeforeDate(anyString(), anyInt())).thenReturn(50);

        // 访问日志
        when(accessLogMapper.countBeforeDate(anyString()))
                .thenReturn(30)
                .thenReturn(0);
        when(accessLogMapper.deleteBeforeDate(anyString(), anyInt())).thenReturn(30);

        scheduler.archiveAuditLogs();

        verify(operationLogMapper).insertToArchive(anyString(), eq(5000));
        verify(accessLogMapper).insertToArchive(anyString(), eq(5000));
    }
}
