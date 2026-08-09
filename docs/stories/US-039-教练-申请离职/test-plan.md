# US-039 教练申请离职测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 40% | Jest | 状态机转换、工单字段校验、幂等键生成 |
| 集成测试 | 45% | Jest + Supertest | 申请、登记 action、提交 API 端到端 |
| E2E 测试 | 10% | 微信小程序自动化 | 教练端离职入口与工单页交互 |
| 性能测试 | 5% | k6 | 提交接口并发与幂等 |

---

## 2. TDD 任务清单

### Task 1：数据模型与状态机

- [ ] **1.1 RED**：编写 `coach_resignation_ticket` / `coach_resignation_action` 模型缺失测试（字段、索引、外键）
- [ ] **1.2 GREEN**：创建 Knex migration 与模型
- [ ] **1.3 RED**：编写 coach 状态机 `1 → 4` 单元测试
- [ ] **1.4 GREEN**：实现状态转换校验函数
- [ ] **1.5 REFACTOR**：抽取 resignation state machine 公共模块
- [ ] **1.6 COMMIT**：`feat(us-039): add resignation ticket tables and state machine`

**对应**：user-story.md 场景 1、3、4 / tech-design §3、§5

### Task 2：提交离职申请接口

- [ ] **2.1 RED**：编写 `POST /api/coach/resignation/apply` 成功生成工单测试
- [ ] **2.2 RED**：编写 status ≠ 1 时返回 403 的测试
- [ ] **2.3 RED**：编写重复提交返回 409 的测试
- [ ] **2.4 GREEN**：实现申请接口、幂等键去重、工单创建
- [ ] **2.5 REFACTOR**：封装 resignation apply service
- [ ] **2.6 COMMIT**：`feat(us-039): coach resignation apply endpoint`

**对应**：user-story.md 场景 1、3、4 / tech-design §4.1

### Task 3：工单详情与套餐清单

- [ ] **3.1 RED**：编写 `POST /api/coach/resignation/detail` 返回工单 + active 套餐清单测试
- [ ] **3.2 RED**：编写无 active 套餐时返回空清单测试
- [ ] **3.3 GREEN**：实现工单查询与 package 聚合
- [ ] **3.4 REFACTOR**：统一套餐清单 DTO
- [ ] **3.5 COMMIT**：`feat(us-039): resignation ticket detail and package list`

**对应**：user-story.md 场景 1、边界场景 2 / tech-design §4.2

### Task 4：登记套餐处理结果

- [ ] **4.1 RED**：编写确认全额退款并自动生成 `refund_record` 的测试
- [ ] **4.2 RED**：编写 `refund_record` 金额为「单价 × 剩余课时」的测试
- [ ] **4.3 RED**：编写确认非自己名下套餐返回 403 的测试
- [ ] **4.4 RED**：编写工单非 processing 状态时确认失败的测试
- [ ] **4.5 RED**：编写提交时未确认套餐自动按 refund 生成 `refund_record` 的测试
- [ ] **4.6 GREEN**：实现退款确认接口与校验，同步生成退款记录
- [ ] **4.7 REFACTOR**：抽取 refund confirmation validator
- [ ] **4.8 COMMIT**：`feat(us-039): register resignation refund confirmation with refund record`

**对应**：user-story.md 场景 2、5 / tech-design §4.3

### Task 5：提交工单至管理员审批队列

- [ ] **5.1 RED**：编写教练提交工单至 `pending_audit` 的测试
- [ ] **5.2 RED**：编写提交时未确认套餐自动按 refund 生成 `refund_record` 的测试
- [ ] **5.3 RED**：编写非 processing 状态不可提交的测试
- [ ] **5.4 GREEN**：实现 submit 接口；submit 时未确认套餐默认生成退款记录
- [ ] **5.5 COMMIT**：`feat(us-039): submit resignation ticket to admin queue with default refund`

**对应**：user-story.md 场景 1 / tech-design §4.4

### Task 6：离职处理中页

- [ ] **6.1 RED**：编写提交成功后跳转 C-离职处理中页的 E2E 测试
- [ ] **6.2 RED**：编写 coach.status=4 时「我的」页面展示「查看离职申请」入口的 E2E 测试
- [ ] **6.3 RED**：编写教练主动点击「查看离职申请」进入 C-离职处理中页的 E2E 测试
- [ ] **6.4 RED**：编写 C-离职处理中页展示工单号、进度、客服入口的测试
- [ ] **6.5 GREEN**：实现离职处理中页数据查询与前端入口
- [ ] **6.6 COMMIT**：`feat(us-039): coach resignation processing page`

**对应**：user-story.md 场景 6、7

### Task 7：审计与缓存失效

- [ ] **7.1 RED**：编写 coach_audit_log 写入测试（status 变更、action 登记）
- [ ] **7.2 RED**：编写 coach.status 缓存失效测试
- [ ] **7.3 GREEN**：实现审计日志与缓存失效
- [ ] **7.4 COMMIT**：`feat(us-039): audit log and cache invalidation`

**对应**：tech-design §6、§8

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 教练成功提交离职申请 | 6.1 | `test_apply_resignation_success` | coach.status=4，生成 processing 工单，清单含 3 份套餐 |
| 2 | 教练确认学员套餐处理结果 | 6.2 | `test_register_refund_confirmation_success` | coach_resignation_action +1，action=refund/transfer/continue |
| 3 | 非已通过教练无法申请 | 6.3 | `test_not_approved_cannot_apply` | 入口隐藏或接口 403 |
| 4 | 重复提交离职申请 | 6.4 | `test_apply_resignation_duplicate` | HTTP 409，不生成新工单 |
| 5 | 登记非自己名下套餐 | 6.5 | `test_register_action_not_own_package` | HTTP 403，错误码 NOT_OWN_PACKAGE |
| 6 | 提交后跳转离职处理中页 | 6.6 | `test_resignation_processing_page_after_apply` | 展示标题、工单号、进度、客服入口 |
| 7 | status=4 时主动查看离职申请 | 6.7 | `test_view_resignation_processing_page` | 「我的」页面显示入口，点击进入处理中页，不拦截正常流程 |
| 8 | 教练名下无 active 套餐 | 8.2 | `test_apply_resignation_no_active_packages` | 允许提交，工单清单为空 |
| 9 | 并发提交幂等 | 8.3 | `test_apply_resignation_idempotent` | 仅 1 条工单 |
| 10 | 未登记套餐默认全额退款 | 6.2 / §12 | `test_submit_default_refund_record` | 自动生成 refund_record，金额 = 单价 × 剩余课时 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 状态机转换 100% 覆盖（1→4、processing→pending_audit）
- [ ] 离职处理中页入口与展示覆盖（提交后跳转、「我的」页面入口、主动点击查看）
- [ ] 无 TBD/TODO 遗留

---

## 5. 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-30 | 初版 |
| v1.1 | 2026-08-05 | 补充 C-离职处理中页任务与测试用例；移除 MVP 不支持的"撤销离职申请"相关任务与验收项，与 user-story.md §8.1 保持一致 |
