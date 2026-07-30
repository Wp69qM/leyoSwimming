# Tasks: US-028 管理员处理退款并原路退回

> 本文档对应 `docs/stories/US-028-.../test-plan.md` 的 OpenSpec 映射版本。
> 每个 Task 严格遵循 RED → GREEN → REFACTOR → COMMIT 循环。

## Task 1: Refund Admin Service — 审批/驳回与渠道退款 [P0]

**Files:**
- Create: `backend/src/services/refund-admin.ts`
- Test: `backend/tests/services/refund-admin.test.ts`

**Spec coverage:** REQ-028-1 / REQ-028-2 / REQ-028-3 全部场景

- [ ] **RED:** Write failing tests — approve updates order/package and creates transaction; reject restores order/package; channel failure keeps state; idempotent approval; non-admin forbidden
- [ ] **GREEN:** Implement `RefundAdminService.approve()` and `RefundAdminService.reject()`
- [ ] **REFACTOR:** Extract channel refund adapter and amount validator
- [ ] **COMMIT:** `feat(refund): add admin refund approval service`

## Task 2: Admin Refund List/Detail API [P0]

**Files:**
- Create: `backend/src/controllers/refund-admin.ts`, `backend/src/routes/refund-admin.ts`
- Test: `backend/tests/controllers/refund-admin.test.ts`

- [ ] **RED:** Write failing tests — 200 list with pagination; 200 detail; 403 non-admin; 404 not found
- [ ] **GREEN:** Implement controller + route
- [ ] **REFACTOR:** Share pagination and admin auth middleware
- [ ] **COMMIT:** `feat(api): add admin refund list and detail endpoints`

## Task 3: Admin Approve/Reject API [P0]

**Files:**
- Modify: controller/route
- Test: `backend/tests/controllers/refund-admin.test.ts`

- [ ] **RED:** Write failing tests — 200 approve/reject; 400 duplicate; 400 invalid amount; 403 forbidden
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Extract admin action validation
- [ ] **COMMIT:** `feat(api): add admin refund approve and reject endpoints`

## Task 4: Channel Refund Adapter & Retry Job [P1]

**Files:**
- Create: `backend/src/adapters/refund-channel.ts`, `backend/src/jobs/retry-failed-refunds.ts`
- Test: 对应测试文件

- [ ] **RED:** Write failing tests — WeChat/Alipay refund request; retry failed transactions
- [ ] **GREEN:** Implement adapter and cron retry job
- [ ] **REFACTOR:** Abstract channel interface
- [ ] **COMMIT:** `feat(refund): add channel refund adapter and retry job`

## Task 5: Web Admin Refund Pages [P1]

**Files:**
- Create: `web-admin/src/pages/refunds/index.tsx`, `web-admin/src/pages/refunds/detail.tsx`
- Test: 对应测试文件

- [ ] **RED:** Write failing tests — renders list; detail shows actions; handles errors
- [ ] **GREEN:** Implement pages
- [ ] **REFACTOR:** Extract `<RefundStatusBadge />`
- [ ] **COMMIT:** `feat(admin): add refund processing pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-3），P1 选做（Task 4-5）
