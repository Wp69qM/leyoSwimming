## ADDED Requirements

### Requirement: REQ-001 Coach shall apply for resignation

The system MUST allow a coach with `coach.status = 1` to apply for resignation, update the coach status to `4`, and create a resignation ticket listing all active packages under that coach. The system MUST generate a 100% refund record for any active package when the coach confirms refund or submits the ticket to the admin queue, where `refund_amount = unit_price × remaining_hours` and consumed hours are not refundable.

#### Scenario: Successful resignation application

- **GIVEN** coach C is logged in and `coach.status = 1`
- **AND** coach C has 3 active packages
- **WHEN** coach C submits resignation with reason "个人发展"
- **THEN** `coach.status` is updated to `4`
- **AND** a resignation ticket is created with `ticket.status = "processing"`
- **AND** the ticket lists all 3 active packages with remaining hours
- **AND** the API returns HTTP 200 with message "离职申请已提交，请处理学员套餐"

### Requirement: REQ-002 Coach shall confirm full refund for package on resignation ticket

The system MUST require a coach to confirm a full refund decision for each active package on the resignation ticket. The system MUST persist the confirmation in `coach_resignation_action` with `action = 'refund'`. The system MUST create a `refund_record` with `refund_amount = unit_price × remaining_hours` (consumed hours are not refundable). When the coach submits the ticket to the admin queue, any active package without a confirmed refund MUST default to "refund" and generate a pending `refund_record`.

#### Scenario: Successful refund confirmation with auto-generated refund record

- **GIVEN** coach C has a resignation ticket in "processing" status
- **AND** student S's package P1 (active, 5 hours remaining, unit price 300 CNY) is in the ticket
- **WHEN** coach C confirms "refund" for P1
- **THEN** a `coach_resignation_action` record is created
- **AND** `action = 'refund'`, `status = 'registered'`
- **AND** a `refund_record` is created with `refund_amount = 1500` CNY
- **AND** ticket progress is updated to "1 / 3 已确认"

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
