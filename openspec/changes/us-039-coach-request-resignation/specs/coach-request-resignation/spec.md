## ADDED Requirements

### Requirement: REQ-001 Coach shall apply for resignation

The system MUST allow a coach with `coach.status = 1` to apply for resignation, update the coach status to `4`, and create a resignation ticket listing all active packages under that coach.

#### Scenario: Successful resignation application

- **GIVEN** coach C is logged in and `coach.status = 1`
- **AND** coach C has 3 active packages
- **WHEN** coach C submits resignation with reason "个人发展"
- **THEN** `coach.status` is updated to `4`
- **AND** a resignation ticket is created with `ticket.status = "processing"`
- **AND** the ticket lists all 3 active packages with remaining hours
- **AND** the API returns HTTP 200 with message "离职申请已提交，请处理学员套餐"

### Requirement: REQ-002 Coach shall register package action on resignation ticket

The system MUST allow a coach to register a handling decision for each active package on the resignation ticket, choosing between transfer, refund, or continue.

#### Scenario: Successful transfer action registration

- **GIVEN** coach C has a resignation ticket in "processing" status
- **AND** student S's package P1 (active, 5 hours remaining) is in the ticket
- **WHEN** coach C registers action "transfer" with `target_coach_id = 200`
- **THEN** a `coach_resignation_action` record is created
- **AND** `action = 'transfer'`, `target_coach_id = 200`, `status = 'registered'`
- **AND** ticket progress is updated to "1 / 3 已处理"

### Requirement: REQ-003 System shall reject resignation application from non-approved coaches

The system MUST prevent coaches whose status is not `1` from accessing or submitting the resignation application.

#### Scenario: Pending-review coach cannot apply

- **GIVEN** coach C has `coach.status = 0`
- **WHEN** coach C opens the resignation entry
- **THEN** the entry is hidden or disabled
- **AND** a message "入驻审核通过后可申请离职" is shown

### Requirement: REQ-004 System shall prevent duplicate resignation applications

The system MUST ensure that a coach cannot submit multiple resignation applications while one is already pending.

#### Scenario: Duplicate application rejected

- **GIVEN** coach C has `coach.status = 4`
- **WHEN** coach C calls the resignation apply API again
- **THEN** the API returns HTTP 409 with error code `RESIGNATION_ALREADY_PENDING`
- **AND** `coach.status` remains `4`
- **AND** no new ticket is created

### Requirement: REQ-005 System shall enforce package ownership for action registration

The system MUST reject any attempt to register an action for a package that does not belong to the requesting coach.

#### Scenario: Coach registers action for non-owned package

- **GIVEN** coach C has a resignation ticket
- **AND** package P2 does not belong to coach C
- **WHEN** coach C registers action "refund" for P2
- **THEN** the API returns HTTP 403 with error code `NOT_OWN_PACKAGE`
- **AND** no `coach_resignation_action` record is written
