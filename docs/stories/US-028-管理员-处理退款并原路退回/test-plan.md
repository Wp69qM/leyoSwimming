# Test Plan: US-028 管理员处理退款并原路退回

## Task 1: Refund Admin Service — 审批/驳回与渠道退款 [P0]

**Files:**
- Create: `backend/src/services/refund-admin.ts`
- Test: `backend/tests/services/refund-admin.test.ts`

**Spec coverage:** 全部 5 个 GWT 场景

- [ ] **RED:** Write failing tests — approve refund updates order to 退款处理中 then 已退款 on channel success, package.status active→refunded, creates transaction; reject refund sets order.status=退款被拒（7）, keeps package active and sets booking_frozen=false; channel failure keeps package active and booking_frozen=true; idempotent duplicate approval; forbid non-admin
- [ ] **GREEN:** Implement `RefundAdminService.approve(refundId, adminId, amount?, remark?)` and `RefundAdminService.reject(refundId, adminId, reason)`
- [ ] **REFACTOR:** Extract channel refund adapter and amount validator
- [ ] **COMMIT:** `feat(refund): add admin refund approval service`

## Task 2: POST /api/admin/order/list & POST /api/admin/order/detail [P0]

**Files:**
- Reuse: US-046 `backend/src/controllers/admin/order.ts`, `backend/src/routes/admin/order.ts`
- Test: `backend/tests/controllers/admin/order.test.ts`（追加退款订单场景）

- [ ] **RED:** Write failing tests — 200 list with pagination filtered by `type='refund'`; 200 detail for refund order; 403 non-admin; 404 not found
- [ ] **GREEN:** 在 US-046 order controller 中支持 `type='refund'` 过滤与详情查询
- [ ] **REFACTOR:** Share pagination and admin auth middleware
- [ ] **COMMIT:** `feat(api): reuse POST /api/admin/order/list and POST /api/admin/order/detail for refund orders`

## Task 3: POST /api/admin/order/approve-refund & POST /api/admin/order/reject-refund [P0]

**Files:**
- Modify: US-046 `backend/src/controllers/admin/order.ts`, `backend/src/routes/admin/order.ts`
- Test: `backend/tests/controllers/admin/order.test.ts`（追加退款审批场景）

- [ ] **RED:** Write failing tests — 200 approve refund with transaction; 200 reject refund; 400 duplicate; 400 invalid amount; 403 forbidden
- [ ] **GREEN:** 在 US-046 order controller 中实现退款批准/驳回接口
- [ ] **REFACTOR:** Extract admin action validation
- [ ] **COMMIT:** `feat(api): reuse POST /api/admin/order/approve-refund and POST /api/admin/order/reject-refund`

## Task 4: Channel Refund Adapter & Retry Job [P1]

**Files:**
- Create: `backend/src/adapters/refund-channel.ts`, `backend/src/jobs/retry-failed-refunds.ts`
- Test: 对应测试文件

- [ ] **RED:** Write failing tests — WeChat/Alipay refund request; retry failed transactions
- [ ] **GREEN:** Implement adapter and cron retry job
- [ ] **REFACTOR:** Abstract channel interface
- [ ] **COMMIT:** `feat(refund): add channel refund adapter and retry job`

## Task 5: 管理端退款处理页面 [P1]

**Files:**
- Create: `web-admin/src/pages/refunds/index.tsx`, `web-admin/src/pages/refunds/detail.tsx`
- Test: 对应测试文件

- [ ] **RED:** Write failing tests — renders refund list; detail shows approve/reject buttons; handles API errors
- [ ] **GREEN:** Implement pages
- [ ] **REFACTOR:** Extract `<RefundStatusBadge />`
- [ ] **COMMIT:** `feat(admin): add refund processing pages`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = RED → GREEN → REFACTOR → COMMIT
- 禁止 placeholder
