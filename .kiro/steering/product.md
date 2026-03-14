# 产品概述

地下停车场管理系统 — 面向物业管理方和业主的企业级多小区 SaaS 平台。

## 核心领域

- 管理多个住宅小区（Community）的地下停车场
- 两个用户端：Owner_App（业主小程序）和 Admin_Portal（物业管理后台）
- 按 Community（`community_id`）和按 Data_Domain（`community_id + house_no`）严格隔离数据

## 关键业务概念

- **Primary_Vehicle（Primary 车辆）**：每个 Data_Domain（`community_id + house_no`）最多绑定 1 辆 Primary 车辆，享有自动入场权限（先到先得）
- **Normal_Vehicle（普通车辆）**：业主绑定的非 Primary 车辆
- **Visitor_Vehicle（Visitor 车辆）**：业主申请 → 物业审批 → 首次入场激活。激活窗口 24 小时，累计停车 24 小时，每个 Data_Domain 每月配额 72 小时（Monthly_Quota）
- **Available_Spaces（可用车位）**：`total_spaces - 当前在场车辆数`，Primary 车辆与 Visitor 车辆共享车位池
- **Visitor_Available_Spaces（Visitor 可开放车位数）**：`total_spaces - 当前在场车辆数`，用于 Visitor 申请校验
- **Zombie_Vehicle（僵尸车辆）**：连续在场超过 7 天的车辆，标记后由物业处理
- **Data_Domain（数据域）**：由 `community_id + house_no` 组成的业主侧核心数据单位

## 角色

| 角色 | 权限范围 |
|------|---------|
| Super_Admin（超级管理员） | 跨 Community 操作、高风险操作、账号停用 |
| Property_Admin（物业管理员） | 单 Community 范围内的审批、配置、报表 |
| Owner（业主） | 仅限本 Data_Domain 数据域 |

## 关键约束

- 所有 Error_Code 遵循 `PARKING_XXXX` 格式
- 统一 JSON 响应：`{code, message, data, requestId}`
- 所有写入/审批操作需携带 Idempotency_Key
- 使用 Redis 分布式锁保证车位计数一致性
- 停车记录按月分表存储（Sharding_Table：`parking_car_record_yyyymm`）
- 完整的 Audit_Log（Operation_Log + Access_Log），不可变，6 个月后归档
- 查询响应中对手机号、身份证号等敏感信息进行 Desensitization 处理
- Rate_Limiting、基于 Nonce 的防重放、Signature 验证
- 高风险操作强制 IP_Whitelist 校验

## 语言规范（强制要求）

- 所有自然语言描述（包括本文件及后续自动生成的说明性文本）必须使用**中文（简体）**。
- 除非用户在当前对话中明确要求英文，否则：
  - 不得使用英文长句描述需求、设计或产品目标；
  - 可以使用英文**仅限于**：类名、方法名、API 路径、JSON 字段名、错误码、环境变量等技术标识。
- 若已经用英文生成了某段说明，需要在后续输出中用等价的中文描述补齐或替换。
- 所有关键业务名词必须与 `requirements.md` 中 Glossary 定义的专有词保持一致（如 Primary_Vehicle、Visitor_Vehicle、Data_Domain、Available_Spaces 等），不得自行翻译或使用其他表述。
