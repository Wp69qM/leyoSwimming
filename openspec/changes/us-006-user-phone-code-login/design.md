# Design: US-006 用户手机号验证码登录

> 本文档对应 `docs/stories/US-006-用户-手机号验证码登录/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-006 提供手机号验证码登录方式，是用户除微信授权外进入系统的入口。核心是 2 个 API（发送验证码、手机号验证码登录）+ 验证码服务 + 会话管理。登录时必须校验用户已勾选《用户须知》和《隐私协议》。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | INSERT/UPDATE | 首次登录 INSERT：`phone`, `identity_status='注册用户'`, `profile_completed=false`, `status=0`；已登录 UPDATE：`last_login_at`, `login_ip` |
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
- Response 429: `SMS_RATE_LIMIT`（60 秒内已发送）

### POST /api/auth/login/phone

- 鉴权：否
- Request: `{ phone: string, code: string, termsAccepted: boolean, privacyAccepted: boolean }`
- Response 200: `{ access_token, refresh_token, expires_in: 7200, is_new_user, profile_completed, user_id }`
- Response 400: `TERMS_NOT_ACCEPTED`（未勾选《用户须知》或《隐私协议》）
- Response 401: `INVALID_SMS_CODE`（验证码错误或已过期）

### 登录态管理

- 前端收到登录响应后，将 `access_token` 与 `refresh_token` 存储到本地（如 `Taro.setStorageSync`），并记录 `expires_in`
- 后续请求在 HTTP Header `Authorization: Bearer {access_token}` 中携带 token
- 后端校验 `access_token` 有效后方可访问受保护接口
- `access_token` 过期但 `refresh_token` 有效时，前端调用刷新接口换发新的 `access_token`
- 本地 token 不存在或 `refresh_token` 过期时，前端引导用户重新登录
- 复用 US-004 的 JWT/Session 服务实现

### 业务规则

- 必须校验 `termsAccepted=true` 且 `privacyAccepted=true`，否则直接返回 `TERMS_NOT_ACCEPTED`
- 验证码 6 位数字，TTL 5 分钟，单次使用
- 短信发送限流 1 次/分钟/手机号
- 手机号未注册或手机号存在但 `status=1`（已注销）且验证码正确时，按 PRD §5.2.1 第 4 条重新创建用户记录，不绑定原账号数据
- 登录成功后按 `profile_completed` 分流：false → US-005，true → 首页

## State Machine

### 用户身份状态机

```
游客 ──(US-006 首次验证码登录)──→ 注册用户 ──(US-020 购买正价套餐)──→ 学员
```

本 US 首次登录时触发 `游客 → 注册用户` 状态转换。

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `sms:limit:{phone}` | 60s | 验证码发送限流 | 自然过期 |
| Redis | `sms:code:{phone}:{scene}` | 300s | 验证码缓存 | 使用成功/过期后清除 |
| Redis | `wechat:session_key:{user_id}` | 7200s | 微信 session_key（US-004 使用） | 重新登录时覆盖 |
| Redis | `auth:idempotent:wechat-login:{code}` | 300s | code 幂等键（US-004 使用） | 自然过期 |
| Redis | `session:{user_id}` | 7d | 登录态缓存（refresh_token 有效期） | 退出登录/注销时删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 登录接口 P50 | < 150ms |
| 登录接口 P99 | < 300ms |
| 验证码发送接口 P99 | < 500ms |
| 并发 200 QPS P99 | < 800ms |

## Security

- `POST /api/auth/*` 无需登录鉴权
- 验证码 6 位数字，TTL 5 分钟，单次使用
- 短信发送限流 1 次/分钟/手机号
- 登录日志记录 IP、设备指纹
- 异地登录发送提醒通知

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 共享 | 复用 JWT 与会话管理 |
| US-005 | 后续 | 资料补充：首次登录后 `profile_completed=false` 跳转 |
| US-052 | 被依赖 | 用户退出登录需要登录态 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-006-.../tech-design.md` §1 |
| API Design | `docs/stories/US-006-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-006-.../tech-design.md` §3 |
| Caching | `docs/stories/US-006-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-006-.../tech-design.md` §5 |
