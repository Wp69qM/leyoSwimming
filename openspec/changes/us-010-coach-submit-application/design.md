# Design: US-010 教练提交入驻资料

> 本文档对应 `docs/stories/US-010-教练-提交入驻资料/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-010 是教练端流程起点，教练提交资质资料后进入待审核状态。核心是 4 个 API（提交、草稿、查询、图片上传）+ 字段校验 + 教练状态机初始转换 + 3 个教练小程序页面。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | INSERT / UPDATE | `coach_id`, `openid` UK, `union_id` UK, `phone` UK, `name`, `avatar_url`, `id_card_no`（AES 加密）, `teaching_years`, `total_students`, `total_hours`, `teaching_strokes`, `bio`, `reference_price`, `status`（默认 -1）, `submitted_at`, `approved_at`, `rejection_reason`, `created_at`, `updated_at` |
| `coach_certificate` | INSERT / UPDATE | `cert_id`, `coach_id` FK, `cert_type`（ID_CARD_FRONT/ID_CARD_BACK/COACH_CERT/HEALTH_CERT/PORTRAIT/OTHER）, `image_url`, `sort_order`, `created_at` |
| `coach_audit_log` | INSERT | `log_id`, `coach_id`, `action='submit'`, `operator_id`, `remark`, `created_at` |

### 索引

```sql
-- 微信 openid/unionid/phone 唯一索引
CREATE UNIQUE INDEX idx_coach_openid ON coach(openid);
CREATE UNIQUE INDEX idx_coach_union_id ON coach(union_id);
CREATE UNIQUE INDEX idx_coach_phone ON coach(phone);

-- 待审核/已通过/已驳回/未提交记录的唯一性，用于防重复提交
CREATE UNIQUE INDEX idx_coach_active_application ON coach(openid) WHERE status IN (-1, 0, 1, 2);

-- 证书查询索引
CREATE INDEX idx_coach_certificate_coach_id_type ON coach_certificate(coach_id, cert_type);
```

### coach.status 字段

| 值 | 业务含义 |
|----|---------|
| `-1` | 未提交入驻资料 |
| `0` | 待审核 |
| `1` | 已通过 |
| `2` | 驳回 |
| `3` | 已离职 |
| `4` | 申请离职中 |

## API Design

### POST /api/coach/application

- 鉴权：是（教练端登录态）
- Request: `{ name, id_card_no, teaching_years, total_students, total_hours, teaching_strokes, bio, reference_price, certificates: [{cert_type, image_url}] }`
- Response 200: `{ coach_id, status: 0, submitted_at }`
- Response 400: `COACH_APPLICATION_PENDING`（已有待审核/已通过记录；已驳回记录可重新提交）
- Response 400: `INVALID_REFERENCE_PRICE`（参考单价超出 50-2000 范围）
- Response 400: `MISSING_REQUIRED_FIELDS`（必填字段或必填资质缺失）
- Response 400: `INVALID_ID_CARD`（身份证号格式不合法）
- Response 400: `IMAGE_TOO_LARGE` / `INVALID_IMAGE_FORMAT`（图片不合规）

### PUT /api/coach/application/draft

- 鉴权：是
- Request: 同提交接口（允许部分字段）
- Response 200: `{ coach_id, status: 0, submitted_at: null }`
- 说明：保存草稿与提交审核均写入 `status = 0`，不新增独立草稿态；`submitted_at = NULL` 表示草稿。US-011 审核列表必须过滤 `submitted_at IS NOT NULL` 的记录，避免草稿进入审核队列。`status = -1` 首次保存草稿后变为 0 但 submitted_at 为空；`status = 2` 保存草稿或重新提交时 status 重置为 0 并清空 `rejection_reason`。

### GET /api/coach/application

- 鉴权：是
- Response 200: 完整资料含 `certificates` 列表，手机号与身份证号脱敏展示
- Response 404: `NO_APPLICATION`

### POST /api/upload/image

- 鉴权：是
- Request: multipart/form-data，字段名 `file`
- 约束：≤5MB，JPG/PNG
- Response 200: `{ url: string }`
- Response 400: `IMAGE_TOO_LARGE` / `INVALID_IMAGE_FORMAT`

## State Machine

### 教练状态机

```
-1 未提交 ──[保存草稿]──→ 0 待审核（submitted_at = NULL）
-1 未提交 ──[提交审核]──→ 0 待审核（submitted_at = now）
  2 已驳回 ──[重新提交]──→ 0 待审核（submitted_at = now，rejection_reason = NULL）
```

本 US 触发教练状态机初始转换与驳回后重新提交转换。后续 US-011 触发：0 → 1（通过）或 0 → 2（驳回）。

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `coach:application:{openid}` | 5min | 教练申请状态缓存 | 提交/审核后失效 |
| Redis | `upload:temp:{file_key}` | 1h | 上传图片临时链接 | 提交成功后失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 提交接口 P99 | < 500ms（不含图片上传） |
| 状态查询 P99 | < 100ms |
| 图片上传 P99 | < 2s |
| 并发 50 QPS P99 | < 1s |

## Security

- 所有接口必须教练端登录鉴权
- `id_card_no` 后端 AES 加密存储；返回前端时脱敏
- 图片上传校验格式（JPG/PNG）与大小（≤5MB）
- 防重复提交（数据库唯一索引 + 幂等键 `coach_apply:{openid}:{timestamp}`）
- 敏感字段后端校验（身份证、参考单价、任教年限等）
- 防止 XSS/SQL 注入（参数化查询、图片 URL 白名单）

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-009 | 依赖 | 教练需先同意隐私协议与用户须知 |
| US-051 | 依赖 | 教练端微信授权登录态 |
| US-011 | 被依赖 | 产生待审核数据供管理员审核 |
| US-012 / US-013 / US-014 | 被依赖 | 审核通过后解锁教练端功能 |
| US-040 | 被依赖 | 已离职教练重新入驻复用本 US 接口与页面 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-010-.../tech-design.md` §1 |
| API Design | `docs/stories/US-010-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-010-.../tech-design.md` §3 |
| Caching | `docs/stories/US-010-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-010-.../tech-design.md` §5 |
