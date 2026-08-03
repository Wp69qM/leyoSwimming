# Design: US-051 教练微信授权登录并进入教练端

> 本文档对应 `docs/stories/US-051-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-051 是教练端小程序的入口 US。核心是在 US-004 微信 OAuth 能力基础上，增加 `app_type=coach` 分支，查询 `coach` 表并根据 `coach.status` 返回 `redirect_page`，实现登录后的自动分流。教练端首次登录成功后、进入目标页面前，需先完成 US-009 隐私协议授权。本 US 不修改教练状态机，仅读取状态。

## Data Model

### 读取/新增表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | 读取/新增 | 同 US-004：`id`, `openid`, `union_id`, `identity_status`, `profile_completed`, `status` |
| `coach` | 读取 | `id`, `user_id`, `status`, `rejection_reason`, `submitted_at` |
| `user_session` | 新增 | 同 US-004：`id`, `user_id`, `session_key_encrypted`, `refresh_token_hash`, `expires_at` |

### coach.status 字段

| 值 | 业务含义 | redirect_page |
|----|---------|---------------|
| `null`（无记录） | 未申请入驻 | `coach_onboarding` |
| `0` | 待审核 | `coach_pending` |
| `1` | 已通过 | `coach_home` |
| `2` | 已驳回 | `coach_rejected` |
| `3` | 已离职 | `coach_resigned` |
| `4` | 离职中 | `coach_resigning` |

### 索引

```sql
-- 已存在（US-004）
CREATE UNIQUE INDEX idx_user_union_id ON user(union_id) WHERE status = 0;

-- coach 表需支持按 user_id 或 union_id 查询
CREATE INDEX idx_coach_user_id ON coach(user_id);
```

## API Design

### 改造：POST /api/v1/auth/wechat-login

- 鉴权：否（登录入口）
- 幂等：是（以 `code` 为键，5 分钟内有效）
- Request: `{ code: string, app_type?: "user" | "coach" }`（默认 `user`）
- Response 200（app_type=coach）:
  ```json
  {
    "access_token": "string",
    "refresh_token": "string",
    "expires_in": 7200,
    "is_new_user": false,
    "profile_completed": false,
    "coach_status": null,
    "redirect_page": "coach_onboarding"
  }
  ```
- Response 200（app_type=user 或缺失）：与 US-004 保持一致
- Response 400: `VALIDATION_ERROR`（缺少 code 或非法 app_type）
- Response 401: `WECHAT_CODE_INVALID`
- Response 502: `WECHAT_API_ERROR`
- Response 504: `WECHAT_API_TIMEOUT`

### 新增：GET /api/v1/coach/me/status

- 鉴权：是（需有效 access_token）
- Request: 无
- Response 200:
  ```json
  {
    "coach_status": 1,
    "rejection_reason": null,
    "redirect_page": "coach_home"
  }
  ```

## Redirect Page Decision Logic

```
function resolveRedirectPage(coachStatus, rejectionReason) {
  switch (coachStatus) {
    case null: return 'coach_onboarding';
    case 0: return 'coach_pending';
    case 1: return 'coach_home';
    case 2: return 'coach_rejected';
    case 3: return 'coach_resigned';
    case 4: return 'coach_resigning';
    default: return 'coach_onboarding';
  }
}
```

## State Machine

### 用户身份状态机

```
游客 ──(US-051 微信授权登录，user 表无记录)──→ 注册用户
```

本 US 仅在 user 表无记录时触发 `游客 → 注册用户` 转换；老用户复用账号时不触发转换。

### 教练状态机

本 US **只读不写**。`coach.status` 转换由以下 US 负责：
- US-010: null → 0
- US-011: 0 → 1 或 0 → 2
- US-039: 1 → 4
- US-040: 2 → 0 或 3 → 0
- US-041: 4 → 3

## Wechat OAuth Flow（教练端）

```
教练端小程序 → wx.login() → code
           → POST /auth/wechat-login { code, app_type='coach' }
后端 → code2session(code) → openid + union_id + session_key
后端 → findByUnionId(union_id) → user 表
  ├─ 命中 active → 复用
  └─ 未命中 → create (identity_status='注册用户')
后端 → findCoachByUserId(user_id) → coach 表
后端 → 计算 redirect_page
后端 → 签发 JWT + 写 user_session + 缓存 session_key
后端 → 返回 token + is_new_user + profile_completed + coach_status + redirect_page
前端 → 校验隐私协议同意状态（调用 US-009 /api/user/privacy/status）
  ├─ 未同意 → 跳转隐私协议页（US-009）
  └─ 已同意 → 按 redirect_page 跳转
```

## JWT & Session

复用 US-004 设计：
- `access_token` 有效期 2h，HS256 签名
- `refresh_token` 有效期 7d，存储 SHA-256 hash
- JWT payload: `{ sub, identity_status, profile_completed, iat, exp }`
- `session_key` AES-256-CBC 加密存储

## Caching

复用 US-004 缓存策略：
- Redis `wechat:session_key:{user_id}` TTL 7200s
- Redis `auth:idempotent:wechat-login:{code}` TTL 300s

## Performance Targets

| 指标 | 目标 |
|------|------|
| 登录接口 P50 | < 500ms |
| 登录接口 P99 | < 1500ms（含微信 code2session + coach 查询） |
| coach 状态查询 P99 | < 50ms |

## Security

- `app_type` 白名单校验，非法值返回 400
- `session_key` 绝不返回前端
- `GET /api/v1/coach/me/status` 必须登录鉴权
- 防刷：同一 IP 1 分钟 > 30 次 → 429 限流

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖/扩展 | 复用微信 OAuth 基础设施与 JWT/Session 逻辑 |
| US-009 | 依赖 | 教练端首次登录后、进入目标页面前需先完成隐私协议授权 |
| US-010 | 被依赖 | 无 coach 记录时跳转至入驻资料页 |
| US-011 | 被依赖 | 审核后 coach.status 0→1/2 |
| US-012 | 被依赖 | coach.status=1 时跳转至教练首页 |
| US-039 | 被依赖 | 触发 coach.status 1→4 |
| US-040 | 被依赖 | 处理 coach.status 2/3 的重新提交 |
| US-041 | 被依赖 | 触发 coach.status 4→3 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-051-.../tech-design.md` §1-2 |
| API Design | `docs/stories/US-051-.../tech-design.md` §3 |
| State Machine | `docs/stories/US-051-.../tech-design.md` §4 |
| Wechat OAuth Flow | `docs/stories/US-051-.../tech-design.md` §3 |
| JWT & Session | `docs/stories/US-051-.../tech-design.md` §3 |
| Security | `docs/stories/US-051-.../tech-design.md` §5 |
