# Test Plan: US-027 学员申请退款

## Task 1: Refund Eligibility Service [P0]

**Files:**
- Create: `backend/src/services/refund-eligibility.ts`
- Test: `backend/tests/services/refund-eligibility.test.ts`

**Spec coverage:** 退款资格校验、金额计算、套餐状态校验

- [ ] **RED:** Return eligible + amount for active package; reject PACKAGE_ALREADY_REFUNDED; reject PACKAGE_FROZEN; reject ORDER_NOT_PAID; calculate amount = paid × (total - consumed) / total
- [ ] **GREEN:** Implement eligibility check + amount calculator
- [ ] **REFACTOR:** Extract amount calculator
- [ ] **COMMIT:** `feat(refund): add refund eligibility service`

## Task 2: Refund Submission Service [P0]

**Files:**
- Create: `backend/src/services/refund-submission.ts`
- Test: `backend/tests/services/refund-submission.test.ts`

**Spec coverage:** 创建 refund_record + order/package 状态更新 + 通知

- [ ] **RED:** Create refund_record with status=待审批; order → 退款审批中; package.status → frozen(refund_pending); 释放 reserved_count → 0; 自动取消已预约 booking; 触发 US-024 候补转正; reject REFUND_IN_PROGRESS; idempotent on duplicate submit
- [ ] **GREEN:** Implement submission with transaction
- [ ] **REFACTOR:** Extract notification helper
- [ ] **COMMIT:** `feat(refund): add refund submission service`

## Task 3: Refund APIs [P0]

**Files:**
- Create: `backend/src/controllers/refund.ts`, `backend/src/routes/refund.ts`
- Test: `backend/tests/controllers/refund.test.ts`

**Spec coverage:** 2 个 API 端点

- [ ] **RED:** GET /refund/check returns 200 with eligibility; POST /refund returns 201; 400 for each error code; 401 guest; 403 non-owner
- [ ] **GREEN:** Implement endpoints
- [ ] **REFACTOR:** Share DTO
- [ ] **COMMIT:** `feat(api): add refund endpoints`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
