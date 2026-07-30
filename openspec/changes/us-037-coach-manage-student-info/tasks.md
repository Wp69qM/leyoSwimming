## 1. Setup

- [ ] 1.1 Create Knex migration for `coach_student_profile` table with unique index on `(coach_id, student_user_id)`
- [ ] 1.2 Add `coach-students` route module in backend
- [ ] 1.3 Add「我的学员」page entry in coach miniapp

## 2. Core Implementation

- [ ] 2.1 Implement `GET /api/coach/v1/students` to list associated students — maps to REQ-001 / Scenario: Successful view
- [ ] 2.2 Implement `GET /api/coach/v1/students/{id}/profile` to return profile slice — maps to REQ-001 / Scenario: Successful view
- [ ] 2.3 Implement `PUT /api/coach/v1/students/{id}/profile` with field validation — maps to REQ-002 / Scenarios: Successful update of adult, Successful update of minor
- [ ] 2.4 Add guardian_phone mandatory and format validation — maps to REQ-003 / Scenarios: Missing guardian phone, Invalid guardian phone format
- [ ] 2.5 Enforce association check and return NOT_ASSOCIATED_STUDENT — maps to REQ-004 / Scenario: Coach attempts to edit non-associated student

## 3. Security & Audit

- [ ] 3.1 Encrypt `guardian_phone` with AES-256 before storing
- [ ] 3.2 Escape `notes` and `basics` fields to prevent XSS
- [ ] 3.3 Write audit_log entry on every profile update with before/after JSON
- [ ] 3.4 Implement idempotency middleware using `idempotency_key`

## 4. Performance

- [ ] 4.1 Cache student list in Redis with key `coach:students:{coach_id}`
- [ ] 4.2 Cache profile detail in Redis with key `coach:student:profile:{coach_id}:{student_user_id}`
- [ ] 4.3 Invalidate cache on successful profile update

## 5. Frontend

- [ ] 5.1 Build coach student list page with empty/loading/error states
- [ ] 5.2 Build student detail/edit page with minor toggle and guardian form
- [ ] 5.3 Integrate save API and display validation errors

## 6. Verification

- [ ] 6.1 Run unit tests for validation, encryption, and XSS escape
- [ ] 6.2 Run integration tests for all 5 GWT scenarios
- [ ] 6.3 Run `openspec validate us-037-coach-manage-student-info --json` and fix issues
