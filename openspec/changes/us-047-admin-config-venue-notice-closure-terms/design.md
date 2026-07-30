# Design: US-047 管理员配置场馆、公告、闭馆换水与《用户须知》

> OpenSpec 映射版本。

## Overview

场馆运营配置 US，涉及场馆、公告、闭馆、用户须知四块配置及通知/签署联动。

## Data Model

### 新增/修改表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `venue` | 场馆信息 | name, address, navigation, years_open, pool_status |
| `notice` | 公告 | title, content, target_roles, status, publish_time, expire_time |
| `venue_closure` | 闭馆/换水 | date, reason |
| `terms_of_service` | 用户须知 | version, content, is_active |
| `user_terms_sign` | 签署记录 | user_id, version, status, signed_at |
| `booking` / `package` | 修改 | 闭馆时取消并释放课时 |

### 索引

```sql
CREATE UNIQUE INDEX idx_venue_closure_date ON venue_closure(date);
CREATE INDEX idx_notice_status_time ON notice(status, publish_time);
CREATE INDEX idx_terms_active ON terms_of_service(is_active);
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
- Response 200 / 409

### GET/POST /api/admin/terms

- 鉴权：管理员登录 + `venue:write`
- Response 200 / 400

### GET /api/admin/terms/sign-records

- 鉴权：管理员登录 + `venue:read`
- Response 200

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `venue:info` | 600s | 场馆变更时失效 |
| Redis | `notices:active` | 300s | 公告变更时失效 |
| Redis | `terms:active` | 600s | 用户须知变更时失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 场馆配置接口 P99 | < 200ms |
| 公告列表 P99 | < 200ms |
| 闭馆影响计算 | 异步，< 5s |

## Security

- 登录 + RBAC
- 操作日志
- 用户须知内容 XSS 过滤

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-014 / US-029 | 被依赖 | 预约数据 |
| US-002 / US-019 | 依赖本 US | 展示场馆/公告/用户须知 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model / API / Caching / Performance / Security | `docs/stories/US-047-.../tech-design.md` |
