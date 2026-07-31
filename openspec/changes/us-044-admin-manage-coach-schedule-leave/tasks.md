## 1. Setup

- [ ] 1.1 Create Knex migration to add `is_admin_created` to `schedule_slot`
- [ ] 1.2 Add `admin-schedule` route module in backend
- [ ] 1.3 Add「排班管理」and「请假审批」menu items in web-admin

## 2. Schedule Management

- [ ] 2.1 Implement `GET /api/admin/v1/coaches/{coach_id}/schedule-slots` — maps to REQ-002 / Scenario: Update schedule slot successfully
- [ ] 2.2 Implement `POST /api/admin/v1/coaches/{coach_id}/schedule-slots` for batch adding slots
- [ ] 2.3 Implement `PUT /api/admin/v1/schedule-slots/{slot_id}` — maps to REQ-005 / Scenario: Update causes time conflict
- [ ] 2.4 Implement `DELETE /api/admin/v1/schedule-slots/{slot_id}` with optional booking cancellation
- [ ] 2.5 Implement time conflict checker for same coach

## 3. Leave Approval Queue

- [ ] 3.1 Implement `GET /api/admin/v1/leave-requests` returning pending list
- [ ] 3.2 Implement `GET /api/admin/v1/leave-requests/{id}` returning affected bookings

## 4. Leave Approval/Rejection

- [ ] 4.1 Implement `POST /api/admin/v1/leave-requests/{id}/approve` — maps to REQ-001 / Scenario: Approve leave successfully
- [ ] 4.2 Implement `POST /api/admin/v1/leave-requests/{id}/reject` — maps to REQ-003 / Scenario: Reject leave successfully
- [ ] 4.3 Cancel upcoming bookings within leave period with `cancel_reason = 5`（教练请假）
- [ ] 4.4 Release `package.reserved_count` back to `available_count`
- [ ] 4.5 Reject approval for non-pending leave requests — maps to REQ-004

## 5. Notification & Audit

- [ ] 5.1 Send cancellation notifications to affected students on leave approval
- [ ] 5.2 Send notifications when deleting booked slots
- [ ] 5.3 Write `audit_log` for schedule changes and leave approvals/rejections

## 6. Security

- [ ] 6.1 Verify `MANAGE_SCHEDULE` permission on all endpoints
- [ ] 6.2 Use optimistic lock on leave approval to prevent concurrent processing

## 7. Frontend

- [ ] 7.1 Build schedule management page with weekly calendar view
- [ ] 7.2 Build schedule slot editor modal
- [ ] 7.3 Build leave approval queue page
- [ ] 7.4 Build leave approval/rejection confirmation modal with affected bookings list

## 8. Verification

- [ ] 8.1 Run unit tests for time conflict checker
- [ ] 8.2 Run integration tests for all 5 GWT scenarios
- [ ] 8.3 Run notification delivery tests
- [ ] 8.4 Run `openspec validate us-044-admin-manage-coach-schedule-leave --json` and fix issues
