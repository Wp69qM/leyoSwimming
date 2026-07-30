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
| status | TINYINT | IDX | 0=正常 1=注销 |
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

- **鉴权**：管理员 JWT，`MANAGE_USER_ACCOUNT`
- **查询参数**：`identity`、`start_date`、`end_date`、`keyword`、`page`、`page_size`
- **响应 200**：用户列表分页

### 4.2 `GET /api/admin/v1/users/{user_id}`

- **鉴权**：管理员 JWT
- **响应 200**：用户详情含角色列表
- **错误码**：`USER_NOT_FOUND`（404）

### 4.3 `POST /api/admin/v1/users/{user_id}/reset-password`

- **鉴权**：管理员 JWT
- **功能**：生成随机密码，更新 password_hash，设置 force_change_password=true
- **响应 200**：`{ "message": "密码已重置" }`

### 4.4 `PUT /api/admin/v1/users/{user_id}/phone`

- **鉴权**：管理员 JWT
- **请求体**：`{ "phone": "13900139000", "version": 1 }`
- **响应 200**：更新后的用户信息
- **错误码**：`PHONE_ALREADY_EXISTS`（409）、`USER_CONCURRENTLY_UPDATED`（409）

### 4.5 `PUT /api/admin/v1/users/{user_id}/email`

- **鉴权**：管理员 JWT
- **请求体**：`{ "email": "new@example.com", "version": 1 }`
- **响应 200**：更新后的用户信息
- **错误码**：`EMAIL_ALREADY_EXISTS`（409）

### 4.6 `PUT /api/admin/v1/users/{user_id}/roles`

- **鉴权**：管理员 JWT
- **请求体**：`{ "role_ids": [1, 2] }`
- **响应 200**：更新后的角色列表

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

## 8. 安全

- 接口校验 `MANAGE_USER_ACCOUNT` 权限
- 禁止修改超级管理员（user_id=1 或角色 SUPER_ADMIN）的关键字段
- 重置密码使用加密安全的随机字符串
- 敏感操作写入 audit_log

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
