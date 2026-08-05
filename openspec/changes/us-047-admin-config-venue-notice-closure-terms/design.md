> **OpenSpec Design | 映射自 `docs/stories/US-047-管理员-配置场馆公告闭馆换水与用户须知和隐私协议/tech-design.md`**

## Overview

US-047 实现场馆运营后台配置，涵盖场馆信息、公告、闭馆/换水、《用户须知》与《隐私协议》五个配置域。协议管理域与 US-009 统一表名与数据模型。

## Data Model

### 新增/修改表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `venue` | 场馆信息 | `name`, `address`, `navigation`, `years_open`, `pool_status` |
| `notice` | 公告 | `title`, `content`, `target_roles`, `status`, `publish_time`, `expire_time` |
| `venue_closure` | 闭馆/换水 | `date`, `reason` |
| `terms_policy` | 《用户须知》版本 | `version`（PK）, `content`, `is_current`, `effective_at`, `created_at` |
| `privacy_policy` | 《隐私协议》版本 | `version`（PK）, `content`, `is_current`, `effective_at`, `created_at` |
| `user_terms_consent` | 《用户须知》同意记录 | `consent_id`, `user_id`, `version`, `status`, `agreed_at` |
| `user_privacy_consent` | 《隐私协议》同意记录 | `consent_id`, `user_id`, `version`, `status`, `agreed_at` |
| `booking` / `package` | 修改 | 闭馆时取消并释放课时 |

### 索引

```sql
CREATE UNIQUE INDEX idx_venue_closure_date ON venue_closure(date);
CREATE INDEX idx_notice_status_time ON notice(status, publish_time);
CREATE INDEX idx_terms_policy_current ON terms_policy(is_current, effective_at);
CREATE INDEX idx_privacy_policy_current ON privacy_policy(is_current, effective_at);
CREATE INDEX idx_user_terms_consent_user_id ON user_terms_consent(user_id);
CREATE INDEX idx_user_privacy_consent_user_id ON user_privacy_consent(user_id);
```

## API Design

### GET/PUT /api/admin/venue

- 鉴权：管理员登录 + `venue:write`
- Response 200 / 403

### GET/POST/PUT /api/admin/notices

- 鉴权：管理员登录 + `venue:write`
- Response 200 / 400

### GET/POST /api/admin/venue-closures

- 鉴权：管理员登录 + `venue:write`
- Response 202 / 409

### GET/POST /api/admin/terms

- 鉴权：管理员登录 + `venue:write`
- Response 200 / 400

### GET/POST /api/admin/privacy

- 鉴权：管理员登录 + `venue:write`
- Response 200 / 400

### GET /api/admin/terms/consent-records

- 鉴权：管理员登录 + `venue:read`
- Response 200

### GET /api/admin/privacy/consent-records

- 鉴权：管理员登录 + `venue:read`
- Response 200

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `venue:info` | 600s | 场馆变更时失效 |
| Redis | `notices:active` | 300s | 公告变更时失效 |
| Redis | `terms_policy:current` | 1h | 用户须知发布新版本时失效 |
| Redis | `privacy_policy:current` | 1h | 隐私协议发布新版本时失效 |

## State Machine

```
booking.status: 已预约 ──[闭馆]──→ 已取消 (cancel_reason = 4，场馆闭馆)
terms_policy.is_current: 仅一个 true
privacy_policy.is_current: 仅一个 true
user_terms_consent.status: 已同意 ──[版本更新]──→ 待重新同意
user_privacy_consent.status: 已同意 ──[版本更新]──→ 待重新同意
```

> **cancel_reason 字段类型**（v3 评审 P0 修复）：TINYINT 整型，全项目统一枚举 `1=学员取消 / 2=教练离职 / 3=学员旷课 / 4=场馆闭馆 / 5=教练请假 / 6=套餐冻结`。本 US 闭馆取消课程使用 `4=场馆闭馆`。

## Performance Targets

| 指标 | 目标 |
|------|------|
| 场馆配置接口 P99 | < 200ms |
| 公告列表 P99 | < 200ms |
| 闭馆影响计算 | 异步，< 5s |
| 协议发布接口 P99 | < 300ms |

## Security

- 登录 + RBAC
- 操作日志
- 用户须知/隐私协议内容 XSS 过滤
- 同意记录不可删除，仅可追加/标记状态

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-014 / US-029 | 被依赖 | 预约数据 |
| US-002 / US-019 | 依赖本 US | 展示场馆/公告/用户须知 |
| US-009 | 依赖本 US | 协议版本内容由本 US 管理，同意记录表与本 US 统一 |
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model / API / Caching / Performance / Security | `docs/stories/US-047-.../tech-design.md` |
