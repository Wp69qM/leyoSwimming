## ADDED Requirements

### Requirement: REQ-001 Coach shall view associated student list and complete profile
The system MUST allow an approved coach to list all students associated with them through at least one `booking` or `package` record, and to view the complete profile for each student. The complete profile consists of the US-005 user-owned profile (read-only) and the coach-specific profile slice.

#### Scenario: Successful view of associated student profile
- **GIVEN** an approved coach C and an associated student S who has completed US-005 profile with avatar_url, name, phone 13800138000, age 25, gender male, has_swim_basis true, swim_strokes "蛙泳/自由泳", swim_years "3年", personal_desc "想提高自由泳"
- **WHEN** coach C opens the student detail page for student S
- **THEN** the system returns the complete profile including:
  - `user_profile`: avatar_url, name, phone_masked "138****8000", age 25, gender male, has_swim_basis true, swim_strokes "蛙泳/自由泳", swim_years "3年", personal_desc "想提高自由泳" (read-only)
  - `coach_slice`: learning_strokes, swim_level, basics, notes, is_minor, guardian_name, guardian_phone_masked

### Requirement: REQ-002 Coach shall update the coach-specific profile slice and notes
The system MUST allow an approved coach to update the coach-specific profile fields and notes for an associated student and persist the changes to `coach_student_profile`. US-005 user-owned fields must not be modified.

#### Scenario: Successful update of adult student info with notes
- **GIVEN** an approved coach C and an adult associated student S with is_minor=false
- **WHEN** coach C updates learning_strokes to "自由泳", swim_level to 2, and notes to "学员水性较好，可加快进度", then submits
- **THEN** the system stores the updated values in `coach_student_profile`, does not modify `user` table, returns HTTP 200, and writes one audit log entry

#### Scenario: Successful update of minor student with guardian
- **GIVEN** an approved coach C and an associated student S
- **WHEN** coach C marks the student as is_minor=true, sets guardian_name="王芳", guardian_phone="13800138000", notes="需家长陪同首次下水", then submits
- **THEN** the system encrypts guardian_phone, stores all fields in `coach_student_profile`, does not modify `user` table, and returns HTTP 200

### Requirement: REQ-003 System shall enforce guardian phone for minors
The system MUST reject any update that sets is_minor=true without a valid guardian_phone, and return a clear error code.

#### Scenario: Missing guardian phone for minor
- **WHEN** an approved coach sets is_minor=true but leaves guardian_phone empty
- **THEN** the system returns HTTP 400 with error code GUARDIAN_PHONE_REQUIRED and does not modify the database

#### Scenario: Invalid guardian phone format
- **WHEN** an approved coach sets is_minor=true and guardian_phone="12345"
- **THEN** the system returns HTTP 400 with error code INVALID_PHONE and does not modify the database

### Requirement: REQ-004 System shall restrict profile access to associated students
The system MUST refuse profile read or update requests from a coach for students who are not associated with that coach.

#### Scenario: Coach attempts to view/edit non-associated student
- **WHEN** an approved coach calls the profile API for a student with no booking or package link to them
- **THEN** the system returns HTTP 403 with error code NOT_ASSOCIATED_STUDENT

### Requirement: REQ-005 System shall protect US-005 user-owned fields from coach modification
The system MUST ignore or reject any request that attempts to modify US-005 user-owned fields (avatar_url, name, phone, age, gender, has_swim_basis, swim_strokes, swim_years, personal_desc) through the coach profile update API.

#### Scenario: Coach attempts to modify read-only US-005 fields
- **WHEN** an approved coach submits a PUT request containing user-owned fields like name="new name" or age=99
- **THEN** the system ignores those fields or returns HTTP 400 with error code READONLY_USER_PROFILE, and the `user` table remains unchanged
