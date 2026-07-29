# US-001 技术设计文档

> **US 关联**：[user-story.md](./user-story.md)（US-001 游客浏览教练列表与详情）
> **文档角色**：技术设计（plan.md / design.md 角色）
> **作者**：开发　|　**最后更新**：2026-07-30
> **状态**：✅ 已填写

---

## 0. 文档定位

本文档由**开发**填写，作为 US-001 的**技术设计层**（对应 SDD 工作流 Step [3] plan.md 角色）。
它告诉团队"要实现什么"以及"整体技术方案是什么"，但**不替代** [test-plan.md](./test-plan.md) 中的逐任务执行细节。

- **用户故事** = 业务层 WHAT（用户看到什么）
- **本文档** = 设计层 HOW（数据模型 / API / 状态机 / 缓存）
- **test-plan.md** = 执行层 DO（RED→GREEN→COMMIT 的逐任务）

---

## 1. 数据模型影响

### 1.1 新增/修改的表

> 仅列出与 US-001 直接相关的字段；完整字段定义见 PRD §9。

| 表名 | 操作 | 字段 | 说明 |
|------|------|------|------|
| `coach` | 读 | `id`, `name`, `status`, `rating`, `years_of_teaching`, `real_time_status` | 列表 + 详情主表 |
| `coach_certificate` | 读 | `coach_id`, `name`, `level` | 详情页证书列表 |
| `coach_review` | 读 | `coach_id`, `rating`, `content`, `created_at` | 详情页评价列表 |
| `coach_availability` | 读 | `coach_id`, `date`, `start_time`, `end_time` | 详情页可约时间 |

### 1.2 索引

```sql
-- 列表页按 status + rating 查询，是核心查询
CREATE INDEX idx_coach_status_rating ON coach(status, rating DESC);

-- 详情页按 id 主键查询即可
-- coach_certificate / coach_review / coach_availability 已按 coach_id 建索引
```

### 1.3 coach.status 与 real_time_status 区分

| 字段 | 类型 | 取值范围 | 业务含义 | 公开可见规则 |
|------|------|---------|---------|-------------|
| `status` | TINYINT | 0=申请中, 1=在职, 2=休息, 3=离职审批中, 4=已离职 | 入驻生命周期状态 | 仅 status=1 出现在公开列表 |
| `real_time_status` | TINYINT/VARCHAR | 空闲中/上课中/休息中/已下班/请假中 | 当日实时可约状态 | status=1 时展示，影响"立即预约"按钮 |

---

## 2. API 设计

### 2.1 GET /coaches（列表）

| 属性 | 值 |
|------|----|
| 路径 | `GET /api/v1/coaches` |
| 鉴权 | 否（游客可访问） |
| 幂等 | 是 |

**Query Parameters**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | 1 | 页码 |
| `size` | int | 否 | 10 | 每页条数，最大 50 |

**Response 200**

```json
{
  "items": [
    {
      "id": 1,
      "name": "王教练",
      "avatar": "https://cdn.example.com/avatar/1.jpg",
      "rating": 4.9,
      "yearsOfTeaching": 8,
      "certificates": ["国家一级运动员", "高级游泳教练"],
      "realTimeStatus": "空闲中"
    }
  ],
  "total": 42,
  "page": 1,
  "size": 10
}
```

**业务规则**
- 仅返回 `coach.status = 1`（在职）的记录
- 按 `rating DESC` 排序
- `realTimeStatus` 返回中文文案，映射规则见 PRD §5.2.2

### 2.2 GET /coaches/:id（详情）

| 属性 | 值 |
|------|----|
| 路径 | `GET /api/v1/coaches/:id` |
| 鉴权 | 否 |
| 幂等 | 是 |

**Path Parameters**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | int | 是 | 教练 ID |

**Response 200**

```json
{
  "id": 1,
  "name": "王教练",
  "avatar": "https://cdn.example.com/avatar/1.jpg",
  "rating": 4.9,
  "yearsOfTeaching": 8,
  "certificates": [
    { "name": "国家一级运动员", "level": "国家级" },
    { "name": "高级游泳教练", "level": "高级" }
  ],
  "reviews": [
    { "id": 101, "rating": 5, "content": "非常专业", "createdAt": "2026-07-20T10:00:00Z" }
  ],
  "availableTimes": [
    { "date": "2026-08-01", "slots": ["09:00-10:00", "10:00-11:00"] }
  ],
  "realTimeStatus": "空闲中"
}
```

**Response 404**

```json
{
  "error": "COACH_NOT_FOUND",
  "message": "教练信息不存在"
}
```

**业务规则**
- `id` 不存在 → 404
- `coach.status = 0`（申请中） → 404（与不存在等效处理）
- `coach.status = 4`（已离职） → 404
- 其他 status（1/2/3）均可查，但 `realTimeStatus` 决定"立即预约"按钮是否展示

---

## 3. 状态机影响

US-001 是**只读 US**，不修改任何实体状态。但读取依赖以下状态：

```
coach.status
  0 申请中 ──→ 不公开
  1 在职   ──→ 公开，且 real_time_status 决定 UI
  2 休息   ──→ 公开（教练可自己标记）
  3 离职审批中 ──→ 公开？待 PM 确认（当前按 PRD 保守处理为不公开）
  4 已离职 ──→ 不公开

coach.real_time_status（仅 status=1 时有效）
  空闲中   ──→ 展示"立即预约"
  上课中   ──→ 不展示"立即预约"
  休息中   ──→ 不展示"立即预约"
  已下班   ──→ 不展示"立即预约"
  请假中   ──→ 不展示"立即预约" + 顶部提示条
```

> **待确认项**：status=3（离职审批中）是否公开？当前按"不公开"处理，避免法律风险。

---

## 4. 缓存策略

### 4.1 列表页缓存

| 维度 | 策略 |
|------|------|
| 缓存层 | Redis |
| Key | `coaches:list:page:{page}:size:{size}` |
| TTL | 60s（教练状态变化频繁，TTL 不宜过长） |
| 失效 | `coach.status` 或 `coach.real_time_status` 变更时通过 MQ 广播失效 |
| 穿透 | 空列表也缓存 30s，避免 DB 空查 |

### 4.2 详情页缓存

| 维度 | 策略 |
|------|------|
| 缓存层 | Redis |
| Key | `coach:detail:{id}` |
| TTL | 300s（详情页相对稳定） |
| 失效 | `coach` / `coach_certificate` / `coach_review` / `coach_availability` 任意变更时失效 |

### 4.3 前端缓存

- 微信小程序列表页使用 `Taro.setStorageSync('coaches_list', ...)` 缓存最近 1 页数据
- 首次进入先展示缓存 → 后台静默刷新 → 更新 UI

---

## 5. 性能指标

| 指标 | 目标 | 验证方式 |
|------|------|---------|
| 列表页首屏 | P50 < 300ms，P99 < 800ms | test-plan.md Task 3 + 压测 |
| 详情页首屏 | P50 < 200ms，P99 < 500ms | test-plan.md Task 4 + 压测 |
| 列表页 DB 查询 | < 50ms（含排序） | SQL explain + 慢查询监控 |
| 并发 | 1000 QPS 下 P99 < 1s | P1 压测 |

---

## 6. 安全 / 鉴权

- 两个接口均**无需登录**（游客可访问）
- 防刷：同一 IP 1 分钟内请求 > 200 次，触发限流 429
- 防止 ID 遍历：`coach` 主键使用自增 ID，但返回的错误统一为 404（不暴露是否存在）

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-010 | 被依赖 | 教练提交入驻资料，产生 coach 数据 |
| US-011 | 被依赖 | 管理员审核，coach.status 从 0 变为 1 |
| US-012 | 被依赖 | 教练管理主页，填充证书/任教年限等 |
| US-013 | 被依赖 | 教练管理实时状态，更新 real_time_status |
| US-017 | 依赖本 US | 从教练详情页跳转购买体验课 |
| US-018 | 依赖本 US | 从教练详情页跳转购买标准套餐 |
| US-029 | 依赖本 US | "立即预约"按钮跳转预约页 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 无在职教练 | 列表返回 `items: []` + 200，前端展示空状态 |
| coach_id 不存在 | 404 + `COACH_NOT_FOUND` |
| coach.status = 0/4 | 与不存在等效处理，404 + `COACH_NOT_FOUND` |
| Redis 宕机 | 降级直接查 DB，记录告警 |
| DB 慢查询 | 超过 100ms 触发慢查询告警 |

---

## 9. 实现顺序（与 test-plan.md 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 + 索引 | Task 1, Task 2（Repository） |
| §2.1 GET /coaches | Task 3（列表 API） |
| §2.2 GET /coaches/:id | Task 4（详情 API） |
| §4 缓存策略 | Task 5（前端缓存） + 后端缓存后续补充 |
| §5 性能指标 | Task 1-5 的验收指标 |

---

## 10. 上下游引用

- **用户故事**：[./user-story.md](./user-story.md)（业务需求 + GWT + Figma 章节）
- **测试计划**：[./test-plan.md](./test-plan.md)（TDD 任务清单）
- **全局设计规范**：[../../spec/figma/README.md](../../spec/figma/README.md)
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-07-30 | Dev | 初版占位 |
| v1.0 | 2026-07-30 | Dev | 完整填写：数据模型 / API / 状态机 / 缓存 / 性能 / 安全 / 跨 US 依赖 |
