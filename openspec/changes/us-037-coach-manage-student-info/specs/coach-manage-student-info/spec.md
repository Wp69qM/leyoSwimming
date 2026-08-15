# Spec Delta: coach-manage-student-info

> 本 spec 为 US-037 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 Coach shall view associated student list and complete profile

The system MUST allow an approved coach to list all students associated with them through at least one `booking` or `package` record, and to view the complete profile for each student. The complete profile consists of the US-005 user-owned profile (read-only) and the coach-specific profile slice.

#### Scenario: Successful view of associated student list

- **GIVEN** an approved coach C
- **WHEN** coach C calls `POST /api/coach/student/list` with `{ "tab": "active" }`
- **THEN** the system returns HTTP 200 with a list of associated students
- **AND** each item contains `studentUserId`, `avatarUrl`, `name`, `gender`, `age`, `isMinor`, `updatedAt`

#### Scenario: Successful view of associated student profile

- **GIVEN** an approved coach C and an associated student S who has completed US-005 profile with avatar_url, name, phone 13800138000, age 25, gender male, has_swim_basis true, swim_strokes "蛙泳/自由泳", swim_years "3年", personal_desc "想提高自由泳", is_minor false
- **WHEN** coach C calls `POST /api/coach/student/detail` with `{ "studentId": 10001 }`
- **THEN** the system returns the complete profile including:
  - `userProfile`: avatarUrl, name, phoneMasked "138****8000", age 25, gender male, hasSwimBasis true, swimStrokes "蛙泳/自由泳", swimYears "3年", personalDesc "想提高自由泳", isMinor false (read-only)
  - `coachSlice`: learningStrokes, swimLevel, basics, notes

#### Scenario: Non-associated student is forbidden

- **GIVEN** an approved coach C and a non-associated student S
- **WHEN** coach C calls `POST /api/coach/student/detail` with `{ "studentId": 99999 }`
- **THEN** the system returns HTTP 403 with error code `NOT_ASSOCIATED_STUDENT`

### Requirement: REQ-002 Coach shall view associated package cards on student detail page

The system MUST allow an approved coach to view all `package` instances associated with both the current coach and the selected student, presented as cards sorted by purchase time descending. Each card MUST display package name, package mode, validity period, status label, and remaining hours only.

#### Scenario: Successful view of associated package cards

- **GIVEN** an approved coach C and an associated student S with 3 packages:
  - package 1: status='active', package_mode='standard', available=4, total_hours=10, created_at='2026-08-01'
  - package 2: status='exhausted', package_mode='standard', available=0, total_hours=8, created_at='2026-07-15'
  - package 3: status='expired', package_mode='experience', available=2, total_hours=6, created_at='2026-06-20'
- **WHEN** coach C calls `POST /api/coach/student/package/list` with `{ "studentId": 10001 }`
- **THEN** the API returns HTTP 200 with the "关联套餐" card list with 3 cards sorted by created_at descending
- **AND** package 1 card shows `statusLabel` "使用中" and `remainingHours` 4
- **AND** package 2 card shows `statusLabel` "已使用" and `remainingHours` 0
- **AND** package 3 card shows `statusLabel` "已过期" and `remainingHours` 2
- **AND** clicking package 1 card navigates to the US-021 coach-view package usage detail page

### Requirement: REQ-003 Coach shall update the coach-specific profile slice and notes

The system MUST allow an approved coach to update the coach-specific profile fields and notes for an associated student and persist the changes to `coach_student_profile`. US-005 user-owned fields must not be modified.

#### Scenario: Successful update of student info with notes

- **GIVEN** an approved coach C and an associated student S
- **WHEN** coach C calls `POST /api/coach/student/update` with `{ "studentId": 10001, "learningStrokes": "自由泳", "swimLevel": 2, "basics": "怕水，需循序渐进", "notes": "学员水性较好，可加快进度", "idempotencyKey": "..." }`
- **THEN** the system stores the updated values in `coach_student_profile`, does not modify `user` table, returns HTTP 200 with `{ "message": "保存成功" }`, and writes one audit log entry

### Requirement: REQ-004 System shall restrict profile access to associated students

The system MUST refuse profile read or update requests, as well as associated package list requests, from a coach for students who are not associated with that coach.

#### Scenario: Coach attempts to view/edit/list non-associated student

- **WHEN** an approved coach calls `POST /api/coach/student/detail`, `POST /api/coach/student/update`, or `POST /api/coach/student/package/list` for a student with no booking or package link to them
- **THEN** the system returns HTTP 403 with error code `NOT_ASSOCIATED_STUDENT`

### Requirement: REQ-005 System shall protect US-005 user-owned fields from coach modification

The system MUST ignore or reject any request that attempts to modify US-005 user-owned fields (`avatarUrl`, `name`, `phone`, `age`, `gender`, `hasSwimBasis`, `swimStrokes`, `swimYears`, `personalDesc`, `isMinor`) through the coach profile update API.

#### Scenario: Coach attempts to modify read-only US-005 fields

- **WHEN** an approved coach submits a `POST /api/coach/student/update` request containing user-owned fields like `name="new name"` or `age=99`
- **THEN** the system ignores those fields or returns HTTP 400 with error code `READONLY_USER_PROFILE`, and the `user` table remains unchanged
