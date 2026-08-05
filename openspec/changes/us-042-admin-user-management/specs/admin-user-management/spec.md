> **OpenSpec Spec | 映射自 `docs/stories/US-042-管理员-管理用户账号/user-story.md` §6**

## Capability

管理员管理用户账号

## ADDED Requirements

### Requirement: REQ-001 用户列表与详情查询

系统 MUST 提供管理后台用户列表与详情查询能力。列表 MUST 支持分页、按身份状态筛选、按账号状态筛选（正常/注销/封禁）、按注册时间范围筛选、按关键字搜索。管理员只能看到其 RBAC 权限范围内的用户数据。用户详情 MUST 展示完整档案信息，未成年人（`age < 18`）的监护人信息 MUST 脱敏展示。

#### Scenario: 管理员查看用户列表
- **GIVEN** 管理员 M 已登录且具有 `USER:READ` 权限
- **WHEN** 管理员 M 进入「用户管理 → 用户账号」
- **THEN** 系统返回用户列表
- **AND** 列表包含用户 ID、头像、姓名、手机号（脱敏）、身份状态、账号状态、注册时间
- **AND** 支持按状态筛选正常/注销/封禁用户

### Requirement: REQ-002 管理员重置用户密码

系统 MUST 允许具有 `USER:PASSWORD_RESET` 权限的管理员重置用户密码。重置后系统 MUST 生成随机密码，设置 `user.force_change_password = true`，并记录审计日志。

#### Scenario: 管理员重置用户密码
- **GIVEN** 管理员 M 已登录且具有用户管理权限
- **AND** 用户 U 的手机号为"13800138000"
- **WHEN** 管理员 M 对用户 U 点击「重置密码」
- **THEN** 系统生成随机密码并更新 `user.password_hash`
- **AND** `user.force_change_password = true`
- **AND** `audit_log` 新增 1 条 `action='ADMIN_RESET_PASSWORD'` 记录
- **AND** 返回 HTTP 200 与提示"密码已重置，用户首次登录需修改密码"

### Requirement: REQ-003 管理员修改用户手机号

系统 MUST 允许具有 `USER:WRITE` 权限的管理员修改用户手机号。修改前 MUST 校验新手机号唯一性。优先通过原手机号短信验证码验证用户所有权；无法验证时支持「强制变更」，但 MUST 填写变更原因并记录到 `audit_log.remark`。

#### Scenario: 管理员通过短信验证码修改用户手机号
- **GIVEN** 管理员 M 已登录且具有用户管理权限
- **AND** 用户 U 当前手机号为"13800138000"
- **AND** 新手机号"13900139000"未被占用
- **AND** 管理员 M 已通过原手机号"13800138000"短信验证码验证用户所有权
- **WHEN** 管理员 M 将用户 U 手机号修改为"13900139000"
- **THEN** `user.phone` 更新为"13900139000"
- **AND** `audit_log` 新增 1 条 `action='ADMIN_UPDATE_PHONE'` 记录，remark 含短信验证流水号
- **AND** 返回 HTTP 200 与提示"手机号已更新"

#### Scenario: 管理员强制变更用户手机号并记录原因
- **GIVEN** 管理员 M 已登录且具有用户管理权限
- **AND** 用户 U 当前手机号为"13800138000"且无法接收短信
- **AND** 新手机号"13900139000"未被占用
- **WHEN** 管理员 M 选择"强制变更"并填写原因"原手机号已停机"
- **THEN** `user.phone` 更新为"13900139000"
- **AND** `audit_log` 新增 1 条 `action='ADMIN_UPDATE_PHONE'` 记录，remark="原手机号已停机（强制变更）"
- **AND** 返回 HTTP 200 与提示"手机号已强制更新"

### Requirement: REQ-004 管理员配置用户角色权限

系统 MUST 允许具有 `USER:ROLE_ASSIGN` 权限的管理员配置用户角色。`admin` 角色 MUST 不可授予 `super_admin` 角色，也不可修改 `super_admin` 账号。

#### Scenario: admin 尝试授予 super_admin 角色失败
- **GIVEN** 管理员 M 的角色为 `admin`
- **AND** 用户 U 的角色为普通用户
- **WHEN** 管理员 M 尝试将用户 U 的角色设置为 `super_admin`
- **THEN** 返回错误码 `ADMIN_PERMISSION_DENIED`
- **AND** HTTP 状态码 403
- **AND** 用户 U 的角色保持不变

### Requirement: REQ-005 管理员封禁/解封用户账号

系统 MUST 允许具有 `USER:BAN` 权限的管理员封禁或解封用户账号。操作 MUST 填写原因并记录审计日志。封禁后 `user.status = 2`，解封后 `user.status = 0`。

#### Scenario: 管理员封禁用户账号
- **GIVEN** 管理员 M 已登录且具有用户管理权限
- **AND** 用户 U 当前状态为"正常"，`user_id=1001`
- **WHEN** 管理员 M 对用户 U 执行"封禁账号"并填写原因"涉嫌违规"
- **THEN** `user.status` 更新为 2（封禁）
- **AND** `user.updated_at` 更新
- **AND** `audit_log` 新增 1 条 `action='ADMIN_BAN_USER'` 记录，remark="涉嫌违规"
- **AND** 返回 HTTP 200 与提示"账号已封禁"

### Requirement: REQ-006 权限与存在性校验

系统 MUST 在用户管理操作前校验管理员权限与目标用户存在性。无权限时返回 `ADMIN_PERMISSION_DENIED`（403），目标用户不存在时返回 `USER_NOT_FOUND`（404）。

#### Scenario: 无权限管理员操作失败
- **GIVEN** 管理员 M2 已登录但无用户管理权限
- **WHEN** 管理员 M2 调用用户列表或重置密码接口
- **THEN** 返回错误码 `ADMIN_PERMISSION_DENIED`
- **AND** HTTP 状态码 403
- **AND** 不修改任何用户数据

#### Scenario: 目标用户不存在
- **GIVEN** 管理员 M 已登录且具有用户管理权限
- **AND** 用户 ID 999999 不存在
- **WHEN** 管理员 M 查询或修改该用户
- **THEN** 返回错误码 `USER_NOT_FOUND`
- **AND** HTTP 状态码 404

### Requirement: REQ-007 手机号唯一性校验

管理员修改用户手机号时，系统 MUST 校验新手机号未被其他用户占用。

#### Scenario: 手机号已被占用
- **GIVEN** 管理员 M 已登录且具有用户管理权限
- **AND** 用户 U 当前手机号为"13800138000"
- **AND** 手机号"13900139000"已被用户 U2 占用
- **WHEN** 管理员 M 将用户 U 手机号修改为"13900139000"
- **THEN** 返回错误码 `PHONE_ALREADY_EXISTS`
- **AND** HTTP 状态码 409
- **AND** `user.phone` 保持"13800138000"不变

## Delta Header

```yaml
delta:
  change: us-042-admin-user-management
  capability: admin-user-management
  type: revise
  rationale: 前置 US 补充 US-005（用户档案）与 US-007（账号注销状态），确保后台用户管理可查看完整用户信息与 status=1 注销状态
  scope: docs/stories/US-042, openspec/changes/us-042-admin-user-management
```
