# Design: US-010 教练提交入驻资料

> 本文档对应 `docs/stories/US-010-教练-提交入驻资料/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-010 是教练端流程起点，教练提交资质资料后进入待审核状态。核心引入 `coach_application` 快照表，将「待审核/草稿资料」与 `coach` 表「生效资料」分离：pending 期间生效资料保持不变，审核通过后才由 US-011 将快照覆盖写入 coach 表。已离职教练（status=3）重新入驻时亦由 US-010 统一处理，复用原 coach 记录，不回滚、不隔离历史数据。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | INSERT / UPDATE | `coach_id`, `openid` UK, `union_id` UK, `phone` UK, `name`, `gender` TINYINT（1=男 / 2=女）, `age` INT（18-80）, `email` VARCHAR(128), `wechat_qr_url`, `id_card_no`（AES 加密）, `teaching_years`, `total_students`, `total_hours`, `teaching_strokes`, `bio`, `reference_price`, `status`（默认 -1）, `submitted_at`, `approved_at`, `created_at`, `updated_at` |
| `coach_application` | INSERT / UPDATE | `application_id` PK, `coach_id` FK, `status` ENUM（draft/pending/approved/rejected）, `previous_coach_status` TINYINT, 与 coach 资料字段同构的快照字段（`name`/`gender`/`age`/`email`/`wechat_qr_url`/`id_card_no`/`teaching_years`/`total_students`/`total_hours`/`teaching_strokes`/`bio`/`reference_price`）, `submitted_at`, `approved_at`, `approved_by`, `rejection_reason`, `created_at`, `updated_at` |
| `coach_certificate_application` | INSERT / UPDATE | `cert_id` PK, `application_id` FK, `cert_type`（ID_CARD_FRONT/ID_CARD_BACK/COACH_CERT/HEALTH_CERT/PORTRAIT/OTHER）, `image_url`, `sort_order`, `created_at` |
| `coach_certificate` | 只读（写入由 US-011 负责） | `cert_id`, `coach_id` FK, `cert_type`, `image_url`, `sort_order`, `created_at` |
| `coach_audit_log` | INSERT | `log_id`, `coach_id`, `application_id`, `admin_id`, `action`（submit/approve/reject/draft_save）, `from_status`, `to_status`, `reason`, `created_at`；本 US 触发 `action='submit'` 与 `action='draft_save'` |

### 索引

```sql
-- 微信 openid/unionid/phone 唯一索引
CREATE UNIQUE INDEX idx_coach_openid ON coach(openid);
CREATE UNIQUE INDEX idx_coach_union_id ON coach(union_id);
CREATE UNIQUE INDEX idx_coach_phone ON coach(phone);

-- 每个教练只能有一条待审核申请
CREATE UNIQUE INDEX idx_coach_pending_application ON coach_application(coach_id) WHERE status = 'pending';

-- 查询教练最新申请/草稿
CREATE INDEX idx_coach_application_coach_status ON coach_application(coach_id, status, created_at DESC);

-- 快照证书查询索引
CREATE INDEX idx_coach_certificate_application_id_type ON coach_certificate_application(application_id, cert_type);

-- 生效证书查询索引
CREATE INDEX idx_coach_certificate_coach_id_type ON coach_certificate(coach_id, cert_type);
```

### coach.status 字段

| 值 | 业务含义 |
|----|---------|
| `-1` | 未提交入驻资料 |
| `0` | 待审核 |
| `1` | 已通过 |
| `2` | 已驳回 |
| `3` | 已离职 |
| `4` | 申请离职中 |

## API Design

### POST /api/coach/application/submit

- 鉴权：是（教练端登录态）
- Request: `{ name, gender, age, email, wechat_qr_url, id_card_no, teaching_years, total_students, total_hours, teaching_strokes, bio, reference_price, certificates: [{cert_type, image_url}] }`
- Response 200: `{ coach_id, application_id, status: 0, submitted_at }`
- Response 400: `COACH_APPLICATION_PENDING`（已存在 pending 状态的 coach_application，禁止重复提交）
- Response 400: `INVALID_REFERENCE_PRICE`（参考单价超出 50-2000 范围）
- Response 400: `MISSING_REQUIRED_FIELDS`（必填字段或必填资质缺失）
- Response 400: `INVALID_ID_CARD`（身份证号格式不合法）
- Response 400: `IMAGE_TOO_LARGE` / `INVALID_IMAGE_FORMAT`（图片不合规）
- 说明：校验通过后创建 `coach_application` 快照，`status = pending`，记录 `previous_coach_status` 与 `submitted_at`；更新 `coach.status = 0`、`coach.submitted_at = now`；coach 表生效资料此时**不更新**，等待 US-011 审核通过后再覆盖；写入 `coach_audit_log`：`action='submit'`、`from_status=previous_coach_status`、`to_status=0`。

### POST /api/coach/application/save-draft

- 鉴权：是
- Request: 同提交接口（允许部分字段）
- Response 200: `{ coach_id, application_id, status: "draft", submitted_at: null }`
- 说明：保存草稿仅创建或更新 `coach_application` 快照，`status = draft`，不修改 `coach.status`；`submitted_at = NULL` 表示草稿；US-011 审核列表仅查询 `coach_application.status = pending` 的记录，避免草稿进入审核队列；`status = 3` 的教练重新入驻时复用原 coach 记录，历史数据不回滚、不隔离。

### POST /api/coach/application/detail

- 鉴权：是
- Request: 空 JSON body（`{}`）
- Response 200: 返回**最新 coach_application 快照**（draft/pending/rejected）完整资料含 `certificates` 列表；若不存在任何 application 快照且 coach.status=3，则回显 coach 表历史生效资料；身份证号脱敏展示；额外返回派生字段 `entry_type`（draft/first/rejected/reapply，由 coach.status 与 coach_application.status 推导）与 `prompt_message`（来自 `rejection_reason` 或固定文案）
- Response 404: `NO_APPLICATION`（coach 记录不存在）

### POST /api/common/file/upload

- 鉴权：是
- Request: multipart/form-data，字段名 `file`
- 约束：≤5MB，JPG/PNG
- Response 200: `{ url: string }`
- Response 400: `IMAGE_TOO_LARGE` / `INVALID_IMAGE_FORMAT`

## State Machine

### 教练状态机

```
-1 未提交 ──[保存草稿]──→ -1 未提交（coach_application.status = draft）
-1 未提交 ──[提交审核]──→ 0 待审核（coach_application.status = pending, previous_coach_status=-1）
  2 已驳回 ──[保存草稿]──→ 2 已驳回（coach_application.status = draft）
  2 已驳回 ──[重新提交]──→ 0 待审核（coach_application.status = pending, previous_coach_status=2）
  3 已离职 ──[保存草稿]──→ 3 已离职（coach_application.status = draft）
  3 已离职 ──[重新入驻提交]──→ 0 待审核（coach_application.status = pending, previous_coach_status=3）
```

### 申请快照状态机

```
draft 草稿 ──[提交审核]──→ pending 待审核
pending 待审核 ──[管理员通过]──→ approved 已通过
pending 待审核 ──[管理员驳回]──→ rejected 已驳回
```

本 US 触发 coach.status 的初始转换（-1 → 0）、驳回后重新提交转换（2 → 0）与已离职重新入驻转换（3 → 0）。保存草稿不触发 coach.status 转换。US-011 触发审核结果：coach.status 0 → 1（通过）或 0 → previous_coach_status（驳回）；coach_application.status pending → approved/rejected。pending 期间 coach 表生效资料保持不变；审核通过后将 coach_application 快照字段覆盖写入 coach 表及 coach_certificate 表。

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
- `id_card_no` 后端 AES 加密存储；返回给前端时脱敏
- 图片上传校验格式（JPG/PNG）与大小（≤5MB）
- 防重复提交（数据库唯一索引 `idx_coach_pending_application` + 幂等键 `coach_apply:{openid}:{timestamp}`）
- 参考单价、任教年限、总学员数、总课时数等敏感/计费字段必须后端二次校验
- 防止 XSS/SQL 注入：使用参数化查询，图片 URL 做白名单校验

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-009 | 依赖 | 教练需先同意隐私协议与用户须知 |
| US-051 / US-054 | 依赖 | 教练端登录态 |
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
