> **状态**：待开发填写
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-04

---

## 1. 概述

本文档承接 [user-story.md](./user-story.md)，聚焦用户手机号验证码登录的技术实现：
- 已注册手机号验证码登录
- 未注册手机号首次验证码登录即自动注册
- 登录成功后按 `profile_completed` 分流
- 短信验证码发送、校验、限流

> **核心设计**：
> - 不设置密码，登录仅用手机号 + 短信验证码。
> - 未注册手机号首次验证码登录成功时，系统自动创建 `user` 记录，`identity_status='注册用户'`，`profile_completed=false`。
> - 与 US-004 并列为注册入口，共享 `user` 表与 JWT 签发逻辑。

---

## 2. 数据模型

### 2.1 新增/修改的表

| 表 | 操作 | 关键字段 | 说明 |
|----|------|---------|------|
| `user` | INSERT/UPDATE | `phone`, `identity_status`, `profile_completed`, `last_login_at`, `login_ip`, `status` | 未注册时 INSERT，已注册时 UPDATE last_login_at/login_ip |
| `user_login_log` | INSERT | `user_id`, `login_time`, `login_ip`, `device_info` | 记录登录历史 |
| `sms_code` | INSERT/UPDATE | `phone`, `code`, `scene`, `expires_at`, `used` | 存储登录验证码 |

### 2.2 字段定义

**user 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `user_id` | BIGINT | PK | 用户 ID |
| `phone` | VARCHAR(64) | 唯一索引 | 手机号（AES-256 加密） |
| `identity_status` | VARCHAR(20) | - | `注册用户` / `学员` |
| `profile_completed` | BOOLEAN | 默认 false | 是否已完善资料 |
| `status` | TINYINT | 默认 0 | 0=正常, 1=软删除, 2=封禁 |
| `last_login_at` | DATETIME | 可空 | 最后登录时间 |

**sms_code 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `code_id` | BIGINT | PK | ID |
| `phone_hash` | VARCHAR(64) | 索引 | 手机号哈希（不存明文） |
| `code` | VARCHAR(8) | 非空 | 验证码 |
| `scene` | VARCHAR(32) | 非空 | login |
| `expires_at` | DATETIME | 非空 | 过期时间 |
| `used` | TINYINT | 默认 0 | 是否已使用 |

### 2.3 索引

```sql
-- 手机号唯一索引（仅 status=0）
CREATE UNIQUE INDEX idx_user_phone ON user(phone) WHERE status = 0;

-- 验证码查询
CREATE INDEX idx_sms_code_phone_scene ON sms_code(phone_hash, scene);
```

---

## 3. API 设计

### 3.1 POST /api/common/sms/send

发送登录验证码。

- **鉴权**：否
- **幂等**：否（受 60s 限流）
- **Request**:
  ```json
  { "phone": "13800138000", "scene": "login" }
  ```
- **Response 200**:
  ```json
  { "sent": true, "expire_seconds": 300 }
  ```
- **Response 400**: `INVALID_PHONE`
- **Response 429**: `SMS_RATE_LIMIT`
- **Response 502**: `SMS_SERVICE_ERROR`

### 3.2 POST /api/user/auth/phone-login

手机号验证码登录。

- **鉴权**：否
- **幂等**：是（以 `phone+code` 为键，5 分钟内有效）
- **Request**:
  ```json
  {
    "phone": "13800138000",
    "code": "123456",
    "terms_accepted": true,
    "privacy_accepted": true
  }
  ```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | string | 是 | 手机号 |
| `code` | string | 是 | 短信验证码 |
| `terms_accepted` | boolean | 是 | 是否已勾选《用户须知》，必须为 `true` |
| `privacy_accepted` | boolean | 是 | 是否已勾选《隐私协议》，必须为 `true` |

- **Response 200（已注册且资料完善）**:
  ```json
  {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 7200,
    "isNewUser": false,
    "profileCompleted": true,
    "userId": 1001
  }
  ```
- **Response 200（未注册首次登录）**:
  ```json
  {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 7200,
    "isNewUser": true,
    "profileCompleted": false,
    "userId": 1002
  }
  ```
- **Response 400**: `INVALID_SMS_CODE`
- **Response 400**: `TERMS_NOT_ACCEPTED`（`terms_accepted` 或 `privacy_accepted` 未勾选）
- **Response 429**: `SMS_VERIFY_LIMIT`

---

## 4. 状态机

### 4.1 用户身份状态机

```
游客 ──(US-004 微信授权登录 或 US-006 手机号验证码登录)──→ 注册用户
```

### 4.2 本 US 的状态转换

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 游客 → 注册用户 | 未注册手机号首次验证码登录成功 | 新建 user 记录，`identity_status='注册用户'`，`profile_completed=false`，`phone=输入手机号` |
| 已注销 → 注册用户 | 已注销手机号（`status=1`）验证码登录成功 | 按 PRD §5.2.1 第 4 条新建 user 记录，`identity_status='注册用户'`，`profile_completed=false`，`phone=输入手机号`；新记录 `user_id` 与原注销账号不同 |
| 无 | 已注册用户验证码登录成功 | 仅更新 `last_login_at`、`login_ip` |

---

## 5. 缓存策略

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `sms:limit:{phone_hash}` | 60s | 验证码发送限流 | 自然过期 |
| Redis | `sms:verify:limit:{phone_hash}` | 300s | 验证码校验失败次数限流 | 自然过期 |
| Redis | `auth:idempotent:phone-login:{phone}:{code}` | 300s | 手机号验证码登录幂等键 | 自然过期 |
| Redis | `session:{refreshTokenHash}` | 7 天 | refreshToken 会话 | 注销时删除 |

---

## 6. 性能目标

| 指标 | 目标 |
|------|------|
| 登录接口 P99 | < 300ms |
| 验证码发送接口 P99 | < 500ms |
| DB 写入 | < 50ms |

---

## 7. 安全

- `POST /api/user/auth/phone-login` 无需登录鉴权
- 验证码 6 位数字，TTL 5 分钟
- 同一手机号 60 秒内只能发送 1 条验证码
- 同一手机号连续 5 次验证码错误 → 锁定 30 分钟
- 手机号 AES-256 加密存储
- 登录日志记录 IP、设备指纹
- 防刷：同一 IP 1 分钟内 > 20 次验证码发送 → 429

---

## 8. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 共享 | 微信授权登录：共享 user 表、JWT 签发逻辑 |
| US-009 | 依赖 | 隐私协议与用户须知授权：本 US 需校验用户已勾选《用户须知》和《隐私协议》 |
| US-005 | 被依赖 | 用户完善个人资料：本 US 首次登录后置 `profile_completed=false`，US-005 完成后置 true |
| US-052 | 被依赖 | 用户退出登录：依赖本 US 产生的登录态 |

---

## 9. 异常与边界

| 场景 | 处理 |
|------|------|
| 未勾选《用户须知》或《隐私协议》 | 返回 400 `TERMS_NOT_ACCEPTED` |
| 手机号未注册 | 验证码正确后自动创建账号 |
| 验证码错误 | 返回 `INVALID_SMS_CODE` |
| 验证码过期 | 返回 `INVALID_SMS_CODE` |
| 60s 内重复获取验证码 | 返回 `SMS_RATE_LIMIT` |
| 连续 5 次验证码错误 | 锁定 30 分钟 |
| 短信服务不可用 | 返回 `SMS_SERVICE_ERROR` |
| 账号已注销（status=1） | 按 PRD §5.2.1 第 4 条重新创建新 user 记录并登录成功，新记录 `user_id` 与原注销账号不同 |
| 同一 code 重复提交 | 返回首次结果（幂等） |

---

## 10. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：含账号密码登录 |
| v2.0 | 2026-08-04 | Dev | 重大修订：移除账号密码登录；改为手机号验证码登录兼注册；新增未注册手机号自动创建账号逻辑 |
| v2.1 | 2026-08-08 | PM | 按 user-story 口径统一已注销手机号处理：删除 `ACCOUNT_DELETED` 响应码，改为重新创建新 user 记录并登录成功；同步状态机与异常边界说明 |
