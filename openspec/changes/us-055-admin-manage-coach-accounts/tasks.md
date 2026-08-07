## 1. Setup

- [ ] 1.1 Add `admin-coach` route module in backend
- [ ] 1.2 Add「教练管理」menu item in web-admin user management section

## 2. Coach List & Detail

- [ ] 2.1 Implement `POST /api/admin/coach/list` with filtering and pagination — maps to REQ-001 / Scenario: List coaches
- [ ] 2.2 Implement `POST /api/admin/coach/detail` returning coach profile, certificates, history — maps to REQ-002

## 3. Create & Update Coach

- [ ] 3.1 Implement `POST /api/admin/coach/add` — maps to REQ-003 / Scenario: Create coach successfully
- [ ] 3.2 Implement `POST /api/admin/coach/update` — maps to REQ-004 / Scenario: Update coach profile
- [ ] 3.3 Validate phone/id_card uniqueness on add/update
- [ ] 3.4 Write `coach_audit_log` for create/update actions

## 4. Cancel Entry

- [ ] 4.1 Implement `POST /api/admin/coach/cancelEntry` — maps to REQ-005
- [ ] 4.2 Validate coach.status = 1 before cancel
- [ ] 4.3 Write `coach_audit_log` with reason
- [ ] 4.4 Trigger US-041 coach resignation handling after status updated

## 5. Security & Audit

- [ ] 5.1 Verify granular permissions (`COACH:READ` / `COACH:WRITE` / `COACH:CANCEL_ENTRY`) on endpoints — maps to REQ-006
- [ ] 5.2 Write audit_log for all write operations
- [ ] 5.3 Invalidate coach detail and list caches after updates

## 6. Frontend

- [ ] 6.1 Build coach management list page with filters
- [ ] 6.2 Build coach detail page (read-only profile with history)
- [ ] 6.3 Build coach edit modal (new / edit modes, all fields editable by admin)
- [ ] 6.4 Build cancel-entry confirmation modal with reason input

## 7. Verification

- [ ] 7.1 Run unit tests for uniqueness validation and status transitions
- [ ] 7.2 Run integration tests for all 7 GWT scenarios
- [ ] 7.3 Run `openspec validate us-055-admin-manage-coach-accounts --json` and fix issues
