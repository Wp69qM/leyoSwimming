# US-042 管理员管理用户账号技术设计

---

## 1. 上下文

本 US 实现管理后台的用户账号管理功能，包括用户列表查询、重置密码、修改手机号/邮箱、配置角色权限。所有敏感操作需记录审计日志。

---

## 2. 目标 / 非目标

**目标：**
- 管理员可按身份、注册时间筛选用户
- 管理员可查看用户详情
- 管理员可修改手机号、修改邮箱
- 管理员可配置用户角色权限

**非目标：**
- 不实现用户自助修改资料（由 US-005 实现）
- 不实现账号注销（由 US-007 实现）
- 不实现复杂的组织架构权限

---

## 3. 数据模型

### 3.1 读取/修改表

#### `user`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| user_id | BIGINT PK | | |
| phone | VARCHAR(20) | UK | |
| email | VARCHAR(128) | UK | nullable |
| identity | TINYINT | IDX | 0=游客 1=注册用户 2=学员 |
| status | TINYINT | IDX | 0=正常 1=注销 2=封禁 |
| avatar_url | VARCHAR(512) | | 头像 URL |
| name | VARCHAR(64) | | 用户姓名/昵称 |
| gender | TINYINT | | 1=男 2=女；未完善资料时为 NULL |
| age | INT | | 年龄 |
| has_swim_basis | BOOLEAN | | 是否有游泳基础 |
| swim_strokes | VARCHAR(128) | | 会什么泳姿，逗号分隔：蛙泳/自由泳/仰泳/蝶泳 |
| swim_years | INT | | 游泳年限 |
| personal_desc | VARCHAR(512) | | 个人描述 |
| guardian_name | VARCHAR(64) | | 监护人姓名，age < 18 时必填 |
| guardian_phone | VARCHAR(20) | | 监护人手机号，age < 18 时必填 |
| profile_completed | BOOLEAN | IDX | 资料是否已完善 |
| created_at | DATETIME | IDX | |
| updated_at | DATETIME | | |
| version | INT | | 乐观锁 |

#### `role`

| 字段 | 类型 | 备注 |
|------|------|------|
| role_id | INT PK | |
| role_code | VARCHAR(64) | `MANAGE_USER_ACCOUNT` 等 |
| role_name | VARCHAR(64) | |

#### `user_role`

| 字段 | 类型 | 备注 |
|------|------|------|
| user_id | BIGINT FK | |
| role_id | INT FK | |

### 3.2 读取表

- `audit_log`：写入操作日志

---

## 4. API 设计

### 4.1 `GET /api/admin/v1/users`

- **鉴权**：管理员 JWT + `USER:READ`
- **查询参数**：`identity`、`status`、`profile_completed`、`start_date`、`end_date`、`keyword`、`page`、`page_size`
- **响应 200**：用户列表分页，返回字段：user_id, avatar_url, name, phone, gender, age, identity, profile_completed, status, created_at

### 4.2 `GET /api/admin/v1/users/{user_id}`

- **鉴权**：管理员 JWT + `USER:READ`
- **响应 200**：用户详情含角色列表与完整档案
- **返回档案字段**：avatar_url, name, phone, email, gender, age, identity, status, profile_completed, has_swim_basis, swim_strokes, swim_years, personal_desc, guardian_name, guardian_phone, created_at, updated_at, last_login_at
- **错误码**：`USER_NOT_FOUND`（404）

### 4.3 `PUT /api/admin/v1/users/{user_id}/profile`

- **鉴权**：管理员 JWT + `USER:WRITE`
- **功能**：管理员编辑用户资料（头像、姓名、性别、年龄、游泳基础、泳姿、游泳年限、个人描述、监护人信息等）
- **请求体**：`{ "avatar_url": "...", "name": "...", "gender": 1, "age": 25, "has_swim_basis": true, "swim_strokes": "蛙泳,自由泳", "swim_years": 5, "personal_desc": "...", "guardian_name": "...", "guardian_phone": "...", "version": 1 }`
- **业务规则**：
  - `age < 18` 时，`guardian_name` 和 `guardian_phone` 必填
  - `has_swim_basis = false` 时，`swim_strokes` 和 `swim_years` 可空
  - 修改成功后记录 `audit_log` action='ADMIN_UPDATE_PROFILE'
- **响应 200**：更新后的用户详情
- **错误码**：`USER_NOT_FOUND`（404）、`USER_CONCURRENTLY_UPDATED`（409）、`INVALID_GUARDIAN_INFO`（400）

### 4.4 `PUT /api/admin/v1/users/{user_id}/phone`

- **鉴权**：管理员 JWT + `USER:WRITE`
- **请求体（正常变更）**：`{ "phone": "13900139000", "sms_code": "123456", "version": 1 }`
- **请求体（强制变更）**：`{ "phone": "13900139000", "force": true, "force_reason": "原手机号已停机", "version": 1 }`
- **响应 200**：更新后的用户信息
- **错误码**：`PHONE_ALREADY_EXISTS`（409）、`USER_CONCURRENTLY_UPDATED`（409）、`PHONE_OWNERSHIP_VERIFY_FAILED`（400）

### 4.5 `PUT /api/admin/v1/users/{user_id}/email`

- **鉴权**：管理员 JWT + `USER:WRITE`
- **请求体**：`{ "email": "new@example.com", "version": 1 }`
- **响应 200**：更新后的用户信息
- **错误码**：`EMAIL_ALREADY_EXISTS`（409）

### 4.6 `PUT /api/admin/v1/users/{user_id}/roles`

- **鉴权**：管理员 JWT + `USER:ROLE_ASSIGN`
- **请求体**：`{ "role_ids": [1, 2] }`
- **响应 200**：更新后的角色列表
- **约束**：`admin` 角色不可授予 `super_admin`（role_id=1）

### 4.7 `POST /api/admin/v1/users/{user_id}/ban`

- **鉴权**：管理员 JWT + `USER:BAN`
- **请求体**：`{ "reason": "涉嫌违规" }`
- **响应 200**：更新后的用户信息
- **业务规则**：仅允许对 `status=0` 的用户执行；更新 `status=2` 并写入 `audit_log`

### 4.8 `POST /api/admin/v1/users/{user_id}/unban`

- **鉴权**：管理员 JWT + `USER:BAN`
- **请求体**：`{ "reason": "申诉通过" }`
- **响应 200**：更新后的用户信息
- **业务规则**：仅允许对 `status=2` 的用户执行；更新 `status=0` 并写入 `audit_log`

---

## 5. 状态机

本 US 不涉及业务状态机转换。

---

## 6. 缓存策略

- 用户列表缓存 30 秒
- 用户详情缓存 1 分钟，修改后失效
- 角色权限缓存长期有效，修改后失效

---

## 7. 性能指标

- `GET /api/admin/v1/users` P99 < 200ms
- `GET /api/admin/v1/users/{id}` P99 < 150ms
- 修改类接口 P99 < 200ms

---

## 8. 安全 / RBAC 权限映射

### 8.1 权限码定义

| 权限码 | 说明 |
|--------|------|
| `USER:READ` | 查看用户列表与详情 |
| `USER:WRITE` | 修改用户手机号、邮箱 |
| `USER:ROLE_ASSIGN` | 配置用户角色权限 |
| `USER:BAN` | 封禁/解封用户账号 |

### 8.2 角色权限矩阵

| 操作 | `super_admin` | `admin` | 说明 |
|------|---------------|---------|------|
| 查看用户列表/详情 | ✅ | ✅ | 均持有 `USER:READ` |
| 修改手机号/邮箱 | ✅ | ✅（仅限非管理员用户） | 均持有 `USER:WRITE`；`admin` 不可修改 `super_admin` 或其他 `admin` |
| 配置角色权限 | ✅ | ✅（不可授予 `super_admin`） | 均持有 `USER:ROLE_ASSIGN`；`admin` 不可将普通用户提升为 `super_admin` |
| 封禁/解封账号 | ✅ | ✅（仅限非管理员用户） | 均持有 `USER:BAN`；`admin` 不可封禁 `super_admin` |
| 删除账号 | ❌ | ❌ | 管理员无删除权限；账号注销由 US-007 用户自助完成 |

### 8.3 其他安全约束

- 接口按上表校验 JWT 与细粒度权限码
- 禁止修改超级管理员（user_id=1 或角色 `super_admin`）的关键字段
- 敏感操作写入 audit_log
- 修改手机号必须通过原手机号短信验证码验证所有权；无法验证时须强制变更并记录原因

---

## 9. 跨 US 依赖

- 依赖 US-004 / US-006 产生 user 记录
- 依赖 US-005 产生用户档案字段（头像、姓名、年龄、性别、游泳档案、监护人信息等），本 US 在后台展示与编辑这些字段
- 角色权限体系影响所有管理后台 US

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 修改手机号 | `test_admin_update_phone_success` | 集成 |
| 无权限 | `test_admin_no_permission_denied` | 集成 |
| 用户不存在 | `test_admin_user_not_found` | 集成 |
| 手机号已存在 | `test_admin_phone_already_exists` | 集成 |
| 并发修改 | `test_admin_update_phone_concurrent` | 集成 |
