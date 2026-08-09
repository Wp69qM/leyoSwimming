## Context

本 US 实现管理后台的管理员账号管理功能。`super_admin` 可执行所有写操作，`admin` 仅可查看列表和自身详情。所有写操作记录 `admin_audit_log`。

## Goals / Non-Goals

**Goals:**
- `super_admin` 可分页查询管理员账号列表
- `super_admin` 可查看管理员账号详情
- `super_admin` 可新建、编辑管理员账号
- `super_admin` 可禁用/启用、删除管理员账号
- `super_admin` 可重置管理员密码
- `admin` 可查看列表和自身详情
- 敏感操作记录 `admin_audit_log`

**Non-Goals:**
- 不实现管理员自助注册（由系统预置或 `super_admin` 创建）
- 不实现 RBAC 权限点配置（本 US 只维护账号本身）
- 不实现登录认证（由 US-053 负责）

## Data Model

### 复用/写入的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `admin_user` | 查询/新增/更新/删除 | `id`, `username`, `password_hash`, `name`, `role` (`super_admin`/`admin`), `status` (0=启用/1=禁用), `last_login_at`, `deleted_at`, `created_at`, `updated_at` |
| `admin_session` | 查询/更新/删除 | `id`, `admin_user_id`, `token_hash`, `expires_at`, `revoked_at`, `created_at` |
| `admin_audit_log` | 新增 | `id`, `admin_user_id`（操作人）, `target_admin_user_id`, `action` (`CREATE`/`UPDATE`/`DISABLE`/`ENABLE`/`DELETE`/`RESET_PASSWORD`), `before_snapshot`, `after_snapshot`, `reason`, `ip`, `created_at` |

### 索引

- `admin_user.username` 唯一索引（`deleted_at IS NULL`）
- `admin_user(role, status)` 复合索引
- `admin_session(admin_user_id)` 索引
- `admin_audit_log(target_admin_user_id)` 索引

## API Design

> 统一使用 POST，URL 按动作命名，参数通过 JSON body 传递。

### POST /api/admin/admin/list

- **鉴权**：管理员 JWT（`admin` 与 `super_admin` 均可）
- **请求体**：`{ "keyword": "", "role": "", "status": null, "page": 1, "size": 20 }`
- **响应 200**：管理员列表分页，返回字段：id, username, name, role, status, last_login_at, created_at, allowed_actions
- **业务规则**：`allowed_actions` 由后端根据当前登录管理员角色与目标账号状态计算

### POST /api/admin/admin/detail

- **鉴权**：管理员 JWT
- **请求体**：`{ "id": 1 }`
- **响应 200**：管理员详情
- **业务规则**：`admin` 角色只能查看自身资料；查看其他管理员返回 `ADMIN_PERMISSION_DENIED`；目标不存在返回 `ADMIN_NOT_FOUND`

### POST /api/admin/admin/add

- **鉴权**：管理员 JWT（仅 `super_admin`）
- **请求体**：`{ "username": "new_admin", "name": "新管理员", "password": "Admin@1234", "role": "admin" }`
- **业务规则**：
  - `username` 3-32 位，字母/数字/下划线，唯一
  - `name` 2-32 位
  - `password` 8-32 位，需包含字母+数字+特殊字符
  - `role` 为 `super_admin` 或 `admin`
  - 新建账号默认 `status=0`
  - 写入 `admin_audit_log` action='CREATE'
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）、`ADMIN_USERNAME_ALREADY_EXISTS`（400）、`VALIDATION_ERROR`（400）

### POST /api/admin/admin/update

- **鉴权**：管理员 JWT（仅 `super_admin`）
- **请求体**：`{ "id": 2, "name": "管理员改名", "role": "super_admin" }`
- **业务规则**：
  - 不允许修改 `username`
  - 目标不存在返回 `ADMIN_NOT_FOUND`
  - 写入 `admin_audit_log` action='UPDATE'
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）、`ADMIN_NOT_FOUND`（404）

### POST /api/admin/admin/toggle-status

- **鉴权**：管理员 JWT（仅 `super_admin`）
- **请求体**：`{ "id": 2, "status": 1, "reason": "离职交接" }`
- **业务规则**：
  - 目标为当前登录账号 → 返回 `ADMIN_CANNOT_DISABLE_SELF`
  - 禁用后无启用状态的 `super_admin` → 返回 `ADMIN_LAST_SUPER_ADMIN_PROTECTED`
  - 禁用后将该管理员的 `admin_session` 全部失效
  - 写入 `admin_audit_log` action='DISABLE' 或 'ENABLE'
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）、`ADMIN_CANNOT_DISABLE_SELF`（400）、`ADMIN_LAST_SUPER_ADMIN_PROTECTED`（400）

### POST /api/admin/admin/delete

- **鉴权**：管理员 JWT（仅 `super_admin`）
- **请求体**：`{ "id": 2, "reason": "账号清理" }`
- **业务规则**：
  - 目标为当前登录账号 → 返回 `ADMIN_CANNOT_DELETE_SELF`
  - 删除后无启用状态的 `super_admin` → 返回 `ADMIN_LAST_SUPER_ADMIN_PROTECTED`
  - 推荐逻辑删除（`deleted_at=now()`）
  - 删除前将该管理员的 `admin_session` 全部失效
  - 写入 `admin_audit_log` action='DELETE'
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）、`ADMIN_CANNOT_DELETE_SELF`（400）、`ADMIN_LAST_SUPER_ADMIN_PROTECTED`（400）

### POST /api/admin/admin/reset-password

- **鉴权**：管理员 JWT（仅 `super_admin`）
- **请求体**：`{ "id": 2 }`
- **响应 200**：`{ "temp_password": "xK9#mP2$vL7@" }`
- **业务规则**：
  - 生成 12 位随机强密码
  - 使用 bcrypt 更新 `admin_user.password_hash`
  - `temp_password` 仅在此响应中明文返回一次
  - 写入 `admin_audit_log` action='RESET_PASSWORD'（不记录明文密码）
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）、`ADMIN_NOT_FOUND`（404）

## RBAC Permission Matrix

| 操作 | `super_admin` | `admin` |
|------|---------------|---------|
| 查看列表 | ✅ | ✅（仅 VIEW） |
| 查看自身详情 | ✅ | ✅ |
| 查看他人详情 | ✅ | ❌ |
| 新建账号 | ✅ | ❌ |
| 编辑资料 | ✅ | ❌ |
| 禁用/启用 | ✅ | ❌ |
| 删除账号 | ✅ | ❌ |
| 重置密码 | ✅ | ❌ |

## State Machine

| 状态维度 | 状态值 | 转换说明 |
|----------|--------|----------|
| 管理员账号启用状态 | `status=0`（启用） | 新建默认启用；由禁用恢复 |
| 管理员账号启用状态 | `status=1`（禁用） | 由启用变为禁用，禁用后无法登录 |
| 管理员账号存在性 | 存在 | 创建新账号 |
| 管理员账号存在性 | 已删除 | 逻辑删除，保留审计关联 |

## Cache Strategy

- 管理员列表缓存 30 秒
- 管理员详情缓存 1 分钟，修改/禁用/删除后失效

## Performance

- `POST /api/admin/admin/list` P99 < 200ms
- `POST /api/admin/admin/detail` P99 < 150ms
- 修改类接口 P99 < 200ms

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-053 | 被本 US 依赖 | 共用 `admin_user`、`admin_session` 表；US-053 负责登录态创建，US-057 负责账号生命周期与会话失效 |
| US-041 ~ US-049 | 反向依赖 | 这些管理后台 US 的操作者账号由本 US 维护 |
