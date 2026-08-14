## 1. Setup

- [ ] 1.1 Verify `package` table columns: snapshot fields + `status`, `frozen_reason`, `reserved_count`, `available_count`, `version`
- [ ] 1.2 Verify `order` and `refund_record` tables support refund order creation
- [ ] 1.3 Add `admin-package` route module in backend
- [ ] 1.4 Add「套餐管理」menu item under「套餐订单」in web-admin

## 2. List & Detail Implementation

- [ ] 2.1 Implement `POST /api/admin/package/list` with filters and pagination
- [ ] 2.2 Implement `POST /api/admin/package/detail` including snapshot, class records, and operation logs
- [ ] 2.3 Build package management list page
- [ ] 2.4 Build package detail page with tabs

## 3. Freeze Implementation

- [ ] 3.1 Implement `POST /api/admin/package/freeze`
- [ ] 3.2 Validate package is active before freezing
- [ ] 3.3 Release `reserved_count` to `available_count` on freeze
- [ ] 3.4 Cancel upcoming bookings with `cancel_reason = 6`（套餐冻结）

## 4. Unfreeze Implementation

- [ ] 4.1 Implement `POST /api/admin/package/unfreeze`
- [ ] 4.2 Validate package is frozen before unfreezing
- [ ] 4.3 Clear `frozen_reason` on unfreeze

## 5. Extend Implementation

- [ ] 5.1 Implement `POST /api/admin/package/extend`
- [ ] 5.2 Validate package status ∈ {active, expired} and available + reserved > 0
- [ ] 5.3 Validate extension `reason` is present and ≤ 200 characters
- [ ] 5.4 Update `expire_at`, `extend_reason`, and status from expired to active when applicable

## 6. Refund Request Implementation

- [ ] 6.1 Implement `POST /api/admin/package/refund`
- [ ] 6.2 Validate package is active, `refund_enabled = true`, and within `refund_valid_days`
- [ ] 6.3 Create refund order (`order.type = 'refund'`, `status = 'refund_pending'`)
- [ ] 6.4 Create `refund_record` linked to the refund order
- [ ] 6.5 Set package status to frozen with `frozen_reason = 'refund_pending'`

## 7. Permission & Concurrency

- [ ] 7.1 Verify `MANAGE_PACKAGE` permission on all endpoints
- [ ] 7.2 Implement optimistic lock using `package.version`
- [ ] 7.3 Return `PACKAGE_CONCURRENTLY_UPDATED` on version mismatch

## 8. Audit & Cache

- [ ] 8.1 Write `audit_log` entries for list/view-altering operations
- [ ] 8.2 Invalidate `package:{package_id}`, `user:packages:{user_id}`, and `coach:packages:{coach_id}` caches on mutations

## 9. Frontend Modals

- [ ] 9.1 Build freeze confirmation modal with reason selector
- [ ] 9.2 Build unfreeze confirmation modal
- [ ] 9.3 Build extend date picker modal
- [ ] 9.4 Build refund reason modal

## 10. Verification

- [ ] 10.1 Run unit tests for state machine and refund/extend calculations
- [ ] 10.2 Run integration tests for all 12 GWT scenarios
- [ ] 10.3 Run concurrency tests for freeze/unfreeze/extend
- [ ] 10.4 Run `openspec validate us-043-admin-freeze-unfreeze-package --json` and fix issues
