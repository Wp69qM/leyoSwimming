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

### Requirement: REQ-002 Coach shall register action for package on resignation ticket

The system MUST require a coach to register a processing action for each active package on the resignation ticket (PRD §5.4.7 three-way choice: transfer to new coach / full refund / continue remaining lessons). The system MUST persist the decision in `coach_resignation_action` with `action` ∈ {refund, transfer, continue}. When `action = refund`, the system MUST create a `refund_record` with `refund_amount = price_per_hour × (reserved_count + available_count)` (PRD §6.4.5, consumed hours are not refundable). When `action = transfer`, the system MUST require `target_coach_id` and not generate a refund_record. When `action = continue`, no refund_record is generated and no freeze is applied. When the coach submits the ticket to the admin queue, any active package without a registered action MUST default to "refund" and generate a pending `refund_record`.

#### Scenario: Successful refund action registration with auto-generated refund record

- **GIVEN** coach C has a resignation ticket in "processing" status
- **AND** student S's package P1 (active, reserved_count=2, available_count=3, price_per_hour=300 CNY) is in the ticket
- **WHEN** coach C registers action="refund" for P1
- **THEN** a `coach_resignation_action` record is created
- **AND** `action = 'refund'`, `status = 'registered'`
- **AND** a `refund_record` is created with `refund_amount = 300 × (2 + 3) = 1500` CNY (PRD §6.4.5)
- **AND** ticket progress is updated to "1 / 3 已确认"

#### Scenario: Successful transfer action registration

- **GIVEN** coach C has a resignation ticket in "processing" status
- **AND** student S's package P1 (active) is in the ticket
- **WHEN** coach C registers action="transfer" with target_coach_id=200 for P1
- **THEN** a `coach_resignation_action` record is created with `action = 'transfer'`, `target_coach_id = 200`
- **AND** no `refund_record` is created for P1
- **AND** the actual coach change is deferred to US-041 approval

#### Scenario: Successful continue action registration

- **GIVEN** coach C has a resignation ticket in "processing" status
- **AND** student S's package P1 (active) is in the ticket
- **WHEN** coach C registers action="continue" for P1
- **THEN** a `coach_resignation_action` record is created with `action = 'continue'`
- **AND** no `refund_record` is created for P1
- **AND** P1 will remain active after US-041 approval (no freeze, no refund)

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

### Requirement: REQ-006 System shall display the coach resignation processing page

The system MUST redirect the coach to the "C-教练端离职处理中页" after a successful resignation application. The system MUST display the current ticket status, ticket number, progress message, estimated processing time, and provide entry points to view the resignation ticket and contact customer service. The system MUST redirect a coach with `coach.status = 4` to the processing page upon login via US-051/US-054 with `redirect_page = "coach_resigning"`.

#### Scenario: Coach enters resignation processing page after application

- **GIVEN** coach C is logged in and coach.status = 1
- **AND** coach C has 2 active packages
- **WHEN** coach C submits resignation with reason "个人原因"
- **THEN** coach.status is updated to 4
- **AND** a resignation ticket is created with ticket.status = "processing"
- **AND** the page redirects to C-教练端离职处理中页
- **AND** the page shows title "离职申请已提交，正在处理中"
- **AND** the page shows ticket number and progress "等待教练处理学员套餐"
- **AND** the page shows "预计 1-3 个工作日内完成审批"
- **AND** the page provides "查看离职工单" primary button
- **AND** the page provides "联系客服 / 帮助" text entry

#### Scenario: Coach with status=4 logs in and redirects to resignation processing page

- **GIVEN** coach C has coach.status = 4
- **AND** there is an existing resignation ticket with status = "processing"
- **WHEN** coach C logs in via US-051/US-054
- **THEN** the system returns coach_status = 4 and redirect_page = "coach_resigning"
- **AND** the frontend redirects to C-教练端离职处理中页
- **AND** the page displays current ticket progress per the previous scenario
