> **OpenSpec Design | 映射自 `docs/stories/US-009-用户-隐私协议与用户须知授权/tech-design.md`**

## Overview

US-009 实现《用户须知》与《隐私协议》的内容管理、登录页浮层同意、同意记录与登录后查看。不支持撤回授权。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `terms_policy` | INSERT/SELECT | `version`（PK）, `content`, `effective_at`, `is_current` |
| `privacy_policy` | INSERT/SELECT | `version`（PK）, `content`, `effective_at`, `is_current` |
| `user_terms_consent` | INSERT | `consent_id`, `user_id`, `version`, `agreed_at` |
| `user_privacy_consent` | INSERT | `consent_id`, `user_id`, `version`, `agreed_at` |
| `audit_log` | INSERT | `action`（terms_agree/privacy_agree）, `user_id`, `version` |

### 索引

```sql
CREATE INDEX idx_terms_policy_current ON terms_policy(is_current, effective_at);
CREATE INDEX idx_privacy_policy_current ON privacy_policy(is_current, effective_at);
CREATE UNIQUE INDEX idx_user_terms_user_version ON user_terms_consent(user_id, version);
CREATE UNIQUE INDEX idx_user_privacy_user_version ON user_privacy_consent(user_id, version);
```

## API Design

### POST /api/common/terms/current

- 鉴权：否（游客可见）
- Response 200: `{ version: 'v2.0', content: string, effective_at: string }`
- Response 404: `NO_CURRENT_TERMS_POLICY`

### POST /api/common/privacy/current

- 鉴权：否（游客可见）
- Response 200: `{ version: 'v2.0', content: string, effective_at: string }`
- Response 404: `NO_CURRENT_PRIVACY_POLICY`

### POST /api/user/terms/status

- 鉴权：是
- Response 200: `{ status: 'agreed'|'none', version: string|null, required_version: string }`
- Response 401: `UNAUTHORIZED`

### POST /api/user/privacy/status

- 鉴权：是
- Response 200: `{ status: 'agreed'|'none', version: string|null, required_version: string }`
- Response 401: `UNAUTHORIZED`

### POST /api/user/terms/consent

- 鉴权：是
- Request: `{ version: string }`
- Response 200: `{ status: 'agreed', version: string }`
- Response 400: `VERSION_MISMATCH`（非当前版本）
- Response 409: `ALREADY_AGREED`（重复同意当前版本，亦可幂等返回 200）

### POST /api/user/privacy/consent

- 鉴权：是
- Request: `{ version: string }`
- Response 200: `{ status: 'agreed', version: string }`
- Response 400: `VERSION_MISMATCH`（非当前版本）
- Response 409: `ALREADY_AGREED`（重复同意当前版本，亦可幂等返回 200）

> **游客态拦截**：`POST /api/user/terms/status`、`POST /api/user/privacy/status`、`POST /api/user/terms/consent`、`POST /api/user/privacy/consent` 必须登录；未登录统一返回 `401 UNAUTHORIZED`。

## State Machine

### 协议同意状态机

```
未同意 ──[同意]──→ 已同意
```

无撤回状态。版本变化时，已同意态不自动回退，但下次登录（会话失效后）需重新同意。

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 未同意 → 已同意 | 用户在登录页浮层点击「同意」 | `user_terms_consent.agreed_at` / `user_privacy_consent.agreed_at` 赋值 |

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `terms_policy:current` | 1h | 当前生效《用户须知》内容 | 管理员发布新版本时失效 |
| Redis | `privacy_policy:current` | 1h | 当前生效《隐私协议》内容 | 管理员发布新版本时失效 |
| Redis | `user:terms:{user_id}` | 30min | 用户《用户须知》同意状态 | 同意记录写入时失效 |
| Redis | `user:privacy:{user_id}` | 30min | 用户《隐私协议》同意状态 | 同意记录写入时失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 协议查询 P99 | < 100ms |
| 同意记录接口 P99 | < 200ms |
| 授权状态查询 P99 | < 100ms |
| 审计日志写入 | < 50ms |

## Security

- 协议内容不可篡改（仅管理员可更新版本）
- 同意记录不可删除，仅可追加
- 敏感操作记录审计日志
- 合规留存：同意记录至少 3 年
- 游客态禁止提交授权变更

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 微信授权登录成功后需记录协议同意版本 |
| US-006 | 依赖 | 手机号验证码登录成功后需记录协议同意版本 |
| US-047 | 依赖 | 《用户须知》与《隐私协议》版本内容由 US-047 后台管理 |
| US-051 | 依赖/被依赖 | 教练端登录成功后需校验并记录协议同意版本 |
| US-054 | 依赖/被依赖 | 教练手机号验证码登录成功后需校验并记录协议同意版本 |
| US-052 | 相邻 | 用户退出登录后重新登录需再次勾选协议 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-009-.../tech-design.md` §1 |
| API Design | `docs/stories/US-009-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-009-.../tech-design.md` §3 |
| Caching | `docs/stories/US-009-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-009-.../tech-design.md` §5 |
