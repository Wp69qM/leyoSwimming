> **OpenSpec Spec | 映射自 `docs/stories/US-057-管理员-管理管理员账号/user-story.md` §6**

## Capability

管理员管理后台管理员账号

## ADDED Requirements

### Requirement: REQ-001 管理员账号列表与详情查询

系统 MUST 提供管理后台管理员账号列表与详情查询能力。列表 MUST 支持分页、按角色筛选、按状态筛选、按关键字搜索。`admin` 角色 MUST 只能查看列表和自身详情；`super_admin` 可查看所有详情。

#### Scenario: super_admin 查看管理员列表
- **GIVEN** 管理员 M 已登录且角色为 super_admin
- **AND** 系统中存在 2 位 super_admin 和 3 位 admin
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/list`
- **THEN** 系统返回管理员账号列表
- **AND** 列表每行展示管理员 ID、姓名、登录账号、角色、状态、最近登录时间、创建时间
- **AND** 默认按创建时间降序排列

#### Scenario: admin 查看列表仅返回 VIEW 权限
- **GIVEN** 管理员 M 已登录且角色为 admin
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/list`
- **THEN** 系统返回列表
- **AND** 每个条目的 `allowed_actions` 仅包含 `["VIEW"]`

### Requirement: REQ-002 新建管理员账号

系统 MUST 允许 `super_admin` 新建管理员账号。新建 MUST 校验登录账号唯一性、密码复杂度、角色合法性，并记录审计日志。

#### Scenario: super_admin 新建管理员账号
- **GIVEN** 管理员 M 已登录且角色为 super_admin
- **AND** 登录账号 "new_admin" 未被占用
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/add` 传入 `{ "username": "new_admin", "name": "新管理员", "password": "Admin@1234", "role": "admin" }`
- **THEN** `admin_user` 表新增 1 条记录
- **AND** 新记录 `name="新管理员"`、`username="new_admin"`、`role="admin"`、`status=0`
- **AND** `admin_audit_log` 新增 1 条 `action='CREATE'` 记录

### Requirement: REQ-003 编辑管理员资料

系统 MUST 允许 `super_admin` 编辑管理员资料（姓名、角色）。不允许修改登录账号。

#### Scenario: super_admin 编辑管理员角色
- **GIVEN** 管理员 M 已登录且角色为 super_admin
- **AND** 管理员 A 存在，`id=1001`，角色为 admin
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/update` 传入 `{ "id": 1001, "role": "super_admin" }`
- **THEN** `admin_user` 表中管理员 A 的 role 更新为 "super_admin"
- **AND** `admin_audit_log` 新增 1 条 `action='UPDATE'` 记录

### Requirement: REQ-004 禁用/启用管理员账号

系统 MUST 允许 `super_admin` 禁用或启用管理员账号。禁用 MUST 使该账号所有会话失效，并 MUST 防止禁用最后一个启用的 `super_admin`。

#### Scenario: super_admin 禁用管理员账号
- **GIVEN** 管理员 M 已登录且角色为 super_admin
- **AND** 管理员 A 存在，`id=1001`，`status=0`
- **AND** 系统中启用的 super_admin 数量 >= 2
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/toggle-status` 传入 `{ "id": 1001, "status": 1, "reason": "离职交接" }`
- **THEN** `admin_user` 表中管理员 A 的 `status` 更新为 1
- **AND** 该账号所有 `admin_session` 记录被置为失效
- **AND** `admin_audit_log` 新增 1 条 `action='DISABLE'` 记录

### Requirement: REQ-005 删除管理员账号

系统 MUST 允许 `super_admin` 删除管理员账号。删除 MUST 防止删除自身和删除最后一个启用的 `super_admin`，并 MUST 使会话失效。

#### Scenario: super_admin 删除管理员账号
- **GIVEN** 管理员 M 已登录且角色为 super_admin
- **AND** 管理员 A 存在，`id=1001`
- **AND** 管理员 A 不是当前登录账号
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/delete` 传入 `{ "id": 1001, "reason": "账号清理" }`
- **THEN** `admin_user` 表中管理员 A 的记录被逻辑删除
- **AND** 该账号所有 `admin_session` 记录被失效
- **AND** `admin_audit_log` 新增 1 条 `action='DELETE'` 记录

### Requirement: REQ-006 重置管理员密码

系统 MUST 允许 `super_admin` 重置管理员密码，生成随机强密码并仅展示一次。

#### Scenario: super_admin 重置管理员密码
- **GIVEN** 管理员 M 已登录且角色为 super_admin
- **AND** 管理员 A 存在，`id=1001`
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/reset-password` 传入 `{ "id": 1001 }`
- **THEN** 系统生成 12 位随机强密码
- **AND** `admin_user.password_hash` 被更新
- **AND** 响应返回明文 `temp_password`
- **AND** `admin_audit_log` 新增 1 条 `action='RESET_PASSWORD'` 记录

### Requirement: REQ-007 权限与边界校验

系统 MUST 拒绝无权限操作，并 MUST 在自删除、自禁用、最后 super_admin 保护等场景返回明确错误码。

#### Scenario: admin 尝试新建管理员账号失败
- **GIVEN** 管理员 M 已登录且角色为 admin
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/add`
- **THEN** 系统返回 HTTP 403，错误码 `ADMIN_PERMISSION_DENIED`

#### Scenario: super_admin 尝试删除自己
- **GIVEN** 管理员 M 已登录且角色为 super_admin，`id=1001`
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/delete` 传入 `{ "id": 1001 }`
- **THEN** 系统返回 HTTP 400，错误码 `ADMIN_CANNOT_DELETE_SELF`

#### Scenario: 禁用最后一个启用的 super_admin
- **GIVEN** 管理员 M 已登录且角色为 super_admin
- **AND** 系统中 `status=0` 的 super_admin 仅管理员 M 1 人
- **WHEN** 管理员 M 调用 `POST /api/admin/admin/toggle-status` 传入 `{ "id": 自身ID, "status": 1 }`
- **THEN** 系统返回 HTTP 400，错误码 `ADMIN_LAST_SUPER_ADMIN_PROTECTED`
