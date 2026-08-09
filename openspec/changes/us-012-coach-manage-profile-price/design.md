# Design: US-012 教练管理个人主页与参考单价

> 本文档对应 `docs/stories/US-012-教练-管理个人主页与参考单价/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-012 让通过入驻审核的教练维护个人主页信息与参考单价。核心是 2 个写入 API + 1 个查询 API + 证书上传复用 + 参考单价校验与改价频率限制 + 敏感词过滤 + 缓存失效。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `coach` | UPDATE | `name`、`gender`、`age`、`email`、`wechat_qr_url`、`teaching_years`、`teaching_strokes`、`bio`、`reference_price`、`price_changed_at`、`price_change_count_today` |
| `coach_certificate` | INSERT/UPDATE/DELETE | `cert_id`、`coach_id`、`image_url`、`sort_order`、`created_at` |
| `coach_update_log` | INSERT | `log_id`、`coach_id`、`field_name`、`old_value`、`new_value`、`created_at` |

### 索引

```sql
-- 教练证书查询索引
CREATE INDEX idx_coach_certificate_coach_id ON coach_certificate(coach_id);

-- 教练变更日志查询索引
CREATE INDEX idx_coach_update_log_coach_id ON coach_update_log(coach_id);
```

## API Design

### POST /api/coach/profile/detail

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: 空 JSON body（`{}`）
- Response 200: `{ coach_id, name, gender, age, email, phone, wechat_qr_url, teaching_years, teaching_strokes, bio, reference_price, portrait_url, certificates: [...] }`
- Response 403: `COACH_STATUS_NOT_ALLOWED`（教练未通过审核）

### POST /api/coach/profile/update

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: `{ name?: string, gender?: number, age?: number, email?: string, wechat_qr_url?: string, portrait_url?: string, teaching_years?: number, teaching_strokes?: string[], bio?: string, certificates?: [{ image_url, sort_order }] }`
- Response 200: `{ coach_id, name, gender, age, email, wechat_qr_url, portrait_url, teaching_years, teaching_strokes, bio, certificates }`
- Response 400: `SENSITIVE_CONTENT`（简介含敏感词）
- Response 400: `IMAGE_TOO_LARGE`（证书图片超过 5MB）
- Response 400: `INVALID_IMAGE_FORMAT`（图片格式非 JPG/PNG）
- Response 403: `COACH_STATUS_NOT_ALLOWED`（教练状态异常）

### POST /api/coach/reference-price/update

- 鉴权：是（需教练登录态且 `coach.status = 1`）
- Request: `{ reference_price: number }`
- Response 200: `{ coach_id, reference_price, price_changed_at, price_change_count_today }`
- Response 400: `INVALID_REFERENCE_PRICE`（不在 50-2000 元范围）
- Response 429: `PRICE_CHANGE_LIMIT`（今日改价次数已达 3 次）

## State Machine

### 教练状态机

本 US 不改变 `coach.status`，仅允许 `coach.status = 1（已通过）` 的教练操作。

```
coach.status = 1（已通过） ──[本 US 读写资料/单价]──→ coach.status 保持 1
```

### 参考单价变更影响

- 已购套餐价格：不变
- 新购套餐价格基准：按最新 `coach.reference_price` 计算

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `coach:{coach_id}` | 5 分钟 | 教练详情缓存 | 个人主页/单价更新后立即失效 |
| Redis | `coach:list:*` | 5 分钟 | 教练列表缓存 | 单价更新后立即失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 更新个人主页接口 P99 | < 300ms |
| 更新参考单价接口 P99 | < 200ms |
| 个人主页查询接口 P99 | < 100ms |
| 学员端详情页查询 P99 | < 150ms |

## Security

- `POST /api/coach/profile/update` 与 `POST /api/coach/reference-price/update` 必须教练登录鉴权
- 后端强校验 `coach.status = 1`，防止状态异常教练修改资料
- 个人简介敏感词过滤，防止违规内容展示
- 参考单价后端校验 50-2000 元范围，禁止前端绕过
- 图片大小与格式校验，防止上传恶意文件
- 敏感操作记录审计日志 `coach_update_log`

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 依赖 | 管理员审核通过后 coach.status = 1，方可使用本 US 功能 |
| US-017/US-020 | 被依赖 | 套餐价格计算以 coach.reference_price 为基准 |
| US-038 | 被依赖 | 教练分享个人主页依赖本 US 维护的展示信息 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-012-.../tech-design.md` §1 |
| API Design | `docs/stories/US-012-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-012-.../tech-design.md` §3 |
| Caching | `docs/stories/US-012-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-012-.../tech-design.md` §5 |
