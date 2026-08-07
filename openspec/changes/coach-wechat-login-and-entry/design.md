# Design: US-051 教练微信授权登录并进入教练端

> 本文档对应 `docs/stories/US-051-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-051 是教练端小程序的入口 US。核心是为 `POST /api/v1/auth/wechat-login` 增加 `app_type=coach` 分支，此时后端直接查询/写入 `coach` 表与 `coach_session` 表，不再读/写 `user` 表或 `user_session` 表；登录成功后返回 `coach.status`，由前端映射跳转目标页，实现入驻状态分流。教练端没有游客身份，也没有 `profile_completed` 概念。

## Data Model

### 读取/新增表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | 读取/新增 | `id`, `openid`, `union_id`, `phone`, `nickname`, `status`, `rejection_reason`, `submitted_at` |
| `coach_session` | 新增 | `id`, `coach_id`, `session_key_encrypted`, `refresh_token_hash`, `expires_at` |

### coach.status 字段

| 值 | 业务含义 | 前端跳转目标页 |
|----|---------|-----------------|
| `-1` | 未提交入驻资料 | 入驻资料页 |
| `0` | 待审核 | 等待审核页 |
| `1` | 已通过 | 教练首页 |
| `2` | 已驳回 | 重新提交入驻页 |
| `3` | 已离职 | 重新入驻页 |
| `4` | 申请离职中 | 离职处理中页 |

### 索引

```sql
-- 非离职教练按 union_id 唯一（status=3 的离职教练允许重新注册新账号）
CREATE UNIQUE INDEX idx_coach_union_id_active ON coach(union_id) WHERE status != 3;

-- openid 兜底查询
CREATE INDEX idx_coach_openid ON coach(openid);

-- 会话表按 coach 查询
CREATE INDEX idx_coach_session_coach_id ON coach_session(coach_id);

-- 按 refresh_token_hash 查找刷新
CREATE UNIQUE INDEX idx_coach_session_refresh_hash ON coach_session(refresh_token_hash);
```

## API Design

### 改造：POST /api/v1/auth/wechat-login

- 鉴权：否（登录入口）
- 幂等：是（以 `code` 为键，5 分钟内有效）
- Request (`app_type=coach`):
  ```json
  {
    "code": "string",
    "encryptedData": "string",
    "iv": "string",
    "terms_accepted": true,
    "privacy_accepted": true,
    "app_type": "coach"
  }
  ```
- Response 200 (`app_type=coach`):
  ```json
  {
    "access_token": "string",
    "refresh_token": "string",
    "expires_in": 7200,
    "is_new_coach": true,
    "coach_status": -1
  }
  ```
- Response 200 (`app_type=user`）：与 US-004 保持一致，不含 `is_new_coach`/`coach_status`
- Response 400: `VALIDATION_ERROR`（缺少字段或非法 app_type）
- Response 400: `TERMS_NOT_ACCEPTED`（协议未勾选）
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
    "rejection_reason": null
  }
  ```
- 若 coach 记录不存在，返回 `coach_status=-1`。

## Frontend Status-to-Page Mapping

前端在本地维护 `coach_status` 到目标页面的映射，后端不再返回 `redirect_page`：

```
coach_status: -1 → 入驻资料页
             0 → 等待审核页
             1 → 教练首页
             2 → 重新提交入驻页
             3 → 重新入驻页
             4 → 离职处理中页
```

## State Machine

### 教练状态机

本 US 仅初始化 `coach.status = -1`（首次登录无记录时新建 coach），不触发其他转换。`coach.status` 的转换由以下 US 负责：

- US-010: -1 → 0
- US-011: 0 → 1 或 0 → 2
- US-039: 1 → 4
- US-040: 2 → 0 或 3 → 0
- US-041: 4 → 3

## Wechat OAuth Flow（教练端）

```
教练端小程序 → 勾选协议 → wx.login() → code
           → wx.getPhoneNumber() → encryptedData + iv
           → POST /auth/wechat-login { code, encryptedData, iv, terms_accepted, privacy_accepted, app_type='coach' }
后端 → 校验协议勾选
   → 校验 app_type='coach'
   → code2session(code) → openid + union_id + session_key
   → session_key 解密手机号 → phone
   → findCoachByUnionId(union_id) → coach 表
     ├─ 命中且 status != 3 → 复用
     └─ 未命中 或 status = 3 → create coach (status=-1, phone, nickname)
   → 签发 JWT + 写 coach_session + 缓存 session_key
   → 返回 token + is_new_coach + coach_status
前端 → 存储 token
   → onShow 检查 token：过期则刷新，refresh 过期则重新登录
   → 按 coach_status 映射跳转
```

## JWT & Session

- `access_token` 有效期 2h，HS256 签名。
- `refresh_token` 有效期 7d，存储 SHA-256 hash。
- JWT payload（教练端）：`{ sub: coach_id, app_type: 'coach', iat, exp }`。
- `session_key` AES-256-CBC 加密存储。

## Caching

- Redis `auth:idempotent:wechat-login:{code}` TTL 300s。
- Redis `coach:session_key:{coach_id}` TTL 7200s（可选）。

## Performance Targets

| 指标 | 目标 |
|------|------|
| 登录接口 P50 | < 500ms |
| 登录接口 P99 | < 1500ms（含微信 code2session + 手机号解密） |
| coach 状态查询 P99 | < 50ms |

## Security

- `app_type` 白名单校验，非法值返回 400。
- `session_key` 绝不返回前端。
- `GET /api/v1/coach/me/status` 必须登录鉴权。
- 协议勾选后端二次校验。
- 防刷：同一 IP 1 分钟 > 30 次 → 429。

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 参考 | 复用微信 OAuth 协议层与 JWT 设计思路；账号层独立 |
| US-009 | 依赖 | 教练端登录需校验《用户须知》和《隐私协议》勾选 |
| US-010 | 被依赖 | status=-1 时跳转至入驻资料页 |
| US-011 | 被依赖 | 审核后 coach.status 0→1/2 |
| US-012 | 被依赖 | coach.status=1 时跳转至教练首页 |
| US-039 | 被依赖 | 触发 coach.status 1→4 |
| US-040 | 被依赖 | 处理 coach.status 2/3 的重新提交 |
| US-041 | 被依赖 | 触发 coach.status 4→3 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-051-.../tech-design.md` §2 |
| API Design | `docs/stories/US-051-.../tech-design.md` §3 |
| State Machine | `docs/stories/US-051-.../tech-design.md` §4 |
| Wechat OAuth Flow | `docs/stories/US-051-.../tech-design.md` §5 |
| JWT & Session | `docs/stories/US-051-.../tech-design.md` §6 |
| Security | `docs/stories/US-051-.../tech-design.md` §7 |
