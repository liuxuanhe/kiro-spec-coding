package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 验证码实体
 */
@Data
public class VerificationCode {
    private Long id;
    private String phoneNumber;
    private String code;
    private String codeType;
    private Integer failCount;
    private String status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
