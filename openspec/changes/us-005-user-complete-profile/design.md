# Design: US-005 用户补充注册资料

> 本文档对应 `docs/stories/US-005-用户-补充注册资料/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-005 完成微信授权登录后的资料补充，触发用户资料完成状态转换（`profile_completed`: false → true）。`identity_status` 已在 US-004 置为「注册用户」，本 US 不再修改。核心是 1 个写入 API + 1 个查询 API + 唯一性/强度校验 + 小程序资料补充页。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | UPDATE | `phone`（AES-256 加密）, `username`, `password_hash`（bcrypt）, `email`, `profile_completed=true`, `status=0`, `updated_at` |

### 索引

```sql
-- 手机号唯一索引（加密后存储，仍保持唯一）
CREATE UNIQUE INDEX idx_user_phone ON user(phone);

-- 用户名唯一索引
CREATE UNIQUE INDEX idx_user_username ON user(username);
```

### identity_status 字段

| 值 | 业务含义 | 触发 US |
|----|---------|---------|
| `注册用户` | 已通过微信授权登录 | US-004 |
| `学员` | 已购买正价套餐 | US-020（购买正价套餐后） |

### profile_completed 字段

| 值 | 业务含义 | 触发 US |
|----|---------|---------|
| `false` | 用户资料不完整，需补充 | US-004（首次登录时设置） |
| `true` | 用户资料已补充完整 | **US-005（本 US）** |

## API Design

### PUT /api/user/profile

- 鉴权：是（需登录态）
- 幂等：是（`Idempotency-Key: {oauth_union_id}:{timestamp}`，TTL 300s）
- Request: `{ phone: string, username: string, password: string, email?: string, idempotency_key: string }`
- Response 200: `{ user_id, identity_status: '注册用户', username }`
- Response 400: `PHONE_ALREADY_BOUND`（手机号已绑定）
- Response 400: `USERNAME_TAKEN`（用户名已占用）
- Response 400: `INVALID_PHONE`（手机号格式非法）
- Response 400: `WEAK_PASSWORD`（密码强度不足）
- Response 409: `IDEMPOTENCY_REUSED`（幂等键已使用但请求体不一致）

### GET /api/user/phone/exists

- 鉴权：是（需登录态）
- Request query: `phone`
- Response 200: `{ exists: boolean }`
- Response 400: `INVALID_PHONE`（手机号格式非法）
- Response 429: `RATE_LIMIT_EXCEEDED`（查询过于频繁）

## State Machine

### 用户资料完成状态机

```
profile_completed=false ──(US-005 资料补充完成)──→ profile_completed=true
```

> `identity_status` 由 US-004 在首次微信登录时置为「注册用户」，本 US 不再修改。

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| `profile_completed` false → true | 资料补充成功 | `user.profile_completed` 从 `false` 更新为 `true` |

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `phone:exists:{phone_hash}` | 300s | 手机号唯一性校验缓存 | 用户资料变更时失效 |
| Redis | `username:exists:{username}` | 300s | 用户名唯一性校验缓存 | 用户资料变更时失效 |
| Redis | `user:{user_id}` | 1800s | 用户资料缓存 | 资料更新时立即失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 补充资料接口 P50 | < 150ms |
| 补充资料接口 P99 | < 300ms |
| 手机号存在性查询 P99 | < 100ms |
| DB 写入 | < 50ms |
| 并发 100 QPS P99 | < 500ms |

## Security

- `PUT /api/user/profile` 必须登录鉴权
- 手机号 AES-256 加密存储，返回前端时脱敏
- 密码 bcrypt 哈希（cost=12），禁止明文传输与存储
- 接口限流：同一用户 1 分钟 > 10 次 → 429
- 必须校验用户已同意当前生效的隐私协议（checkbox + version 校验）
- 敏感操作记录审计日志
- 幂等键防止重复提交导致数据异常

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 微信授权登录后创建用户并置 `profile_completed=false` |
| US-009 | 依赖 | 隐私协议授权能力：本 US 需校验用户已同意隐私协议 |
| US-008 | 被依赖 | 账号安全设置需要完整注册资料 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-005-.../tech-design.md` §1 |
| API Design | `docs/stories/US-005-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-005-.../tech-design.md` §3 |
| Caching | `docs/stories/US-005-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-005-.../tech-design.md` §5 |
