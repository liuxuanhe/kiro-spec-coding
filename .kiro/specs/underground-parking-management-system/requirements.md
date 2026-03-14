# Requirements Document

## Introduction

地下停车场管理系统是一个企业级多小区 SaaS 平台，服务于物业管理方与业主，实现停车场智能化管理、车位管控、访客授权、数据追溯与审计合规。系统支持多小区数据隔离、房屋号数据域管理、Primary 车辆先到先得机制、Visitor 权限时长配额管理，确保功能闭环、数据准确可追溯、关键路径无核心 bug。

## Glossary

- **System**: 地下停车场管理系统
- **Owner_App**: 业主小程序端
- **Admin_Portal**: 物业管理后台
- **Super_Admin**: 超级管理员角色
- **Property_Admin**: 物业管理员角色
- **Owner**: 业主角色
- **Community**: 小区实体，由 community_id 唯一标识
- **House_No**: 房屋号，在小区内唯一标识一个房屋单元
- **Data_Domain**: 数据域，由 community_id + house_no 组成的业主侧核心数据单位
- **Primary_Vehicle**: 主车辆，每个房屋号最多只能有1个 Primary 车辆
- **Normal_Vehicle**: 普通车辆，业主绑定的非 Primary 车辆
- **Visitor_Vehicle**: 访客车辆，由业主申请、物业审批的临时访问车辆
- **Parking_Space**: 停车位
- **Available_Spaces**: 可用车位数，计算公式为 total_spaces - 当前在场车辆数
- **Visitor_Available_Spaces**: Visitor 可开放车位数
- **Monthly_Quota**: 月度配额，每个房屋号每自然月最多72小时 Visitor 访问时长
- **Entry_Record**: 入场记录
- **Exit_Record**: 出场记录
- **Audit_Log**: 审计日志
- **Operation_Log**: 操作日志
- **Access_Log**: 访问日志
- **Idempotency_Key**: 幂等键
- **Request_ID**: 请求唯一标识符
- **Error_Code**: 错误码，格式为 PARKING_XXXX
- **Desensitization**: 脱敏处理
- **Sharding_Table**: 分表，按月分表的入场记录表
- **Cursor_Pagination**: 游标分页
- **Rate_Limiting**: 限流
- **IP_Whitelist**: IP 白名单
- **Signature**: 签名验证
- **Nonce**: 防重放随机数
- **Zombie_Vehicle**: 僵尸车辆，长时间未出场的车辆

## Requirements


### Requirement 1: 业主注册与身份验证

**User Story:** 作为业主，我希望通过手机号和房屋号注册账号，以便使用停车场管理功能。

#### Acceptance Criteria

1. WHEN 业主提交注册信息（手机号、验证码、小区房屋号、身份证后4位），THE Owner_App SHALL 验证所有字段格式的有效性
2. WHEN 验证码验证失败次数达到3次，THE System SHALL 锁定该手机号10分钟并返回错误码 PARKING_1001
3. WHEN 验证码超过5分钟未使用，THE System SHALL 使验证码失效并返回错误码 PARKING_1002
4. WHEN 注册信息验证通过，THE System SHALL 创建业主账号并设置状态为 pending
5. WHEN 业主账号创建成功，THE System SHALL 绑定账号到指定的 community_id 和 house_no
6. WHEN 业主账号创建成功，THE System SHALL 记录操作日志到 sys_operation_log 表
7. THE System SHALL 允许同一 house_no 下存在多个业主账号

### Requirement 2: 业主审核流程

**User Story:** 作为物业管理员，我希望审核业主注册申请，以便确保业主身份真实有效。

#### Acceptance Criteria

1. WHEN 物业管理员审核业主申请并选择通过，THE Admin_Portal SHALL 将业主账号状态从 pending 更新为 approved
2. WHEN 物业管理员审核业主申请并选择驳回，THE Admin_Portal SHALL 将业主账号状态从 pending 更新为 rejected 并要求填写驳回原因
3. IF 业主账号状态不是 pending，THEN THE System SHALL 拒绝审核操作并返回错误码 PARKING_2001
4. WHEN 审核操作完成，THE System SHALL 记录操作日志包含 before 和 after 状态
5. WHEN 审核操作完成，THE System SHALL 通过订阅消息推送通知业主审核结果
6. IF 订阅消息推送失败，THEN THE System SHALL 执行重试机制最多3次
7. WHEN 业主账号被驳回后重新提交，THE System SHALL 保留所有审核版本历史
8. THE System SHALL 使用幂等键防止并发重复审核同一申请


### Requirement 3: 车牌管理与格式验证

**User Story:** 作为业主，我希望添加和管理我的车牌信息，以便使用停车场服务。

#### Acceptance Criteria

1. THE Owner_App SHALL 限制每个业主账号最多绑定5个车牌
2. WHEN 业主添加车牌，THE System SHALL 验证车牌格式符合中国车牌标准
3. WHEN 业主添加车牌，THE System SHALL 验证该车牌在当前 community_id 下未被其他业主绑定
4. WHEN 业主添加车牌成功，THE System SHALL 将车牌状态设置为 normal
5. WHEN 业主尝试添加第6个车牌，THE System SHALL 拒绝操作并返回错误码 PARKING_3001
6. WHEN 业主删除普通车牌，THE System SHALL 验证该车牌当前不在场
7. IF 车牌当前在场，THEN THE System SHALL 拒绝删除操作并返回错误码 PARKING_3002
8. WHEN 业主删除车牌成功，THE System SHALL 执行逻辑删除并设置 is_deleted 为 true
9. THE System SHALL 记录所有车牌操作到 sys_operation_log 表

### Requirement 4: Primary 车辆设置与房屋号约束

**User Story:** 作为业主，我希望设置一个 Primary 车辆，以便享受自动入场权限。

#### Acceptance Criteria

1. THE System SHALL 确保每个 Data_Domain（community_id + house_no）最多只能有1个 Primary_Vehicle
2. WHEN 业主设置 Primary 车辆，THE System SHALL 验证该房屋号下所有车辆均不在场
3. WHEN 业主设置 Primary 车辆，THE System SHALL 验证原 Primary 车辆无未完成的入场申请
4. WHEN 业主设置 Primary 车辆，THE System SHALL 要求业主进行二次确认
5. WHEN Primary 车辆切换成功，THE System SHALL 将旧 Primary 车辆状态更新为 normal
6. WHEN Primary 车辆切换成功，THE System SHALL 将新 Primary 车辆状态更新为 primary
7. IF 房屋号下任何车辆在场，THEN THE System SHALL 拒绝 Primary 切换并返回错误码 PARKING_4001
8. IF 原 Primary 车辆有未完成入场申请，THEN THE System SHALL 拒绝切换并返回错误码 PARKING_4002
9. THE System SHALL 使用数据库唯一索引确保 (community_id, house_no, status=primary) 的唯一性
10. WHEN Primary 车辆设置操作执行，THE System SHALL 使用行级锁防止并发冲突


### Requirement 5: Primary 车辆自动入场与先到先得机制

**User Story:** 作为业主，我希望我的 Primary 车辆能够自动入场，无需手动申请。

#### Acceptance Criteria

1. WHEN Primary_Vehicle 到达停车场入口，THE System SHALL 自动执行入场校验
2. WHEN 入场校验执行时，THE System SHALL 计算 Available_Spaces = total_spaces - 当前在场车辆数
3. IF Available_Spaces > 0，THEN THE System SHALL 允许 Primary_Vehicle 入场
4. IF Available_Spaces ≤ 0，THEN THE System SHALL 拒绝入场并返回错误码 PARKING_5001
5. WHEN Primary_Vehicle 入场成功，THE System SHALL 创建入场记录并设置状态为 entered
6. WHEN Primary_Vehicle 入场成功，THE System SHALL 记录 enter_time、car_number、community_id、house_no
7. WHEN 同一车牌在5分钟内重复触发入场事件，THE System SHALL 使用幂等键防止重复创建入场记录
8. THE System SHALL 允许 Primary_Vehicle 注册数量超过停车场总车位数
9. THE System SHALL 使用先到先得机制分配车位给 Primary_Vehicle 和 Visitor_Vehicle
10. WHEN 入场操作执行，THE System SHALL 使用分布式锁确保车位数量计算的一致性

### Requirement 6: 车辆出场记录

**User Story:** 作为业主，我希望系统记录我的车辆出场信息，以便查询停车历史。

#### Acceptance Criteria

1. WHEN 车辆离开停车场，THE System SHALL 查找对应的入场记录
2. IF 找到入场记录，THEN THE System SHALL 更新入场记录状态为 exited 并记录 exit_time
3. IF 未找到入场记录，THEN THE System SHALL 创建异常出场记录并设置状态为 exit_exception
4. WHEN 创建异常出场记录，THE System SHALL 记录车牌号、出场时间、异常原因
5. WHEN 异常出场发生，THE System SHALL 通知物业管理员处理
6. WHEN 物业管理员处理异常出场，THE System SHALL 要求填写处理原因并记录到 sys_operation_log
7. WHEN 车辆出场成功，THE System SHALL 更新 Available_Spaces 计算
8. THE System SHALL 将出场记录写入按月分表 parking_car_record_yyyymm


### Requirement 7: Visitor 权限申请与审批

**User Story:** 作为业主，我希望为我的非 Primary 车辆申请 Visitor 访问权限，以便临时使用停车场。

#### Acceptance Criteria

1. WHEN 业主为 Normal_Vehicle 申请 Visitor 权限，THE Owner_App SHALL 验证该车牌已绑定到业主账号
2. WHEN 业主提交 Visitor 申请，THE System SHALL 验证该房屋号当月累计 Visitor 时长未超过72小时
3. IF 当月累计时长 ≥ 72小时，THEN THE System SHALL 拒绝申请并返回错误码 PARKING_7001
4. WHEN Visitor 申请提交成功，THE System SHALL 创建申请记录并设置状态为 submitted
5. WHEN 物业管理员审批 Visitor 申请并选择通过，THE Admin_Portal SHALL 更新申请状态为 approved_pending_activation
6. WHEN 物业管理员审批 Visitor 申请并选择驳回，THE Admin_Portal SHALL 更新申请状态为 rejected 并要求填写驳回原因
7. WHEN Visitor 申请审批通过，THE System SHALL 设置权限有效期为24小时待激活窗口
8. WHEN Visitor 申请审批通过，THE System SHALL 通过订阅消息通知业主
9. WHEN Visitor 申请审批操作执行，THE System SHALL 记录操作日志到 sys_operation_log
10. THE System SHALL 使用幂等键防止并发重复审批同一申请

### Requirement 8: Visitor 权限激活与时长累计

**User Story:** 作为业主，我希望 Visitor 车辆在首次入场后激活权限并开始累计停放时长。

#### Acceptance Criteria

1. WHEN Visitor_Vehicle 首次入场，THE System SHALL 验证权限状态为 approved_pending_activation
2. WHEN Visitor_Vehicle 首次入场，THE System SHALL 验证当前时间在24小时待激活窗口内
3. IF 当前时间超过待激活窗口，THEN THE System SHALL 拒绝入场并将权限状态更新为 canceled_no_entry
4. WHEN Visitor_Vehicle 首次入场成功，THE System SHALL 更新权限状态为 activated
5. WHEN Visitor_Vehicle 首次入场成功，THE System SHALL 开始累计停放时长
6. WHILE Visitor_Vehicle 在场，THE System SHALL 持续累计停放时长
7. WHEN Visitor_Vehicle 出场，THE System SHALL 停止累计停放时长
8. WHEN Visitor_Vehicle 再次入场，THE System SHALL 继续累计停放时长
9. WHEN 累计停放时长达到24小时，THE System SHALL 记录超时并触发提醒
10. WHEN 累计停放时长超过24小时且车辆仍在场，THE System SHALL 通知业主和物业管理员
11. THE System SHALL 将 Visitor 停放时长累计到房屋号月度配额统计


### Requirement 9: Visitor 可开放车位数计算与动态调整

**User Story:** 作为物业管理员，我希望系统动态计算 Visitor 可开放车位数，以便合理分配停车资源。

#### Acceptance Criteria

1. THE System SHALL 计算 Visitor_Available_Spaces = total_spaces - 当前在场车辆数
2. WHEN Visitor_Available_Spaces ≤ 0，THE System SHALL 拒绝新的 Visitor 申请并返回错误码 PARKING_9001
3. WHEN 车辆入场，THE System SHALL 更新 Visitor_Available_Spaces 计算
4. WHEN 车辆出场，THE System SHALL 更新 Visitor_Available_Spaces 计算
5. WHEN 物业管理员修改 total_spaces，THE System SHALL 立即重新计算 Visitor_Available_Spaces
6. WHEN 物业管理员修改 total_spaces，THE System SHALL 验证新 total_spaces ≥ 当前在场车辆数
7. IF 新 total_spaces < 当前在场车辆数，THEN THE System SHALL 拒绝修改并返回错误码 PARKING_9002
8. WHEN total_spaces 修改后导致 Visitor_Available_Spaces ≤ 0，THE System SHALL 将已审批但未入场的 Visitor 权限状态更新为 unavailable
9. THE System SHALL 使用分布式锁确保 Visitor_Available_Spaces 计算的最终一致性
10. THE System SHALL 允许 Visitor_Vehicle 和 Primary_Vehicle 参与先到先得竞争

### Requirement 10: 月度 Visitor 配额管理

**User Story:** 作为物业管理员，我希望系统限制每个房屋号的月度 Visitor 访问时长，以便公平分配停车资源。

#### Acceptance Criteria

1. THE System SHALL 为每个 Data_Domain 设置月度配额为72小时
2. WHEN 计算月度配额使用量，THE System SHALL 按 community_id + house_no 汇总当月所有 Visitor 车辆的实际在场停放时长
3. WHEN 房屋号当月累计 Visitor 时长达到或超过72小时，THE System SHALL 自动驳回该房屋号的新 Visitor 申请
4. WHEN 新自然月开始，THE System SHALL 重置所有房屋号的月度配额使用量
5. WHEN 业主查询月度配额，THE Owner_App SHALL 显示已使用时长和剩余时长
6. WHEN 物业管理员查询月度配额，THE Admin_Portal SHALL 显示所有房屋号的配额使用情况
7. WHEN 房屋号月度配额超过60小时，THE System SHALL 向业主发送提醒通知
8. THE System SHALL 在 Visitor 申请审批时校验月度配额是否充足


### Requirement 11: 房屋号数据域同步

**User Story:** 作为业主，我希望同一房屋号下的所有业主账号能够查看相同的车辆和记录信息。

#### Acceptance Criteria

1. WHEN 业主查询车牌列表，THE Owner_App SHALL 返回该房屋号下所有绑定的车牌
2. WHEN 业主查询入场记录，THE Owner_App SHALL 返回该房屋号下所有车辆的入场记录
3. WHEN 业主查询 Visitor 权限，THE Owner_App SHALL 返回该房屋号下所有 Visitor 申请和授权
4. WHEN 业主查询月度配额，THE Owner_App SHALL 返回该房屋号的配额使用情况
5. THE System SHALL 确保同一 Data_Domain 下的所有业主账号访问相同的数据集
6. WHEN 任一业主修改车牌信息，THE System SHALL 使该房屋号下所有业主账号立即可见
7. THE System SHALL 在所有查询接口中使用 community_id + house_no 作为数据过滤条件

### Requirement 12: 多小区数据隔离

**User Story:** 作为物业管理员，我希望系统严格隔离不同小区的数据，以便保护数据安全。

#### Acceptance Criteria

1. THE System SHALL 为每个 Community 分配唯一的 community_id
2. WHEN 物业管理员登录，THE System SHALL 绑定登录态到唯一的 community_id
3. WHEN 超级管理员登录，THE System SHALL 允许切换或指定 community_id
4. THE System SHALL 在所有业务表中包含 community_id 字段
5. THE System SHALL 在所有查询接口中强制校验 community_id
6. THE System SHALL 在所有写入接口中强制校验 community_id
7. IF 用户尝试访问非授权 community_id 的数据，THEN THE System SHALL 拒绝操作并返回错误码 PARKING_12001
8. THE System SHALL 在数据库层面使用索引确保 community_id 查询性能
9. THE System SHALL 在所有操作日志中记录 community_id


### Requirement 13: 管理员账号初始化与安全

**User Story:** 作为系统管理员，我希望首次启动时能够安全初始化管理员账号。

#### Acceptance Criteria

1. WHEN 系统首次启动，THE System SHALL 生成随机密码用于管理员账号初始化
2. THE System SHALL 禁止使用弱口令作为默认密码
3. WHEN 管理员首次登录，THE System SHALL 强制要求修改密码
4. WHEN 管理员修改密码，THE System SHALL 验证新密码强度（至少8位，包含大小写字母、数字、特殊字符）
5. WHEN 管理员登录失败次数达到5次，THE System SHALL 锁定账号
6. WHEN 管理员账号被锁定，THE System SHALL 仅允许超级管理员解锁
7. WHEN 管理员账号解锁，THE System SHALL 记录操作日志到 sys_operation_log
8. THE System SHALL 记录所有管理员登录尝试到 sys_access_log

### Requirement 14: 业主账号注销

**User Story:** 作为超级管理员，我希望能够注销业主账号，以便处理特殊情况。

#### Acceptance Criteria

1. THE System SHALL 仅允许超级管理员执行业主账号注销操作
2. WHEN 超级管理员注销业主账号，THE System SHALL 验证该业主所有车辆均不在场
3. IF 业主有任何车辆在场，THEN THE System SHALL 拒绝注销并返回错误码 PARKING_14001
4. WHEN 业主账号注销成功，THE System SHALL 将账号状态设置为 disabled
5. WHEN 业主账号注销成功，THE System SHALL 将该业主所有车牌状态设置为 disabled
6. WHEN 业主账号注销成功，THE System SHALL 记录操作日志包含注销原因
7. THE System SHALL 确保注销后的账号和车牌不可恢复
8. THE System SHALL 保留注销账号的历史记录用于审计


### Requirement 15: 入场记录按月分表

**User Story:** 作为系统架构师，我希望入场记录按月分表存储，以便提升查询性能和数据管理效率。

#### Acceptance Criteria

1. THE System SHALL 使用表名格式 parking_car_record_yyyymm 存储入场记录
2. WHEN 写入入场记录，THE System SHALL 根据 enter_time 路由到对应月份的分表
3. WHEN 查询入场记录，THE System SHALL 根据时间范围计算月份列表并使用 UNION ALL 合并结果
4. THE System SHALL 支持跨月和跨年的入场记录查询
5. THE System SHALL 在所有入场记录列表和报表查询中强制要求时间范围参数
6. THE System SHALL 在分表上创建索引 (community_id, enter_time)
7. THE System SHALL 在分表上创建索引 (community_id, car_number, enter_time)
8. THE System SHALL 在分表上创建索引 (community_id, house_no, enter_time)
9. THE System SHALL 在分表上创建索引 (community_id, status, enter_time)
10. THE System SHALL 提前创建未来3个月的分表
11. THE System SHALL 使用定时任务自动创建新月份分表

### Requirement 16: 游标分页与导出

**User Story:** 作为物业管理员，我希望能够高效分页查询和导出入场记录。

#### Acceptance Criteria

1. THE System SHALL 禁止跨分表使用深 offset 分页
2. THE System SHALL 使用游标分页基于 (enter_time, id) 组合
3. WHEN 执行分页查询，THE System SHALL 返回下一页游标值
4. WHEN 导出入场记录，THE System SHALL 使用异步任务处理
5. WHEN 导出入场记录，THE System SHALL 按月份分片拉取数据并合并
6. WHEN 导出任务完成，THE System SHALL 通知用户下载链接
7. WHEN 导出任务失败，THE System SHALL 记录错误日志并通知用户
8. THE System SHALL 限制单次导出记录数不超过100000条
9. THE System SHALL 在导出文件中默认执行脱敏处理


### Requirement 17: 数据脱敏

**User Story:** 作为系统安全负责人，我希望系统对敏感数据进行脱敏处理，以便保护用户隐私。

#### Acceptance Criteria

1. WHEN 显示手机号，THE System SHALL 脱敏为 "138****5678" 格式
2. WHEN 显示身份证号，THE System SHALL 仅显示后4位
3. WHEN 导出数据，THE System SHALL 默认对手机号和身份证号执行脱敏
4. WHEN 超级管理员申请导出原始数据，THE System SHALL 要求审批流程
5. WHEN 原始数据导出审批通过，THE System SHALL 记录操作日志到 sys_operation_log
6. WHEN 原始数据导出审批通过，THE System SHALL 验证操作人 IP 在白名单内
7. IF 操作人 IP 不在白名单内，THEN THE System SHALL 拒绝导出并返回错误码 PARKING_17001
8. THE System SHALL 在所有查询接口响应中自动执行脱敏处理

### Requirement 18: 操作日志与审计

**User Story:** 作为审计人员，我希望系统记录所有关键操作日志，以便进行审计和追溯。

#### Acceptance Criteria

1. THE System SHALL 记录所有写操作到 sys_operation_log 表
2. WHEN 记录操作日志，THE System SHALL 包含 request_id、community_id、操作人、IP、操作时间、操作类型、before 状态、after 状态
3. THE System SHALL 记录所有查询操作到 sys_access_log 表
4. WHEN 记录访问日志，THE System SHALL 包含 request_id、community_id、访问人、IP、访问时间、访问接口、查询参数、响应结果
5. THE System SHALL 确保操作日志和访问日志不可删除
6. THE System SHALL 确保操作日志和访问日志不可篡改
7. THE System SHALL 使用按月分区存储操作日志和访问日志
8. THE System SHALL 默认查询最近30天的日志
9. THE System SHALL 将6个月以上的日志归档到只读归档库
10. THE System SHALL 永久保留操作日志和访问日志


### Requirement 19: 限流与防重放

**User Story:** 作为系统安全负责人，我希望系统实施限流和防重放机制，以便防止恶意攻击。

#### Acceptance Criteria

1. THE System SHALL 对注册接口实施 IP 级限流，每个 IP 每小时最多10次请求
2. THE System SHALL 对登录接口实施 IP 级限流，每个 IP 每小时最多20次请求
3. THE System SHALL 对管理端接口实施账号级限流，每个账号每分钟最多100次请求
4. WHEN 接收到请求，THE System SHALL 验证请求包含 timestamp、nonce、signature
5. WHEN 验证 timestamp，THE System SHALL 确保请求时间在5分钟窗口内
6. IF timestamp 超过5分钟窗口，THEN THE System SHALL 拒绝请求并返回错误码 PARKING_19001
7. WHEN 验证 nonce，THE System SHALL 确保 nonce 在5分钟窗口内未被使用过
8. IF nonce 已被使用，THEN THE System SHALL 拒绝请求并返回错误码 PARKING_19002
9. WHEN 验证 signature，THE System SHALL 使用约定的签名算法验证请求完整性
10. IF signature 验证失败，THEN THE System SHALL 拒绝请求并返回错误码 PARKING_19003
11. THE System SHALL 使用 Redis 存储 nonce 并设置5分钟过期时间

### Requirement 20: 高危操作 IP 白名单

**User Story:** 作为系统安全负责人，我希望对高危操作实施 IP 白名单限制，以便增强安全性。

#### Acceptance Criteria

1. THE System SHALL 定义高危操作包括：修改车位配置、注销账号、导出原始数据
2. WHEN 执行高危操作，THE System SHALL 验证操作人 IP 在白名单内
3. IF 操作人 IP 不在白名单内，THEN THE System SHALL 拒绝操作并返回错误码 PARKING_20001
4. WHEN 超级管理员配置 IP 白名单，THE System SHALL 记录操作日志到 sys_operation_log
5. THE System SHALL 支持 IP 白名单的添加、删除、查询操作
6. THE System SHALL 在 IP 白名单变更时发送通知给所有超级管理员
7. THE System SHALL 使用 Redis 缓存 IP 白名单并设置1小时过期时间


### Requirement 21: 报表生成与性能优化

**User Story:** 作为物业管理员，我希望快速生成停车场使用报表，以便分析运营情况。

#### Acceptance Criteria

1. THE System SHALL 生成报表包括：入场趋势、车位使用率、峰值时段、僵尸车辆统计
2. WHEN 查询1年数据的报表，THE System SHALL 在3秒内返回结果
3. THE System SHALL 使用预聚合表 parking_stat_daily 存储每日统计数据
4. WHEN 车辆入场或出场，THE System SHALL 增量更新 parking_stat_daily 表
5. THE System SHALL 使用定时任务每日凌晨回补校准 parking_stat_daily 表
6. THE System SHALL 在报表查询中使用覆盖索引提升性能
7. THE System SHALL 使用 Redis 缓存报表结果并设置1小时过期时间
8. WHEN 车辆入场、出场或配置变更，THE System SHALL 主动失效相关报表缓存
9. THE System SHALL 在 parking_stat_daily 表上创建索引 (community_id, stat_date)

### Requirement 22: 僵尸车辆识别与处理

**User Story:** 作为物业管理员，我希望系统识别僵尸车辆，以便及时处理长时间占用车位的情况。

#### Acceptance Criteria

1. THE System SHALL 定义僵尸车辆为连续在场超过7天的车辆
2. THE System SHALL 使用定时任务每日扫描识别僵尸车辆
3. WHEN 识别到僵尸车辆，THE System SHALL 创建僵尸车辆记录并设置状态为 unhandled
4. WHEN 识别到僵尸车辆，THE System SHALL 通知物业管理员
5. WHEN 物业管理员处理僵尸车辆，THE System SHALL 允许选择处理方式：contacted、resolved、ignored
6. WHEN 物业管理员选择 contacted，THE System SHALL 要求填写联系记录
7. WHEN 物业管理员选择 resolved，THE System SHALL 要求填写解决方案
8. WHEN 物业管理员选择 ignored，THE System SHALL 要求填写忽略原因
9. THE System SHALL 在报表中统计僵尸车辆数量和处理情况
10. THE System SHALL 记录所有僵尸车辆处理操作到 sys_operation_log


### Requirement 23: 批量审核操作

**User Story:** 作为物业管理员，我希望能够批量审核业主申请，以便提高工作效率。

#### Acceptance Criteria

1. THE System SHALL 限制批量审核操作每次最多处理50条记录
2. WHEN 执行批量审核，THE System SHALL 验证所有记录状态为 pending
3. IF 任何记录状态不是 pending，THEN THE System SHALL 跳过该记录并在结果中标注
4. WHEN 批量审核完成，THE System SHALL 返回成功数量和失败数量
5. WHEN 批量审核完成，THE System SHALL 记录每条记录的操作日志到 sys_operation_log
6. THE System SHALL 使用事务确保批量审核的原子性
7. THE System SHALL 使用幂等键防止批量审核的重复执行

### Requirement 24: 敏感信息修改审批

**User Story:** 作为业主，我希望能够修改我的敏感信息，但需要经过物业审批。

#### Acceptance Criteria

1. WHEN 业主修改手机号或身份证信息，THE Owner_App SHALL 创建修改申请并设置状态为 pending
2. WHEN 物业管理员审批修改申请并选择通过，THE Admin_Portal SHALL 更新业主信息并设置申请状态为 approved
3. WHEN 物业管理员审批修改申请并选择驳回，THE Admin_Portal SHALL 设置申请状态为 rejected 并要求填写驳回原因
4. WHEN 修改申请审批完成，THE System SHALL 记录操作日志包含 before 和 after 值
5. WHEN 修改申请审批完成，THE System SHALL 通过订阅消息通知业主
6. THE System SHALL 保留所有敏感信息修改的历史版本用于审计


### Requirement 25: 接口性能要求

**User Story:** 作为系统用户，我希望系统接口响应快速，以便获得良好的使用体验。

#### Acceptance Criteria

1. THE System SHALL 确保所有接口平均响应时间 ≤ 500ms
2. THE System SHALL 确保报表接口（1年数据）响应时间 ≤ 3s
3. WHEN 接口响应时间超过阈值，THE System SHALL 记录慢查询日志
4. THE System SHALL 使用数据库连接池优化数据库访问性能
5. THE System SHALL 使用 Redis 缓存热点数据
6. THE System SHALL 使用异步处理优化耗时操作
7. THE System SHALL 定期监控接口性能并生成性能报告

### Requirement 26: 统一响应格式

**User Story:** 作为前端开发者，我希望所有接口使用统一的响应格式，以便简化前端处理逻辑。

#### Acceptance Criteria

1. THE System SHALL 使用统一 JSON 响应格式 {code, message, data, requestId}
2. WHEN 接口调用成功，THE System SHALL 返回 code 为 0
3. WHEN 接口调用失败，THE System SHALL 返回非零 code 和对应的 message
4. THE System SHALL 在响应中包含 requestId 用于请求追踪
5. THE System SHALL 确保所有错误码遵循 PARKING_XXXX 格式
6. THE System SHALL 维护错误码字典文档

### Requirement 27: 数据库命名规范

**User Story:** 作为数据库管理员，我希望数据库遵循统一的命名规范，以便提高可维护性。

#### Acceptance Criteria

1. THE System SHALL 使用 snake_case 命名所有数据库表和字段
2. THE System SHALL 使用 bigint 类型作为所有表的主键
3. THE System SHALL 统一使用 id 作为主键字段名
4. THE System SHALL 在所有表中包含 create_time 和 update_time 字段
5. THE System SHALL 在所有表中包含 is_deleted 和 deleted_time 字段用于逻辑删除
6. THE System SHALL 在所有业务表中包含 community_id 字段


### Requirement 28: 代码与 API 命名规范

**User Story:** 作为开发者，我希望代码和 API 遵循统一的命名规范，以便提高代码可读性。

#### Acceptance Criteria

1. THE System SHALL 使用 camelCase 命名所有 Java 类方法和变量
2. THE System SHALL 使用 camelCase 命名所有 API 接口路径和参数
3. THE System SHALL 使用 camelCase 命名所有 JSON 响应字段
4. THE System SHALL 遵循 RESTful API 设计规范
5. THE System SHALL 使用统一的 API 版本管理策略

### Requirement 29: 硬件对接接口预留

**User Story:** 作为系统架构师，我希望预留硬件对接接口，以便未来集成道闸和车牌识别设备。

#### Acceptance Criteria

1. THE System SHALL 预留道闸控制回调接口
2. THE System SHALL 预留车牌识别回调接口
3. WHEN 接收到车牌识别回调，THE System SHALL 验证请求签名
4. WHEN 接收到车牌识别回调，THE System SHALL 触发入场或出场流程
5. THE System SHALL 记录所有硬件回调到 sys_access_log
6. THE System SHALL 支持硬件设备的注册和管理

### Requirement 30: 缴费功能预留

**User Story:** 作为系统架构师，我希望预留缴费功能接口，以便未来支持停车费用管理。

#### Acceptance Criteria

1. THE System SHALL 预留 parking_fee 表结构
2. THE System SHALL 预留支付回调接口
3. WHEN 接收到支付回调，THE System SHALL 验证支付签名
4. WHEN 接收到支付回调，THE System SHALL 更新缴费记录状态
5. THE System SHALL 记录所有支付操作到 sys_operation_log


### Requirement 31: 测试覆盖率要求

**User Story:** 作为质量保证工程师，我希望系统具有高测试覆盖率，以便确保代码质量。

#### Acceptance Criteria

1. THE System SHALL 确保单元测试覆盖率 ≥ 90%
2. THE System SHALL 确保集成测试覆盖所有关键业务流程
3. THE System SHALL 测试 Primary 车辆自动入场校验逻辑
4. THE System SHALL 测试车位并发一致性控制
5. THE System SHALL 测试幂等键、签名验证、nonce 防重放机制
6. THE System SHALL 测试 Visitor 名额计算公式与重算逻辑
7. THE System SHALL 测试 Visitor 授权待激活24小时未入场自动取消
8. THE System SHALL 测试 Visitor 从首次入场起累计24小时多次进出
9. THE System SHALL 测试超时记录与通知机制
10. THE System SHALL 测试房屋号 one-primary 约束与并发控制
11. THE System SHALL 测试月度72小时累计与自动驳回逻辑
12. THE System SHALL 测试审批并发幂等保护
13. THE System SHALL 测试注册→审核→房屋号绑定→车牌→Primary 设置→自动入场→出场完整流程
14. THE System SHALL 测试 Visitor 申请→审批→24小时内首次入场激活→多次进出累计→累计24小时超时提醒→月度72小时超额规则完整流程
15. THE System SHALL 测试总车位动态修改导致名额变化的边界情况
16. THE System SHALL 测试跨小区越权访问防护
17. THE System SHALL 测试同房屋号多业主数据同步一致性
18. THE System SHALL 测试重复入场事件幂等处理
19. THE System SHALL 测试无入场记录出场异常处理
20. THE System SHALL 测试批量操作与审计完整性


## Error Codes Dictionary

| 错误码 | 错误信息 | 说明 |
|--------|---------|------|
| PARKING_1001 | 验证码错误次数过多，请10分钟后重试 | 验证码验证失败3次后锁定 |
| PARKING_1002 | 验证码已过期，请重新获取 | 验证码超过5分钟失效 |
| PARKING_2001 | 该申请已被审核，无法重复操作 | 防止并发重复审核 |
| PARKING_3001 | 车牌数量已达上限（5个），无法继续添加 | 车牌数量限制 |
| PARKING_3002 | 该车辆当前在场，无法删除 | 删除车牌前置条件 |
| PARKING_4001 | 房屋号下有车辆在场，无法切换 Primary 车辆 | Primary 切换前置条件 |
| PARKING_4002 | 原 Primary 车辆有未完成入场申请，无法切换 | Primary 切换前置条件 |
| PARKING_5001 | 车位已满，无法入场 | 车位不足拒绝入场 |
| PARKING_7001 | 本月 Visitor 时长配额已用完（72小时），无法申请 | 月度配额超限 |
| PARKING_9001 | Visitor 可开放车位不足，无法申请 | Visitor 名额不足 |
| PARKING_9002 | 新车位数小于当前在场车辆数，无法修改 | 车位配置修改校验 |
| PARKING_12001 | 无权访问该小区数据 | 跨小区越权访问 |
| PARKING_14001 | 业主有车辆在场，无法注销账号 | 账号注销前置条件 |
| PARKING_17001 | IP 不在白名单内，无法导出原始数据 | 高危操作 IP 限制 |
| PARKING_19001 | 请求时间戳超出有效窗口 | 防重放时间窗口校验 |
| PARKING_19002 | Nonce 已被使用 | 防重放 nonce 校验 |
| PARKING_19003 | 签名验证失败 | 请求签名校验 |
| PARKING_20001 | IP 不在白名单内，无法执行高危操作 | 高危操作 IP 限制 |

## State Machine Definitions

### 业主审核状态流转

```
pending → approved (审核通过)
pending → rejected (审核驳回)
rejected → pending (重新提交)
```

### 账号状态流转

```
active (正常)
disabled (禁用，不可恢复)
```

### 车牌状态流转

```
normal (普通车辆)
primary (主车辆)
disabled (禁用)
deleted (逻辑删除)
```

### 入场记录状态流转

```
enter_requested (入场请求)
entered (已入场)
exited (已出场)
exit_exception (出场异常)
```

### Visitor 申请/授权状态流转

```
submitted (已提交)
approved_pending_activation (审批通过待激活)
activated (已激活)
rejected (已驳回)
canceled_no_entry (24小时内未入场自动取消)
expired (已过期)
unavailable (名额不足转为不可用)
```

### Visitor 会话状态流转

```
in_park (在场)
out_of_park (离场)
```

### 僵尸车辆处理状态流转

```
unhandled (未处理)
contacted (已联系)
resolved (已解决)
ignored (已忽略)
```


## Concurrency and Consistency Requirements

### 车位数量一致性

- 使用分布式锁（Redis）确保车位数量计算的原子性
- 锁粒度：community_id 级别
- 锁超时时间：5秒
- 入场和出场操作必须在锁保护下更新车位计数

### Primary 车辆设置并发控制

- 使用数据库行级锁（SELECT FOR UPDATE）防止并发冲突
- 使用唯一索引确保 (community_id, house_no, status=primary) 唯一性
- 事务隔离级别：READ COMMITTED

### 审批操作幂等保护

- 使用幂等键：request_id + operation_type + target_id
- 幂等键存储在 Redis，过期时间5分钟
- 重复请求返回原始操作结果

### 入场记录幂等处理

- 使用幂等键：community_id + car_number + enter_time（精确到分钟）
- 5分钟窗口内相同车牌的入场事件视为重复
- 重复事件不创建新记录，返回已有记录

### Visitor 名额重算一致性

- 使用事务确保 total_spaces 修改和 Visitor_Available_Spaces 重算的原子性
- 使用乐观锁（version 字段）防止并发修改冲突
- 失败时自动重试最多3次

## Idempotency Key Strategy

### 幂等键格式

```
{operation_type}:{community_id}:{target_id}:{request_id}
```

### 幂等键应用场景

1. 业主审核：`owner_audit:{community_id}:{owner_id}:{request_id}`
2. Visitor 审批：`visitor_audit:{community_id}:{visitor_id}:{request_id}`
3. 车辆入场：`vehicle_entry:{community_id}:{car_number}:{timestamp_minute}`
4. 批量审核：`batch_audit:{community_id}:{batch_id}:{request_id}`
5. 敏感信息修改审批：`info_modify_audit:{community_id}:{modify_id}:{request_id}`

### 幂等键存储

- 存储介质：Redis
- 过期时间：5分钟
- 值内容：操作结果 JSON（包含 code、message、data）


## Audit and Logging Requirements

### 操作日志（sys_operation_log）

**记录范围：**
- 所有写操作（创建、更新、删除）
- 所有审批操作
- 所有配置修改操作
- 所有高危操作

**必须字段：**
- id (bigint): 主键
- request_id (varchar): 请求唯一标识
- community_id (bigint): 小区 ID
- operator_id (bigint): 操作人 ID
- operator_name (varchar): 操作人姓名
- operator_ip (varchar): 操作人 IP
- operation_type (varchar): 操作类型
- operation_time (datetime): 操作时间
- target_type (varchar): 目标类型
- target_id (bigint): 目标 ID
- before_value (text): 操作前值（JSON）
- after_value (text): 操作后值（JSON）
- operation_result (varchar): 操作结果（success/failure）
- error_message (text): 错误信息
- create_time (datetime): 创建时间

**存储策略：**
- 按月分区
- 永久保留
- 不可删除、不可篡改
- 6个月后归档到只读库

### 访问日志（sys_access_log）

**记录范围：**
- 所有查询操作
- 所有接口访问
- 所有硬件回调

**必须字段：**
- id (bigint): 主键
- request_id (varchar): 请求唯一标识
- community_id (bigint): 小区 ID
- user_id (bigint): 访问人 ID
- user_name (varchar): 访问人姓名
- user_ip (varchar): 访问人 IP
- access_time (datetime): 访问时间
- api_path (varchar): 接口路径
- http_method (varchar): HTTP 方法
- query_params (text): 查询参数（JSON）
- response_code (int): 响应码
- response_time (int): 响应时间（毫秒）
- create_time (datetime): 创建时间

**存储策略：**
- 按月分区
- 永久保留
- 不可删除、不可篡改
- 6个月后归档到只读库

### 审计查询要求

1. 默认查询最近30天日志
2. 支持按 community_id、operator_id、operation_type、时间范围筛选
3. 支持导出审计日志（需超级管理员权限）
4. 导出审计日志时记录导出操作到 sys_operation_log


## Performance Requirements

### 接口响应时间

| 接口类型 | 响应时间要求 | 监控阈值 |
|---------|------------|---------|
| 查询接口 | 平均 ≤ 500ms | P95 ≤ 1s |
| 写入接口 | 平均 ≤ 500ms | P95 ≤ 1s |
| 报表接口（1年数据） | ≤ 3s | P95 ≤ 5s |
| 导出接口 | 异步处理 | 10万条 ≤ 30s |

### 缓存策略

**Redis 缓存内容：**
1. 报表数据：过期时间1小时，入场/出场/配置变更时主动失效
2. IP 白名单：过期时间1小时，配置变更时主动失效
3. 幂等键：过期时间5分钟
4. Nonce：过期时间5分钟
5. 热点数据（车位配置、小区信息）：过期时间30分钟

**缓存失效策略：**
- 车辆入场/出场：失效相关报表缓存
- 车位配置修改：失效车位相关缓存和报表缓存
- IP 白名单修改：失效 IP 白名单缓存

### 数据库优化

**索引策略：**
1. 所有表必须在 community_id 上建立索引
2. 分表必须在 (community_id, enter_time) 上建立复合索引
3. 查询频繁的字段组合建立覆盖索引
4. 定期分析慢查询并优化索引

**连接池配置：**
- 最小连接数：10
- 最大连接数：50
- 连接超时时间：30秒
- 空闲连接回收时间：5分钟

### 预聚合表（parking_stat_daily）

**字段定义：**
- id (bigint): 主键
- community_id (bigint): 小区 ID
- stat_date (date): 统计日期
- total_entry_count (int): 当日入场总数
- total_exit_count (int): 当日出场总数
- peak_hour (int): 峰值时段（小时）
- peak_count (int): 峰值时段车辆数
- avg_parking_duration (int): 平均停放时长（分钟）
- zombie_vehicle_count (int): 僵尸车辆数
- create_time (datetime): 创建时间
- update_time (datetime): 更新时间

**更新策略：**
- 实时增量更新：车辆入场/出场时更新当日统计
- 定时回补校准：每日凌晨2点回补前一日完整统计
- 索引：(community_id, stat_date)


## Role-Based Access Control

### 角色定义

**超级管理员（Super_Admin）：**
- 跨小区管理权限
- 可切换/指定 community_id
- 解锁账号
- 配置高危 IP 白名单
- 审批导出原始敏感数据
- 注销业主账号
- 查看所有审计日志

**物业管理员（Property_Admin）：**
- 仅本小区权限（绑定唯一 community_id）
- 审核业主注册申请
- 配置车位数量
- 管理车辆记录
- 处理异常出场
- 处理僵尸车辆
- 导出脱敏数据
- 审批 Visitor 权限申请
- 审批敏感信息修改申请
- 查看本小区审计日志

**业主（Owner）：**
- 仅本人/本房屋号数据域权限
- 查看本房屋号车辆记录
- 管理本人车牌
- 设置 Primary 车辆
- 申请 Visitor 权限
- 查询月度配额使用情况
- 申请修改敏感信息

### 权限校验规则

1. 所有接口必须验证用户登录态
2. 所有接口必须验证用户角色权限
3. 所有接口必须验证 community_id 访问权限
4. 业主接口必须验证 house_no 数据域权限
5. 高危操作必须验证 IP 白名单
6. 权限校验失败返回统一错误码 PARKING_12001


## Data Retention and Archival

### 热数据保留策略

**在线数据库：**
- 入场记录：最近12个月
- 操作日志：最近6个月
- 访问日志：最近6个月
- 报表统计：最近24个月

### 冷数据归档策略

**归档库（只读）：**
- 入场记录：12个月以上
- 操作日志：6个月以上
- 访问日志：6个月以上

**归档流程：**
1. 每月1日凌晨执行归档任务
2. 将超过保留期的数据迁移到归档库
3. 归档完成后删除在线库中的归档数据
4. 记录归档操作到 sys_operation_log

**归档查询：**
- 支持跨在线库和归档库的联合查询
- 归档数据查询需要超级管理员权限
- 归档数据导出需要审批流程

## Notification Requirements

### 订阅消息推送场景

1. 业主注册审核结果通知
2. Visitor 权限审批结果通知
3. Visitor 权限即将过期提醒（提前2小时）
4. Visitor 停放时长超时提醒
5. 月度配额即将用完提醒（剩余10%）
6. 敏感信息修改审批结果通知
7. 僵尸车辆识别通知（物业）

### 推送失败处理

1. 失败自动重试最多3次
2. 重试间隔：1分钟、5分钟、15分钟
3. 3次失败后记录到失败队列
4. 定时任务每小时扫描失败队列并补偿推送
5. 所有推送操作记录到 sys_operation_log


## Technology Stack Requirements

### 后端技术栈

- Spring Boot 3.2.x
- MyBatis 3.5.x
- MySQL 8.0
- Redis 6.x
- Maven 3.8.x
- JDK 17

### 前端技术栈

**小程序端：**
- uni-app
- Vue 3
- uni-ui

**管理后台：**
- Vue 3
- Vite
- Ant Design Vue 3.2.20
- ECharts 5.x

### 部署要求

- 支持 Docker 容器化部署
- 支持 Kubernetes 编排
- 支持水平扩展
- 支持灰度发布

## Security Requirements

### 密码安全

1. 密码必须使用 BCrypt 加密存储
2. 密码强度要求：至少8位，包含大小写字母、数字、特殊字符
3. 禁止使用弱口令（123456、password 等）
4. 密码错误5次锁定账号
5. 密码定期过期策略（90天）

### 数据传输安全

1. 所有接口必须使用 HTTPS
2. 敏感数据传输必须加密
3. 使用 TLS 1.2 或更高版本

### SQL 注入防护

1. 所有数据库操作使用参数化查询
2. 禁止拼接 SQL 语句
3. 使用 MyBatis 的 #{} 语法而非 ${}

### XSS 防护

1. 所有用户输入必须进行 HTML 转义
2. 使用 Content-Security-Policy 头
3. 前端使用 Vue 的自动转义机制

