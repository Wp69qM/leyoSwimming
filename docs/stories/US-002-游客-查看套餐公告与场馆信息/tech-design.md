# US-002 技术设计文档

> **US 关联**：[user-story.md](./user-story.md)（US-002 游客查看套餐、公告与场馆信息）
> **文档角色**：技术设计（plan.md / design.md 角色）
> **作者**：开发　|　**最后更新**：2026-07-30
> **状态**：✅ 已填写

---

## 0. 文档定位

本文档由**开发**填写，作为 US-002 的**技术设计层**（对应 SDD 工作流 Step [3] plan.md 角色）。
它告诉团队"要实现什么"以及"整体技术方案是什么"，但**不替代** [test-plan.md](./test-plan.md) 中的逐任务执行细节。

- **用户故事** = 业务层 WHAT（用户看到什么）
- **本文档** = 设计层 HOW（数据模型 / API / 状态机 / 缓存）
- **test-plan.md** = 执行层 DO（RED→GREEN→COMMIT 的逐任务）

---

## 1. 数据模型影响

### 1.1 新增/修改的表

> US-002 是**只读 US**，不修改任何表，仅读取以下 4 张表。
> 其中 `venue` / `venue_closure` / `package_template` 的字段定义 PRD §9.2 未完整给出，本节基于 PRD §5.5.3 / §5.5.4 业务描述推导，最终字段以 US-045 / US-047 落地为准。

| 表名 | 操作 | 字段 | 说明 |
|------|------|------|------|
| `notice` | 读 | `notice_id`, `type`, `priority`, `title`, `content`, `visible_scope`, `start_at`, `end_at` | 公告 / 通知栏（PRD §9.2.13 已定义字段）|
| `venue` | 读 | `venue_id`, `name`, `address`, `longitude`, `latitude`, `open_year`, `pool_status`, `image_url` | 场馆主表（PRD §5.5.4 描述字段：名称/地址/导航/开业年限/泳池状态）|
| `venue_closure` | 读 | `venue_id`, `closure_type`, `start_at`, `end_at`, `reason` | 闭馆换水（PRD §9.1 ERD 有 1:N 关系；closure_type: 0=闭馆 1=换水）|
| `package_template` | 读 | `template_id`, `package_type`, `total_hours`, `price`, `valid_days`, `name`, `status`, `sort_order` | 套餐模板（PRD §5.5.3 描述：标准套餐 1/6/8/10 节 + 自定义；package_type: 0=体验 1=正式 2=自定义；status: 0=下架 1=上架）|

### 1.2 索引

```sql
-- notice 表按时间窗 + priority 查询，是核心查询
CREATE INDEX idx_notice_time_priority ON notice(start_at, end_at, priority DESC);

-- venue 表通常只有 1 条记录（单场馆 MVP），主键查询即可
-- venue_closure 按 venue_id + 时间窗查询
CREATE INDEX idx_venue_closure_time ON venue_closure(venue_id, start_at, end_at);

-- package_template 按 status + sort_order 查询
CREATE INDEX idx_package_template_status ON package_template(status, sort_order);
```

### 1.3 notice.type 与 visible_scope 语义

| 字段 | 取值 | 业务含义 | 游客可见规则 |
|------|------|---------|-------------|
| `type` | 0=开放 / 1=换水 / 2=释放 / 3=紧急 | 公告类别 | 全部可见（含游客） |
| `visible_scope` | all / student / coach | 可见范围 | 游客仅可见 `visible_scope = 'all'` |
| `priority` | 0=普通 / 1=提醒 / 2=紧急 | 排序权重 | 降序展示 |

---

## 2. API 设计

### 2.1 GET /packages（套餐列表）

| 属性 | 值 |
|------|----|
| 路径 | `GET /api/v1/packages` |
| 鉴权 | 否（游客可访问） |
| 幂等 | 是 |

**Query Parameters**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | string | 否 | — | 过滤套餐类型：`experience` / `standard` / `custom`；不传返回全部（除 custom） |

**Response 200**

```json
{
  "items": [
    {
      "templateId": 1,
      "packageType": "experience",
      "name": "体验套餐",
      "totalHours": 1,
      "price": 99.00,
      "validDays": 30,
      "sortOrder": 0,
      "tag": "推荐"
    },
    {
      "templateId": 2,
      "packageType": "standard",
      "name": "标准套餐 6 节",
      "totalHours": 6,
      "price": 1080.00,
      "validDays": 90,
      "sortOrder": 1,
      "tag": null
    }
  ]
}
```

**业务规则**
- 仅返回 `status = 1`（上架）的模板
- 仅返回 `package_type ∈ {0 体验, 1 正式}` 的模板；`package_type = 2 自定义` 不在列表接口返回（自定义套餐无固定价格，仅展示入口）
- 过滤 `price = 0` 的赠送类模板（避免被错误上架后出现在游客列表）
- 按 `sort_order ASC` 排序，`sort_order` 相同时 `package_type ASC`（体验在前）
- 空列表返回 200 + `items: []`，不报错

### 2.2 GET /announcements（公告列表）

| 属性 | 值 |
|------|----|
| 路径 | `GET /api/v1/announcements` |
| 鉴权 | 否（游客可访问） |
| 幂等 | 是 |

**Query Parameters**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `limit` | int | 否 | 5 | 返回条数，最大 10 |

**Response 200**

```json
{
  "items": [
    {
      "noticeId": 101,
      "type": "紧急",
      "priority": 2,
      "title": "今日换水通知",
      "content": "14:00-16:00 换水，暂停营业",
      "startAt": "2026-07-30T14:00:00Z",
      "endAt": "2026-07-30T16:00:00Z"
    }
  ]
}
```

**业务规则**
- 仅返回 `visible_scope = 'all'`（游客可见）的公告
- 仅返回 `start_at <= NOW() < end_at` 的公告（时间窗内生效）
- 按 `priority DESC, start_at DESC` 排序
- `limit` 默认 5；Repository 将其上限钳制为 10，即 `Math.min(limit, 10)`
- Controller 对 `limit > 10` 不返回 400，直接返回 200 并最多返回 10 条；仅对非整数或 `limit < 1` 返回 400
- 空列表返回 200 + `items: []`，前端隐藏通知栏区域

### 2.3 GET /venue（场馆信息）

| 属性 | 值 |
|------|----|
| 路径 | `GET /api/v1/venue` |
| 鉴权 | 否（游客可访问） |
| 幂等 | 是 |

**Response 200**

```json
{
  "venueId": 1,
  "name": "乐游游泳馆",
  "address": "北京市朝阳区建国路 88 号",
  "longitude": 116.4817,
  "latitude": 39.9087,
  "openYear": 2018,
  "poolStatus": "正常营业",
  "imageUrl": "https://cdn.example.com/venue/1.jpg",
  "closureNotice": {
    "type": "换水",
    "reason": "月度换水",
    "startAt": "2026-07-30T14:00:00Z",
    "endAt": "2026-07-30T16:00:00Z"
  }
}
```

**Response 404**

```json
{
  "error": "VENUE_NOT_CONFIGURED",
  "message": "场馆信息配置中"
}
```

**业务规则**
- `venue` 表无记录 → 404 + `VENUE_NOT_CONFIGURED`
- `closureNotice` 字段：查询 `venue_closure` 表当前时间窗命中的记录，无命中则该字段为 `null`
- 经纬度用于前端 `wx.openLocation` 唤起微信地图导航

---

## 3. 状态机影响

US-002 是**只读 US**，不修改任何实体状态。但读取依赖以下状态：

```
notice 的时间窗状态
  start_at > NOW()            ──→ 未生效，不返回
  start_at <= NOW() < end_at  ──→ 生效中，返回
  NOW() >= end_at             ──→ 已过期，不返回

notice.visible_scope
  'all'                       ──→ 游客可见
  'student' / 'coach'         ──→ 游客不可见

package_template.status
  0 下架                      ──→ 不返回
  1 上架                      ──→ 返回

venue_closure 时间窗
  命中当前时间                ──→ closureNotice 字段填充
  未命中                      ──→ closureNotice = null
```

> **说明**：本 US 不触发任何状态转换，所有状态转换由管理端 US（US-045/US-047/US-048）触发。

---

## 4. 缓存策略

### 4.1 套餐列表缓存

| 维度 | 策略 |
|------|------|
| 缓存层 | Redis |
| Key | `packages:list:type:{type}`（type 为 `all` / `experience` / `standard` / `custom`） |
| TTL | 300s（套餐价格变动频率低，TTL 可较长） |
| 失效 | 管理端 US-045 上下架套餐模板时主动 DEL 对应 key |
| 穿透 | 空列表也缓存 60s，避免 DB 空查 |

### 4.2 公告列表缓存

| 维度 | 策略 |
|------|------|
| 缓存层 | Redis |
| Key | `announcements:list:limit:{limit}` |
| TTL | 60s（公告时效性强，TTL 短） |
| 失效 | 管理端 US-047/US-048 增删改公告时主动 DEL |
| 穿透 | 空列表也缓存 30s |

### 4.3 场馆信息缓存

| 维度 | 策略 |
|------|------|
| 缓存层 | Redis |
| Key | `venue:info` |
| TTL | 3600s（场馆信息极少变动） |
| 失效 | 管理端 US-047 修改场馆信息 / 闭馆换水时主动 DEL |
| 降级 | Redis 宕机时直接查 DB，记录告警 |

### 4.4 前端缓存

- 微信小程序首页使用 `Taro.setStorageSync` 缓存最近 1 次公告 + 套餐数据
- 首次进入先展示缓存 → 后台静默刷新 → 更新 UI（stale-while-revalidate 模式）

---

## 5. 性能指标

| 指标 | 目标 | 验证方式 |
|------|------|---------|
| 公告列表首屏 | P50 < 100ms，P99 < 300ms | test-plan.md Task 2 + 压测 |
| 套餐列表首屏 | P50 < 150ms，P99 < 400ms | test-plan.md Task 4 + 压测 |
| 场馆信息首屏 | P50 < 100ms，P99 < 300ms | test-plan.md Task 4 + 压测 |
| 公告 DB 查询 | < 30ms（含时间窗 + priority 排序） | SQL explain + 慢查询监控 |
| 并发 | 1000 QPS 下 P99 < 800ms | P1 压测 |

---

## 6. 安全 / 鉴权

- 三个接口均**无需登录**（游客可访问）
- 防刷：同一 IP 1 分钟内请求 > 200 次，触发限流 429
- 防注入：所有 query 参数做类型校验（`limit` 必须为 ≥1 的整数；大于 10 时由 Repository 钳制为 10，Controller 仍返回 200；`type` 必须为枚举值）
- 不暴露管理端字段：`package_template` 的 `created_by` / `updated_at` 等管理端字段不返回给游客

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-045 | 被依赖 | 管理员配置套餐模板，产出 `package_template` 数据 |
| US-047 | 被依赖 | 管理员配置场馆/公告/闭馆换水，产出 `venue` / `notice` / `venue_closure` 数据 |
| US-048 | 被依赖 | 管理员配置首页通知栏，产出 `notice` 数据 |
| US-017 | 依赖本 US | 游客从套餐列表点击购买体验课 |
| US-019 | 依赖本 US | 学员浏览正价套餐（已登录视角，复用套餐查询能力） |
| US-003 | 依赖本 US | 游客查看预约释放倒计时，复用公告 type=2 释放类公告 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 无上架套餐模板 | 列表返回 `items: []` + 200，前端展示空状态文案 |
| 无生效公告 | 列表返回 `items: []` + 200，前端隐藏通知栏区域 |
| 场馆未配置 | 场馆信息接口返回 404 + `VENUE_NOT_CONFIGURED`，前端展示占位文案 |
| 公告 `visible_scope` 不含游客 | 后端 SQL 过滤 `visible_scope = 'all'`，游客看不到 student/coach 类公告 |
| 赠送类套餐模板被错误上架 | 后端 SQL 过滤 `package_type IN (0, 1)`，不返回 `package_type = 2` |
| 套餐模板 `price = 0` | 后端 SQL 过滤 `price > 0`，不返回赠送类模板 |
| 公告 `limit > 10` | Repository 钳制为 10，Controller 仍返回 200 |
| Redis 宕机 | 降级直接查 DB，记录告警 |
| DB 慢查询 | 超过 50ms 触发慢查询告警 |
| 闭馆换水时间窗命中 | `venue.closureNotice` 字段填充，前端置顶红色提示条 |

---

## 9. 实现顺序（与 test-plan.md 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 + 索引 | Task 1, Task 2, Task 3（3 个 Repository） |
| §2.1 GET /packages | Task 4（套餐 API 端点） |
| §2.2 GET /announcements | Task 4（公告 API 端点） |
| §2.3 GET /venue | Task 4（场馆 API 端点） |
| §4 缓存策略 | 后端缓存后续补充（P1） |
| §5 性能指标 | Task 1-4 的验收指标 |

---

## 10. 上下游引用

- **用户故事**：[./user-story.md](./user-story.md)（业务需求 + GWT + Figma 章节）
- **测试计划**：[./test-plan.md](./test-plan.md)（TDD 任务清单）
- **全局设计规范**：[../../figma/README.md](../../figma/README.md)
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版完整填写：3 个只读 API + 4 张表读取 + Redis 缓存（套餐 300s / 公告 60s / 场馆 3600s）+ 性能指标 + 安全防刷 |
| v1.1 | 2026-07-30 | Dev | 合规修复：明确 package_template 过滤 price=0；明确 limit 钳制为 10 且 Controller 返回 200 |
