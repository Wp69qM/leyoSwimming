# US-008 用户账号安全设置 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `user` | 修改 | phone、password_hash、email |
| `user_session` | 修改/删除 | 下线设备 |
| `audit_log` | 新增 | 敏感操作审计 |

### 1.2 字段定义

**user 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | 用户 ID（与 US-004 统一，原 `user_id` 为笔误） |
| `phone` | VARCHAR(16) | 唯一索引 | 手机号 |
| `password_hash` | VARCHAR(128) | 可空 | bcrypt 哈希（US-004 微信登录新用户无密码，可空；US-008 设置密码后写入） |
| `email` | VARCHAR(128) | 可空 | 邮箱 |
| `phone_changed_at` | DATETIME | 可空 | 上次换绑时间 |

**user_session 表**（与 US-004 共享，完整字段定义见 [US-004 tech-design §1.1](../US-004-游客-微信授权登录/tech-design.md)）

> **P1 修复 C3**：user_session 表由 US-004 创建，本 US 仅读写 device_* 字段。原 US-008 重复定义了简化版字段（session_id/user_id/device_name/device_id/last_active_at/created_at）与 US-004 的字段定义（id/session_key_encrypted/refresh_token_hash/expires_at/...）不一致。已统一为 US-004 §1.1 的完整字段定义。

本 US 读写以下字段：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `device_name` | VARCHAR(64) | 可空 | 设备名（如"iPhone 15"） |
| `device_id` | VARCHAR(128) | 可空，索引 | 设备标识 |
| `last_active_at` | DATETIME | 可空 | 最后活跃时间，每次请求时刷新 |
| `updated_at` | DATETIME | ON UPDATE | 更新时间 |

---

## 2. API 设计

### 2.1 接口列表

| 接口 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| `/api/user/security/phone` | PUT | 换绑手机号 | 是 |
| `/api/user/security/password` | PUT | 修改/首次设置密码 | 是 |
| `/api/user/security/email` | PUT | 绑定/换绑邮箱 | 是 |
| `/api/user/security/devices` | GET | 设备列表 | 是 |
| `/api/user/security/devices/{id}` | DELETE | 下线设备 | 是 |

### 2.2 PUT /api/user/security/phone

- **二次验证**：`old_phone_code`（原手机号短信验证码）或 `current_password`（当前密码）二选一必填。
- **请求体**：
  ```json
  {
    "new_phone": "13900139000",
    "new_phone_code": "123456",
    "old_phone_code": "654321",
    "current_password": "Abcd1234"
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "phone": "13900139000"
    }
  }
  ```
- **错误码**：
  - `PHONE_ALREADY_BOUND` (400301)
  - `INVALID_PHONE` (400302)
  - `CODE_INVALID` (400303)
  - `INVALID_CREDENTIALS` (400304)
  - `PHONE_CHANGE_LIMIT` (429301)

### 2.3 PUT /api/user/security/password

- **请求体**：
  ```json
  {
    "old_password": "Abcd1234",
    "new_password": "NewPass123"
  }
  ```
  > **首次设置密码**：若 `password_hash` 为空（US-004 微信新用户未设置过密码），`old_password` 可传空或省略，直接设置新密码。
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "updated": true
    }
  }
  ```
- **错误码**：
  - `INVALID_OLD_PASSWORD` (400305)
  - `WEAK_PASSWORD` (400306)
  - `NEW_PASSWORD_SAME_AS_OLD` (400307)
- **会话策略**：修改密码后，**仅保留当前设备会话**，其他设备 token 立即失效（MVP 默认策略）。

### 2.4 PUT /api/user/security/email

- **请求体**：
  ```json
  {
    "email": "a@b.com",
    "verify_code": "123456"
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "email": "a@b.com"
    }
  }
  ```
- **错误码**：
  - `INVALID_EMAIL` (400308)
  - `CODE_INVALID` (400309)

### 2.5 GET /api/user/security/devices

- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "devices": [
        {
          "session_id": 1001,
          "device_name": "iPhone 15",
          "device_id": "device_xxx",
          "last_active_at": "2026-07-31T10:00:00Z",
          "is_current": true
        }
      ]
    }
  }
  ```

### 2.6 DELETE /api/user/security/devices/{id}

- **响应体**：204 No Content
- **错误码**：
  - `DEVICE_NOT_FOUND` (400310)
  - `CANNOT_REVOKE_CURRENT` (400311)（MVP 默认允许下线当前设备，被下线端重新登录即可）

### 2.7 幂等性设计

所有写接口使用 `Idempotency-Key` 请求头，Redis 缓存 TTL 300s。相同幂等键 + 相同请求体重复提交返回首次结果；请求体不一致返回 409 `IDEMPOTENCY_REUSED`。

---

## 3. 状态机

### 3.1 会话状态机

```
有效 ──(设备下线 / 修改密码后非当前设备)──→ 失效
```

- 本 US 不改变用户 `identity_status`，仅使特定会话 token 失效。
- 修改密码后保留当前设备会话，清除该用户其他所有会话。

### 3.2 密码状态

- `password_hash IS NULL`：仅通过微信登录，尚未设置密码。
- `password_hash IS NOT NULL`：已设置密码，修改时需校验原密码。

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 用户资料缓存 | `user:{user_id}` | 立即失效 | 变更后刷新 |
| 会话缓存 | `session:{token}` | 立即失效 | 下线设备 |
| 换绑限流 | `phone_change:limit:{user_id}` | 24 小时 | 限制频繁换绑 |

---

## 5. 性能与安全

### 5.1 性能

- 安全设置接口 P99 < 300ms
- 设备列表查询 P99 < 200ms

### 5.2 安全

- 敏感操作必须二次验证
- 记录审计日志（操作类型、前后值、IP、设备）
- 换绑手机号限流 1 次/24 小时
- 下线设备后立即失效 token

---

## 6. 跨 US 依赖

- 依赖 US-005 已有手机号/密码
- 依赖 US-006 登录态与密码校验
- 与 US-009 隐私协议与用户须知管理相邻
