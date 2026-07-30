## ADDED Requirements

### Requirement: REQ-001 Admin shall approve coach leave request

The system MUST allow an admin to approve a pending coach leave request, cancel affected upcoming bookings, release reserved hours, and notify students.

#### Scenario: Approve leave successfully

- **GIVEN** admin M is logged in with `MANAGE_SCHEDULE` permission
- **AND** coach C submitted a full-day leave request for 2026-08-01 with status `pending`
- **AND** there are 2 booked sessions within the leave period
- **WHEN** admin M clicks "通过"
- **THEN** `leave_request.status` is updated to `approved`
- **AND** the 2 bookings are cancelled with `cancel_reason = "教练请假"`
- **AND** corresponding `package.reserved_count` decreases and `available_count` increases
- **AND** the system sends cancellation notifications to students
- **AND** the API returns HTTP 200 with message "请假已通过"

### Requirement: REQ-002 Admin shall update coach schedule slot

The system MUST allow an admin to update a coach's schedule slot when there is no time conflict.

#### Scenario: Update schedule slot successfully

- **GIVEN** admin M has schedule management permission
- **AND** coach C has a schedule slot from 09:00 to 10:00 on 2026-08-02
- **AND** the slot has no bookings
- **WHEN** admin M updates it to 10:00-11:00
- **THEN** the `schedule_slot` record is updated successfully
- **AND** there is no time conflict
- **AND** the API returns HTTP 200 with message "排班已更新"

### Requirement: REQ-003 Admin shall reject coach leave request

The system MUST allow an admin to reject a pending leave request and keep the original schedule unchanged.

#### Scenario: Reject leave successfully

- **GIVEN** admin M has schedule management permission
- **AND** coach C's leave request status is `pending`
- **WHEN** admin M clicks "拒绝" and enters reason "场馆活动需要"
- **THEN** `leave_request.status` is updated to `rejected`
- **AND** coach C's existing schedule slots remain unchanged
- **AND** existing bookings are not affected
- **AND** the API returns HTTP 200 with message "请假已拒绝"

### Requirement: REQ-004 System shall prevent duplicate leave approval

The system MUST reject approval/rejection requests for leave requests that are not in `pending` status.

#### Scenario: Leave already approved

- **GIVEN** admin M has schedule management permission
- **AND** coach C's leave request status is `approved`
- **WHEN** admin M calls the approve API again
- **THEN** the API returns HTTP 409 with error code `LEAVE_ALREADY_PROCESSED`
- **AND** `leave_request` status remains `approved`

### Requirement: REQ-005 System shall enforce schedule time conflict checks

The system MUST prevent schedule slot modifications that conflict with existing slots for the same coach.

#### Scenario: Update causes time conflict

- **GIVEN** admin M has schedule management permission
- **AND** coach C has slots 09:00-10:00 and 10:30-11:30 on 2026-08-02
- **WHEN** admin M updates the first slot to 10:00-11:00
- **THEN** the API returns HTTP 409 with error code `SCHEDULE_TIME_CONFLICT`
- **AND** `schedule_slot` remains unchanged
