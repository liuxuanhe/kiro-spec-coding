package com.parking.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员实体
 */
@Data
public class Admin {
    private Long id;
    private Long communityId;
    private String username;
    private String password;
    private String realName;
    private String phoneNumber;
    private String role;
    private String status;
    private Integer loginFailCount;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime passwordExpireTime;
    private Integer mustChangePassword;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private LocalDateTime deletedTime;
}
