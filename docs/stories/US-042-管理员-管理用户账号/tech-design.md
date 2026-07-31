# US-042 管理员管理用户账号技术设计

---

## 1. 上下文

本 US 实现管理后台的用户账号管理功能，包括用户列表查询、重置密码、修改手机号/邮箱、配置角色权限。所有敏感操作需记录审计日志。

---

## 2. 目标 / 非目标

**目标：**
- 管理员可按身份、注册时间筛选用户
- 管理员可查看用户详情
- 管理员可重置密码、修改手机号、修改邮箱
- 管理员可配置用户角色权限

**非目标：**
- 不实现用户自助修改资料（由 US-005/US-008 实现）
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
| password_hash | VARCHAR(255) | | |
| force_change_password | BOOLEAN | | 重置密码后强制修改 |
| identity | TINYINT | IDX | 0=游客 1=注册用户 2=学员 |
| status | TINYINT | IDX | 0=正常 1=注销 2=封禁 |
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
- **查询参数**：`identity`、`start_date`、`end_date`、`keyword`、`page`、`page_size`
- **响应 200**：用户列表分页

### 4.2 `GET /api/admin/v1/users/{user_id}`

- **鉴权**：管理员 JWT + `USER:READ`
- **响应 200**：用户详情含角色列表
- **错误码**：`USER_NOT_FOUND`（404）

### 4.3 `POST /api/admin/v1/users/{user_id}/reset-password`

- **鉴权**：管理员 JWT + `USER:PASSWORD_RESET`
- **功能**：生成随机密码，更新 password_hash，设置 force_change_password=true
- **响应 200**：`{ "message": "密码已重置" }`

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
| `USER:PASSWORD_RESET` | 重置用户密码 |
| `USER:ROLE_ASSIGN` | 配置用户角色权限 |
| `USER:BAN` | 封禁/解封用户账号 |

### 8.2 角色权限矩阵

| 操作 | `super_admin` | `admin` | 说明 |
|------|---------------|---------|------|
| 查看用户列表/详情 | ✅ | ✅ | 均持有 `USER:READ` |
| 修改手机号/邮箱 | ✅ | ✅（仅限非管理员用户） | 均持有 `USER:WRITE`；`admin` 不可修改 `super_admin` 或其他 `admin` |
| 重置密码 | ✅ | ✅（仅限非管理员用户） | 均持有 `USER:PASSWORD_RESET`；`admin` 不可重置 `super_admin` 密码 |
| 配置角色权限 | ✅ | ✅（不可授予 `super_admin`） | 均持有 `USER:ROLE_ASSIGN`；`admin` 不可将普通用户提升为 `super_admin` |
| 封禁/解封账号 | ✅ | ✅（仅限非管理员用户） | 均持有 `USER:BAN`；`admin` 不可封禁 `super_admin` |
| 删除账号 | ❌ | ❌ | 管理员无删除权限；账号注销由 US-007 用户自助完成 |

### 8.3 其他安全约束

- 接口按上表校验 JWT 与细粒度权限码
- 禁止修改超级管理员（user_id=1 或角色 `super_admin`）的关键字段
- 重置密码使用加密安全的随机字符串
- 敏感操作写入 audit_log
- 修改手机号必须通过原手机号短信验证码验证所有权；无法验证时须强制变更并记录原因

---

## 9. 跨 US 依赖

- 依赖 US-004 / US-006 产生 user 记录
- 角色权限体系影响所有管理后台 US

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 重置密码 | `test_admin_reset_password_success` | 集成 |
| 修改手机号 | `test_admin_update_phone_success` | 集成 |
| 无权限 | `test_admin_no_permission_denied` | 集成 |
| 用户不存在 | `test_admin_user_not_found` | 集成 |
| 手机号已存在 | `test_admin_phone_already_exists` | 集成 |
| 并发修改 | `test_admin_update_phone_concurrent` | 集成 |
