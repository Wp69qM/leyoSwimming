# Design: US-005 用户补充注册资料

> 本文档对应 `docs/stories/US-005-用户-补充注册资料/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-005 完成微信授权登录后的资料补充，是用户身份状态机转换（游客→注册用户）的关键 US。核心是 1 个写入 API + 1 个查询 API + 唯一性/强度校验 + 小程序资料补充页。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | UPDATE | `phone`（AES-256 加密）, `username`, `password_hash`（bcrypt）, `email`, `identity=1`, `status=1`, `updated_at` |
| `user_identity_log` | INSERT | `log_id`, `user_id`, `from_identity=0`, `to_identity=1`, `trigger_event='complete_profile'`, `created_at` |

### 索引

```sql
-- 手机号唯一索引（加密后存储，仍保持唯一）
CREATE UNIQUE INDEX idx_user_phone ON user(phone);

-- 用户名唯一索引
CREATE UNIQUE INDEX idx_user_username ON user(username);

-- 身份日志查询索引
CREATE INDEX idx_user_identity_log_user_id ON user_identity_log(user_id);
```

### identity 字段

| 值 | 业务含义 | 触发 US |
|----|---------|---------|
| `0` | 游客 | 默认/隐式 |
| `1` | 注册用户 | **US-005（本 US）** |
| `2` | 学员 | US-020（购买正价套餐后） |

## API Design

### PUT /api/user/profile

- 鉴权：是（需登录态）
- 幂等：是（`Idempotency-Key: {oauth_union_id}:{timestamp}`，TTL 300s）
- Request: `{ phone: string, username: string, password: string, email?: string, idempotency_key: string }`
- Response 200: `{ user_id, identity: 1, username }`
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

### 用户身份状态机

```
游客(0) ──(US-005 资料补充完成)──→ 注册用户(1)
```

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 游客 → 注册用户 | 资料补充成功 | `user.identity` 从 `0` 更新为 `1` |

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
| DB 写入（含日志） | < 50ms |
| 并发 100 QPS P99 | < 500ms |

## Security

- `PUT /api/user/profile` 必须登录鉴权
- 手机号 AES-256 加密存储，返回前端时脱敏
- 密码 bcrypt 哈希（cost=12），禁止明文传输与存储
- 接口限流：同一用户 1 分钟 > 10 次 → 429
- 敏感操作记录审计日志
- 幂等键防止重复提交导致数据异常

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 微信授权登录后进入资料补充页 |
| US-008 | 被依赖 | 账号安全设置需要完整注册资料 |
| US-009 | 被依赖 | 隐私协议授权需要注册用户身份 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-005-.../tech-design.md` §1 |
| API Design | `docs/stories/US-005-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-005-.../tech-design.md` §3 |
| Caching | `docs/stories/US-005-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-005-.../tech-design.md` §5 |
