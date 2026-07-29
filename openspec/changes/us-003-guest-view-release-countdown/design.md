# Design: US-003 游客查看预约释放倒计时

> 本文档对应 `docs/stories/US-003-.../tech-design.md` 的 OpenSpec 映射版本。

## Overview

US-003 是只读 US，不修改任何实体状态。核心是 1 个公开 API + 倒计时计算服务 + Redis 缓存层 + 小程序倒计时组件。

## Data Model

### 读取的表（不修改）

| 表 | 用途 | 关键字段 |
|----|------|---------|
| `release_rule` | 释放规则配置（由 US-015 维护） | `id`, `release_day_of_week`, `release_time`, `release_scope`, `status`, `manual_override_at`, `manual_override_note`, `updated_at` |

### release_rule 字段语义

| 字段 | 类型 | 取值 | 说明 |
|------|------|------|------|
| `release_day_of_week` | TINYINT | 1-7 | 每周固定释放日（默认 3=周三） |
| `release_time` | TIME | HH:MM:SS | 释放时刻（默认 10:00:00） |
| `release_scope` | VARCHAR | `next_week` / `next_7_days` | 释放范围 |
| `status` | TINYINT | 0/1 | 仅 status=1 时倒计时生效 |
| `manual_override_at` | DATETIME | NULL 或时间 | 管理员手动覆盖时间（节假日提前/延迟） |

### 索引

```sql
-- release_rule 表通常只有 1 条启用记录（status=1）
-- 由 US-015 负责建表和索引
```

## API Design

### GET /api/v1/release-countdown

- 鉴权：否（游客可访问）
- Query：无
- 缓存：Redis TTL 10s

**Response 200（显示倒计时）**

```json
{
  "show": true,
  "releaseAt": "2026-08-06T10:00:00+08:00",
  "serverNow": "2026-08-05T12:00:00+08:00",
  "message": "距离下周预约开放还有 22 小时",
  "remainingSeconds": 79200,
  "isReleaseDay": false
}
```

**Response 200（不显示倒计时）**

```json
{
  "show": false,
  "releaseAt": null,
  "serverNow": "2026-08-06T10:01:00+08:00",
  "message": null,
  "remainingSeconds": null,
  "isReleaseDay": false
}
```

**业务规则**
- `release_rule` 不存在或 `status=0` → `show: false`
- 当前时间距下次释放 > 24 小时 → `show: false`
- 当前时间距下次释放 ≤ 24 小时且非释放当天 → `show: true`，message = 「距离下周预约开放还有 XX 小时」
- 当前时间为释放当天且在释放时刻前 → `show: true`，message = 「今日 10:00 开放下周预约」
- 当前时间 ≥ 释放时刻 → `show: false`

## Calculation Logic

### 计算下次释放时刻

1. 优先使用未过期的 `manual_override_at`
2. 否则计算本周常规释放时刻（`release_day_of_week` + `release_time`）
3. 若本周释放时刻已过，返回下周释放时刻

### 判断显示逻辑

| 条件 | show | isReleaseDay | 文案 |
|------|------|--------------|------|
| 已过释放时刻 | false | false | 无 |
| 距下次释放 > 24h | false | false | 无 |
| 距下次释放 ≤ 24h 且非同一天 | true | false | 「距离下周预约开放还有 XX 小时」 |
| 距下次释放 ≤ 24h 且同一天 | true | true | 「今日 XX:XX 开放下周预约」 |

## Caching

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `release:countdown` | 10s | `release_rule` 变更时由 US-015 主动 `DEL` |
| 小程序本地 | 无（不缓存，每次拉最新） | — | 前端每秒本地递减 + 每 10s 拉 API 校准 |

## Performance Targets

| 指标 | 目标 |
|------|------|
| API P50 | < 50ms |
| API P99 | < 200ms |
| Redis 命中率 | > 99% |
| DB 查询 | < 20ms（单条 release_rule） |
| 并发 500 QPS P99 | < 200ms |

## Security

- 接口无需登录（游客可访问）
- 防刷：同一 IP 1 分钟 > 300 次 → 429 限流
- 时间戳防篡改：前端不信任客户端时间，以 `serverNow` 为基准
- 无敏感数据泄露风险（释放时间是公开信息）

## State Machine Impact

US-003 是只读 US，不修改任何实体状态。读取依赖 `release_rule.status`（由 US-015 维护）和 `manual_override_at`。

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-015 | 被依赖 | 管理员配置 `release_rule`，本 US 读取 |
| US-016 | 协作 | 释放时刻到达时由 US-016 执行实际释放；本 US 仅展示倒计时 |
| US-023 | 依赖本 US | 关注时段用户在释放前收到订阅消息提醒 |

## Open Questions

- 释放时刻到达后，倒计时是否需要立即触发首页自动刷新（拉取新的可约时段）？当前按"组件淡出消失"处理，刷新由用户主动下拉触发。

## Mapping to Source Documents

| 本文档章节 | 源文档 |
|-----------|--------|
| Data Model | `docs/stories/US-003-.../tech-design.md` §1 |
| API Design | `docs/stories/US-003-.../tech-design.md` §2 |
| Calculation Logic | `docs/stories/US-003-.../tech-design.md` §3 |
| Caching | `docs/stories/US-003-.../tech-design.md` §4 |
| Performance | `docs/stories/US-003-.../tech-design.md` §5 |
| Security | `docs/stories/US-003-.../tech-design.md` §6 |
