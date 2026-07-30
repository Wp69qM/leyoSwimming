## ADDED Requirements

### Requirement: REQ-001 Admin shall freeze an active package

The system MUST allow an admin to freeze an active package, select a reason, release reserved hours, and cancel upcoming bookings.

#### Scenario: Freeze active package successfully

- **GIVEN** admin M is logged in with `MANAGE_PACKAGE` permission
- **AND** package P1 is active with `reserved_count = 2` and `available_count = 3`
- **WHEN** admin M freezes P1 with reason_detail "投诉处理中"
- **THEN** `package.status` is updated to `frozen`
- **AND** `frozen_reason` is set to `admin_frozen`（v3 评审 P0-1 修复：对齐 PRD §5.5.1.2 VARCHAR(32) 统一枚举）
- **AND** `reserved_count` becomes `0` and `available_count` becomes `5`
- **AND** upcoming bookings are cancelled with `cancel_reason = 6`（套餐冻结）
- **AND** one `audit_log` entry with `action = 'ADMIN_FREEZE_PACKAGE'` and `remark = "投诉处理中"` is created
- **AND** the API returns HTTP 200 with message "套餐已冻结"

### Requirement: REQ-002 Admin shall unfreeze a frozen package

The system MUST allow an admin to unfreeze a frozen package and restore it to active status.

#### Scenario: Unfreeze frozen package successfully

- **GIVEN** admin M has package management permission
- **AND** package P1 is frozen with `frozen_reason = "admin_frozen"`
- **WHEN** admin M unfreezes P1
- **THEN** `package.status` is updated to `active`
- **AND** `frozen_reason` is cleared
- **AND** one `audit_log` entry with `action = 'ADMIN_UNFREEZE_PACKAGE'` is created
- **AND** the API returns HTTP 200 with message "套餐已解冻"

### Requirement: REQ-003 System shall enforce package management permission

The system MUST reject freeze/unfreeze operations from admins without the required permission.

#### Scenario: Admin without permission is denied

- **GIVEN** admin M2 is logged in but does not have `MANAGE_PACKAGE` permission
- **WHEN** admin M2 calls the freeze or unfreeze API
- **THEN** the API returns HTTP 403 with error code `ADMIN_PERMISSION_DENIED`
- **AND** package status remains unchanged

### Requirement: REQ-004 System shall validate package status for freeze

The system MUST only allow freezing packages that are currently active.

#### Scenario: Freeze non-active package fails

- **GIVEN** admin M has package management permission
- **AND** package P1 is already frozen
- **WHEN** admin M calls the freeze API for P1
- **THEN** the API returns HTTP 409 with error code `PACKAGE_NOT_ACTIVE`
- **AND** package status remains frozen

### Requirement: REQ-005 System shall validate package status for unfreeze

The system MUST only allow unfreezing packages that are currently frozen.

#### Scenario: Unfreeze non-frozen package fails

- **GIVEN** admin M has package management permission
- **AND** package P1 is active
- **WHEN** admin M calls the unfreeze API for P1
- **THEN** the API returns HTTP 409 with error code `PACKAGE_NOT_FROZEN`
- **AND** package status remains active
