## ADDED Requirements

### Requirement: REQ-001 Resigned coach shall reapply for entry

The system MUST allow a coach with `coach.status = 3` to initiate re-application for entry, update the coach status to `0`, and create a new coach application marked as reapply.

#### Scenario: Successful reapply submission

- **GIVEN** coach C has `coach.status = 3`
- **AND** coach C's account is in good standing
- **WHEN** coach C clicks "重新入驻" and confirms
- **THEN** `coach.status` is updated to `0`
- **AND** a new `coach_application` record is created with `is_reapply = true` and `status = "pending"`
- **AND** the API returns HTTP 200 with message "入驻申请已提交，请等待审核"

### Requirement: REQ-002 Admin shall approve coach reapply

The system MUST allow an admin to approve a reapply application and restore the coach status to `1`.

#### Scenario: Admin approves reapply

- **GIVEN** coach C has `coach.status = 0` and a pending reapply application
- **WHEN** admin clicks "通过审核"
- **THEN** `coach.status` is updated to `1`
- **AND** `coach_application.status` is updated to "approved"
- **AND** historical ratings remain visible to old students but hidden from new students

### Requirement: REQ-003 System shall restrict reapply to resigned coaches

The system MUST prevent coaches whose status is not `3` from accessing or submitting the reapply entry.

#### Scenario: Approved coach cannot reapply

- **GIVEN** coach C has `coach.status = 1`
- **WHEN** coach C opens the reapply entry
- **THEN** the entry is hidden or disabled
- **AND** a message "当前状态不可重新入驻" is shown

### Requirement: REQ-004 System shall prevent duplicate reapply applications

The system MUST ensure that a coach cannot submit multiple reapply applications while one is already pending.

#### Scenario: Duplicate reapply rejected

- **GIVEN** coach C has `coach.status = 0`
- **WHEN** coach C calls the reapply API again
- **THEN** the API returns HTTP 409 with error code `REAPPLY_ALREADY_PENDING`
- **AND** `coach.status` remains `0`
- **AND** no new application record is created

### Requirement: REQ-005 Admin shall reject coach reapply

The system MUST allow an admin to reject a reapply application and restore the coach status to `3`.

#### Scenario: Admin rejects reapply

- **GIVEN** coach C has `coach.status = 0` and a pending reapply application
- **WHEN** admin clicks "拒绝审核" and enters reason "资料不完整"
- **THEN** `coach.status` is updated to `3`
- **AND** `coach_application.status` is updated to "rejected"
- **AND** coach app shows "审核未通过，原因：资料不完整，可再次申请"
