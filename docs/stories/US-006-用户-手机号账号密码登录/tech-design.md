# US-006 用户手机号/账号密码登录 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `user` | 修改 | 更新 last_login_at、login_ip、failed_login_count |
| `user_login_log` | 新增 | 记录登录历史 |
| `sms_code` | 新增 | 存储验证码 |

### 1.2 字段定义

**user 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `user_id` | BIGINT | PK | 用户 ID |
| `phone` | VARCHAR(16) | 唯一索引 | 手机号（加密）|
| `password_hash` | VARCHAR(128) | 可空 | bcrypt 哈希 |
| `status` | TINYINT | 默认 1 | 1=正常, 2=注销, 3=封禁 |
| `last_login_at` | DATETIME | 可空 | 最后登录时间 |
| `failed_login_count` | TINYINT | 默认 0 | 连续失败次数 |
| `locked_until` | DATETIME | 可空 | 锁定截止时间 |

**sms_code 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `code_id` | BIGINT | PK | ID |
| `phone` | VARCHAR(16) | 索引 | 手机号 |
| `code` | VARCHAR(8) | 非空 | 验证码 |
| `scene` | VARCHAR(32) | 非空 | login |
| `expires_at` | DATETIME | 非空 | 过期时间 |
| `used` | TINYINT | 默认 0 | 是否已使用 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/sms/code` | POST | 发送登录验证码 |
| `/api/auth/login/phone` | POST | 手机号验证码登录 |
| `/api/auth/login/password` | POST | 账号密码登录 |

### 2.1 POST /api/auth/login/password

- **请求体**：
  ```json
  {
    "account": "swimmer01",
    "password": "Abcd1234"
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "token": "jwt_token",
      "expires_in": 2592000
    }
  }
  ```
- **错误码**：`INVALID_CREDENTIALS` (401001), `ACCOUNT_LOCKED` (401002)

---

## 3. 状态机

- 无身份状态变化，仅会话状态变化

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 登录失败次数 | `login:fail:{account}` | 30 分钟 | 累计连续失败次数 |
| 验证码发送限流 | `sms:limit:{phone}` | 60 秒 | 防止频繁发送 |
| 登录态 | `session:{token}` | 30 天 | JWT 或 Redis Session |

---

## 5. 性能与安全

### 5.1 性能

- 登录接口 P99 < 300ms
- 验证码发送接口 P99 < 500ms

### 5.2 安全

- bcrypt 校验密码，防时序攻击
- 连续 5 次失败锁定 30 分钟
- 验证码 6 位数字，TTL 5 分钟
- 短信发送限流 1 次/分钟/手机号
- 登录日志记录 IP、设备指纹

---

## 6. 跨 US 依赖

- 依赖 US-005 完成资料补充后才有手机号/密码
- 支撑 US-008 账号安全设置
