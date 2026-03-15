# 进度日志

[2025-01-15 16:25] TASK-1 - DONE - 编写 Bug 条件探索性测试，安装 vitest 测试框架，测试在未修复代码上如预期失败（isLoggedIn=true），成功证明 setLoginInfo 提前调用的 bug 存在。

[2025-01-15 19:34] TASK-2 - DONE - 编写保持性属性测试（Property 2: Preservation），验证 mustChangePassword=false 时非首次登录行为不变，5 个测试用例在未修复代码上全部通过，确认基线行为已被捕获。

[2026-03-15 20:01] TASK-3 - DONE - 修复首次登录强制修改密码对话框不弹出的问题（第二轮方案）。auth store 新增 mustChangePassword 状态，路由守卫增加修改密码检查，handleLogin 和 handleChangePassword 重写。8 个测试全部通过，已 git commit（7bcf40a）。

[2026-03-15 20:01] TASK-4 - DONE - 检查点确认：所有 8 个测试通过（bug-condition 3 个 + preservation 5 个），修复已提交。

[2026-03-15 20:11] TASK-3 - DONE - 修复 a-modal 属性绑定错误：ant-design-vue 3.x 使用 v-model:visible 而非 4.x 的 v-model:open，这是对话框弹不出来的真正根因。已提交（d0fd1a9）。

[2026-03-15 20:26] TASK-1 - DONE - 编写 Bug Condition 探索性属性测试，4 个属性全部在未修复代码上 FAIL，确认 schema.sql 中存在 DELIMITER、CREATE DATABASE、USE parking_db、分区表主键缺失分区列等 bug

[2026-03-15 20:28] TASK-2 - DONE - 编写保全性属性测试（Property 2: Preservation），验证未受 bug 影响的 23 张表 DDL 在修复前后一致，4 个属性测试在未修复代码上全部通过，确认基线行为已捕获

[2026-03-15 20:35] TASK-3.1 - DONE - 实施 schema.sql 修复：移除 CREATE DATABASE/USE 语句、删除 DELIMITER 存储过程、修复 sys_operation_log 和 sys_access_log 分区表主键为复合主键、确认 sys_car_plate ALTER TABLE 语法无误。已 git commit（0dec284）

[2026-03-15 20:33] TASK-3.2 - DONE - 验证 Bug Condition 探索性测试通过。发现 schema.sql 注释中 `CREATE DATABASE` 文本触发正则误报，修改注释措辞后全部 4 个属性测试通过，确认 bug 已修复。

[2026-03-15 14:00] TASK-26.3 - DONE - 实现小区列表查询与切换接口：创建 CommunityMapper.xml、CommunityService、CommunityServiceImpl、CommunityController，支持 Super_Admin 查看所有小区并切换操作小区
[2026-03-15 14:10] TASK-26.4 - DONE - 前端路由增加 meta.roles 角色守卫，新增 /admins 和 /ip-whitelist 路由（Super_Admin 专属）
[2026-03-15 14:20] TASK-26.5 - DONE - 侧边栏菜单按角色动态渲染，新增管理员管理和 IP 白名单菜单（Super_Admin 专属），顶部导航栏显示角色标识和小区切换下拉
[2026-03-15 14:30] TASK-26.6 - DONE - 创建管理员管理页面 AdminManageView.vue 和 admin.js API，支持创建/解锁/重置密码
[2026-03-15 14:35] TASK-26.7 - DONE - 创建 IP 白名单管理页面 IpWhitelistView.vue 和 ipWhitelist.js API，支持添加/删除白名单
[2026-03-15 14:40] TASK-26.8 - DONE - 业主审核页面增加注销功能入口，仅 Super_Admin 可见，含确认对话框和注销原因输入
[2026-03-15 14:45] TASK-26.9 - DONE - 审计日志页面导出按钮增加 v-if 角色权限控制，仅 Super_Admin 可见

[2026-03-15 23:15] TASK-27 - DONE - 实现小区管理功能（Community CRUD），包含后端创建/更新接口（POST/PUT /api/v1/communities）、前端小区管理页面（CommunityManageView.vue）、路由和菜单注册。解决首次登录无 Community 时大部分功能不可用的问题。
