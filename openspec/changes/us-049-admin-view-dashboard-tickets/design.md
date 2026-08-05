# Design: US-049 管理员查看数据看板与处理客服工单

> 本文档对应 `docs/stories/US-049-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-049 是管理员后台数据看板与客服工单处理 US，读取已有业务数据生成 5 项基础看板，并新增 `support_ticket`、`ticket_reply` 表提供工单流转能力。

## Data Model

### 新增表

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `support_ticket` | 客服工单 | id, ticket_no, type, user_id, coach_id, order_id, title, content, status, handler_id, created_at, updated_at |
| `ticket_reply` | 工单回复 | id, ticket_id, replier_type, replier_id, content, created_at |

### 读取表

| 表 | 用途 |
|----|------|
| `booking` / `package` / `coach` / `order` / `user` | 看板数据源 |

### 索引

```sql
CREATE INDEX idx_support_ticket_status ON support_ticket(status);
CREATE INDEX idx_support_ticket_user ON support_ticket(user_id, status);
CREATE INDEX idx_ticket_reply_ticket ON ticket_reply(ticket_id);
```

## API Design

### GET /api/admin/dashboard/peak-hours

- 鉴权：管理员登录 + `dashboard:read`
- Query：`course_type`, `start_date`, `end_date`
- Response 200 / 400 / 403

### GET /api/admin/dashboard/monthly-hours

- 鉴权：管理员登录 + `dashboard:read`
- Query：`start_month`, `end_month`
- Response 200 / 400 / 403

### GET /api/admin/dashboard/coach-ratings

- 鉴权：管理员登录 + `dashboard:read`
- Query：`limit`
- Response 200 / 403

### GET /api/admin/dashboard/student-portrait

- 鉴权：管理员登录 + `dashboard:read`
- Response 200 / 403

### GET /api/admin/tickets

- 鉴权：管理员登录 + `ticket:read`
- Query：`status`, `type`, `page`, `size`
- Response 200 / 403

### GET /api/admin/tickets/:id

- 鉴权：管理员登录 + `ticket:read`
- Response 200 / 404 / 403

### PUT /api/admin/tickets/:id/assign

- 鉴权：管理员登录 + `ticket:write`
- Body：`{ handler_id }`
- Response 200 / 400 / 403

### POST /api/admin/tickets/:id/reply

- 鉴权：管理员登录 + `ticket:write`
- 幂等性：`Idempotency-Key`
- Body：`{ content, status? }`
- Response 201 / 400 / 403

### PUT /api/admin/tickets/:id/status

- 鉴权：管理员登录 + `ticket:write`
- Body：`{ status }`
- Response 200 / 400 / 403

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `dashboard:peak-hours:{type}:{range}` | 300s | 新预约/取消时失效 |
| Redis | `dashboard:monthly-hours:{range}` | 600s | 上课记录变更时失效 |
| Redis | `dashboard:coach-ratings` | 600s | 教练评分变更时失效 |
| Redis | `dashboard:student-portrait` | 600s | 用户身份变更时失效 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 看板接口 P50 | < 200ms |
| 看板接口 P99 | < 500ms |
| 工单列表 P99 | < 200ms |
| 工单详情 P99 | < 150ms |
| 并发 100 QPS | 无 5xx |

## Security

- 所有管理接口登录 + RBAC
- 看板数据脱敏（不返回用户手机号等敏感字段）
- 工单回复内容 XSS 过滤
- 操作日志记录管理员 ID、IP、变更前后快照

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 / US-025 / US-032 | 被依赖 | 用户、订单、上课记录数据 |
| US-027 / US-030 | 本 US 依赖 | 退款/取消纠纷工单由 US-027（学员申请退款）/ US-030（学员取消正价课程）产生，由本 US 处理 |
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |

> **依赖方向澄清**（v7 P0-H 修复，v1.1 半落地修复）：US-027/US-030 是 US-049 的**前置**（US-049 依赖 US-027/US-030 产生工单数据源），并非"US-027/US-030 依赖 US-049"。原 design.md 表格方向列写反，本次将表格方向列同步修正为「本 US 依赖」，与 user-story §9 对齐。

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-049-.../tech-design.md` §1 |
| API Design | `docs/stories/US-049-.../tech-design.md` §2 |
| Caching | `docs/stories/US-049-.../tech-design.md` §4 |
| Performance | `docs/stories/US-049-.../tech-design.md` §5 |
| Security | `docs/stories/US-049-.../tech-design.md` §6 |
