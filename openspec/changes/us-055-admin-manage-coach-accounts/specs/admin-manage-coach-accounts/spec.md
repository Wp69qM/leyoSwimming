## ADDED Requirements

### Requirement: REQ-001 Admin shall list coaches

The system MUST provide `POST /api/admin/coach/list` for admins to query coach accounts. The request body MUST support `page`, `pageSize`, `keyword`, `status`, and `realtimeStatus`. The system MUST return a paginated list sorted by `approved_at` descending. Each item MUST show coach ID, name, gender, age, teaching years, teaching strokes, approved time, employment status, and realtime status. The system MUST NOT show avatar in the list.

#### Scenario: List coaches with default sorting

- **GIVEN** admin M is logged in with `COACH:READ` permission
- **WHEN** admin M opens the coach management list
- **THEN** the system returns paginated coaches sorted by `approved_at` descending
- **AND** each item shows coachId, name, gender, age, teachingYears, teachingStrokes, approvedAt, status, realtimeStatus

### Requirement: REQ-002 Admin shall view coach details

The system MUST provide `POST /api/admin/coach/detail` for admins to view a coach's effective profile. The request body MUST contain `coachId`. The response MUST include basic info (name, phone, gender, age, email, wechat QR), real-name & certificates (id card no, id card front/back, qualification, health certificate, portrait), teaching experience (teaching years, total students, total hours, teaching strokes, bio), service settings (reference price), certificate list, application history, and audit log history.

#### Scenario: View coach details successfully

- **GIVEN** admin M has `COACH:READ` permission
- **AND** coach C exists with coach_id=2001 and status=1
- **WHEN** admin M queries coach detail with `{ "coachId": 2001 }`
- **THEN** the system returns the complete effective profile of coach C
- **AND** the system returns coach C's certificates and operation history

### Requirement: REQ-003 Admin shall create a coach

The system MUST provide `POST /api/admin/coach/add` for admins with `COACH:WRITE` permission to create a coach directly. The request body MUST contain all fields required by US-010. The system MUST validate phone and id card uniqueness. Upon success, the system MUST create a `coach` record with `status=1` and `approved_at=now`, write certificates to `coach_certificate`, and record an `ADMIN_CREATE_COACH` audit log entry.

#### Scenario: Create coach successfully

- **GIVEN** admin M has `COACH:WRITE` permission
- **AND** phone "13800138000" is not used by any coach
- **AND** id card "110101199001011234" is not used by any coach
- **WHEN** admin M creates coach with valid profile and certificates
- **THEN** a new coach record is inserted with status=1
- **AND** certificates are written to `coach_certificate`
- **AND** one `coach_audit_log` entry with `action='ADMIN_CREATE_COACH'` is created

### Requirement: REQ-004 Admin shall update coach profile

The system MUST provide `POST /api/admin/coach/update` for admins with `COACH:WRITE` permission to edit a coach's profile. The request body MUST contain `coachId`, `profile`, and `certificates`. The system MUST validate phone and id card uniqueness excluding the current coach. The system MUST allow editing real-name and certificate fields without re-audit. Upon success, the system MUST update `coach`, update `coach_certificate`, and record an `ADMIN_UPDATE_COACH_PROFILE` audit log entry.

#### Scenario: Update coach profile successfully

- **GIVEN** admin M has `COACH:WRITE` permission
- **AND** coach C exists with coach_id=2001
- **WHEN** admin M updates coach C's bio and portrait certificate
- **THEN** `coach.bio` is updated
- **AND** `coach_certificate` is updated for portrait
- **AND** one `coach_audit_log` entry with `action='ADMIN_UPDATE_COACH_PROFILE'` is created

### Requirement: REQ-005 Admin shall cancel coach entry

The system MUST provide `POST /api/admin/coach/cancelEntry` for admins with `COACH:CANCEL_ENTRY` permission. The request body MUST contain `coachId` and `reason`. The system MUST reject if `coach.status != 1`. Upon success, the system MUST update `coach.status` to 3 (resigned), record an `ADMIN_CANCEL_COACH_ENTRY` audit log entry with the reason, and trigger US-041 resignation handling.

#### Scenario: Cancel coach entry successfully

- **GIVEN** admin M has `COACH:CANCEL_ENTRY` permission
- **AND** coach C exists with coach_id=2001 and status=1
- **WHEN** admin M cancels coach C's entry with reason "主动离职"
- **THEN** `coach.status` is updated to 3
- **AND** one `coach_audit_log` entry with `action='ADMIN_CANCEL_COACH_ENTRY'` and `remark="主动离职"` is created

### Requirement: REQ-006 System shall enforce admin permission

The system MUST reject coach management operations from admins without the required permission.

#### Scenario: Admin without permission is denied

- **GIVEN** admin M2 is logged in without `COACH:READ` permission
- **WHEN** admin M2 calls `POST /api/admin/coach/list`
- **THEN** the API returns HTTP 403 with error code `ADMIN_PERMISSION_DENIED`
- **AND** no coach data is returned

#### Scenario: Admin without write permission cannot create coach

- **GIVEN** admin M2 is logged in with only `COACH:READ` permission
- **WHEN** admin M2 calls `POST /api/admin/coach/add`
- **THEN** the API returns HTTP 403 with error code `ADMIN_PERMISSION_DENIED`
- **AND** no coach record is created
