# Design: US-004 用户微信授权登录

> 本文档对应 `docs/stories/US-004-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-004 是注册登录模块的首个 US，也是用户身份状态机的起点。核心是 1 个写入 API + 微信 OAuth 流程 + JWT 签发 + 小程序登录页。本 US 触发 `游客 → 注册用户` 状态转换。

## Data Model

### 新增写入的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | INSERT（首次登录） | `id`, `openid`, `union_id`, `phone`, `avatar_url`, `name`, `identity_status='注册用户'`, `profile_completed=false`, `status=0`, `created_at` |
| `user_session` | INSERT（每次登录） | `id`, `user_id`, `session_key_encrypted`, `refresh_token_hash`, `expires_at`, `created_at` |

### user.status 字段类型

| 值 | 业务含义 | 触发 US |
|----|---------|---------|
| `0` | 正常 | US-004（首次登录时设置）、US-007（重新注册） |
| `1` | 软删除 | US-007（账号注销） |
| `2` | 封禁 | 管理员后台（US-042） |

> **字段类型**（v3 评审 P0 修复）：`status` 为 TINYINT 整型，对齐 PRD §9.2.1。

### 索引

```sql
-- union_id 唯一索引（仅 status=0 正常账号，允许 status=1 软删除账号的 union_id 被新账号复用）
CREATE UNIQUE INDEX idx_user_union_id ON user(union_id) WHERE status = 0;

-- openid 兜底唯一键
CREATE UNIQUE INDEX idx_user_openid ON user(openid) WHERE status = 0;

-- user_session 查询索引
CREATE INDEX idx_user_session_user_id ON user_session(user_id);
CREATE INDEX idx_user_session_refresh_token ON user_session(refresh_token_hash);
```

### identity_status 字段

| 值 | 业务含义 | 触发 US |
|----|---------|---------|
| `注册用户` | 已通过微信授权登录 | **US-004（本 US，首次登录时设置）** |
| `学员` | 已购买正价套餐 | US-020（购买正价套餐后转换） |

> `游客` 状态是隐式的（未登录或未注册），不需要在 user 表中持久化。

## API Design

### POST /api/v1/auth/wechat-login

- 鉴权：否（登录入口）
- 幂等：是（以 `code` 为键，5 分钟内有效）
- Request: `{ code: string, phoneEncryptedData: string, phoneIv: string, avatarUrl: string, nickName?: string, termsAccepted: boolean, privacyAccepted: boolean }`
- Response 200: `{ accessToken, refreshToken, expiresIn: 7200, isNewUser, profileCompleted, userId }`
- Response 400: `TERMS_NOT_ACCEPTED`（未勾选《用户须知》或《隐私协议》）
- Response 401: `WECHAT_CODE_INVALID`（code 已失效）
- Response 502: `WECHAT_API_ERROR`（微信接口错误）
- Response 504: `WECHAT_API_TIMEOUT`（微信接口超时 3s）
- Response 400: `VALIDATION_ERROR`（缺少必要字段）
- Response 400: `PHONE_DECRYPT_FAILED`（手机号解密失败）

### 业务规则

- 必须校验 `termsAccepted=true` 且 `privacyAccepted=true`，否则直接返回 `TERMS_NOT_ACCEPTED`，不调用微信接口
- `code` 调用 `code2session` 失败时按 errcode 区分：`40029` → 401，其他 → 502
- `union_id` 命中 `status=0` 用户 → 复用，`isNewUser=false`
- `union_id` 未命中 → 新建用户，`identity_status='注册用户'`，写入 `phone`、`avatar_url`、`name`，`isNewUser=true`
- `union_id` 命中 `status=1` 用户 → 新建账号，不绑定原数据（PRD §5.2.1 第 4 条）
- `union_id` 缺失 → 以 `openid` 兜底
- 手机号解密：使用 `session_key` 解密 `phoneEncryptedData`，失败返回 `PHONE_DECRYPT_FAILED`
- 事务边界：`查询用户 + 创建用户 + 签发 token + 写 session` 必须在同一事务内

## State Machine

### 用户身份状态机（PRD §9.2 身份体系）

```
游客 ──(US-004 微信授权登录)──→ 注册用户 ──(US-020 购买正价套餐)──→ 学员
```

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 游客 → 注册用户 | 首次微信授权登录成功 | 新建记录时 `identity_status` 直接置为 `注册用户` |

> 注意：`游客` 状态不持久化，只有 `注册用户` 及以上状态对应 user 表记录。

## Wechat OAuth Flow

```
小程序 → wx.login() → code
小程序 → getPhoneNumber → encryptedData + iv
小程序 → POST /auth/wechat-login(code, phoneEncryptedData, phoneIv, avatarUrl, nickName) → 后端
后端 → code2session(code) → 微信开放平台 → openid + union_id + session_key
后端 → 解密手机号
后端 → findByUnionId(union_id) → user 表
  ├─ 命中 active → 复用
  └─ 未命中 → create (identity_status='注册用户', phone, avatar_url, name)
后端 → 签发 JWT + 写 user_session + 缓存 session_key
后端 → 返回 accessToken + refreshToken + isNewUser + profileCompleted
前端 → 按 profileCompleted 跳转（完善个人资料页 / 首页）
```

### code2session 调用

- 接口：`GET https://api.weixin.qq.com/sns/jscode2session`
- 超时：3s（超时返回 504）
- 重试：不重试（code 一次性使用）
- errcode 处理：`40029` → 401，`45011` → 502，其他 → 502

## JWT & Session

| Token | 用途 | 有效期 | 存储 |
|-------|------|--------|------|
| `access_token` | API 鉴权（HS256） | 2h | 前端 storage |
| `refresh_token` | 刷新 access_token | 7d | 前端加密 storage + 后端 SHA-256 hash |

JWT payload: `{ sub, identity_status, profile_completed, iat, exp }`

### 登录态管理

- 前端收到登录响应后，将 `access_token` 与 `refresh_token` 存储到本地（如 `Taro.setStorageSync`），并记录 `expires_in`
- 后续访问受登录态保护的接口时，前端在 HTTP Header `Authorization: Bearer {access_token}` 中携带 token
- 后端校验 `access_token` 有效后方可访问受保护接口
- 前端在 app 启动或「我的」等依赖登录态的页面 `onShow` 时检查本地 token：若不存在或已过期，引导用户重新登录
- `access_token` 过期但 `refresh_token` 有效时，前端调用刷新接口换发新的 `access_token`

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `wechat:session_key:{user_id}` | 7200s | 微信 session_key | 重新登录时覆盖 |
| Redis | `user:info:{user_id}` | 3600s | 用户基本信息缓存 | 用户资料变更时失效 |
| Redis | `auth:idempotent:wechat-login:{code}` | 300s | code 幂等键 | 自然过期 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 登录接口 P50 | < 500ms |
| 登录接口 P99 | < 1500ms（含微信 code2session） |
| code2session 调用 95 分位 | < 1s |
| DB 写入（新用户） | < 50ms |
| 并发 200 QPS P99 | < 2s |

## Security

- `POST /auth/wechat-login` 无需登录鉴权
- `session_key` 绝不返回前端
- `access_token` HS256 签名，密钥从环境变量读取
- `refresh_token` 使用 `crypto.randomBytes(32)` 生成，存储 SHA-256 hash
- `session_key` 在 DB 中 AES-256-CBC 加密存储
- 防刷：同一 IP 1 分钟 > 30 次 → 429 限流
- 防 code 重放：Redis 记录已使用 code，5 分钟内幂等

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-005 | 依赖本 US | 完善个人资料：本 US 创建用户并写入 `phone`、`avatar_url`，置 `profile_completed=false` |
| US-006 | 共享 | 手机号验证码登录共享 user 表与 JWT 逻辑 |
| US-007 | 依赖本 US | 注销：软删除本 US 创建的用户记录 |
| US-017 | 依赖本 US | 购买体验课需先登录 |
| US-020 | 依赖本 US | 购买正价套餐触发 注册用户→学员 |

## Open Questions

- 微信开放平台未绑定导致 `union_id` 缺失时，是否允许仅用 `openid`？当前按"允许兜底 + 记录告警"处理。

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-004-.../tech-design.md` §1 |
| API Design | `docs/stories/US-004-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-004-.../tech-design.md` §3 |
| Wechat OAuth Flow | `docs/stories/US-004-.../tech-design.md` §4 |
| JWT & Session | `docs/stories/US-004-.../tech-design.md` §5 |
| Caching | `docs/stories/US-004-.../tech-design.md` §6 |
| Performance | `docs/stories/US-004-.../tech-design.md` §7 |
| Security | `docs/stories/US-004-.../tech-design.md` §8 |
