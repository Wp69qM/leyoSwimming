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
| `user_id` | BIGINT | PK | 用户 ID |
| `phone` | VARCHAR(16) | 唯一索引 | 手机号 |
| `password_hash` | VARCHAR(128) | 非空 | bcrypt 哈希 |
| `email` | VARCHAR(128) | 可空 | 邮箱 |
| `phone_changed_at` | DATETIME | 可空 | 上次换绑时间 |

**user_session 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `session_id` | BIGINT | PK | 会话 ID |
| `user_id` | BIGINT | FK | 用户 ID |
| `device_name` | VARCHAR(64) | 可空 | 设备名 |
| `device_id` | VARCHAR(128) | 索引 | 设备标识 |
| `last_active_at` | DATETIME | 可空 | 最后活跃时间 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/security/phone` | PUT | 换绑手机号 |
| `/api/user/security/password` | PUT | 修改密码 |
| `/api/user/security/email` | PUT | 绑定/换绑邮箱 |
| `/api/user/security/devices` | GET | 设备列表 |
| `/api/user/security/devices/{id}` | DELETE | 下线设备 |

### 2.1 PUT /api/user/security/phone

- **请求体**：
  ```json
  {
    "new_phone": "13900139000",
    "verify_code": "123456"
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
- **错误码**：`PHONE_ALREADY_BOUND` (400301), `CODE_INVALID` (400302)

---

## 3. 状态机

- 无身份状态变化

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
- 与 US-009 隐私协议管理相邻
