## 1. Setup

- [ ] 1.1 Create Flyway migration for `coach_student_profile` table with unique index on `(coach_id, student_user_id)`; table does NOT include `is_minor`, `guardian_name`, `guardian_phone`
- [ ] 1.2 Add `coach-students` route module in backend
- [ ] 1.3 Add「我的学员」page entry in coach miniapp

## 2. Core Implementation

- [ ] 2.1 Implement `GET /api/coach/v1/students` to list associated students with US-005 avatar_url, name, gender, age, is_minor — maps to REQ-001 / Scenario: Successful view of associated student profile
- [ ] 2.2 Implement `GET /api/coach/v1/students/{id}/profile` to return complete profile (`user_profile` read-only + `coach_slice` without guardian fields) — maps to REQ-001 / Scenario: Successful view of associated student profile
- [ ] 2.3 Implement `PUT /api/coach/v1/students/{id}/profile` with field validation, accepting only coach-slice fields (learning_strokes, swim_level, basics, notes) and ignoring US-005 fields — maps to REQ-003 / Scenario: Successful update of student info with notes
- [ ] 2.4 Implement `GET /api/coach/v1/students/{id}/packages` to return associated package cards sorted by purchase time descending, displaying remaining hours only — maps to REQ-002 / Scenario: Successful view of associated package cards
- [ ] 2.5 Enforce association check for profile and packages APIs and return NOT_ASSOCIATED_STUDENT — maps to REQ-004 / Scenario: Coach attempts to view/edit/list non-associated student
- [ ] 2.6 Add protection against modifying US-005 user-owned fields — maps to REQ-005 / Scenario: Coach attempts to modify read-only US-005 fields

## 3. Security & Audit

- [ ] 3.1 Escape `notes` and `basics` fields to prevent XSS
- [ ] 3.2 Write audit_log entry on every profile update with before/after JSON
- [ ] 3.3 Implement idempotency middleware using `idempotency_key`

## 4. Performance

- [ ] 4.1 Cache student list in Redis with key `coach:students:{coach_id}`
- [ ] 4.2 Cache profile detail in Redis with key `coach:student:profile:{coach_id}:{student_user_id}`
- [ ] 4.3 Cache package list in Redis with key `coach:student:packages:{coach_id}:{student_user_id}`
- [ ] 4.4 Invalidate cache on successful profile update; package list cache invalidated by US-021 / US-050 writes

## 5. Frontend

- [ ] 5.1 Build coach student list page with empty/loading/error states
- [ ] 5.2 Build student detail/edit page with US-005 read-only section, coach slice editable section, and associated package card section
- [ ] 5.3 Add notes input field for coach to add/edit remarks
- [ ] 5.4 Build package card component showing package name, mode, validity, status label, and remaining hours only
- [ ] 5.5 Integrate save API and display validation errors
- [ ] 5.6 On package card click, navigate to US-021 coach-view package usage detail page with `package_id`

## 6. Verification

- [ ] 6.1 Run unit tests for validation, status label mapping, and XSS escape
- [ ] 6.2 Run integration tests for all GWT scenarios including package cards
- [ ] 6.3 Run `openspec validate us-037-coach-manage-student-info --json` and fix issues
