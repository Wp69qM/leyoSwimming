# Design: US-009 用户隐私协议授权与撤回

> 本文档对应 `docs/stories/US-009-用户-隐私协议授权与撤回/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-009 实现隐私协议同意、撤回与版本管理，满足合规要求。核心是 3 个 API + 2 张表 + 状态机 + 授权拦截。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `privacy_policy` | INSERT/SELECT | `version`（PK）, `content`, `effective_at`, `is_current` |
| `user_privacy_consent` | INSERT/UPDATE | `consent_id`, `user_id`, `version`, `status`, `agreed_at`, `revoked_at` |
| `audit_log` | INSERT | `action`（privacy_agree/privacy_revoke）, `user_id`, `version` |

### 索引

```sql
-- 当前生效协议查询索引
CREATE INDEX idx_privacy_policy_current ON privacy_policy(is_current, effective_at);

-- 用户授权查询索引
CREATE INDEX idx_user_privacy_consent_user_id ON user_privacy_consent(user_id);
```

## API Design

### GET /api/privacy-policy/current

- 鉴权：否（游客可见）
- Response 200: `{ version: 'v2.0', content: string, effective_at: string }`
- Response 404: `NO_CURRENT_PRIVACY_POLICY`

### GET /api/user/privacy/status

- 鉴权：是
- Response 200: `{ status: 'agreed'|'revoked'|'none', version: string|null, required_version: string }`
- Response 401: `UNAUTHORIZED`

### POST /api/user/privacy/consent

- 鉴权：是
- Request: `{ version: string, action: 'agree'|'revoke' }`
- Response 200: `{ status: 'agreed'|'revoked', version: string }`
- Response 400: `VERSION_MISMATCH`（非当前版本）
- Response 400: `INVALID_ACTION`
- Response 409: `ALREADY_REVOKED`

> **游客态拦截**：`GET /api/user/privacy/status` 与 `POST /api/user/privacy/consent` 必须登录；未登录统一返回 `401 UNAUTHORIZED`。`GET /api/privacy-policy/current` 对游客可见。

## State Machine

### 隐私授权状态机

```
未同意 ──[同意]──→ 已同意 ──[撤回]──→ 已撤回
```

> **版本 diff 检测**：系统每次展示隐私协议前，比对用户最近一次同意的版本号与当前生效版本号；若版本号变化，强制回到「未同意」态并触发重新授权。

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 未同意 → 已同意 | 用户点击同意 | `user_privacy_consent.status='agreed'`, `agreed_at` 赋值 |
| 已同意 → 已撤回 | 用户点击撤回 | `user_privacy_consent.status='revoked'`, `revoked_at` 赋值 |

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `privacy_policy:current` | 1h | 当前生效协议内容 | 管理员发布新版本时失效 |
| Redis | `user:privacy:{user_id}` | 30min | 用户授权状态 | 同意/撤回时失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 协议查询 P99 | < 100ms |
| 同意/撤回接口 P99 | < 200ms |
| 授权状态查询 P99 | < 100ms |
| 审计日志写入 | < 50ms |

## Security

- 协议内容不可篡改（仅管理员可更新版本）
- 同意/撤回记录不可删除，仅可追加
- 撤回后限制非必要数据收集
- 敏感操作记录审计日志
- 合规留存：同意记录至少 3 年

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-005 | 依赖 | 需要注册用户身份 |
| US-008 | 相邻 | 隐私设置同处账号安全模块 |
| US-017 / US-020 | 被依赖 | 购买套餐等需授权功能需检查隐私状态 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-009-.../tech-design.md` §1 |
| API Design | `docs/stories/US-009-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-009-.../tech-design.md` §3 |
| Caching | `docs/stories/US-009-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-009-.../tech-design.md` §5 |
