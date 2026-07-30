## 1. Setup

- [ ] 1.1 Create Knex migration to add `is_reapply` to `coach_application`
- [ ] 1.2 Create Knex migration to add `is_visible_to_new` to `coach_rating` and `review`
- [ ] 1.3 Add `coach-reapply` route module in backend
- [ ] 1.4 Add「重新入驻」entry in coach miniapp "我的" page

## 2. Core Implementation

- [ ] 2.1 Implement `POST /api/coach/v1/reapply` with status check and idempotency — maps to REQ-001 / Scenario: Successful reapply submission
- [ ] 2.2 Implement `GET /api/coach/v1/reapply/status` to return latest reapply application status
- [ ] 2.3 Implement `POST /api/admin/v1/coaches/{coach_id}/reapply/approve` — maps to REQ-002 / Scenario: Admin approves reapply
- [ ] 2.4 Implement `POST /api/admin/v1/coaches/{coach_id}/reapply/reject` — maps to REQ-005 / Scenario: Admin rejects reapply

## 3. Validation & State Machine

- [ ] 3.1 Reject reapply when `coach.status != 3` — maps to REQ-003
- [ ] 3.2 Reject duplicate reapply when `coach.status = 0` — maps to REQ-004
- [ ] 3.3 Validate admin approval/rejection only for pending applications

## 4. Historical Rating Visibility

- [ ] 4.1 Update rating queries to filter by `is_visible_to_new` for new students
- [ ] 4.2 Ensure old students still see historical ratings
- [ ] 4.3 Set `is_visible_to_new = false` for existing ratings on reapply approval

## 5. Security & Audit

- [ ] 5.1 Verify JWT identity on coach endpoints
- [ ] 5.2 Verify `MANAGE_COACH` permission on admin endpoints
- [ ] 5.3 Write audit_log on status change and admin approval/rejection
- [ ] 5.4 Invalidate `coach:status:{coach_id}` and `coach:profile:{coach_id}` caches on status change

## 6. Frontend

- [ ] 6.1 Build reapply entry page with status-aware visibility
- [ ] 6.2 Build reapply status page showing pending/approved/rejected states
- [ ] 6.3 Update admin application review list with `is_reapply` tag

## 7. Verification

- [ ] 7.1 Run unit tests for state machine and visibility filtering
- [ ] 7.2 Run integration tests for all 5 GWT scenarios
- [ ] 7.3 Run `openspec validate us-040-coach-reapply-entry --json` and fix issues
