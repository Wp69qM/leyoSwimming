## 1. Setup

- [ ] 1.1 Create Knex migration for `coach_resignation_ticket` and `coach_resignation_action` tables with indexes
- [ ] 1.2 Add `coach-resignation` route module in backend
- [ ] 1.3 Add「申请离职」entry in coach miniapp "我的" page

## 2. Core Implementation

- [ ] 2.1 Implement `POST /api/coach/v1/resignation/apply` with status check and idempotency — maps to REQ-001 / Scenario: Successful resignation application
- [ ] 2.2 Implement `GET /api/coach/v1/resignation/ticket` to return ticket and active package list — maps to REQ-001 / Scenario: Successful resignation application
- [ ] 2.3 Implement `PUT /api/coach/v1/resignation/tickets/{id}/packages/{package_id}/action` with ownership check — maps to REQ-002 / Scenario: Successful refund action registration with auto-generated refund record and REQ-005
- [ ] 2.4 Implement `POST /api/coach/v1/resignation/tickets/{id}/submit` to move ticket to pending_audit and generate default refund records for unregistered active packages

## 3. Validation & State Machine

- [ ] 3.1 Reject apply when `coach.status != 1` — maps to REQ-003
- [ ] 3.2 Reject duplicate apply when `coach.status = 4` — maps to REQ-004
- [ ] 3.3 Enforce action enum value fixed to `refund` (MVP forces 100% refund; transfer/continue not allowed)
- [ ] 3.4 Generate `refund_record` with `refund_amount = unit_price × remaining_hours` when action = refund or package remains unregistered at submit

## 4. Security & Audit

- [ ] 4.1 Verify JWT coach_id matches ticket owner on all endpoints
- [ ] 4.2 Write audit_log on status change and action registration
- [ ] 4.3 Invalidate coach.status cache on apply

## 5. Frontend

- [ ] 5.1 Build resignation entry page with status-aware visibility
- [ ] 5.2 Build resignation ticket page with package list and action form
- [ ] 5.3 Build submit confirmation dialog (no cancel dialog in MVP)

## 6. Verification

- [ ] 6.1 Run unit tests for state machine and idempotency
- [ ] 6.2 Run integration tests for all 5 GWT scenarios
- [ ] 6.3 Run `openspec validate us-039-coach-request-resignation --json` and fix issues
