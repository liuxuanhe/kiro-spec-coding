package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Visitor 会话实体
 */
@Data
public class VisitorSession {
    private Long id;
    private Long communityId;
    private String houseNo;
    private Long authorizationId;
    private String carNumber;
    private LocalDateTime sessionStart;
    private LocalDateTime lastEntryTime;
    private Integer accumulatedDuration;
    private String status;
    private Integer timeoutNotified;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
