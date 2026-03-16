-- ============================================================
-- 地下停车场管理系统 - 数据库表结构
-- ============================================================

-- 注意：数据库由 Docker 的 MYSQL_DATABASE 环境变量自动创建，
-- 此脚本通过 docker-entrypoint-initdb.d 在该数据库上下文中执行，
-- 无需额外的建库或切库语句。

-- ============================================================
-- 2.1 核心业务表
-- ============================================================

-- 1. 小区表
CREATE TABLE IF NOT EXISTS sys_community (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '小区ID',
    community_name VARCHAR(100) NOT NULL COMMENT '小区名称',
    community_code VARCHAR(50) NOT NULL UNIQUE COMMENT '小区编码',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区县',
    address VARCHAR(200) NOT NULL COMMENT '详细地址',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active-正常, disabled-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_community_code (community_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小区表';

-- 2. 管理员表
CREATE TABLE IF NOT EXISTS sys_admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    community_id BIGINT NOT NULL COMMENT '小区ID, 0表示超级管理员',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone_number VARCHAR(20) NOT NULL COMMENT '手机号',
    role VARCHAR(20) NOT NULL COMMENT '角色: super_admin-超级管理员, property_admin-物业管理员',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active-正常, locked-锁定, disabled-禁用',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    password_expire_time DATETIME COMMENT '密码过期时间',
    must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT '是否必须修改密码: 0-否, 1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_community_id (community_id),
    INDEX idx_username (username),
    INDEX idx_phone_number (phone_number),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 3. 业主表
CREATE TABLE IF NOT EXISTS sys_owner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '业主ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    phone_number VARCHAR(20) NOT NULL COMMENT '手机号',
    id_card_last4 VARCHAR(4) NOT NULL COMMENT '身份证后4位',
    real_name VARCHAR(50) COMMENT '真实姓名',
    status VARCHAR(30) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待审核, approved-已通过, rejected-已驳回',
    reject_reason TEXT COMMENT '驳回原因',
    audit_admin_id BIGINT COMMENT '审核管理员ID',
    audit_time DATETIME COMMENT '审核时间',
    account_status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '账号状态: active-正常, disabled-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_community_house (community_id, house_no),
    INDEX idx_phone_number (phone_number),
    INDEX idx_status (status),
    INDEX idx_account_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业主表';

-- 4. 房屋号表
CREATE TABLE IF NOT EXISTS sys_house (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '房屋ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    building VARCHAR(50) NOT NULL COMMENT '楼栋',
    unit VARCHAR(50) COMMENT '单元',
    floor VARCHAR(20) COMMENT '楼层',
    room VARCHAR(20) COMMENT '房间号',
    area DECIMAL(10,2) COMMENT '面积(平方米)',
    status VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '状态: normal-正常, disabled-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    UNIQUE KEY uk_community_house (community_id, house_no),
    INDEX idx_building (community_id, building)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋号表';

-- 5. 业主房屋号关联表
CREATE TABLE IF NOT EXISTS sys_owner_house_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    owner_id BIGINT NOT NULL COMMENT '业主ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    relation_type VARCHAR(20) NOT NULL COMMENT '关系类型: owner-业主, family-家属, tenant-租户',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_owner (owner_id),
    INDEX idx_community_house (community_id, house_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业主房屋号关联表';

-- 6. 车牌表（包含 uk_community_house_primary 唯一索引）
CREATE TABLE IF NOT EXISTS sys_car_plate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '车牌ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    owner_id BIGINT NOT NULL COMMENT '业主ID',
    car_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    car_brand VARCHAR(50) COMMENT '车辆品牌',
    car_model VARCHAR(50) COMMENT '车辆型号',
    car_color VARCHAR(20) COMMENT '车辆颜色',
    status VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '状态: normal-普通, primary-主车辆, disabled-禁用, deleted-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    UNIQUE KEY uk_community_car (community_id, car_number, is_deleted),
    INDEX idx_owner (owner_id),
    INDEX idx_community_house (community_id, house_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车牌表';

-- Primary 车辆唯一约束：通过生成列 + 唯一索引实现条件唯一索引
-- MySQL 不支持部分索引，使用生成列模拟：当 status='primary' 时生成 house_no，否则为 NULL
ALTER TABLE sys_car_plate
    ADD COLUMN primary_house_no VARCHAR(50) GENERATED ALWAYS AS (
        CASE WHEN status = 'primary' AND is_deleted = 0 THEN house_no ELSE NULL END
    ) STORED COMMENT '用于 Primary 唯一约束的生成列';

ALTER TABLE sys_car_plate
    ADD UNIQUE KEY uk_community_house_primary (community_id, primary_house_no);

-- 7. 停车场配置表
CREATE TABLE IF NOT EXISTS parking_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    community_id BIGINT NOT NULL UNIQUE COMMENT '小区ID',
    total_spaces INT NOT NULL COMMENT '总车位数',
    reserved_spaces INT NOT NULL DEFAULT 0 COMMENT '预留车位数',
    visitor_quota_hours INT NOT NULL DEFAULT 72 COMMENT '月度Visitor配额(小时)',
    visitor_single_duration_hours INT NOT NULL DEFAULT 24 COMMENT '单次Visitor时长限制(小时)',
    visitor_activation_window_hours INT NOT NULL DEFAULT 24 COMMENT 'Visitor激活窗口(小时)',
    zombie_vehicle_threshold_days INT NOT NULL DEFAULT 7 COMMENT '僵尸车辆阈值(天)',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_community_id (community_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='停车场配置表';

-- ============================================================
-- 2.2 入场记录分表
-- ============================================================

-- 分表模板
CREATE TABLE IF NOT EXISTS parking_car_record_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    car_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    vehicle_type VARCHAR(20) NOT NULL COMMENT '车辆类型: primary-主车辆, visitor-访客车辆',
    enter_time DATETIME NOT NULL COMMENT '入场时间',
    exit_time DATETIME COMMENT '出场时间',
    duration INT COMMENT '停放时长(分钟)',
    status VARCHAR(30) NOT NULL COMMENT '状态: entered-已入场, exited-已出场, exit_exception-出场异常',
    exception_reason TEXT COMMENT '异常原因',
    handler_admin_id BIGINT COMMENT '处理管理员ID',
    handle_time DATETIME COMMENT '处理时间',
    handle_remark TEXT COMMENT '处理备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_community_enter_time (community_id, enter_time),
    INDEX idx_community_car_enter (community_id, car_number, enter_time),
    INDEX idx_community_house_enter (community_id, house_no, enter_time),
    INDEX idx_community_status_enter (community_id, status, enter_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入场记录分表模板';

-- 当前月份及未来3个月的分表（基于 2026-03）
CREATE TABLE IF NOT EXISTS parking_car_record_202603 LIKE parking_car_record_template;
ALTER TABLE parking_car_record_202603 COMMENT='入场记录表-2026年3月';

CREATE TABLE IF NOT EXISTS parking_car_record_202604 LIKE parking_car_record_template;
ALTER TABLE parking_car_record_202604 COMMENT='入场记录表-2026年4月';

CREATE TABLE IF NOT EXISTS parking_car_record_202605 LIKE parking_car_record_template;
ALTER TABLE parking_car_record_202605 COMMENT='入场记录表-2026年5月';

CREATE TABLE IF NOT EXISTS parking_car_record_202606 LIKE parking_car_record_template;
ALTER TABLE parking_car_record_202606 COMMENT='入场记录表-2026年6月';

-- ============================================================
-- 2.3 Visitor 相关表
-- ============================================================

-- Visitor 申请表
CREATE TABLE IF NOT EXISTS visitor_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    owner_id BIGINT NOT NULL COMMENT '申请业主ID',
    car_plate_id BIGINT NOT NULL COMMENT '车牌ID',
    car_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    apply_reason TEXT COMMENT '申请原因',
    status VARCHAR(30) NOT NULL DEFAULT 'submitted' COMMENT '状态: submitted-已提交, approved_pending_activation-审批通过待激活, rejected-已驳回',
    reject_reason TEXT COMMENT '驳回原因',
    audit_admin_id BIGINT COMMENT '审批管理员ID',
    audit_time DATETIME COMMENT '审批时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_community_house (community_id, house_no),
    INDEX idx_owner (owner_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Visitor申请表';

-- Visitor 授权表
CREATE TABLE IF NOT EXISTS visitor_authorization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '授权ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    application_id BIGINT NOT NULL COMMENT '申请ID',
    car_plate_id BIGINT NOT NULL COMMENT '车牌ID',
    car_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    status VARCHAR(30) NOT NULL COMMENT '状态: approved_pending_activation-待激活, activated-已激活, canceled_no_entry-未入场取消, expired-已过期, unavailable-不可用',
    start_time DATETIME NOT NULL COMMENT '授权开始时间',
    expire_time DATETIME NOT NULL COMMENT '授权过期时间(24小时激活窗口)',
    activation_time DATETIME COMMENT '激活时间(首次入场时间)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_community_car (community_id, car_number),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time),
    INDEX idx_application (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Visitor授权表';

-- Visitor 会话表
CREATE TABLE IF NOT EXISTS visitor_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    authorization_id BIGINT NOT NULL COMMENT '授权ID',
    car_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    session_start DATETIME NOT NULL COMMENT '会话开始时间(首次入场时间)',
    last_entry_time DATETIME COMMENT '最后一次入场时间',
    accumulated_duration INT NOT NULL DEFAULT 0 COMMENT '累计停放时长(分钟)',
    status VARCHAR(20) NOT NULL COMMENT '状态: in_park-在场, out_of_park-离场',
    timeout_notified TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送超时提醒: 0-否, 1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_authorization (authorization_id),
    INDEX idx_community_house (community_id, house_no),
    INDEX idx_status (status),
    INDEX idx_accumulated_duration (accumulated_duration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Visitor会话表';

-- ============================================================
-- 2.4 审计日志表
-- ============================================================

-- 操作日志表（按月分区）
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    request_id VARCHAR(100) NOT NULL COMMENT '请求唯一标识',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    operator_role VARCHAR(20) NOT NULL COMMENT '操作人角色',
    operator_ip VARCHAR(50) NOT NULL COMMENT '操作人IP',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_time DATETIME NOT NULL COMMENT '操作时间',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    before_value TEXT COMMENT '操作前值(JSON)',
    after_value TEXT COMMENT '操作后值(JSON)',
    operation_result VARCHAR(20) NOT NULL COMMENT '操作结果: success-成功, failure-失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_request_id (request_id),
    INDEX idx_community_id (community_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_operation_time (operation_time),
    INDEX idx_target (target_type, target_id),
    PRIMARY KEY (id, operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表'
PARTITION BY RANGE (TO_DAYS(operation_time)) (
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p202604 VALUES LESS THAN (TO_DAYS('2026-05-01')),
    PARTITION p202605 VALUES LESS THAN (TO_DAYS('2026-06-01')),
    PARTITION p202606 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p202607 VALUES LESS THAN (TO_DAYS('2026-08-01')),
    PARTITION p202608 VALUES LESS THAN (TO_DAYS('2026-09-01')),
    PARTITION p202609 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p202610 VALUES LESS THAN (TO_DAYS('2026-11-01')),
    PARTITION p202611 VALUES LESS THAN (TO_DAYS('2026-12-01')),
    PARTITION p202612 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- 访问日志表（按月分区）
CREATE TABLE IF NOT EXISTS sys_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    request_id VARCHAR(100) NOT NULL COMMENT '请求唯一标识',
    community_id BIGINT COMMENT '小区ID',
    user_id BIGINT COMMENT '访问人ID',
    user_name VARCHAR(50) COMMENT '访问人姓名',
    user_role VARCHAR(20) COMMENT '访问人角色',
    user_ip VARCHAR(50) NOT NULL COMMENT '访问人IP',
    access_time DATETIME NOT NULL COMMENT '访问时间',
    api_path VARCHAR(200) NOT NULL COMMENT '接口路径',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    query_params TEXT COMMENT '查询参数(JSON)',
    request_body TEXT COMMENT '请求体(JSON)',
    response_code INT NOT NULL COMMENT '响应码',
    response_time INT NOT NULL COMMENT '响应时间(毫秒)',
    user_agent VARCHAR(500) COMMENT 'User-Agent',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_request_id (request_id),
    INDEX idx_community_id (community_id),
    INDEX idx_user_id (user_id),
    INDEX idx_access_time (access_time),
    INDEX idx_api_path (api_path),
    INDEX idx_response_time (response_time),
    PRIMARY KEY (id, access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表'
PARTITION BY RANGE (TO_DAYS(access_time)) (
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p202604 VALUES LESS THAN (TO_DAYS('2026-05-01')),
    PARTITION p202605 VALUES LESS THAN (TO_DAYS('2026-06-01')),
    PARTITION p202606 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p202607 VALUES LESS THAN (TO_DAYS('2026-08-01')),
    PARTITION p202608 VALUES LESS THAN (TO_DAYS('2026-09-01')),
    PARTITION p202609 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p202610 VALUES LESS THAN (TO_DAYS('2026-11-01')),
    PARTITION p202611 VALUES LESS THAN (TO_DAYS('2026-12-01')),
    PARTITION p202612 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- ============================================================
-- 2.5 辅助功能表
-- ============================================================

-- 每日统计预聚合表
CREATE TABLE IF NOT EXISTS parking_stat_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_entry_count INT NOT NULL DEFAULT 0 COMMENT '当日入场总数',
    total_exit_count INT NOT NULL DEFAULT 0 COMMENT '当日出场总数',
    primary_entry_count INT NOT NULL DEFAULT 0 COMMENT 'Primary车辆入场数',
    visitor_entry_count INT NOT NULL DEFAULT 0 COMMENT 'Visitor车辆入场数',
    peak_hour INT COMMENT '峰值时段(小时)',
    peak_count INT COMMENT '峰值时段车辆数',
    avg_parking_duration INT COMMENT '平均停放时长(分钟)',
    zombie_vehicle_count INT NOT NULL DEFAULT 0 COMMENT '僵尸车辆数',
    exception_exit_count INT NOT NULL DEFAULT 0 COMMENT '异常出场数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_community_date (community_id, stat_date),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计预聚合表';

-- IP 白名单表
CREATE TABLE IF NOT EXISTS sys_ip_whitelist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '白名单ID',
    community_id BIGINT NOT NULL COMMENT '小区ID, 0表示全局',
    ip_address VARCHAR(50) NOT NULL COMMENT 'IP地址',
    ip_range VARCHAR(100) COMMENT 'IP段(CIDR格式)',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型: export_raw_data-导出原始数据, modify_config-修改配置, disable_account-注销账号',
    description VARCHAR(200) COMMENT '描述',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active-启用, disabled-禁用',
    create_admin_id BIGINT NOT NULL COMMENT '创建管理员ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_community_id (community_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_operation_type (operation_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP白名单表';

-- 僵尸车辆表
CREATE TABLE IF NOT EXISTS zombie_vehicle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '僵尸车辆ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    car_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    entry_record_id BIGINT NOT NULL COMMENT '入场记录ID',
    enter_time DATETIME NOT NULL COMMENT '入场时间',
    continuous_days INT NOT NULL COMMENT '连续在场天数',
    status VARCHAR(20) NOT NULL DEFAULT 'unhandled' COMMENT '状态: unhandled-未处理, contacted-已联系, resolved-已解决, ignored-已忽略',
    contact_record TEXT COMMENT '联系记录',
    solution TEXT COMMENT '解决方案',
    ignore_reason TEXT COMMENT '忽略原因',
    handler_admin_id BIGINT COMMENT '处理管理员ID',
    handle_time DATETIME COMMENT '处理时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_community_id (community_id),
    INDEX idx_car_number (car_number),
    INDEX idx_status (status),
    INDEX idx_continuous_days (continuous_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='僵尸车辆表';

-- 敏感信息修改申请表
CREATE TABLE IF NOT EXISTS owner_info_modify_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    owner_id BIGINT NOT NULL COMMENT '业主ID',
    modify_type VARCHAR(20) NOT NULL COMMENT '修改类型: phone-手机号, id_card-身份证',
    old_value VARCHAR(100) NOT NULL COMMENT '原值(加密)',
    new_value VARCHAR(100) NOT NULL COMMENT '新值(加密)',
    apply_reason TEXT COMMENT '申请原因',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待审核, approved-已通过, rejected-已驳回',
    reject_reason TEXT COMMENT '驳回原因',
    audit_admin_id BIGINT COMMENT '审批管理员ID',
    audit_time DATETIME COMMENT '审批时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_owner (owner_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感信息修改申请表';

-- 导出任务表
CREATE TABLE IF NOT EXISTS export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    export_type VARCHAR(50) NOT NULL COMMENT '导出类型: parking_records-入场记录, audit_logs-审计日志',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    query_params TEXT NOT NULL COMMENT '查询参数(JSON)',
    need_raw_data TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要原始数据: 0-否, 1-是',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待处理, processing-处理中, completed-已完成, failed-失败',
    file_url VARCHAR(500) COMMENT '文件下载地址',
    file_size BIGINT COMMENT '文件大小(字节)',
    record_count INT COMMENT '记录数量',
    error_message TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    expire_time DATETIME COMMENT '文件过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_community_id (community_id),
    INDEX idx_operator_id (operator_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务表';

-- 验证码表
CREATE TABLE IF NOT EXISTS verification_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '验证码ID',
    phone_number VARCHAR(20) NOT NULL COMMENT '手机号',
    code VARCHAR(10) NOT NULL COMMENT '验证码',
    code_type VARCHAR(20) NOT NULL COMMENT '验证码类型: register-注册, login-登录, modify-修改信息',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '验证失败次数',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active-有效, used-已使用, expired-已过期, locked-已锁定',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_phone_number (phone_number),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码表';

-- 硬件设备表（预留）
CREATE TABLE IF NOT EXISTS hardware_device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    device_type VARCHAR(20) NOT NULL COMMENT '设备类型: gate-道闸, camera-车牌识别',
    device_code VARCHAR(50) NOT NULL UNIQUE COMMENT '设备编码',
    device_name VARCHAR(100) NOT NULL COMMENT '设备名称',
    location VARCHAR(200) COMMENT '设备位置',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    secret_key VARCHAR(100) NOT NULL COMMENT '签名密钥',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active-正常, offline-离线, disabled-禁用',
    last_heartbeat_time DATETIME COMMENT '最后心跳时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    deleted_time DATETIME COMMENT '删除时间',
    INDEX idx_community_id (community_id),
    INDEX idx_device_code (device_code),
    INDEX idx_device_type (device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='硬件设备表';

-- 停车费用表（预留）
CREATE TABLE IF NOT EXISTS parking_fee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '费用ID',
    community_id BIGINT NOT NULL COMMENT '小区ID',
    house_no VARCHAR(50) NOT NULL COMMENT '房屋号',
    car_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    entry_record_id BIGINT NOT NULL COMMENT '入场记录ID',
    fee_amount DECIMAL(10,2) NOT NULL COMMENT '费用金额',
    fee_type VARCHAR(20) NOT NULL COMMENT '费用类型: parking-停车费, overtime-超时费',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'unpaid' COMMENT '支付状态: unpaid-未支付, paid-已支付, refunded-已退款',
    payment_method VARCHAR(20) COMMENT '支付方式: wechat-微信, alipay-支付宝',
    payment_time DATETIME COMMENT '支付时间',
    transaction_id VARCHAR(100) COMMENT '交易流水号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_community_id (community_id),
    INDEX idx_house_no (house_no),
    INDEX idx_entry_record (entry_record_id),
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='停车费用表';
