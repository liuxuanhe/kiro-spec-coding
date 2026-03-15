package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 导出任务实体
 */
@Data
public class ExportTask {
    private Long id;
    private Long communityId;
    private String exportType;
    private Long operatorId;
    private String operatorName;
    private String queryParams;
    private Integer needRawData;
    private String status;
    private String fileUrl;
    private Long fileSize;
    private Integer recordCount;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
