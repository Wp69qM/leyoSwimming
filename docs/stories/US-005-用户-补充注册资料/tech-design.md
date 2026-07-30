# US-005 用户补充注册资料 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `user` | 修改 | 补充手机号、用户名、密码哈希、邮箱 |
| `user_identity_log` | 新增 | 记录身份变更日志 |

### 1.2 字段定义

**user 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `user_id` | BIGINT | PK | 用户 ID |
| `union_id` | VARCHAR(64) | 唯一索引 | 微信 union_id |
| `phone` | VARCHAR(16) | 唯一索引 | 手机号，AES-256 加密存储 |
| `username` | VARCHAR(32) | 唯一索引 | 用户名 |
| `password_hash` | VARCHAR(128) | 非空 | bcrypt 哈希 |
| `email` | VARCHAR(128) | 可空 | 邮箱 |
| `identity` | TINYINT | 默认 0 | 0=游客, 1=注册用户, 2=学员 |
| `status` | TINYINT | 默认 1 | 1=正常, 2=注销, 3=封禁 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | 默认 CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**user_identity_log 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | 日志 ID |
| `user_id` | BIGINT | FK → user | 用户 ID |
| `from_identity` | TINYINT | 非空 | 变更前身份 |
| `to_identity` | TINYINT | 非空 | 变更后身份 |
| `trigger_event` | VARCHAR(64) | 非空 | 触发事件 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 时间 |

---

## 2. API 设计

### 2.1 接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/profile` | PUT | 补充/更新用户资料 |
| `/api/user/phone/exists` | GET | 校验手机号是否已注册 |

### 2.2 详细定义

**PUT /api/user/profile**

- **请求体**：
  ```json
  {
    "phone": "13800138000",
    "username": "swimmer01",
    "password": "Abcd1234",
    "email": "a@b.com",
    "idempotency_key": "union_id_xxx:timestamp"
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "user_id": 10001,
      "identity": 1,
      "username": "swimmer01"
    }
  }
  ```
- **错误码**：
  - `PHONE_ALREADY_BOUND` (400101)
  - `USERNAME_TAKEN` (400102)
  - `INVALID_PHONE` (400103)
  - `WEAK_PASSWORD` (400104)

---

## 3. 状态机

### 3.1 用户身份状态机

```
游客(0) ──[资料补充完成]──→ 注册用户(1)
```

- 本 US 触发：游客 → 注册用户
- 后续 US-020 购买套餐后：注册用户 → 学员

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 手机号存在性缓存 | `phone:exists:{phone_hash}` | 5 分钟 | 减轻唯一性校验压力 |
| 用户名存在性缓存 | `username:exists:{username}` | 5 分钟 | 减轻唯一性校验压力 |
| 用户资料缓存 | `user:{user_id}` | 30 分钟 | 登录态缓存 |

---

## 5. 性能与安全

### 5.1 性能

- 接口 P99 < 300ms
- 高并发注册场景（如活动推广）限流 100 QPS/用户

### 5.2 安全

- 手机号 AES-256 加密存储
- 密码 bcrypt 哈希（cost=12）
- 接口限流 + 验证码防止撞库
- 敏感操作记录审计日志

---

## 6. 跨 US 依赖

- 依赖 US-004 完成微信 OAuth 登录
- 支撑 US-008、US-009 的账号安全与隐私功能
