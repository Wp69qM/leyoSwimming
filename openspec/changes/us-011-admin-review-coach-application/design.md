# Design: US-011 管理员审核教练入驻资质

> 本文档对应 `docs/stories/US-011-管理员-审核教练入驻资质/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-011 是教练上线的质量关卡，管理员对 `coach_application` 快照进行通过/驳回操作。审核通过时，快照字段覆盖写入 `coach` 生效资料，快照证书覆盖写入 `coach_certificate`；审核驳回时，`coach.status` 恢复为 `previous_coach_status`，生效资料保持不变。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach_application` | UPDATE | `status` pending → approved/rejected，`approved_at`、`approved_by`、`rejection_reason` |
| `coach` | UPDATE | 通过时由 `coach_application` 快照覆盖生效资料；`status`、`approved_at` 更新 |
| `coach_certificate_application` | 读取 | 通过时读取快照证书 |
| `coach_certificate` | INSERT / UPDATE | 通过时由 `coach_certificate_application` 快照覆盖写入 |
| `coach_audit_log` | INSERT | `log_id`, `coach_id`, `application_id`, `admin_id`, `action`（approve/reject）, `from_status`, `to_status`, `reason`, `created_at` |
| `notification` | INSERT | `notification_id`, `user_id`, `type`, `title`, `content`, `created_at` |

### 索引

```sql
-- 待审核列表查询索引
CREATE INDEX idx_coach_application_status_submitted ON coach_application(status, submitted_at);
CREATE INDEX idx_coach_application_coach_status ON coach_application(coach_id, status, created_at DESC);

-- 审核日志查询索引
CREATE INDEX idx_coach_audit_log_coach_id ON coach_audit_log(coach_id);
CREATE INDEX idx_coach_audit_log_application_id ON coach_audit_log(application_id);
```

## API Design

> 本变更所有接口遵循 [API 接口规范](../../../docs/tech/api-convention.md)：统一使用 `POST`，URL 按动作命名，参数通过 JSON body 传递。

### POST /api/admin/coach/application/list

- 鉴权：是（管理员 + `coach:audit` 权限）
- Request body: `{ page, pageSize, keyword, status }`（`status` 默认 `pending`，可选值 `all` / `pending` / `approved` / `rejected`）
- Response 200: `{ total, list: [{ coach_id, name, gender, age, teaching_years, teaching_strokes, latest_submitted_at, status, previous_coach_status, latest_application_id }] }`
- 说明：按 `coach_id` 聚合，每个教练一行，展示该教练在筛选状态下的最新申请；默认仅返回存在 `coach_application.status = pending` 的教练；支持按状态筛选全部 / 待审核 / 已通过 / 已驳回，避免草稿进入审核队列；`previous_coach_status=3` 标记重新入驻申请；列表不展示头像，字段聚焦审核决策所需信息
- Response 403: `FORBIDDEN`

### POST /api/admin/coach/application/detail

- 鉴权：是（管理员 + `coach:audit` 权限）
- Request body: `{ applicationId }`
- Response 200: `{ application_id, coach_id, previous_coach_status, submitted_at, profile: { name, phone, gender, age, email, wechat_qr_url, id_card_no, teaching_years, total_students, total_hours, teaching_strokes, bio, reference_price }, certificates: [{ cert_type, image_url }], application_history: [{ application_id, status, submitted_at, approved_at, approved_by, rejection_reason }] }`
- 说明：返回 coach_application 快照完整资料，并附带该教练（coach_id）的所有历史申请记录；管理员作为审核人员，手机号与身份证号须完整展示，不脱敏
- Response 403: `FORBIDDEN`
- Response 404: `APPLICATION_NOT_FOUND`

### POST /api/admin/coach/application/approve

- 鉴权：是（管理员 + `coach:audit` 权限）
- Request body: `{ applicationId, remark? }`
- Response 200: `{ coach_id, application_id, status: 1, approved_at }`
- Response 400: `NOT_PENDING`
- Response 403: `FORBIDDEN`
- Response 409: `ALREADY_REVIEWED`
- 说明：校验 `coach_application.status = pending` 后，将快照字段覆盖写入 `coach` 表，将快照证书覆盖写入 `coach_certificate` 表；更新 `coach.status=1`、`coach.approved_at=now`；更新 `coach_application.status=approved`、`approved_at`、`approved_by`；写入 `coach_audit_log`；异步发送通知；失效相关缓存。

### POST /api/admin/coach/application/reject

- 鉴权：是（管理员 + `coach:audit` 权限）
- Request body: `{ applicationId, reason }`（`reason` 必填）
- Response 200: `{ coach_id, application_id, status: <恢复后的status>, rejection_reason }`
- Response 400: `MISSING_REJECTION_REASON`
- Response 400: `NOT_PENDING`
- Response 403: `FORBIDDEN`
- Response 409: `ALREADY_REVIEWED`
- 说明：校验 `coach_application.status = pending` 后，更新 `coach_application.status=rejected`、`rejection_reason=reason`；根据 `previous_coach_status` 恢复 `coach.status`（-1→2，2→2，3→3）；写入 `coach_audit_log`；异步发送通知；失效相关缓存。

## State Machine

### 教练状态机

```
待审核(0) ──[通过]──→ 已通过(1)
待审核(0) ──[驳回]──→ previous_coach_status
            ├── previous=-1 → 驳回(2)
            ├── previous=2  → 驳回(2)
            └── previous=3  → 已离职(3)
驳回(2) ──[重新提交，US-010]──→ 待审核(0)
已离职(3) ──[重新入驻提交，US-010]──→ 待审核(0)
```

### 申请快照状态机

```
draft 草稿 ──[提交审核，US-010]──→ pending 待审核
pending 待审核 ──[通过]──→ approved 已通过
pending 待审核 ──[驳回]──→ rejected 已驳回
```

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 待审核 → 已通过 | 管理员通过 | `coach.status=1`，`coach.approved_at` 赋值；`coach_application` 快照覆盖 `coach` 生效资料；`coach_certificate_application` 快照覆盖 `coach_certificate`；写入 coach_audit_log（from_status=0, to_status=1）|
| 待审核 → previous_coach_status | 管理员驳回 | `coach.status` 恢复为 previous_coach_status；`coach_application.status=rejected`，`rejection_reason` 赋值；写入 coach_audit_log（from_status=0, to_status=恢复后的status, reason=驳回原因）|

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `admin:coach:applications` | 1min | 待审核列表缓存 | 新提交/审核后失效 |
| Redis | `coach:{coach_id}` | 立即失效 | 教练详情缓存 | 审核后清除 |
| Redis | `coach:application:{openid}` | 立即失效 | 教练申请状态缓存 | 审核后清除 |

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
- 状态机校验（仅 `coach_application.status = pending` 可转 approved/rejected）
- 记录审计日志
- 异步发送通知

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-010 | 依赖 | 产生待审核快照数据 |
| US-012 / US-013 / US-014 | 被依赖 | 审核通过后解锁教练端功能 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-011-.../tech-design.md` §1 |
| API Design | `docs/stories/US-011-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-011-.../tech-design.md` §3 |
| Caching | `docs/stories/US-011-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-011-.../tech-design.md` §5 |
