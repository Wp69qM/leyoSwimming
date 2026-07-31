# Design: US-048 管理员配置首页运营内容

> 本文档对应 `docs/stories/US-048-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-048 是管理员后台首页运营配置 US，复用 `notice` 表并新增 `homepage_banner`、`homepage_card` 两张表，提供 5 个 API 与 Redis 缓存层。

## Data Model

### 新增表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `homepage_banner` | 首页 Banner | id, name, image_url, link_url, visible_scope, sort_order, start_at, end_at, status |
| `homepage_card` | 首页运营卡片 | id, title, icon_url, link_url, visible_scope, sort_order, start_at, end_at, status |

### 复用表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `notice` | 通知栏公告 | id, title, content, visible_scope, priority, start_at, end_at, status |

### 索引

```sql
CREATE INDEX idx_notice_visible_time ON notice(status, visible_scope, start_at, end_at);
CREATE INDEX idx_banner_visible_time ON homepage_banner(status, visible_scope, start_at, end_at);
CREATE INDEX idx_card_visible_time ON homepage_card(status, visible_scope, start_at, end_at);
```

## API Design

### GET/POST/PUT/DELETE /api/admin/homepage/notices

- 鉴权：管理员登录 + `homepage:write`
- Response 200 / 201 / 400 / 403 / 404

### GET/POST/PUT/DELETE /api/admin/homepage/banners

- 鉴权：管理员登录 + `homepage:write`
- POST Body: `{ name, image_url, link_url, visible_scope, sort_order, start_at, end_at }`
- Response 201 / 400 / 403 / 404

### GET/POST/PUT/DELETE /api/admin/homepage/cards

- 鉴权：管理员登录 + `homepage:write`
- POST Body: `{ title, icon_url, link_url, visible_scope, sort_order, start_at, end_at }`
- Response 201 / 400 / 403 / 404

### POST /api/admin/homepage/preview

- 鉴权：管理员登录 + `homepage:read`
- Body: `{ identity }`
- Response 200: `{ notices, banners, cards }`

### GET /api/homepage/config

- 鉴权：公开或登录
- Query: `identity`
- Response 200: `{ notices, banners, cards }`

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `homepage:config:{identity}` | 300s | 配置变更时主动失效 |
| Redis | `homepage:notices:active` | 300s | notice 变更时失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 列表接口 P50 | < 150ms |
| 列表接口 P99 | < 250ms |
| 首页配置接口 P99 | < 150ms |
| 并发 100 QPS | 无 5xx |

## Security

- 所有管理接口登录 + RBAC
- 内容 XSS 过滤
- 图片 URL 域名白名单
- 字段校验通过后直接写入业务表并即时生效（PRD §5.5.5 #6 / D47：预览机制无需审核直接生效）
- 操作日志记录变更前后快照

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 / US-042 | 被依赖 | 身份与权限基础 |
| US-002 / US-019 | 依赖本 US | 游客/学员端首页展示 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-048-.../tech-design.md` §1 |
| API Design | `docs/stories/US-048-.../tech-design.md` §2 |
| Caching | `docs/stories/US-048-.../tech-design.md` §4 |
| Performance | `docs/stories/US-048-.../tech-design.md` §5 |
| Security | `docs/stories/US-048-.../tech-design.md` §6 |
