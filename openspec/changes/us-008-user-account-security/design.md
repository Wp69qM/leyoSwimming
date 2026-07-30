# Design: US-008 用户账号安全设置

> 本文档对应 `docs/stories/US-008-用户-账号安全设置/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-008 是注册登录模块的收尾 US，提供手机号换绑、密码修改、邮箱绑定、登录设备管理功能。核心是 5 个 API + 二次验证 + 审计日志 + 设备会话管理。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | UPDATE | `phone`, `password_hash`, `email`, `phone_changed_at` |
| `user_session` | SELECT/DELETE | `device_name`, `device_id`, `last_active_at` |
| `audit_log` | INSERT | `action`（phone_change/password_change/device_revoke）, `old_value`, `new_value` |

### 索引

```sql
-- 设备查询索引
CREATE INDEX idx_user_session_user_id ON user_session(user_id);

-- 审计日志查询索引
CREATE INDEX idx_audit_log_user_action ON audit_log(user_id, action);
```

## API Design

### PUT /api/user/security/phone

- 鉴权：是
- Request: `{ new_phone: string, verify_code: string }`
- Response 200: `{ phone: '13900139000' }`
- Response 400: `PHONE_ALREADY_BOUND`
- Response 400: `INVALID_PHONE`
- Response 400: `CODE_INVALID`
- Response 429: `PHONE_CHANGE_LIMIT`

### PUT /api/user/security/password

- 鉴权：是
- Request: `{ old_password: string, new_password: string }`
- Response 200: `{ updated: true }`
- Response 400: `INVALID_OLD_PASSWORD`
- Response 400: `WEAK_PASSWORD`
- Response 400: `NEW_PASSWORD_SAME_AS_OLD`

### PUT /api/user/security/email

- 鉴权：是
- Request: `{ email: string, verify_code: string }`
- Response 200: `{ email: 'a@b.com' }`
- Response 400: `INVALID_EMAIL`
- Response 400: `CODE_INVALID`

### GET /api/user/security/devices

- 鉴权：是
- Response 200: `{ devices: [{ session_id, device_name, device_id, last_active_at, is_current }] }`

### DELETE /api/user/security/devices/{id}

- 鉴权：是
- Response 204
- Response 400: `DEVICE_NOT_FOUND`
- Response 403: `CANNOT_REVOKE_CURRENT`（可选，是否允许下线当前设备）

## State Machine

### 会话状态机

```
有效 ──(设备下线)──→ 失效
```

本 US 不改变用户 `identity`，仅使特定会话 token 失效。

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `user:{user_id}` | 立即失效 | 用户资料缓存 | 换绑/改密/邮箱变更后清除 |
| Redis | `session:{token}` | 立即失效 | 登录态 | 设备下线后删除 |
| Redis | `phone_change:limit:{user_id}` | 24h | 换绑频率限制 | 自然过期 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 安全设置接口 P99 | < 300ms |
| 设备列表查询 P99 | < 200ms |
| 设备下线生效 P99 | < 100ms |
| 审计日志写入 | < 50ms |

## Security

- 所有接口必须登录鉴权
- 敏感操作必须二次验证（换绑手机用验证码，修改密码用原密码）
- 记录审计日志（操作类型、前后值、IP、设备）
- 换绑手机号限流 1 次/24 小时
- 下线设备后立即失效 token
- 新密码不能与旧密码相同

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-005 | 依赖 | 需要已有手机号/密码 |
| US-006 | 依赖 | 需要登录态与密码校验 |
| US-009 | 相邻 | 隐私协议管理同处设置模块 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-008-.../tech-design.md` §1 |
| API Design | `docs/stories/US-008-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-008-.../tech-design.md` §3 |
| Caching | `docs/stories/US-008-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-008-.../tech-design.md` §5 |
