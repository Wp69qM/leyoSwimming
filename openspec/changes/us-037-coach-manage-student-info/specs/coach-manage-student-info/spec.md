## ADDED Requirements

### Requirement: REQ-001 Coach shall view associated student list and profile
The system MUST allow an approved coach to list all students associated with them through at least one `booking` or `package` record, and to view the coach-specific profile slice for each student.

#### Scenario: Successful view of associated student profile
- **WHEN** an approved coach opens the student detail page for a student associated with them
- **THEN** the system returns the student's profile slice including learning strokes, swim level, basics, notes, is_minor flag, and masked guardian contact

### Requirement: REQ-002 Coach shall update the student profile slice
The system MUST allow an approved coach to update the coach-specific profile fields for an associated student and persist the changes to `coach_student_profile`.

#### Scenario: Successful update of adult student info
- **WHEN** an approved coach updates learning_strokes to "自由泳" and swim_level to 2 for an adult associated student and submits
- **THEN** the system stores the updated values, returns HTTP 200, and writes one audit log entry

#### Scenario: Successful update of minor student with guardian
- **WHEN** an approved coach marks a student as is_minor=true, sets guardian_name="王芳", and guardian_phone="13800138000", then submits
- **THEN** the system encrypts guardian_phone, stores all fields, and returns HTTP 200

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

#### Scenario: Coach attempts to edit non-associated student
- **WHEN** an approved coach calls the profile update API for a student with no booking or package link to them
- **THEN** the system returns HTTP 403 with error code NOT_ASSOCIATED_STUDENT
