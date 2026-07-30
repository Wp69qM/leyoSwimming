# Design: US-045 管理员配置标准与自定义套餐

> 本文档对应 `docs/stories/US-045-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-045 是管理员后台配置 US，核心新增 `package_template` 与 `custom_package_config` 两张表，提供 4 个管理员 API 与 Redis 缓存层。

## Data Model

### 新增表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `package_template` | 标准套餐模板 | id, name, coach_id, total_hours, valid_days, price, status |
| `custom_package_config` | 自定义套餐规则 | min_hours, max_hours, default_valid_days |

### 读取表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `coach` | 关联教练与参考单价 | id, name, reference_price |

### 索引

```sql
CREATE UNIQUE INDEX idx_package_template_name ON package_template(name);
CREATE INDEX idx_package_template_coach_status ON package_template(coach_id, status);
```

## API Design

### GET /api/admin/package-templates

- 鉴权：管理员登录 + `package:read`
- Query: `page`, `size`, `coach_id`, `status`
- Response 200: `{ items: PackageTemplate[], total, page, size }`
- Response 403: `{ error: 'FORBIDDEN' }`

### POST /api/admin/package-templates

- 鉴权：管理员登录 + `package:write`
- 幂等：`Idempotency-Key`
- Body: `{ name, coach_id, total_hours, valid_days, price, status }`
- Response 201 / 400 / 409

### PUT /api/admin/package-templates/:id

- 鉴权：管理员登录 + `package:write`
- Body: `{ name, coach_id, total_hours, valid_days, price, status }`
- Response 200 / 404

### POST /api/admin/package-templates/:id/toggle-status

- 鉴权：管理员登录 + `package:write`
- Body: `{ status: 0 | 1 }`
- Response 200 / 404

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `package:templates:active` | 300s | 模板变更时主动失效 |
| Redis | `package:template:{id}` | 300s | 模板变更时主动失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 列表接口 P50 | < 150ms |
| 列表接口 P99 | < 300ms |
| 创建/更新接口 P99 | < 200ms |
| 并发 100 QPS | 无 5xx |

## Security

- 所有接口登录 + RBAC
- 操作日志记录变更前后快照
- 防 ID 遍历：无权限统一 403

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-010 / US-011 | 被依赖 | 教练数据 |
| US-019 / US-020 | 依赖本 US | 学员浏览/购买套餐 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-045-.../tech-design.md` §1 |
| API Design | `docs/stories/US-045-.../tech-design.md` §2 |
| Caching | `docs/stories/US-045-.../tech-design.md` §4 |
| Performance | `docs/stories/US-045-.../tech-design.md` §5 |
| Security | `docs/stories/US-045-.../tech-design.md` §6 |
