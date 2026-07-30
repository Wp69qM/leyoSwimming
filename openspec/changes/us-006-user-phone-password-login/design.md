# Design: US-006 用户手机号/账号密码登录

> 本文档对应 `docs/stories/US-006-用户-手机号账号密码登录/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-006 提供手机号验证码与账号密码两种登录方式，是用户除了微信授权外进入系统的入口。核心是 3 个 API + 验证码服务 + bcrypt 密码校验 + 失败锁定 + 会话管理。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | UPDATE | `last_login_at`, `login_ip`, `failed_login_count`, `locked_until` |
| `sms_code` | INSERT | `code_id`, `phone`, `code`, `scene='login'`, `expires_at`, `used` |
| `user_login_log` | INSERT | `log_id`, `user_id`, `login_time`, `ip`, `device` |

### 索引

```sql
-- 验证码查询索引
CREATE INDEX idx_sms_code_phone_scene ON sms_code(phone, scene);

-- 登录日志查询索引
CREATE INDEX idx_user_login_log_user_id ON user_login_log(user_id);
```

## API Design

### POST /api/auth/sms/code

- 鉴权：否
- Request: `{ phone: string, scene: 'login' }`
- Response 200: `{ sent: true }`
- Response 400: `INVALID_PHONE`（手机号格式非法）
- Response 404: `PHONE_NOT_REGISTERED`（手机号未注册）
- Response 429: `SMS_RATE_LIMIT`（60 秒内已发送）

### POST /api/auth/login/phone

- 鉴权：否
- Request: `{ phone: string, code: string }`
- Response 200: `{ token, expires_in: 2592000 }`
- Response 401: `INVALID_CREDENTIALS`（验证码错误）
- Response 401: `CODE_EXPIRED`（验证码已过期）
- Response 429: `RATE_LIMIT_EXCEEDED`（登录过于频繁）

### POST /api/auth/login/password

- 鉴权：否
- Request: `{ account: string, password: string }`
- Response 200: `{ token, expires_in: 2592000 }`
- Response 401: `INVALID_CREDENTIALS`（账号或密码错误）
- Response 401: `ACCOUNT_LOCKED`（连续失败锁定）
- Response 401: `ACCOUNT_DEACTIVATED`（账号已注销）
- Response 401: `ACCOUNT_BANNED`（账号已封禁）

## State Machine

### 会话状态机

```
未登录 ──(登录成功)──→ 已登录
```

本 US 不改变用户 `identity`，仅生成会话 token。

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `login:fail:{account}` | 1800s | 连续密码失败次数 | 登录成功/锁定到期后清除 |
| Redis | `sms:limit:{phone}` | 60s | 验证码发送限流 | 自然过期 |
| Redis | `sms:code:{phone}:{scene}` | 300s | 验证码缓存 | 使用成功/过期后清除 |
| Redis | `session:{token}` | 30 天 | 登录态缓存 | 退出登录/注销时删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 登录接口 P50 | < 150ms |
| 登录接口 P99 | < 300ms |
| 验证码发送接口 P99 | < 500ms |
| bcrypt 校验 P99 | < 100ms |
| 并发 200 QPS P99 | < 800ms |

## Security

- `POST /api/auth/*` 无需登录鉴权
- bcrypt 校验密码，防止时序攻击
- 连续 5 次失败锁定 30 分钟
- 验证码 6 位数字，TTL 5 分钟，单次使用
- 短信发送限流 1 次/分钟/手机号
- 登录日志记录 IP、设备指纹
- 异地登录发送提醒通知

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-005 | 依赖 | 资料补充后才有手机号/密码 |
| US-004 | 共享 | 复用 JWT 与会话管理 |
| US-008 | 被依赖 | 账号安全设置需要登录态与密码校验 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-006-.../tech-design.md` §1 |
| API Design | `docs/stories/US-006-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-006-.../tech-design.md` §3 |
| Caching | `docs/stories/US-006-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-006-.../tech-design.md` §5 |
