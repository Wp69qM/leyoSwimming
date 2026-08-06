# US-051 教练微信授权登录并进入教练端 — 技术设计文档

> **状态**：已完成
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-05

---

## 1. 概述

本文档承接 [user-story.md](./user-story.md)，明确教练端微信授权登录的技术实现：

- 教练端账号体系完全独立于用户端：`app_type=coach` 时后端只操作 `coach` 表与 `coach_session` 表，不读、不写、不关联 `user` 表或 `user_session` 表。
- 登录流程复用微信 OAuth 协议层：`wx.login()` 取 `code`，`wx.getPhoneNumber` 取加密手机号 `encryptedData` + `iv`，协议勾选校验与用户端一致。
- 教练端没有「游客」身份，也没有 `profile_completed` 概念；登录成功后返回 `coach.status`，由前端映射到对应页面。
- 新增 `GET /api/v1/coach/me/status` 供前端在登录态有效期内兜底查询入驻状态。
- 登录成功后前端在 `onShow` 检查 token，过期用 `refresh_token` 刷新，`refresh_token` 过期则重新登录。

---

## 2. 数据模型

### 2.1 `coach` 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint PK | 教练主键 |
| `openid` | string | 教练端微信小程序 openid |
| `union_id` | string | 微信 union_id，跨小程序唯一 |
| `phone` | string | 解密后的微信手机号 |
| `avatar_url` | string | 微信头像 URL |
| `nickname` / `name` | string | 微信昵称（可选） |
| `status` | smallint | -1=未提交入驻资料，0=待审核，1=已通过，2=已驳回，3=已离职，4=申请离职中 |
| `rejection_reason` | text | 驳回原因（status=2 时非空） |
| `submitted_at` | timestamp | 提交入驻资料时间 |
| `created_at` | timestamp | 记录创建时间 |
| `updated_at` | timestamp | 记录更新时间 |

### 2.2 `coach_session` 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint PK | 会话主键 |
| `coach_id` | bigint FK → coach(id) | 教练 ID |
| `session_key_encrypted` | string | AES 加密后的微信 `session_key` |
| `refresh_token_hash` | string | refresh_token SHA-256 hash |
| `expires_at` | timestamp | refresh_token 过期时间 |
| `created_at` | timestamp | 记录创建时间 |
| `updated_at` | timestamp | 记录更新时间 |

### 2.3 索引

```sql
-- 非离职教练按 union_id 唯一（status=3 的离职教练允许重新注册新账号）
CREATE UNIQUE INDEX idx_coach_union_id_active ON coach(union_id) WHERE status != 3;

-- openid 兜底查询
CREATE INDEX idx_coach_openid ON coach(openid);

-- 手机号查询（客服/运营场景）
CREATE INDEX idx_coach_phone ON coach(phone);

-- 会话表按 coach 查询
CREATE INDEX idx_coach_session_coach_id ON coach_session(coach_id);

-- 按 refresh_token_hash 查找刷新
CREATE UNIQUE INDEX idx_coach_session_refresh_hash ON coach_session(refresh_token_hash);
```

---

## 3. API 设计

### 3.1 改造接口：`POST /api/v1/auth/wechat-login`

#### 请求参数

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

- `code`：微信 `wx.login()` 返回的临时登录凭证，5 分钟内有效且只能使用一次。
- `encryptedData` / `iv`：微信 `wx.getPhoneNumber` 返回的加密手机号数据。
- `terms_accepted` / `privacy_accepted`：必须同时为 `true`，否则返回 `TERMS_NOT_ACCEPTED`。
- `app_type`：枚举 `user` | `coach`，非法值返回 `VALIDATION_ERROR`。

#### 响应字段（`app_type=coach`）

```json
{
  "access_token": "string",
  "refresh_token": "string",
  "expires_in": 7200,
  "is_new_coach": true,
  "coach_status": -1
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `access_token` | string | JWT access_token，有效期 2h |
| `refresh_token` | string | JWT refresh_token，有效期 7d |
| `expires_in` | int | access_token 有效期秒数，默认 7200 |
| `is_new_coach` | bool | 是否本次登录新建的 coach 记录 |
| `coach_status` | int | -1/0/1/2/3/4，前端据此映射跳转目标页 |

#### 错误码

| HTTP | 错误码 | 场景 |
|------|--------|------|
| 400 | `VALIDATION_ERROR` | 缺少 `code` / `encryptedData` / `iv`，或 `app_type` 非法 |
| 400 | `TERMS_NOT_ACCEPTED` | `terms_accepted` 或 `privacy_accepted` 为 false |
| 401 | `WECHAT_CODE_INVALID` | 微信 `code` 无效或已过期 |
| 502 | `WECHAT_API_ERROR` | 微信 code2session / 手机号解密失败 |
| 504 | `WECHAT_API_TIMEOUT` | 微信接口调用超时（>3s） |
| 429 | `RATE_LIMITED` | 同 IP 1 分钟内超过 30 次 |

### 3.2 新增接口：`GET /api/v1/coach/me/status`

#### 响应字段

```json
{
  "coach_status": 1,
  "rejection_reason": null
}
```

- 需登录鉴权：Header `Authorization: Bearer {access_token}`。
- `coach_status`：当前教练入驻状态；若教练记录不存在，返回 `-1` 并按未提交入驻资料处理（与登录态下调用一致）。
- `rejection_reason`：status=2 时返回，其他状态返回 `null`。
- 前端根据 `coach_status` 映射跳转目标页。

#### 错误码

| HTTP | 错误码 | 场景 |
|------|--------|------|
| 401 | `UNAUTHORIZED` | access_token 缺失/无效/过期 |

---

## 4. 状态机映射

| coach.status | 目标页面（前端映射） |
|-------------|---------------------|
| -1 | 入驻资料页（US-010） |
| 0 | 等待审核页 |
| 1 | 教练首页（US-012） |
| 2 | 重新提交入驻页（US-040） |
| 3 | 重新入驻页（US-040） |
| 4 | 离职处理中页 |

> `coach_onboarding_success`（入驻提交成功页）由 US-010 控制，本 US 不直接返回；前端在 `coach.status=-1` 且提交入驻资料成功后由 US-010 自行跳转到该页。

---

## 5. 微信 OAuth 流程（教练端）

```
教练端小程序
  ├─ 页面展示《用户须知》+《隐私协议》勾选区
  ├─ 教练勾选后点击「微信一键登录」
  ├─ wx.login() → code
  ├─ wx.getPhoneNumber() → encryptedData + iv
  └─ POST /api/v1/auth/wechat-login
        { code, encryptedData, iv, terms_accepted, privacy_accepted, app_type: 'coach' }

后端
  ├─ 校验 terms_accepted / privacy_accepted → 不通过返回 TERMS_NOT_ACCEPTED
  ├─ 校验 app_type = 'coach'
  ├─ code2session(code) → openid + union_id + session_key
  ├─ session_key 解密手机号 → phone
  ├─ findCoachByUnionId(union_id)
  │    ├─ 命中且 status != 3 → 复用 coach 记录
  │    └─ 未命中 或 status = 3 → 新建 coach 记录，status=-1，写入 phone/avatar/nickname
  ├─ 签发 JWT access_token + refresh_token
  ├─ 写入 coach_session 表（session_key 加密）
  └─ 返回 { access_token, refresh_token, expires_in, is_new_coach, coach_status }

前端
  ├─ 存储 access_token / refresh_token / expires_in
  ├─ 按 coach_status 映射跳转目标页
  └─ onShow 检查 token：access_token 过期则刷新；refresh_token 过期则重新登录
```

---

## 6. JWT & Session

- `access_token`：有效期 2h，HS256 签名。
- `refresh_token`：有效期 7d，原始值仅返回一次，数据库存 SHA-256 hash。
- JWT payload（教练端）：`{ sub: coach_id, app_type: 'coach', iat, exp }`。
- `session_key`：AES-256-CBC 加密后存入 `coach_session.session_key_encrypted`，不返回前端。
- Redis 缓存（可选）：`coach:session_key:{coach_id}` TTL 7200s；`auth:idempotent:wechat-login:{code}` TTL 300s。

---

## 7. 安全与性能

| 项目 | 要求 |
|------|------|
| `session_key` | 绝不返回前端 |
| `app_type` | 白名单校验，仅允许 `user` / `coach` |
| `code` | 幂等键，5 分钟内重复提交返回首次结果 |
| 协议勾选 | 后端必须二次校验，不可仅依赖前端拦截 |
| 防刷 | 同 IP 1 分钟 > 30 次 → 429 |
| 登录接口 P50 | < 500ms |
| 登录接口 P99 | < 1500ms（含微信 code2session + 手机号解密） |
| coach 状态查询 P99 | < 50ms |

---

## 8. 跨 US 依赖

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

---

## 9. 边界处理

| 场景 | 处理 |
|------|------|
| 同一 `union_id` 已绑定已离职教练（status=3） | 视为未命中，新建 coach 记录，status=-1，不绑定旧数据 |
| `union_id` 缺失 | 以 `openid` 为兜底唯一键，记录告警日志，不阻断登录 |
| 微信 `code` 已被使用 | 5 分钟内幂等返回首次结果，超过 5 分钟返回 WECHAT_CODE_INVALID |
| 快速重复点击 | 前端按钮点击后立即 disable + loading；后端以 code 为幂等键 |
| access_token 过期 | 前端用 refresh_token 换发新 access_token |
| refresh_token 过期 | 前端引导重新登录 |
