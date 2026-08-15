# Tasks: US-027 学员申请退款

> 本文档对应 `docs/stories/US-027-学员-申请退款/test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Refund Eligibility Service [P0]

**Files:**
- Create: `backend/src/services/refund-eligibility.ts`
- Test: `backend/tests/services/refund-eligibility.test.ts`

**Spec coverage:** REQ-027-1 全部场景（资格检查 + 金额计算 + 套餐状态校验）

- [ ] **RED:** Write failing tests —
  - active package 返回 eligible + amount = paid × (total-consumed)/total
  - expired 套餐有剩余课时返回 eligible=true
  - frozen(coach_resigned) 返回 amount = paidAmount（100% 全额）
  - PACKAGE_EXHAUSTED_NOT_REFUNDABLE 拒绝（exhausted 不可退款）
  - PACKAGE_ALREADY_REFUNDED 拒绝
  - PACKAGE_FROZEN 拒绝（非 coach_resigned）
  - REFUND_NOT_SUPPORTED 拒绝（package 快照 refundEnabled=false）
  - REFUND_EXPIRED 拒绝（超出 refundValidDays）
  - REFUND_IN_PROGRESS 拒绝
- [ ] **GREEN:** Implement `RefundEligibilityService.check(packageId, userId)` 返回 `{ eligible, refundAmount, calculation, reasonCodes }`
- [ ] **REFACTOR:** 提取 `calculateRefundAmount(paidAmount, totalHours, consumedCount)` 为纯函数
- [ ] **COMMIT:** `feat(refund): add refund eligibility service`

## Task 2: Refund Submission Service [P0]

**Files:**
- Create: `backend/src/services/refund-submission.ts`
- Test: `backend/tests/services/refund-submission.test.ts`

**Spec coverage:** REQ-027-2 全部场景（事务创建 refund_record + order/package 状态更新 + 通知 + 幂等）

- [ ] **RED:** Write failing tests —
  - 正常提交：创建 refund_record(status=0) + order→退款审批中 + package.status → frozen(refund_pending) + reserved_count → 0 + 已预约 booking → 已取消 + 触发 US-024 候补转正
  - frozen(coach_resigned) 提交：refundAmount = paidAmount（100% 全额），package.status → frozen(refund_pending)，保留 frozenReason 历史值为 coach_resigned
  - 重复提交返回 REFUND_IN_PROGRESS，不创建新记录
  - PACKAGE_ALREADY_REFUNDED 拒绝
  - PACKAGE_FROZEN 拒绝（非 coach_resigned）
  - REFUND_NOT_SUPPORTED 拒绝（package 快照 refundEnabled=false）
  - REFUND_EXPIRED 拒绝
  - PACKAGE_EXHAUSTED_NOT_REFUNDABLE 拒绝
  - 并发提交幂等（同一 userId + packageId 第二次返回已存在）
  - 事务回滚：模拟 package 更新失败时 refund_record 不落库
  - 通知管理员与学员被调用
- [ ] **GREEN:** Implement `RefundSubmissionService.submit(packageId, userId, reasonType, reasonDetail)` 含事务包裹与幂等锁
- [ ] **REFACTOR:** 提取 `notifyRefundSubmitted(refundId)` 通知 helper 与 `acquireIdempotentLock(userId, packageId)` 锁 helper
- [ ] **COMMIT:** `feat(refund): add refund submission service`

## Task 3: Refund APIs [P0]

**Files:**
- Create: `backend/src/controllers/refund.ts`, `backend/src/routes/refund.ts`
- Test: `backend/tests/controllers/refund.test.ts`

**Spec coverage:** 2 个 API 端点 + 鉴权 + 错误码映射

- [ ] **RED:** Write failing tests —
  - POST /api/package/refund-check 返回 200 + eligible + refundAmount + calculation + reasonCodes
  - POST /package/refund-check 套餐已退款返回 400 PACKAGE_ALREADY_REFUNDED
  - POST /package/refund-check 存在待审批返回 400 REFUND_IN_PROGRESS
  - POST /package/refund-check 未登录返回 401
  - POST /package/refund-check 非套餐所属用户返回 403
  - POST /api/package/refund 返回 201 + refundOrderId + status
  - POST /package/refund 重复提交返回 400 REFUND_IN_PROGRESS
  - POST /package/refund 套餐已退款返回 400 PACKAGE_ALREADY_REFUNDED
  - POST /package/refund 未登录返回 401
  - POST /package/refund 非套餐所属用户返回 403
  - 缓存命中：第二次 POST /api/package/refund-check 走缓存（mock 缓存层被调用）
- [ ] **GREEN:** Implement controller + route，集成 RefundEligibilityService 与 RefundSubmissionService，添加缓存读写
- [ ] **REFACTOR:** 提取共享 DTO（`RefundCheckResponse`、`RefundSubmitRequest`、`RefundSubmitResponse`）与错误码 → HTTP 状态映射中间件
- [ ] **COMMIT:** `feat(api): add refund check and submit endpoints`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "后续实现"）
- 所有 P0 Task 必做，无 P1 任务
- 测试覆盖率：核心业务逻辑 ≥ 80%，状态机转换 100%
