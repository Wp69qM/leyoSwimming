## 1. Setup

- [ ] 1.1 Create Knex migration to add `settlement_status` and `schedule_cleared` to `coach_resignation_ticket`
- [ ] 1.2 Add `admin-resignation` route module in backend
- [ ] 1.3 Add「教练离职审批队列」menu item in web-admin

## 2. Core Implementation

- [ ] 2.1 Implement `GET /api/admin/v1/resignation-tickets` — maps to REQ-001 / Scenario: List pending audit queue
- [ ] 2.2 Implement `GET /api/admin/v1/resignation-tickets/{ticket_id}` returning checklist state
- [ ] 2.3 Implement `POST /api/admin/v1/resignation-tickets/{ticket_id}/approve` — maps to REQ-002 / Scenario: Approve resignation successfully
- [ ] 2.4 Implement `POST /api/admin/v1/resignation-tickets/{ticket_id}/reject` — maps to REQ-003 / Scenario: Reject resignation successfully

## 3. Checklist Validation

- [ ] 3.1 Validate active student count = 0 as an independent checklist item
- [ ] 3.2 Validate all active packages have registered actions (transfer / refund / continue, PRD §5.4.7 three-way choice) when active student count > 0 — maps to REQ-004 / Scenario: Approve blocked by unregistered package actions when active students exist
- [ ] 3.3 Validate future schedule slots are cleared — maps to REQ-004 / Scenario: Approve blocked by uncleared schedule
- [ ] 3.4 Validate settlement status (optional blocking based on policy)

## 4. Batch Updates

- [ ] 4.0 Dispatch by action type (PRD §5.4.7 three-way choice):
  - `action = refund`: generate pending `refund_record` with `refund_amount = price_per_hour × (reserved_count + available_count)` (PRD §6.4.5), move package to frozen(coach_resigned)
  - `action = transfer`: update `package.coach_id` to new coach, package.status stays active
  - `action = continue`: package.status stays active, no freeze, no refund
- [ ] 4.1 Cancel future bookings with `cancel_reason = 2（教练离职）`
- [ ] 4.2 Reset `package.reserved_count` to 0 and increase `available_count` accordingly
- [ ] 4.3 Hide future schedule slots
- [ ] 4.4 Wrap coach.status, ticket, refund_record, booking, package, schedule_slot updates in one transaction

## 5. Concurrency & Security

- [ ] 5.1 Implement optimistic lock on ticket approval/rejection — maps to boundary scenario 1
- [ ] 5.2 Reject non-pending audit approvals — maps to REQ-005
- [ ] 5.3 Verify `MANAGE_COACH_RESIGNATION` permission on all admin endpoints

## 6. Audit & Cache

- [ ] 6.1 Write audit_log for approval/rejection and batch changes
- [ ] 6.2 Invalidate related caches: `coach:status:*`, `coach:profile:*`, `package:*`, `booking:*`, `slot:*`

## 7. Frontend

- [ ] 7.1 Build resignation ticket queue list page
- [ ] 7.2 Build ticket detail page with checklist and package actions
- [ ] 7.3 Build approve/reject confirmation modals

## 8. Verification

- [ ] 8.1 Run unit tests for checklist validation and batch SQL generation
- [ ] 8.2 Run integration tests for all 5 GWT scenarios
- [ ] 8.3 Run concurrency and bulk performance tests
- [ ] 8.4 Run `openspec validate us-041-admin-handle-coach-resignation --json` and fix issues
