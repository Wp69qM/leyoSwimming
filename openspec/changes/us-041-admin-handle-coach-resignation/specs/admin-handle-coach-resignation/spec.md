## ADDED Requirements

### Requirement: REQ-001 Admin shall view pending resignation tickets

The system MUST provide an admin interface to list and view details of resignation tickets awaiting audit.

#### Scenario: List pending audit queue

- **GIVEN** admin M is logged in with `MANAGE_COACH_RESIGNATION` permission
- **WHEN** admin M opens the resignation ticket queue
- **THEN** the system returns all tickets with `ticket.status = "pending_audit"`
- **AND** each item shows coach name, submit time, and handled progress

### Requirement: REQ-002 Admin shall approve coach resignation

The system MUST allow an admin to approve a resignation after all checklist items pass, triggering batch updates. The checklist MUST include: ① active student count = 0; ② if active student count > 0, all active packages have confirmed full refund; ③ coach fees settled; ④ future schedule slots cleared. The system MUST NOT use "or" to weaken item ①.

#### Scenario: Approve resignation successfully

- **GIVEN** admin M has resignation approval permission
- **AND** coach C has `coach.status = 4` and ticket status `pending_audit`
- **AND** coach C has 3 active packages all confirmed as full refund
- **AND** active student count > 0
- **AND** future schedule slots are cleared
- **WHEN** admin M clicks "通过审批"
- **THEN** the system executes the following steps in order within a single transaction (or Saga):
  1. for all active packages (including those without confirmed refund), generate a pending `refund_record` with `refund_amount = unit_price × remaining_hours` (consumed hours are not refundable) and notify the student to choose refund or change coach;
  2. cancel all future bookings with `cancel_reason = 2（教练离职）`
  3. reset `package.reserved_count` to `0` and increase `available_count` accordingly (to absorb any residual reserved hours after booking cancellation)
  4. update all active packages to `frozen` with `frozen_reason = "coach_resigned"`
  5. update future `schedule_slot` records to `hidden`
  6. update `coach.status` to `3`
- **AND** the API returns HTTP 200 with message "审批通过"

### Requirement: REQ-003 Admin shall reject coach resignation

The system MUST allow an admin to reject a resignation and restore the coach to approved status.

#### Scenario: Reject resignation successfully

- **GIVEN** admin M has resignation approval permission
- **AND** coach C has `coach.status = 4` and ticket status `pending_audit`
- **WHEN** admin M clicks "拒绝审批" and enters reason "资料待补充"
- **THEN** `coach.status` is updated to `1`
- **AND** `ticket.status` is updated to `rejected`
- **AND** previously registered package actions remain unchanged
- **AND** the API returns HTTP 200 with message "已拒绝，教练可继续教学"

### Requirement: REQ-004 System shall enforce checklist before approval

The system MUST prevent approval when checklist items are not satisfied.

#### Scenario: Approve blocked by unregistered package actions when active students exist

- **GIVEN** admin M has resignation approval permission
- **AND** coach C's ticket has active students and 2 packages without registered actions (neither transfer / refund / continue)
- **WHEN** admin M clicks "通过审批"
- **THEN** the API returns HTTP 400 with error code `CHECKLIST_NOT_PASSED`
- **AND** `coach.status` remains `4`
- **AND** no batch updates are executed

#### Scenario: Approve blocked by uncleared schedule

- **GIVEN** admin M has resignation approval permission
- **AND** all packages under coach C have registered actions
- **AND** coach C still has future schedule slots not hidden
- **WHEN** admin M clicks "通过审批"
- **THEN** the API returns HTTP 400 with error code `SCHEDULE_NOT_CLEARED`
- **AND** `coach.status` remains `4`

### Requirement: REQ-005 System shall prevent approval of non-pending tickets

The system MUST reject approval/rejection requests for tickets that are not in `pending_audit` status.

#### Scenario: Ticket still in processing

- **GIVEN** admin M has resignation approval permission
- **AND** coach C's ticket status is `processing`
- **WHEN** admin M calls the approve API
- **THEN** the API returns HTTP 409 with error code `TICKET_NOT_PENDING_AUDIT`
- **AND** `coach.status` remains `4`
