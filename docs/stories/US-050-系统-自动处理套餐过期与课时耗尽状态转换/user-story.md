# US-050 系统自动处理套餐过期与课时耗尽状态转换

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-050 |
| **标题** | 系统自动处理套餐过期与课时耗尽状态转换 |
| **角色（Actor）** | 系统（主） |
| **业务价值（Why）** | 保证套餐状态与课时/有效期强一致，避免学员误用已过期或已耗尽套餐进行预约 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上）|

---

## 2. 触发条件

> 什么事件/操作触发该故事

- **触发方**：系统
- **触发动作**：
  1. 定时任务每小时巡检套餐 `expire_at`（扫描范围：`status='active'` 与 `status='exhausted'`）；
  2. 应用层事件：教练确认上课后 `available` 与 `reserved` 计数变化；
  3. 应用层事件：学员取消/改约后计数恢复。
- **触发时机**：到达有效期、课时计数归零、或计数恢复时

---

## 3. 前置条件

- [x] 套餐表 `package` 已存在，含字段：`status`、`total_hours`、`available`、`reserved`、`consumed`、`expire_at`
- [x] 套餐状态机已定义：`active` / `exhausted` / `expired` / `refunded` / `frozen`
- [x] 定时任务调度器或消息队列已接入
- [x] 教练确认上课、学员取消预约等事件可触发状态重算

---

## 4. 业务流程

### 4.1 主路径

1. 系统定时任务每小时扫描 `package` 表中 `status='active'` 且 `expire_at <= NOW()` 的记录
2. 对每条命中记录，原子更新 `status='expired'`
3. 教练确认上课后，系统扣减 `available`、增加 `consumed`
4. 若更新后 `available=0` 且 `reserved=0`，原子更新 `status='exhausted'`
5. 学员取消预约后，系统恢复 `available`、扣减 `reserved`
6. 恢复后仅重新校验状态，不将 `exhausted/expired/refunded/frozen` 回退为 `active`
7. **exhausted → expired 转换**：定时任务同时扫描 `status='exhausted'` 且 `expire_at <= NOW()` 的记录，原子更新 `status='expired'`（套餐已耗尽但仍可能到达有效期边界，此为前进转换，非回退）

### 4.2 异常分支

- **分支 1**：过期套餐上仍有 `reserved` 课时 → 仍标记为 `expired`，保留计数，已预约课程仍有效
- **分支 2**：并发巡检或事件重算同时命中同一套餐 → 通过唯一任务锁/乐观锁保证仅转换一次
- **分支 3**：取消预约使 `available` 从 0 恢复为大于 0 → 不触发 `active` 回退；状态保持原终态

---

## 5. 业务规则引用

> 引用 v11 PRD 章节（[`../../prd/prd.md`](../../prd/prd.md)）

| # | 规则 | 章节 |
|---|------|------|
| 1 | 套餐状态定义：`active` / `exhausted` / `expired` / `refunded` | [§3.4.1](../../prd/prd.md) |
| 2 | `active → exhausted` 触发条件：`available=0` 且 `reserved=0` | [§3.4.1](../../prd/prd.md) |
| 3 | `active → expired` 触发条件：`now() >= expire_at`，每小时巡检 | [§3.4.1](../../prd/prd.md) |
| 4 | 状态-计数不变量：active 必有 `available + reserved > 0`；exhausted 必有 `available=0` 且 `reserved=0` | [§3.4.4](../../prd/prd.md) |
| 5 | 课时预占与消耗规则：reserved 增加只能来自用户预约；consumed 增加只能来自教练确认 | [§6.3.1](../../prd/prd.md) |
| 6 | 套餐状态机完整转换 | [§4.2](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

> 本 US 为 L2（1 人天），场景数 = 2 正常 + 4 异常 = 6。

### 6.1 场景 1：定时任务将到期套餐标记为 expired

```gherkin
Given 系统中存在套餐 A，status='active'，total_hours=10，available=3，reserved=2，consumed=5，expire_at='2026-07-29 23:59:59'
And   当前系统时间为 2026-07-30 01:00:00
When  系统每小时套餐过期巡检任务执行
Then  套餐 A 的 status 更新为 'expired'
And   available 保持为 3，reserved 保持为 2，consumed 保持为 5
And   available + reserved + consumed = total_hours
And   系统记录状态转换日志：from='active', to='expired', reason='EXPIRE_CRON'
```

### 6.2 场景 2：教练确认最后一节课后套餐变为 exhausted

```gherkin
Given 学员持有套餐 B，status='active'，total_hours=10，available=1，reserved=0，consumed=9
And   该学员已预约明日 09:00 的 1 节课（此时 reserved=1，available=0）
When  教练确认该节课已完成
Then  系统扣减 reserved 至 0，增加 consumed 至 10
And   套餐 B 的 status 自动更新为 'exhausted'
And   系统记录状态转换日志：from='active', to='exhausted', reason='HOURS_EXHAUSTED'
```

### 6.3 场景 3：过期套餐上仍有 reserved 课时仍正确标记为 expired

```gherkin
Given 套餐 C 的 expire_at='2026-07-29 23:59:59'，status='active'
And   套餐 C 当前 available=2，reserved=1，consumed=7
When  系统过期巡检任务执行
Then  套餐 C 的 status 更新为 'expired'
And   available 保持为 2，reserved 保持为 1，consumed 保持为 7
And   学员端「我的套餐」显示该套餐已过期，但已预约的 1 节课仍可正常上课
```

### 6.4 场景 4：并发巡检保证同一套餐仅转换一次

```gherkin
Given 套餐 D 的 expire_at='2026-07-29 23:59:59'，status='active'
When  两个定时任务实例在相同时刻扫描到套餐 D 并尝试更新
Then  仅有一个实例成功将 status 更新为 'expired'
And   另一个实例收到 0 行更新或锁冲突，不重复写入状态转换日志
And   套餐 D 的状态转换日志表中仅有 1 条 from='active', to='expired' 记录
```

### 6.5 场景 5：取消预约恢复 available 后不应误将 exhausted 回退为 active

```gherkin
Given 套餐 E 的 status='exhausted'，total_hours=10，available=0，reserved=0，consumed=10
When  学员尝试取消一个已确认上课的历史记录（系统不允许取消已确认课程）
Then  系统拒绝该取消操作，返回错误码 COURSE_ALREADY_CONFIRMED
And   套餐 E 的 status 保持 'exhausted'
And   available / reserved / consumed 保持不变
```

### 6.6 场景 6：已耗尽套餐到达有效期边界转为 expired

```gherkin
Given 套餐 F 的 status='exhausted'，expire_at='2026-07-29 23:59:59'
And   available=0，reserved=0，consumed=10
And   当前系统时间为 2026-07-30 01:00:00
When  系统每小时套餐过期巡检任务执行（扫描 active 与 exhausted）
Then  套餐 F 的 status 从 'exhausted' 更新为 'expired'
And   available / reserved / consumed 保持不变
And   系统记录状态转换日志：from='exhausted', to='expired', reason='EXPIRE_CRON'
```

---

## 7. 数据/API/状态机影响

> 列出本 US 涉及的新增/修改的表、API、状态机转换。详细字段级设计见配套**技术设计文档**。

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package` | 修改 | 新增/更新 `status`、`available`、`reserved`、`consumed`、`expire_at` |
| 2 | `package_status_log` | 新增 | 记录每次状态转换：from、to、reason、created_at |
| 3 | `course_record` | 读取 | 教练确认上课时触发套餐计数扣减（表名与 US-032/US-033 统一） |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | 内部定时任务 | N/A | 新增 | `ExpirePackageCronJob` 每小时执行 |
| 2 | 内部事件处理器 | N/A | 新增 | `PackageStatusReconciler` 监听上课确认/取消事件 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `package.status` | active → expired | 定时任务检测到 `now() >= expire_at` | 终态，不自动回退 |
| 2 | `package.status` | active → exhausted | 事件触发后 `available=0` 且 `reserved=0` | 中间态，仍可能 expired |
| 3 | `package.status` | exhausted → expired | 定时任务检测到 `now() >= expire_at`（扫描 exhausted） | 前进转换，非回退；终态 |

---

## 8. 边界场景

> 至少 3 个异常边界场景（如并发、权限越界、数据缺失、超时、空数据、极值等）

### 8.1 边界场景 1：同一分钟触发过期巡检与上课确认事件

- **触发条件**：套餐刚好在巡检时刻被教练确认最后一节课
- **预期行为**：数据库行锁/事务保证状态转换串行；先完成的事件决定状态，后者基于最新状态重算
- **用户可见反馈**：无直接反馈；系统日志记录连续两次状态转换或跳过无效转换

### 8.2 边界场景 2：定时任务扫描百万级数据

- **触发条件**：生产环境 `package` 表数据量巨大
- **预期行为**：分页/游标扫描 + 批量更新，单次任务执行时间 < 5 分钟
- **用户可见反馈**：无；监控告警任务耗时

### 8.3 边界场景 3：已退款套餐被异常恢复计数

- **触发条件**：退款流程与取消预约事件时序错乱，取消事件命中 `status='refunded'` 的套餐
- **预期行为**：事件处理器校验当前状态，非 `active` 套餐不执行计数恢复与状态重算
- **用户可见反馈**：学员端保持「已退款」状态，不显示可用课时

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-020（学员购买正价套餐）
- [x] US-029（学员预约正价课程）
- [x] US-032（学员签到/签退课程）
- [x] US-033（教练确认上课记录）

### 9.2 后续 US（依赖本故事）

- [ ] US-021（学员查看我的套餐）— 依赖状态准确展示
- [ ] US-030（学员取消/改约正价课程）— 触发计数恢复

---

## 10. INVEST 自检

> 拆分完成后逐项勾选，必须 5/6 通过

- [x] **I**ndependent（独立）- 仅依赖 package 表与事件机制，可独立交付
- [x] **N**egotiable（可协商）- 巡检频率、日志保留时长可协商
- [x] **V**aluable（有价值）- 防止学员使用无效套餐预约，减少客服纠纷
- [x] **E**stimable（可估算）- 1 人天明确
- [x] **S**mall（足够小）- 一个 Sprint 内可完成
- [x] **T**estable（可测试）- 6 个 GWT 场景可验证

---

## 11. 完整性检查

> 合并前必查，逐项勾选

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除 Figma 链接待设计填写）
- [x] 错误码明确（COURSE_ALREADY_CONFIRMED）

### 11.2 业务规则

- [x] 引用 §3.4.1 / §3.4.4 / §6.3.1 / §4.2
- [x] 与套餐状态机一致
- [x] 与数据模型一致

### 11.3 验收标准

- [x] 2 正常 + 4 异常 GWT
- [x] 业务级 Gherkin，不绑死实现
- [x] 用户可观察的结果可被验证

### 11.4 配套文档

- [x] Figma 链接/状态明确
- [x] 技术设计文档链接/状态明确
- [x] 测试计划链接/状态明确

---

## 12. 备注

- **幂等键**：定时任务按 `package.id` + 日期分片去重；状态转换日志使用唯一索引 `(package_id, from_status, to_status, reason, created_at)` 防止重复写入
- **事务边界**：状态读取 → 条件判断 → 状态更新必须在同一数据库事务内完成
- **性能要求**：单次过期巡检 P99 < 5 分钟；事件触发状态重算 P99 < 100ms
- **补偿机制**：状态转换失败时进入死信队列，人工介入排查

---

## 13. Figma 链接

> 本 US 为系统后台自动任务，无独立页面。状态变化体现在「我的套餐」列表（US-021）。Figma **设计系统规范**（token / 组件 / 状态徽标 / 4 态模板 / 文案）见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 「我的套餐」状态标签 Figma file URL | 🔲 待设计填写 | 🔲 |
| 2 | 「我的套餐」已过期/已耗尽状态截图 | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **我的套餐列表** | — | — | — | — | 状态标签变化见 US-021 |

---

## 14. 页面级设计决策

> 本 US 无独立页面，仅涉及状态标签文案。

### 14.1 状态标签文案

- `active`：显示「可使用」
- `exhausted`：显示「课时已用完」
- `expired`：显示「已过期」

### 14.2 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 状态自动变更 | 系统定时任务/事件 | 学员下次进入「我的套餐」看到最新状态 | 无即时推送要求 |

---

## 15. 设计评审记录

> 记录设计稿评审过程中 PM / 开发 / QA 给出的反馈与处置。

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 🔲 | 🔲 | 🔲 | 🔲 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.1 | 2026-07-31 | PM | P1 修复：§2/§4.1/§7.3 补充 `exhausted → expired` 状态转换（定时任务扫描 exhausted）；新增场景 6 验证该转换 |
| v1.2 | 2026-07-31 | PM | P1/P2 修复：§6.1 场景 1 改为部分消耗后过期断言（available + reserved + consumed = total_hours），避免 `consumed_count = 0` 误用 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a>　·　填写示例见 <a href="../../spec/user-story/EXAMPLE.md">EXAMPLE.md</a></sub>
</p>
