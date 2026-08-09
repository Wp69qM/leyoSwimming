# US-054 教练手机号验证码登录 — 技术设计

> **状态**：待填充（本 US 进入 TDD 实现阶段前由开发补充）
> **对应 user-story**：[./user-story.md](./user-story.md)

---

## 1. 概述

教练端手机号验证码登录作为 US-051 微信授权登录的并列入口：

- 账号体系完全独立，仅查/写 `coach` 表与 `coach_session` 表。
- 未注册手机号首次验证码登录自动创建 `coach` 记录，`status=-1`。
- 已注册手机号（含 `status=3` 已离职）登录时复用原记录，返回 `coach_status` 由前端按 US-051 映射分流。
- 复用 US-051 的 JWT 会话机制与状态分流策略。

---

## 2. 数据模型

### 2.1 新增/修改表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 新增/修改 | 未注册手机号首次登录时 INSERT；已注册手机号登录时 UPDATE `last_login_at`、`login_ip`；完整字段定义见 US-010 / US-051 |
| `coach_session` | 新增 | 复用 US-051 会话表，写入 `coach_id`、`session_key_encrypted`、`refresh_token_hash`、`expires_at` |
| `coach_login_log` | 新增 | 记录登录时间、IP、设备 |
| `sms_code` | 新增 | 存储验证码，TTL 5 分钟 |

### 2.2 索引

```sql
-- 手机号查询（加密后）
CREATE INDEX idx_coach_phone ON coach(phone);

-- 会话表索引（复用 US-051）
CREATE INDEX idx_coach_session_coach_id ON coach_session(coach_id);
CREATE UNIQUE INDEX idx_coach_session_refresh_hash ON coach_session(refresh_token_hash);

-- 验证码查询
CREATE INDEX idx_sms_code_phone ON sms_code(phone, created_at DESC);
```

---

## 3. API 设计

### 3.1 `POST /api/common/sms/send`

- **鉴权**：无需登录
- **请求体**：`{ "phone": "13800138000", "app_type": "coach", "scene": "login" }`
- **响应 200**：`{ "code": 0, "data": { "expire_seconds": 300 } }`
- **错误码**：`RATE_LIMITED`（429，60 秒内重复获取）、`INVALID_PHONE`（400）

### 3.2 `POST /api/coach/auth/phone-login`

- **鉴权**：无需登录
- **请求体**：
  ```json
  {
    "phone": "13800138000",
    "sms_code": "123456",
    "terms_accepted": true,
    "privacy_accepted": true,
    "app_type": "coach"
  }
  ```
- **响应 200**：同 US-051 `POST /api/coach/auth/wechat-login`：
  ```json
  {
    "access_token": "string",
    "refresh_token": "string",
    "expires_in": 7200,
    "is_new_coach": true,
    "coach_status": -1
  }
  ```
- **错误码**：
  - `TERMS_NOT_ACCEPTED`（400）
  - `INVALID_SMS_CODE`（400）
  - `SMS_CODE_EXPIRED`（400）
  - `RATE_LIMITED`（429）

---

## 4. 状态机

- 本 US 不改变 `coach.status`，仅读取并按 US-051 状态分流。
- 未注册手机号首次登录：`coach.status` 初始化为 `-1`。
- `status=3` 已离职手机号登录：复用原记录，返回 `coach_status=3`，前端按 US-040 跳转 US-010 重新入驻。

---

## 5. 安全与性能

- 手机号加密存储，日志脱敏。
- 验证码 6 位数字，TTL 5 分钟，60 秒内同一手机号限发 1 条。
- 后端必须二次校验 `terms_accepted` 与 `privacy_accepted`。
- `app_type` 白名单校验，仅允许 `user` / `coach`。
- 登录接口 P99 < 1000ms（不含短信服务耗时）。

---

## 6. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-051 | 依赖 | 复用教练端会话表、JWT 机制、状态分流策略 |
| US-009 | 依赖 | 复用隐私协议与用户须知校验逻辑 |
| US-010 | 被依赖 | 首次登录 `status=-1` 时跳转目标 |
| US-040 | 被依赖 | `status=3` 时跳转重新入驻 |

---

## 7. 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-08-08 | 补充数据模型、API 设计、状态机、安全与性能要求，与 user-story.md 保持一致 |
