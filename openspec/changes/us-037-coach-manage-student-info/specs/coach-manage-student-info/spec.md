## ADDED Requirements

### Requirement: REQ-001 Coach shall view associated student list and complete profile
The system MUST allow an approved coach to list all students associated with them through at least one `booking` or `package` record, and to view the complete profile for each student. The complete profile consists of the US-005 user-owned profile (read-only) and the coach-specific profile slice.

#### Scenario: Successful view of associated student profile
- **GIVEN** an approved coach C and an associated student S who has completed US-005 profile with avatar_url, name, phone 13800138000, age 25, gender male, has_swim_basis true, swim_strokes "蛙泳/自由泳", swim_years "3年", personal_desc "想提高自由泳", is_minor false
- **WHEN** coach C opens the student detail page for student S
- **THEN** the system returns the complete profile including:
  - `user_profile`: avatar_url, name, phone_masked "138****8000", age 25, gender male, has_swim_basis true, swim_strokes "蛙泳/自由泳", swim_years "3年", personal_desc "想提高自由泳", is_minor false (read-only)
  - `coach_slice`: learning_strokes, swim_level, basics, notes

### Requirement: REQ-002 Coach shall view associated package cards on student detail page
The system MUST allow an approved coach to view all `package` instances associated with both the current coach and the selected student, presented as cards sorted by purchase time descending. Each card MUST display package name, package mode, validity period, status label, and remaining hours only.

#### Scenario: Successful view of associated package cards
- **GIVEN** an approved coach C and an associated student S with 3 packages:
  - package 1: status='active', package_mode='standard', available=4, total_hours=10, created_at='2026-08-01'
  - package 2: status='exhausted', package_mode='standard', available=0, total_hours=8, created_at='2026-07-15'
  - package 3: status='expired', package_mode='experience', available=2, total_hours=6, created_at='2026-06-20'
- **WHEN** coach C opens the student detail page for student S
- **THEN** the page displays the "关联套餐" card list with 3 cards sorted by created_at descending
- **AND** package 1 card shows "使用中 剩余 4 课时"
- **AND** package 2 card shows "已使用 剩余 0 课时"
- **AND** package 3 card shows "已过期 剩余 2 课时"
- **AND** clicking package 1 card navigates to the US-021 coach-view package usage detail page

### Requirement: REQ-003 Coach shall update the coach-specific profile slice and notes
The system MUST allow an approved coach to update the coach-specific profile fields and notes for an associated student and persist the changes to `coach_student_profile`. US-005 user-owned fields must not be modified.

#### Scenario: Successful update of student info with notes
- **GIVEN** an approved coach C and an associated student S
- **WHEN** coach C updates learning_strokes to "自由泳", swim_level to 2, basics to "怕水，需循序渐进", and notes to "学员水性较好，可加快进度", then submits
- **THEN** the system stores the updated values in `coach_student_profile`, does not modify `user` table, returns HTTP 200, and writes one audit log entry

### Requirement: REQ-004 System shall restrict profile access to associated students
The system MUST refuse profile read or update requests, as well as associated package list requests, from a coach for students who are not associated with that coach.

#### Scenario: Coach attempts to view/edit/list non-associated student
- **WHEN** an approved coach calls the profile API or packages API for a student with no booking or package link to them
- **THEN** the system returns HTTP 403 with error code NOT_ASSOCIATED_STUDENT

### Requirement: REQ-005 System shall protect US-005 user-owned fields from coach modification
The system MUST ignore or reject any request that attempts to modify US-005 user-owned fields (avatar_url, name, phone, age, gender, has_swim_basis, swim_strokes, swim_years, personal_desc, is_minor) through the coach profile update API.

#### Scenario: Coach attempts to modify read-only US-005 fields
- **WHEN** an approved coach submits a PUT request containing user-owned fields like name="new name" or age=99
- **THEN** the system ignores those fields or returns HTTP 400 with error code READONLY_USER_PROFILE, and the `user` table remains unchanged
