package com.parking.service;

import com.parking.mapper.ExportTaskMapper;
import com.parking.model.ExportTask;
import com.parking.service.impl.ExportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExportServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ExportTaskMapper exportTaskMapper;

    @Mock
    private ExportTaskProcessor exportTaskProcessor;

    @InjectMocks
    private ExportServiceImpl exportService;

    @Test
    @DisplayName("创建入场记录导出任务 - 成功")
    void createParkingRecordExport_success() {
        doAnswer(invocation -> {
            ExportTask t = invocation.getArgument(0);
            t.setId(1L);
            return null;
        }).when(exportTaskMapper).insert(any(ExportTask.class));

        ExportTask result = exportService.createParkingRecordExport(
                1001L, 100L, "管理员A", "{}", 0);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("parking_record", result.getExportType());
        assertEquals("pending", result.getStatus());
        assertEquals(1001L, result.getCommunityId());
        verify(exportTaskProcessor).processExportTask(1L);
    }

    @Test
    @DisplayName("创建入场记录导出任务 - needRawData 为 null 时默认0")
    void createParkingRecordExport_needRawDataNull() {
        doAnswer(invocation -> {
            ExportTask t = invocation.getArgument(0);
            t.setId(2L);
            return null;
        }).when(exportTaskMapper).insert(any(ExportTask.class));

        ExportTask result = exportService.createParkingRecordExport(
                1001L, 100L, "管理员B", null, null);

        assertEquals(0, result.getNeedRawData());
        assertEquals("parking_record", result.getExportType());
    }

    @Test
    @DisplayName("查询导出任务状态 - 任务存在")
    void getExportTaskStatus_found() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setStatus("completed");
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        ExportTask result = exportService.getExportTaskStatus(1L);

        assertNotNull(result);
        assertEquals("completed", result.getStatus());
    }

    @Test
    @DisplayName("查询导出任务状态 - 任务不存在")
    void getExportTaskStatus_notFound() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);

        ExportTask result = exportService.getExportTaskStatus(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("获取可下载任务 - 成功")
    void getDownloadableTask_success() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setStatus("completed");
        task.setFileUrl("/exports/1.csv");
        task.setExpireTime(LocalDateTime.now().plusHours(24));
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        ExportTask result = exportService.getDownloadableTask(1L);

        assertNotNull(result);
        assertEquals("/exports/1.csv", result.getFileUrl());
    }

    @Test
    @DisplayName("获取可下载任务 - 任务不存在抛异常")
    void getDownloadableTask_notFound() {
        when(exportTaskMapper.selectById(999L)).thenReturn(null);

        assertThrows(com.parking.common.BusinessException.class,
                () -> exportService.getDownloadableTask(999L));
    }

    @Test
    @DisplayName("获取可下载任务 - 未完成抛异常")
    void getDownloadableTask_notCompleted() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setStatus("processing");
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        assertThrows(com.parking.common.BusinessException.class,
                () -> exportService.getDownloadableTask(1L));
    }

    @Test
    @DisplayName("获取可下载任务 - 文件已过期抛异常")
    void getDownloadableTask_expired() {
        ExportTask task = new ExportTask();
        task.setId(1L);
        task.setStatus("completed");
        task.setExpireTime(LocalDateTime.now().minusHours(1));
        when(exportTaskMapper.selectById(1L)).thenReturn(task);

        assertThrows(com.parking.common.BusinessException.class,
                () -> exportService.getDownloadableTask(1L));
    }
}
