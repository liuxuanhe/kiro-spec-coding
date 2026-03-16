package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 访问日志实体
 */
@Data
public class AccessLog {
    private Long id;
    private String requestId;
    private Long communityId;
    private Long userId;
    private String userName;
    private String userRole;
    private String userIp;
    private LocalDateTime accessTime;
    private String apiPath;
    private String httpMethod;
    private String queryParams;
    private String requestBody;
    private Integer responseCode;
    private Integer responseTime;
    private String userAgent;
    private LocalDateTime createTime;
}
