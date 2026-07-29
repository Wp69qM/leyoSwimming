# US-003 技术设计文档

> **US 关联**：[user-story.md](./user-story.md)（US-003 游客查看预约释放倒计时）
> **文档角色**：技术设计（plan.md / design.md 角色）
> **作者**：开发　|　**最后更新**：2026-07-30
> **状态**：✅ 已填写

---

## 0. 文档定位

本文档由**开发**填写，作为 US-003 的**技术设计层**（对应 SDD 工作流 Step [3] plan.md 角色）。
它告诉团队"要实现什么"以及"整体技术方案是什么"，但**不替代** [test-plan.md](./test-plan.md) 中的逐任务执行细节。

- **用户故事** = 业务层 WHAT（用户看到什么倒计时）
- **本文档** = 设计层 HOW（数据模型 / API / 缓存 / 计算逻辑）
- **test-plan.md** = 执行层 DO（RED→GREEN→COMMIT 的逐任务）

---

## 1. 数据模型影响

### 1.1 读取的表（不修改）

> `release_rule` 表由 US-015 创建并维护，本 US 仅读取。

| 表名 | 操作 | 字段 | 说明 |
|------|------|------|------|
| `release_rule` | 读 | `id`, `release_day_of_week`, `release_time`, `release_scope`, `status`, `manual_override_at`, `manual_override_note`, `updated_at` | 管理员配置的释放规则 |

### 1.2 release_rule 字段语义

| 字段 | 类型 | 取值范围 | 业务含义 |
|------|------|---------|---------|
| `release_day_of_week` | TINYINT | 1=周一 ... 7=周日 | 每周固定释放日（默认 3=周三） |
| `release_time` | TIME | HH:MM:SS | 释放时刻（默认 10:00:00） |
| `release_scope` | VARCHAR | `next_week` / `next_7_days` | 释放范围：下周整周 / 未来 7 天 |
| `status` | TINYINT | 0=禁用, 1=启用 | 仅 status=1 时倒计时生效 |
| `manual_override_at` | DATETIME | NULL 或具体时间 | 管理员手动覆盖的释放时间（节假日提前/延迟） |
| `manual_override_note` | VARCHAR | 文本 | 手动覆盖原因说明 |

### 1.3 索引

```sql
-- release_rule 表通常只有 1 条启用记录（status=1），无需复杂索引
-- US-015 负责建表，本 US 仅读取
-- 建议在 US-015 中创建：
-- CREATE UNIQUE INDEX idx_release_rule_status ON release_rule(status) WHERE status = 1;
```

---

## 2. API 设计

### 2.1 GET /release-countdown

| 属性 | 值 |
|------|----|
| 路径 | `GET /api/v1/release-countdown` |
| 鉴权 | 否（游客可访问） |
| 幂等 | 是 |

**Query Parameters**

无

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

**字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `show` | boolean | 是否显示倒计时组件 |
| `releaseAt` | string (ISO 8601) | 下次释放时刻（show=true 时非空） |
| `serverNow` | string (ISO 8601) | 服务器当前时间（用于客户端校准） |
| `message` | string | 显示文案（show=true 时非空） |
| `remainingSeconds` | number | 距离释放的剩余秒数（show=true 时非空） |
| `isReleaseDay` | boolean | 是否为释放当天（决定文案类型） |

**业务规则**
- `release_rule` 不存在或 `status=0` → `show: false`
- 当前时间距下次释放 > 24 小时 → `show: false`
- 当前时间距下次释放 ≤ 24 小时且非释放当天 → `show: true`，message = 「距离下周预约开放还有 XX 小时」
- 当前时间为释放当天且在释放时刻前 → `show: true`，message = 「今日 10:00 开放下周预约」
- 当前时间 ≥ 释放时刻 → `show: false`（倒计时消失）

---

## 3. 倒计时计算逻辑

### 3.1 计算下次释放时刻

```typescript
// 伪代码：计算下次释放时刻
function calculateNextReleaseAt(rule: ReleaseRule, serverNow: Date): Date | null {
  // 1. 优先使用 manual_override_at（若未过期）
  if (rule.manual_override_at && rule.manual_override_at > serverNow) {
    return rule.manual_override_at;
  }

  // 2. 计算本周常规释放时刻
  const thisWeekRelease = getThisWeekDateTime(
    rule.release_day_of_week,  // 3 = 周三
    rule.release_time           // 10:00:00
  );

  // 3. 若本周释放时刻还未到，返回本周释放时刻
  if (thisWeekRelease > serverNow) {
    return thisWeekRelease;
  }

  // 4. 否则返回下周释放时刻
  return addDays(thisWeekRelease, 7);
}
```

### 3.2 判断是否显示倒计时

```typescript
function shouldShowCountdown(releaseAt: Date, serverNow: Date): { show: boolean; isReleaseDay: boolean } {
  const diffMs = releaseAt.getTime() - serverNow.getTime();
  const diffHours = diffMs / (1000 * 60 * 60);

  if (diffMs <= 0) {
    return { show: false, isReleaseDay: false };  // 已过释放时刻
  }

  if (diffHours > 24) {
    return { show: false, isReleaseDay: false };  // 超过 24 小时窗口
  }

  // 判断是否为释放当天（同一天）
  const isReleaseDay = isSameDay(releaseAt, serverNow);

  return { show: true, isReleaseDay };
}
```

### 3.3 文案规则

| 条件 | 文案 |
|------|------|
| `isReleaseDay = false` 且 `show = true` | 「距离下周预约开放还有 XX 小时」（XX = `Math.ceil(remainingSeconds / 3600)`） |
| `isReleaseDay = true` 且 `show = true` | 「今日 10:00 开放下周预约」（10:00 取自 `release_time`） |
| `show = false` | 无文案（组件隐藏） |

---

## 4. 缓存策略

### 4.1 倒计时结果缓存

| 维度 | 策略 |
|------|------|
| 缓存层 | Redis |
| Key | `release:countdown` |
| TTL | 10s（倒计时频繁更新，TTL 不宜过长；但 10s 内多次请求命中缓存，避免 DB 空查） |
| 失效 | `release_rule` 变更时主动失效（US-015 触发 `DEL release:countdown`） |
| 穿透 | `show: false` 的结果也缓存 10s |

### 4.2 前端缓存

- 小程序不缓存倒计时结果（每次进入首页都拉最新）
- 前端每秒本地递减 `remainingSeconds`，每 10s 拉一次 API 校准（见 user-story.md §14.2）

### 4.3 缓存 Key 设计考量

- 不带参数（全局唯一一条规则），Key 简化为 `release:countdown`
- 若未来支持多场馆多规则，Key 改为 `release:countdown:venue:{venueId}`

---

## 5. 性能指标

| 指标 | 目标 | 验证方式 |
|------|------|---------|
| API P50 | < 50ms | test-plan.md Task 3 |
| API P99 | < 200ms | test-plan.md Task 3 + 压测 |
| Redis 命中率 | > 99% | 监控 |
| DB 查询 | < 20ms（单条 release_rule 查询） | SQL explain |
| 并发 | 500 QPS 下 P99 < 200ms | P1 压测 |

---

## 6. 安全 / 鉴权

- 接口**无需登录**（游客可访问）
- 防刷：同一 IP 1 分钟内请求 > 300 次，触发限流 429（比 US-001 限流略宽松，因有 10s 缓存）
- 无敏感数据泄露风险（释放时间是公开信息）
- 时间戳防篡改：前端不信任客户端时间，以 `serverNow` 为准

---

## 7. 状态机影响

US-003 是**只读 US**，不修改任何实体状态。

```
release_rule.status（由 US-015 维护）
  0 禁用 ──→ 倒计时不显示
  1 启用 ──→ 倒计时按规则计算

manual_override_at（由 US-015 维护）
  NULL ──→ 按常规 release_day_of_week + release_time 计算
  非NULL 且未过期 ──→ 按手动覆盖时间计算
  非NULL 但已过期 ──→ 忽略，按常规计算
```

---

## 8. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-015 | 被依赖 | 管理员配置 `release_rule`，本 US 读取该配置 |
| US-016 | 协作 | 释放时刻到达时由 US-016 执行实际时段释放；本 US 仅负责倒计时展示，不触发释放 |
| US-023 | 依赖本 US | 关注时段的学员在释放前收到订阅消息提醒（§7.2 第 3 条） |

---

## 9. 异常与边界

| 场景 | 处理 |
|------|------|
| `release_rule` 表无数据 | 返回 `show: false`，记录 WARN 日志 `release_rule not configured` |
| `release_rule.status = 0` | 返回 `show: false`，记录 INFO 日志 `release_rule disabled` |
| `manual_override_at` 已过期 | 忽略该字段，按常规规则计算下次释放时刻 |
| Redis 宕机 | 降级直接查 DB，记录告警 |
| 客户端时间漂移 | 前端以 `serverNow` 为基准，每 10s 校准 |
| 服务器与 DB 时间不一致 | API 计算时使用 DB `NOW()` 函数，不使用应用服务器时间 |

---

## 10. 实现顺序（与 test-plan.md 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 + §3 计算逻辑 | Task 1（ReleaseRule Repository）+ Task 2（倒计时计算逻辑） |
| §2 API 设计 | Task 3（GET /release-countdown API） |
| §4 缓存策略 | Task 3（API 层集成 Redis） |
| 前端展示 | Task 4（小程序首页倒计时组件） |
| §5 性能指标 | Task 1-4 的验收指标 |

---

## 11. 上下游引用

- **用户故事**：[./user-story.md](./user-story.md)（业务需求 + GWT + Figma 章节）
- **测试计划**：[./test-plan.md](./test-plan.md)（TDD 任务清单）
- **全局设计规范**：[../../spec/figma/README.md](../../spec/figma/README.md)
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)
- **依赖 PRD 章节**：[§5.1 第 5 条](../../../prd/prd.md)、[§5.3.2 第 3-4 条](../../../prd/prd.md)、[§5.5.2 第 5 条](../../../prd/prd.md)

---

## 12. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：数据模型 / API / 计算逻辑 / 缓存 / 性能 / 安全 / 跨 US 依赖 |
