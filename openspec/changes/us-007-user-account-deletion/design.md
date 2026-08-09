# Design: US-007 用户账号注销

> 本文档对应 `docs/stories/US-007-用户-账号注销/tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-007 实现用户主动注销账号功能，采用软删除策略保留历史数据 90 天后匿名化。核心是 1 个查询 API + 1 个写入 API + 条件校验 + 会话清除。

## Data Model

### 新增/修改的表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `user` | UPDATE | `status=1`, `deleted_at` |
| `user_session` | DELETE | 清除所有登录态 |
| `audit_log` | INSERT | `action='account_cancel'`, `user_id`, `ip`, `device`, `created_at` |

### 索引

```sql
-- 审计日志查询索引
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
```

## API Design

### POST /api/user/account/cancel-check

- 鉴权：是
- Request: `{}`
- Response 200: `{ can_cancel: boolean, checks: { no_active_package: boolean, no_pending_order: boolean, no_ongoing_booking: boolean } }`
- Response 401: `UNAUTHORIZED`（未登录）

### POST /api/user/account/cancel

- 鉴权：是
- 幂等：是（`cancel:{user_id}:{timestamp}`，TTL 300s）
- Request: `{}`（MVP 仅前端弹窗二次确认）
- Response 200: `{ cancelled: true, anonymous_after: '2026-10-28T12:00:00Z' }`
- Response 400: `ACTIVE_PACKAGE_EXISTS`（存在 active 套餐）
- Response 400: `PENDING_ORDER_EXISTS`（存在未完成订单）
- Response 400: `ONGOING_BOOKING_EXISTS`（存在进行中或待上课预约）
- Response 409: `ALREADY_CANCELLED`（账号已注销）

## State Machine

### 用户账号状态机

```
正常(0) ──(用户确认注销)──→ 软删除(1)
```

本 US 触发的转换：

| 转换 | 触发条件 | 字段变更 |
|------|---------|---------|
| 正常 → 软删除 | 用户确认注销 | `user.status` 从 `0` 更新为 `1`，`deleted_at` 赋值 |

## Caching

| 层 | Key | TTL | 用途 | 失效策略 |
|----|-----|-----|------|---------|
| Redis | `cancel:check:{user_id}` | 60s | 注销条件查询缓存 | 用户套餐/订单变化时失效 |
| Redis | `session:{token}` | 立即删除 | 登录态 | 注销后批量删除 |
| Redis | `user:{user_id}` | 立即删除 | 用户资料缓存 | 注销后删除 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| 注销条件查询 P99 | < 100ms |
| 注销接口 P99 | < 300ms |
| 会话清除 P99 | < 100ms |
| 审计日志写入 | < 50ms |

## Security

- `POST /api/user/account/cancel` 必须登录鉴权
- MVP 阶段二次确认采用弹窗确认/取消，无需密码或验证码
- 注销前校验无 active 套餐、无未完成订单、无进行中或待上课预约
- 记录审计日志（操作人、时间、IP、设备）
- 注销后执行数据保留策略：订单/套餐历史保留 90 天后匿名化（PRD §3.7、§5.2.1、附录 D45）
- 注销后所有 token 立即失效

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 需要登录态 |
| US-020 / US-025 | 依赖 | 套餐/订单状态影响注销条件 |
| US-009 | 关联 | 注销时同步撤回隐私授权 |

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-007-.../tech-design.md` §1 |
| API Design | `docs/stories/US-007-.../tech-design.md` §2 |
| State Machine | `docs/stories/US-007-.../tech-design.md` §3 |
| Caching | `docs/stories/US-007-.../tech-design.md` §4 |
| Performance / Security | `docs/stories/US-007-.../tech-design.md` §5 |
