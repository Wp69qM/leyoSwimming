# Design: US-045 管理员配置标准与自定义套餐

> 本文档对应 `docs/stories/US-045-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-045 是管理员后台配置 US，核心新增 `package_template` 与 `custom_package_config` 两张表，提供 4 个管理员 API 与 Redis 缓存层。

## Data Model

### 新增表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `package_template` | 标准套餐模板 | id, name, total_hours, valid_days, price, status |
| `package_template_coach` | 标准套餐与教练多对多关联 | id, package_template_id, coach_id, reference_price_snapshot |
| `custom_package_config` | 自定义套餐规则 | min_hours, max_hours, default_valid_days, unit_price_floor |

### 读取表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `coach` | 关联教练与参考单价 | id, name, reference_price |

### 索引

```sql
CREATE UNIQUE INDEX idx_package_template_name ON package_template(name);
CREATE UNIQUE INDEX idx_package_template_coach_unique ON package_template_coach(package_template_id, coach_id);
CREATE INDEX idx_package_template_coach_template ON package_template_coach(package_template_id);
CREATE INDEX idx_package_template_coach_coach ON package_template_coach(coach_id);
```

## API Design

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### POST /api/admin/package-template/list

- 鉴权：管理员登录 + `package:read`
- Request：
  ```json
  {
    "page": 1,
    "pageSize": 20,
    "coachId": 1,
    "status": "active"
  }
  ```
  - `coachId` 用于筛选包含指定教练的模板
- Response 200: `{ items: PackageTemplate[], total, page, pageSize }`
- Response 403: `{ error: 'FORBIDDEN' }`

### POST /api/admin/package-template/add

- 鉴权：管理员登录 + `package:write`
- 幂等：`Idempotency-Key` 请求头
- Request：
  ```json
  {
    "name": "标准 6 节",
    "coachIds": [1, 2],
    "totalHours": 6,
    "validDays": 90,
    "price": 108000,
    "status": "active",
    "teachingType": "one_on_one",
    "durationMinutes": 60
  }
  ```
- 规则：`coachIds` 必填且长度 ≥ 1；保存时同步写入 `package_template_coach` 并记录 `referencePriceSnapshot`
- Response 201: 创建后的模板对象
- Response 400: `{ error: 'INVALID_PACKAGE_PARAM' }`
- Response 409: `{ error: 'DUPLICATE_PACKAGE_NAME' }`

### POST /api/admin/package-template/update

- 鉴权：管理员登录 + `package:write`
- Request：
  ```json
  {
    "packageTemplateId": 1,
    "name": "标准 6 节",
    "coachIds": [1, 2],
    "totalHours": 6,
    "validDays": 90,
    "price": 108000,
    "status": "active",
    "teachingType": "one_on_one",
    "durationMinutes": 60
  }
  ```
- 规则：`coachIds` 必填且长度 ≥ 1；保存时按新集合覆盖 `package_template_coach`
- Response 200: 更新后的模板对象
- Response 404: `{ error: 'TEMPLATE_NOT_FOUND' }`

### POST /api/admin/package-template/toggle-status

- 鉴权：管理员登录 + `package:write`
- Request：
  ```json
  {
    "packageTemplateId": 1
  }
  ```
- 业务规则：在 `active` 与 `inactive` 之间切换
- Response 200: 更新后的模板对象
- Response 404: `{ error: 'TEMPLATE_NOT_FOUND' }`

### POST /api/admin/package-template/detail

- 鉴权：管理员登录 + `package:read`
- Request：
  ```json
  {
    "packageTemplateId": 1
  }
  ```
- Response 200: 模板详情对象（含 `coachIds`）
- Response 404: `{ error: 'TEMPLATE_NOT_FOUND' }`

### POST /api/admin/package-template/custom-config

- 鉴权：管理员登录 + `package:write`
- Request：
  ```json
  {
    "minHours": 1,
    "maxHours": 50,
    "defaultValidDays": 60,
    "unitPriceFloor": 10000
  }
  ```
- Response 200: 更新后的全局配置对象
- Response 400: `{ error: 'INVALID_CUSTOM_PACKAGE_CONFIG' }`

### POST /api/admin/image/upload

- 鉴权：管理员登录 + `package:write`
- Content-Type：`multipart/form-data`
- 请求参数：`files: File[]`
- Response 200：`{ urls: string[] }`
- Response 400：`{ error: 'INVALID_IMAGE' }`

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
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-045-.../tech-design.md` §1 |
| API Design | `docs/stories/US-045-.../tech-design.md` §2 |
| Caching | `docs/stories/US-045-.../tech-design.md` §4 |
| Performance | `docs/stories/US-045-.../tech-design.md` §5 |
| Security | `docs/stories/US-045-.../tech-design.md` §6 |
