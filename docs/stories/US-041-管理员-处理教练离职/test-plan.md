# US-041 管理员处理教练离职测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 30% | Jest | checklist 校验、批量更新 SQL 生成 |
| 集成测试 | 55% | Jest + Supertest | 审批队列、通过、拒绝、并发 |
| E2E 测试 | 10% | Playwright | 管理后台审批流程 |
| 性能测试 | 5% | k6 | 批量 package 处理性能 |

---

## 2. TDD 任务清单

### Task 1：审批队列与详情查询

- [ ] **1.1 RED**：编写 `POST /api/admin/coach/resignation-ticket/list` 返回 pending_audit 列表测试
- [ ] **1.2 RED**：编写 `POST /api/admin/coach/resignation-ticket/detail` 返回工单详情与 checklist 状态测试
- [ ] **1.3 GREEN**：实现列表与详情查询
- [ ] **1.4 REFACTOR**：封装 admin resignation DTO
- [ ] **1.5 COMMIT**：`feat(us-041): admin resignation ticket list and detail`

**对应**：user-story.md 场景 1-5 / tech-design §4.1、§4.2

### Task 2：Checklist 校验

- [ ] **2.1 RED**：编写 active 学员数 > 0 且未全部确认全额退款时校验失败测试
- [ ] **2.2 RED**：编写 active 学员数 = 0 时允许通过测试
- [ ] **2.3 RED**：编写未来排班未清空时校验失败测试
- [ ] **2.4 RED**：编写教练费未结算时校验失败测试（可选）
- [ ] **2.5 GREEN**：实现 checklist 校验服务
- [ ] **2.6 COMMIT**：`feat(us-041): resignation approval checklist validation`

**对应**：user-story.md 场景 3、4 / tech-design §4.3

### Task 3：通过审批与批量变更

- [ ] **3.1 RED**：编写通过审批后 coach.status=3 测试
- [ ] **3.2 RED**：编写所有 active package 自动生成 refund_record 测试（含未确认退款的套餐）
- [ ] **3.3 RED**：编写未来 booking 取消测试
- [ ] **3.4 RED**：编写 package active→frozen 测试
- [ ] **3.5 RED**：编写 schedule_slot hidden 测试
- [ ] **3.6 GREEN**：实现 approve 接口与批量更新事务
- [ ] **3.7 REFACTOR**：抽取 batch resignation processor
- [ ] **3.8 COMMIT**：`feat(us-041): approve resignation with batch updates and refund records`

**对应**：user-story.md 场景 1 / tech-design §4.3

### Task 4：拒绝审批与状态恢复

- [ ] **4.1 RED**：编写拒绝后 coach.status=1 测试
- [ ] **4.2 RED**：编写拒绝后已登记 action 保持不变测试
- [ ] **4.3 GREEN**：实现 reject 接口
- [ ] **4.4 COMMIT**：`feat(us-041): reject resignation and restore coach`

**对应**：user-story.md 场景 2 / tech-design §4.4

### Task 5：并发与幂等

- [ ] **5.1 RED**：编写并发 approve 仅一次成功测试
- [ ] **5.2 RED**：编写非 pending_audit 状态调用失败测试
- [ ] **5.3 GREEN**：实现乐观锁/唯一约束
- [ ] **5.4 COMMIT**：`feat(us-041): concurrent approval idempotency`

**对应**：user-story.md 场景 5、边界场景 1 / tech-design §8

### Task 6：审计与缓存失效

- [ ] **6.1 RED**：编写 audit_log 记录批量变更测试
- [ ] **6.2 RED**：编写相关缓存失效测试
- [ ] **6.3 GREEN**：实现审计日志与缓存失效
- [ ] **6.4 COMMIT**：`feat(us-041): audit log and cache invalidation`

**对应**：tech-design §6、§8

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 管理员通过离职审批 | 6.1 | `test_approve_resignation_success` | coach.status=3，booking/package/slot/refund_record 批量变更 |
| 2 | 管理员拒绝离职审批 | 6.2 | `test_reject_resignation_success` | coach.status=1，action 保留 |
| 3 | active 套餐存在且未全部确认全额退款 | 6.3 | `test_approve_checklist_not_passed` | HTTP 400，错误码 CHECKLIST_NOT_PASSED |
| 4 | 未来排班未清空 | 6.4 | `test_approve_schedule_not_cleared` | HTTP 400，错误码 SCHEDULE_NOT_CLEARED |
| 5 | 非 pending_audit 工单 | 6.5 | `test_approve_ticket_not_pending` | HTTP 409，错误码 TICKET_NOT_PENDING_AUDIT |
| 6 | 并发审批幂等 | 8.1 | `test_concurrent_approve_idempotent` | 仅 1 次成功 |
| 7 | 大量 package 批量处理 | 8.2 | `test_approve_bulk_packages` | 100+ package 在 1s 内处理完成 |
| 8 | 进行中课程不取消 | 8.3 | `test_in_progress_booking_not_cancelled` | 上课中 booking 保持原状 |
| 9 | 所有 active package 自动生成退款记录 | 6.1 / §12 | `test_approve_default_refund_record` | refund_record 金额 = 单价 × 剩余课时 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 批量变更事务验证通过
- [ ] 并发幂等验证通过
- [ ] 无 TBD/TODO 遗留
