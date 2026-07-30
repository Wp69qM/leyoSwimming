## 1. Setup

- [ ] 1.1 Verify `package` table columns: `status`, `frozen_reason`, `reserved_count`, `available_count`, `version`
- [ ] 1.2 Add `admin-packages` route module in backend
- [ ] 1.3 Add「套餐管理」menu item in web-admin user management section

## 2. Freeze Implementation

- [ ] 2.1 Implement `POST /api/admin/v1/packages/{package_id}/freeze` — maps to REQ-001 / Scenario: Freeze active package successfully
- [ ] 2.2 Validate package is active before freezing — maps to REQ-004
- [ ] 2.3 Release `reserved_count` to `available_count` on freeze
- [ ] 2.4 Cancel upcoming bookings with `cancel_reason = "package_frozen"`

## 3. Unfreeze Implementation

- [ ] 3.1 Implement `POST /api/admin/v1/packages/{package_id}/unfreeze` — maps to REQ-002 / Scenario: Unfreeze frozen package successfully
- [ ] 3.2 Validate package is frozen before unfreezing — maps to REQ-005
- [ ] 3.3 Clear `frozen_reason` on unfreeze

## 4. Permission & Concurrency

- [ ] 4.1 Verify `MANAGE_PACKAGE` permission on both endpoints — maps to REQ-003
- [ ] 4.2 Implement optimistic lock using `package.version`
- [ ] 4.3 Return `PACKAGE_CONCURRENTLY_UPDATED` on version mismatch

## 5. Audit & Cache

- [ ] 5.1 Write `audit_log` entries for freeze and unfreeze
- [ ] 5.2 Invalidate `package:{package_id}`, `user:packages:{user_id}`, and `coach:packages:{coach_id}` caches

## 6. Frontend

- [ ] 6.1 Build package management list page with status badges
- [ ] 6.2 Build freeze confirmation modal with reason selector
- [ ] 6.3 Build unfreeze confirmation modal

## 7. Verification

- [ ] 7.1 Run unit tests for state machine and reserved release calculation
- [ ] 7.2 Run integration tests for all 5 GWT scenarios
- [ ] 7.3 Run concurrency tests for freeze/unfreeze
- [ ] 7.4 Run `openspec validate us-043-admin-freeze-unfreeze-package --json` and fix issues
