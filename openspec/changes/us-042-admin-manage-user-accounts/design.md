## Context

本 US 实现管理后台的小程序 C 端用户（学员）账号管理功能，包括用户列表查询、查看详情、手动新建、编辑资料、封禁/解封账号。所有敏感操作需记录审计日志。

## Goals / Non-Goals

**Goals:**
- 管理员可按身份、账号状态、资料完善状态、注册时间筛选用户
- 管理员可查看用户详情（含完整档案与监护人信息）
- 管理员可手动新建用户
- 管理员可编辑用户资料（头像、姓名、性别、年龄、游泳档案、监护人信息等）
- 管理员可封禁/解封用户账号
- 敏感操作记录 `audit_log`

**Non-Goals:**
- 不实现用户自助修改资料（由 US-005 实现）
- 不实现账号注销（由 US-007 实现）
- 不实现管理员修改用户手机号（手机号为用户侧登录凭证，MVP 中不支持修改）
- 不实现用户角色权限配置（后台管理员角色权限由 US-057 管理）
- 不操作 `admin_user` 表（由 US-057 负责）

## Data Model

### 复用/写入的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | 查询/新增/更新 | `user_id`, `phone`, `identity`, `status`, `avatar_url`, `name`, `gender`, `age`, `has_swim_basis`, `swim_strokes`, `swim_years`, `personal_desc`, `guardian_name`, `guardian_phone`, `profile_completed`, `source`, `version`, `created_at`, `updated_at` |
| `audit_log` | 新增 | `log_id`, `admin_id`, `target_user_id`, `action`, `before_snapshot`, `after_snapshot`, `reason`, `ip`, `created_at` |

### 索引

- `user.phone` 唯一索引
- `user.identity` 索引
- `user.status` 索引
- `user.profile_completed` 索引
- `user.source` 索引
- `user.created_at` 索引

## API Design

> 统一使用 POST，URL 按动作命名，参数通过 JSON body 传递。

### POST /api/admin/user/list

- **鉴权**：管理员 JWT + `USER:READ`
- **请求体**：`{ "identity": 1, "status": 0, "profileCompleted": true, "startDate": "2026-01-01", "endDate": "2026-12-31", "keyword": "", "page": 1, "pageSize": 20 }`
- **响应 200**：用户列表分页，返回字段：userId, avatarUrl, name, phone, gender, age, identity, profileCompleted, status, createdAt
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）

### POST /api/admin/user/add

- **鉴权**：管理员 JWT + `USER:WRITE`
- **请求体**：`{ "avatarUrl": "...", "phone": "13800138000", "name": "...", "gender": 1, "age": 25, "hasSwimBasis": true, "swimStrokes": "蛙泳,自由泳", "swimYears": 5, "personalDesc": "...", "guardianName": "...", "guardianPhone": "..." }`
- **业务规则**：
  - `phone` 必填，中国大陆手机号格式，提交时校验唯一性
  - `age < 18` 时，`guardian_name` 和 `guardian_phone` 必填
  - `has_swim_basis = false` 时，`swim_strokes` 和 `swim_years` 可空
  - 设置 `identity=1`（注册用户）、`status=0`（正常）、`profile_completed=true`、`source='ADMIN_CREATED'`
  - 创建成功后记录 `audit_log` action='ADMIN_CREATE_USER'
- **响应 201**：创建后的用户详情
- **错误码**：`PHONE_ALREADY_EXISTS`（409）、`INVALID_GUARDIAN_INFO`（400）、`ADMIN_PERMISSION_DENIED`（403）

### POST /api/admin/user/detail

- **鉴权**：管理员 JWT + `USER:READ`
- **请求体**：`{ "userId": 1001 }`
- **响应 200**：用户详情含完整档案
- **返回档案字段**：avatarUrl, name, phone, gender, age, identity, status, profileCompleted, source, hasSwimBasis, swimStrokes, swimYears, personalDesc, guardianName, guardianPhone, createdAt, updatedAt
- **错误码**：`USER_NOT_FOUND`（404）、`ADMIN_PERMISSION_DENIED`（403）

### POST /api/admin/user/update

- **鉴权**：管理员 JWT + `USER:WRITE`
- **请求体**：`{ "userId": 1001, "profile": { "avatarUrl": "...", "name": "...", "gender": 1, "age": 25, "hasSwimBasis": true, "swimStrokes": "蛙泳,自由泳", "swimYears": 5, "personalDesc": "...", "guardianName": "...", "guardianPhone": "..." }, "version": 1 }`
- **业务规则**：
  - `age < 18` 时，`guardian_name` 和 `guardian_phone` 必填
  - `has_swim_basis = false` 时，`swim_strokes` 和 `swim_years` 可空
  - 手机号不可通过本接口修改
  - 修改成功后记录 `audit_log` action='ADMIN_UPDATE_PROFILE'
- **响应 200**：更新后的用户详情
- **错误码**：`USER_NOT_FOUND`（404）、`USER_CONCURRENTLY_UPDATED`（409）、`INVALID_GUARDIAN_INFO`（400）、`ADMIN_PERMISSION_DENIED`（403）

### POST /api/admin/user/ban

- **鉴权**：管理员 JWT + `USER:BAN`
- **请求体**：`{ "userId": 1001, "reason": "涉嫌违规" }`
- **业务规则**：仅允许对 `status=0` 的用户执行；更新 `status=2` 并写入 `audit_log` action='ADMIN_BAN_USER'
- **响应 200**：更新后的用户信息
- **错误码**：`USER_NOT_FOUND`（404）、`ADMIN_PERMISSION_DENIED`（403）

### POST /api/admin/user/unban

- **鉴权**：管理员 JWT + `USER:BAN`
- **请求体**：`{ "userId": 1001, "reason": "申诉通过" }`
- **业务规则**：仅允许对 `status=2` 的用户执行；更新 `status=0` 并写入 `audit_log` action='ADMIN_UNBAN_USER'
- **响应 200**：更新后的用户信息
- **错误码**：`USER_NOT_FOUND`（404）、`ADMIN_PERMISSION_DENIED`（403）

## RBAC Permission Matrix

| 权限码 | 说明 |
|--------|------|
| `USER:READ` | 查看用户列表/详情 |
| `USER:WRITE` | 手动新建用户、编辑用户资料 |
| `USER:BAN` | 封禁/解封用户账号 |

| 操作 | `super_admin` | `admin` |
|------|---------------|---------|
| 查看用户列表/详情 | ✅ | ✅ |
| 手动新建用户 | ✅ | ✅ |
| 编辑用户资料 | ✅ | ✅ |
| 封禁/解封账号 | ✅ | ✅ |
| 删除账号 | ❌ | ❌ |

## State Machine

本 US 不涉及业务状态机转换。

## Cache Strategy

- 用户列表缓存 30 秒
- 用户详情缓存 1 分钟，修改后失效

## Performance

- `POST /api/admin/user/list` P99 < 200ms
- `POST /api/admin/user/detail` P99 < 150ms
- 修改类接口 P99 < 200ms

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 / US-006 | 本 US 依赖 | 产生 user 记录 |
| US-005 | 本 US 依赖 | 完善 user 档案信息 |
| US-007 | 本 US 依赖 | 用户注销产生 status = 1 状态变更 |
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |
| US-057 | 相邻 | US-057 操作 `admin_user` 表，本 US 操作 `user` 表，两者相互独立 |
