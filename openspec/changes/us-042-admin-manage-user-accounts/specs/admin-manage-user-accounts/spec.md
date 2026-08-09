> **OpenSpec Spec | 映射自 `docs/stories/US-042-管理员-管理用户账号/user-story.md` §6**

## Capability

管理员管理小程序 C 端用户账号

## ADDED Requirements

### Requirement: REQ-001 用户列表与详情查询

系统 MUST 提供管理后台用户列表与详情查询能力。列表 MUST 支持分页、按身份筛选、按账号状态筛选（正常/注销/封禁）、按资料完善状态筛选、按注册时间范围筛选、按关键字搜索。管理员只能看到其 RBAC 权限范围内的用户数据。用户详情 MUST 展示完整档案信息，未成年人（`age < 18`）的监护人信息 MUST 脱敏展示。

#### Scenario: 管理员查看用户列表
- **GIVEN** 管理员 M 已登录且具有 `USER:READ` 权限
- **WHEN** 管理员 M 进入「用户管理 → 用户账号」
- **THEN** 系统返回用户列表
- **AND** 列表包含用户 ID、头像、姓名、手机号（脱敏）、身份状态、账号状态、注册时间

#### Scenario: 管理员查看用户详情
- **GIVEN** 管理员 M 已登录且具有 `USER:READ` 权限
- **AND** 用户 U 存在且 userId=1001
- **WHEN** 管理员 M 调用 `POST /api/admin/user/detail` 并传入 `{ "userId": 1001 }`
- **THEN** 系统返回用户 U 的完整档案

### Requirement: REQ-002 管理员手动新建用户

系统 MUST 允许具有 `USER:WRITE` 权限的管理员手动新建用户。新建用户 MUST 校验手机号唯一性，未成年人 MUST 填写监护人信息。创建成功后 MUST 设置 `source='ADMIN_CREATED'` 并记录审计日志。

#### Scenario: 管理员手动新建用户
- **GIVEN** 管理员 M 已登录且具有 `USER:WRITE` 权限
- **AND** 手机号 "13800138000" 未被占用
- **WHEN** 管理员 M 调用 `POST /api/admin/user/add` 传入完整用户资料
- **THEN** `user` 表新增 1 条记录
- **AND** 新记录 `source='ADMIN_CREATED'`、`status=0`、`profile_completed=true`
- **AND** `audit_log` 新增 1 条 `action='ADMIN_CREATE_USER'` 记录

### Requirement: REQ-003 管理员编辑用户资料

系统 MUST 允许具有 `USER:WRITE` 权限的管理员编辑用户资料。编辑 MUST 使用乐观锁防止并发覆盖，手机号不可通过本接口修改。

#### Scenario: 管理员编辑用户资料成功
- **GIVEN** 管理员 M 已登录且具有 `USER:WRITE` 权限
- **AND** 用户 U 存在且当前 version=1
- **WHEN** 管理员 M 调用 `POST /api/admin/user/update` 传入 `{ "userId": 1001, "profile": { ... }, "version": 1 }`
- **THEN** `user` 表对应记录更新
- **AND** `audit_log` 新增 1 条 `action='ADMIN_UPDATE_PROFILE'` 记录

#### Scenario: 并发编辑导致乐观锁冲突
- **GIVEN** 两个管理员同时编辑用户 U，均使用 version=1
- **WHEN** 第一个请求提交成功后第二个请求再提交
- **THEN** 第二个请求返回 `USER_CONCURRENTLY_UPDATED`

### Requirement: REQ-004 管理员封禁/解封用户账号

系统 MUST 允许具有 `USER:BAN` 权限的管理员封禁或解封用户账号。操作 MUST 填写原因并记录审计日志。封禁后 `user.status = 2`，解封后 `user.status = 0`。

#### Scenario: 管理员封禁用户账号
- **GIVEN** 管理员 M 已登录且具有 `USER:BAN` 权限
- **AND** 用户 U 当前状态为"正常"，`user_id=1001`
- **WHEN** 管理员 M 调用 `POST /api/admin/user/ban` 并传入 `{ "userId": 1001, "reason": "涉嫌违规" }`
- **THEN** `user.status` 更新为 2（封禁）
- **AND** `audit_log` 新增 1 条 `action='ADMIN_BAN_USER'` 记录，remark="涉嫌违规"
- **AND** 返回 HTTP 200 与提示"账号已封禁"

#### Scenario: 管理员解封用户账号
- **GIVEN** 管理员 M 已登录且具有 `USER:BAN` 权限
- **AND** 用户 U 当前状态为 2（封禁）
- **WHEN** 管理员 M 调用 `POST /api/admin/user/unban` 并传入 `{ "userId": 1001, "reason": "申诉通过" }`
- **THEN** `user.status` 更新为 0（正常）
- **AND** `audit_log` 新增 1 条 `action='ADMIN_UNBAN_USER'` 记录，remark="申诉通过"
- **AND** 返回 HTTP 200 与提示"账号已解封"

### Requirement: REQ-005 系统权限与数据校验

系统 MUST 拒绝无权限管理员的写操作，并 MUST 在目标用户不存在或手机号重复时返回明确错误码。

#### Scenario: 无权限管理员操作失败
- **GIVEN** 管理员 M2 已登录但无 `USER:WRITE` 权限
- **WHEN** 管理员 M2 调用 `POST /api/admin/user/update`
- **THEN** 系统返回 HTTP 403，错误码 `ADMIN_PERMISSION_DENIED`

#### Scenario: 目标用户不存在
- **GIVEN** 管理员 M 具有 `USER:WRITE` 权限
- **AND** 用户 ID 999999 不存在
- **WHEN** 管理员 M 调用 `POST /api/admin/user/detail` 传入 `{ "userId": 999999 }`
- **THEN** 系统返回 HTTP 404，错误码 `USER_NOT_FOUND`

#### Scenario: 新建用户时手机号已存在
- **GIVEN** 管理员 M 具有 `USER:WRITE` 权限
- **AND** 手机号 "13800138000" 已被其他用户占用
- **WHEN** 管理员 M 调用 `POST /api/admin/user/add` 传入该手机号
- **THEN** 系统返回 HTTP 409，错误码 `PHONE_ALREADY_EXISTS`
