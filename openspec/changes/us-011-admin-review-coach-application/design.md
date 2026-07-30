# Design: US-011 管理员审核教练入驻资质

> 本文档对应 `docs/stories/US-011-管理员-审核教练入驻资质/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-011 是教练上线的质量关卡，管理员对教练入驻资料进行通过/驳回操作。核心是 3 个 API + RBAC 权限 + 状态机校验 + 通知。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | UPDATE | `status`, `approved_at`, `rejection_reason`, `auditor_id` |
| `coach_audit_log` | INSERT | `log_id`, `coach_id`, `admin_id`, `action`, `reason`, `created_at` |
| `notification` | INSERT | `notification_id`, `user_id`, `type`, `title`, `content`, `created_at` |

### 索引

```sql
-- 待审核列表查询索引
CREATE INDEX idx_coach_status_created ON coach(status, created_at);

-- 审核日志查询索引
CREATE INDEX idx_coach_audit_log_coach_id ON coach_audit_log(coach_id);
```

## API Design

### GET /api/admin/coach/applications

- 鉴权：是（管理员 + `coach:audit` 权限）
- Request query: `page`, `page_size`, `keyword`
- Response 200: `{ total, list: [{ coach_id, name, phone, reference_price, status, created_at, certificates }] }`
- Response 403: `FORBIDDEN`

### POST /api/admin/coach/applications/{id}/approve

- 鉴权：是（管理员 + `coach:audit` 权限）
- Response 200: `{ coach_id, status: 1, approved_at }`
- Response 400: `NOT_PENDING`
- Response 403: `FORBIDDEN`
- Response 409: `ALREADY_REVIEWED`

### POST /api/admin/coach/applications/{id}/reject

- 鉴权：是（管理员 + `coach:audit` 权限）
- Request: `{ reason: string }`
- Response 200: `{ coach_id, status: 2, rejection_reason }`
- Response 400: `MISSING_REJECTION_REASON`
- Response 400: `NOT_PENDING`
- Response 403: `FORBIDDEN`

## State Machine

### 教练状态机

```
待审核(0) ──[通过]──→ 已通过(1)
待审核(0) ──[驳回]──→ 驳回(2)
```

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 待审核 → 已通过 | 管理员通过 | `status=1`, `approved_at` 赋值, `auditor_id` 赋值 |
| 待审核 → 驳回 | 管理员驳回 | `status=2`, `rejection_reason` 赋值, `auditor_id` 赋值 |

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `admin:coach:applications` | 1min | 待审核列表缓存 | 新提交/审核后失效 |
| Redis | `coach:{coach_id}` | 立即失效 | 教练详情缓存 | 审核后清除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 审核列表 P99 | < 200ms |
| 审核操作 P99 | < 300ms |
| 通知发送 P99 | < 500ms（异步） |
| 并发 50 QPS P99 | < 500ms |

## Security

- 所有接口必须管理员登录鉴权
- RBAC 权限校验（`coach:audit`）
- 状态机校验（仅 0 可转 1/2）
- 记录审计日志
- 异步发送通知

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-010 | 依赖 | 产生待审核数据 |
| US-012 / US-013 / US-014 | 被依赖 | 审核通过后解锁教练端功能 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-011-.../tech-design.md` §1 |
| API Design | `docs/stories/US-011-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-011-.../tech-design.md` §3 |
| Caching | `docs/stories/US-011-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-011-.../tech-design.md` §5 |
