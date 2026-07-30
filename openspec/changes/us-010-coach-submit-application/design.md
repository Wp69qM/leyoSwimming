# Design: US-010 教练提交入驻资料

> 本文档对应 `docs/stories/US-010-教练-提交入驻资料/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-010 是教练端流程起点，教练提交资质资料后进入待审核状态。核心是 3 个 API + 图片上传 + 字段校验 + 教练状态机初始转换。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | INSERT | `coach_id`, `user_id`, `name`, `teaching_years`, `total_students`, `total_hours`, `bio`, `reference_price`, `status=0`, `created_at` |
| `coach_certificate` | INSERT | `cert_id`, `coach_id`, `image_url`, `sort_order` |
| `coach_audit_log` | INSERT | `log_id`, `coach_id`, `action='submit'`, `created_at` |

### 索引

```sql
-- 教练与用户一对一（或一对多历史）查询索引
CREATE UNIQUE INDEX idx_coach_user_id_active ON coach(user_id) WHERE status IN (0, 1, 2);

-- 证书查询索引
CREATE INDEX idx_coach_certificate_coach_id ON coach_certificate(coach_id);
```

### coach.status 字段

| 值 | 业务含义 |
|----|---------|
| `0` | 待审核 |
| `1` | 已通过 |
| `2` | 驳回 |
| `3` | 已离职 |
| `4` | 申请离职中 |

## API Design

### POST /api/coach/application

- 鉴权：是（教练端登录态）
- Request: `{ name, teaching_years, total_students, total_hours, bio, reference_price, certificates: string[] }`
- Response 200: `{ coach_id, status: 0 }`
- Response 400: `COACH_APPLICATION_PENDING`（已有待审核/已通过记录）
- Response 400: `INVALID_REFERENCE_PRICE`（参考单价超出范围）
- Response 400: `MISSING_REQUIRED_FIELDS`（必填项缺失）

### PUT /api/coach/application/draft

- 鉴权：是
- Request: 同提交接口（部分字段）
- Response 200: `{ coach_id, status: 0, draft: true }`

### GET /api/coach/application

- 鉴权：是
- Response 200: `{ coach_id, status, submitted_at, draft }`
- Response 404: `NO_APPLICATION`

### POST /api/upload/image

- 鉴权：是
- Request: multipart/form-data
- Response 200: `{ url: string }`
- Response 400: `INVALID_IMAGE_FORMAT`
- Response 400: `IMAGE_TOO_LARGE`

## State Machine

### 教练状态机

```
无 ──[提交资料]──→ 待审核(0)
```

本 US 触发教练状态机初始转换。后续 US-011 触发：0 → 1（通过）或 0 → 2（驳回）。

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `coach:application:{user_id}` | 5min | 教练申请状态缓存 | 提交/审核后失效 |
| Redis | `upload:temp:{file_key}` | 1h | 上传图片临时链接 | 提交成功后失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 提交接口 P99 | < 500ms（含图片处理） |
| 状态查询 P99 | < 100ms |
| 图片上传 P99 | < 2s |
| 并发 50 QPS P99 | < 1s |

## Security

- 所有接口必须教练端登录鉴权
- 图片上传校验格式（JPG/PNG）与大小（≤5MB）
- 防重复提交（数据库唯一索引 + 幂等键）
- 敏感字段后端校验
- 参考单价后端校验范围

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 教练端登录态 |
| US-011 | 被依赖 | 产生待审核数据供管理员审核 |
| US-012 / US-013 / US-014 | 被依赖 | 审核通过后解锁教练端功能 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-010-.../tech-design.md` §1 |
| API Design | `docs/stories/US-010-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-010-.../tech-design.md` §3 |
| Caching | `docs/stories/US-010-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-010-.../tech-design.md` §5 |
