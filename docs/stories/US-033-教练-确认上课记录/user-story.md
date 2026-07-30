# US-033 教练确认上课记录

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-033 |
| **标题** | 教练确认上课记录 |
| **角色（Actor）** | 教练（主）、管理员（辅）|
| **业务价值（Why）** | 让教练在课程结束后正式确认并扣除课时，同时支持管理员在特殊情况下返还课时，保证课时消耗准确可审计 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

---

## 2. 触发条件

- **触发方**：教练 / 管理员
- **触发动作**：教练点击「确认上课并扣除课时」；管理员在后台点击「返还课时」
- **触发时机**：课程结束后 24 小时内（教练确认）；任意已扣课时状态（管理员返还）

---

## 3. 前置条件

- [x] 教练已登录且为对应 booking 的教练（依赖 US-011 / US-012）
- [x] 存在 booking.status ∈ {待上课, 上课中}（依赖 US-029）
- [x] 课程开始时间已过
- [x] 管理员返还：管理员已登录，booking.status = 已完成 或 旷课，且 package.consumed_count > 0

---

## 4. 业务流程

### 4.1 主路径

1. 教练在教练端预约卡片中打开已结束课程
2. 系统校验课程已开始且未确认
3. 教练填写上课记录：课程内容、重点标签、掌握程度、课后作业、照片/视频
4. 教练点击「确认上课并扣除课时」
5. 系统校验通过，事务内执行：
   - booking.status → 已完成
   - package.reserved_count -1
   - package.consumed_count +1
   - 创建/更新 course_record
   - 记录 audit_log
6. 系统通知学员「课程已完成，课时已扣除」
7. 学员端上课记录页展示教练记录

### 4.2 异常分支

- **分支 1**：booking 已取消 / 已完成 / 旷课 → 返回 `BOOKING_NOT_CONFIRMable`
- **分支 2**：课程尚未开始 → 返回 `CLASS_NOT_STARTED`
- **分支 3**：非本课程教练 → 返回 403
- **分支 4**：管理员返还时 consumed_count = 0 → 返回 `NO_CONSUMED_HOUR`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 课程结束后教练手动点击确认并扣除课时 | [§5.3.2](../../prd/prd.md) |
| 2 | 教练需在课程结束后 24 小时内完成确认 | [§5.4.3](../../prd/prd.md) |
| 3 | 教练确认后 package.consumed_count +1 | [§6.3.1](../../prd/prd.md) |
| 4 | 管理员可在特殊情况下手动返还课时 | [§5.3.2](../../prd/prd.md) |
| 5 | 预约状态机 | [§6.1](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：教练确认上课并扣除课时

```gherkin
Given 教练已登录
And   存在 booking.status = 上课中，开课时间为今日 10:00，结束时间为 11:00
And   当前时间为今日 11:05
And   对应 package.reserved_count = 1，consumed_count = 2，available_count = 7
When  教练填写课程内容"自由泳打腿"并点击「确认上课并扣除课时」
Then  booking.status = 已完成
And   package.reserved_count = 0，consumed_count = 3，available_count = 7
And   course_record 创建成功，content = "自由泳打腿"
And   学员收到课时扣除通知
And   HTTP 状态码 = 200
```

### 6.2 场景 2：管理员返还课时

```gherkin
Given 管理员已登录
And   存在 booking.status = 已完成
And   对应 package.consumed_count = 3，available_count = 7
When  管理员点击「返还课时」并填写原因"教练误操作"
Then  package.consumed_count = 2，available_count = 8
And   hour_return 记录创建，reason = "教练误操作"
And   audit_log 记录管理员返还操作
And   学员收到课时返还通知
And   HTTP 状态码 = 200
```

### 6.3 场景 3：课程尚未开始

```gherkin
Given 教练已登录
And   存在 booking.status = 已预约，开课时间为今日 14:00
And   当前时间为今日 12:00
When  教练点击「确认上课并扣除课时」
Then  系统返回 HTTP 400，错误码 CLASS_NOT_STARTED
And   package 课时不变
And   不创建 course_record
```

### 6.4 场景 4：booking 已取消

```gherkin
Given 教练已登录
And   存在 booking.status = 已取消
When  教练点击「确认上课并扣除课时」
Then  系统返回 HTTP 400，错误码 BOOKING_NOT_CONFIRMABLE
And   package 课时不变
```

### 6.5 场景 5：非本课程教练操作

```gherkin
Given 教练 B 已登录
And   存在 booking.coach_id = 100
And   教练 B 的 coach_id = 200
When  教练 B 对该 booking 点击「确认上课并扣除课时」
Then  系统返回 HTTP 403
And   booking 与 package 均无变化
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `booking` | 修改 | 教练确认后 status → 已完成 |
| 2 | `package` | 修改 | 确认时 reserved-1 consumed+1；返还时 consumed-1 available+1 |
| 3 | `course_record` | 新增/修改 | 教练填写上课记录 |
| 4 | `hour_return` | 新增 | 管理员返还课时记录 |
| 5 | `audit_log` | 新增 | 记录确认/返还操作 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/coach/bookings/{booking_id}/confirm` | POST | 新增 | 教练确认上课并扣课时 |
| 2 | `/api/admin/bookings/{booking_id}/return-hour` | POST | 新增 | 管理员返还课时 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `booking` | 待上课/上课中 → 已完成 | 教练确认上课 | 课时正式消耗 |
| 2 | `package` | reserved → consumed | 教练确认 | reserved-1, consumed+1 |
| 3 | `package` | consumed → available | 管理员返还 | consumed-1, available+1 |
| 4 | `package` | active → exhausted | 确认最后一课时 | 若 consumed = total_hours |

---

## 8. 边界场景

### 8.1 边界场景 1：确认最后一节课时触发套餐耗尽

- **触发条件**：教练确认后 package.consumed_count = total_hours
- **预期行为**：应用层事件将 package.status active → exhausted
- **用户可见反馈**：学员端提示"本套餐课时已用完"

### 8.2 边界场景 2：教练确认与管理员返还并发

- **触发条件**：教练确认上课和管理员返还课时同时发生
- **预期行为**：通过 package 乐观锁保证数据一致性
- **用户可见反馈**：以最终状态为准提示用户

### 8.3 边界场景 3：超时未确认

- **触发条件**：课程结束后 24 小时教练仍未确认
- **预期行为**：由系统定时任务自动扣减课时
- **用户可见反馈**：教练端收到超时提醒

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）
- [x] US-005（用户补充注册资料）
- [x] US-011（管理员审核教练入驻资质）
- [x] US-012（教练管理个人主页与参考单价）
- [x] US-029（学员预约正价课程）
- [x] US-032（学员签到/签退课程）

### 9.2 后续 US（依赖本故事）

- [ ] US-034（管理员查看上课记录）

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 依赖 US-029/US-032，但确认扣课时逻辑可独立交付
- [x] **N**egotiable（可协商）- 记录字段、返还原因分类可协商
- [x] **V**aluable（有价值）- 核心课时消耗闭环
- [x] **E**stimable（可估算）- 1 人天明确
- [x] **S**mall（足够小）- 仅覆盖确认扣课时与返还
- [x] **T**estable（可测试）- 5 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符
- [x] 错误码明确

### 11.2 业务规则

- [x] 引用 §5.3.2 / §5.4.3 / §6.3.1 / §6.1
- [x] 课时扣减与返还规则一致

### 11.3 验收标准

- [x] 2 正常 + 3 异常 GWT
- [x] 每个 Then 含具体数值/状态码/DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] 链接明确

---

## 12. 备注

- **幂等键**：`idempotency_key = {coach_id}:{booking_id}:confirm`
- **事务边界**：booking 状态更新 + package 扣减 + course_record 创建在同一事务
- **性能要求**：确认接口 P99 < 300ms，返还接口 P99 < 300ms
- **24h 窗口**：教练端在课程结束后 24h 内允许确认，超时由系统定时任务处理

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/spec/figma/README.md](../../spec/figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 教练端课程确认页 Figma file URL | 🔲 待设计填写 | 🔲 |
| 2 | 上课记录填写弹窗 Figma file URL | 🔲 待设计填写 | 🔲 |
| 3 | 管理端返还课时页 Figma file URL | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **教练端课程确认页** | — | ✅ | ✅ | ✅ | 未开始课程禁用确认 |
| **上课记录填写弹窗** | — | ✅ | ✅ | ✅ | |
| **管理端返还课时页** | — | ✅ | ✅ | ✅ | |

---

## 14. 页面级设计决策

### 14.1 确认按钮状态

- 课程未开始：禁用，文案"课程未开始"
- 课程进行中/已结束：启用，文案"确认上课并扣除课时"
- 已确认：隐藏按钮

### 14.2 上课记录字段

- 课程内容：多行文本，最多 500 字
- 今日重点：标签多选（如打腿、换气、划手、转身）
- 掌握程度：单选
- 课后作业：多行文本
- 媒体：最多 5 张图片 / 1 个视频

### 14.3 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 点击确认 | 课程结束后 | 二次确认弹窗 → 成功 Toast | 扣课时 |
| 上传媒体 | 点击上传 | 缩略图 + 可删除 | 教练端 |
| 返还课时 | 管理员点击 | 原因弹窗 → 成功提示 | 审计留痕 |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 🔲 | 🔲 | 🔲 | 🔲 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
