## ADDED Requirements

### Requirement: REQ-001 Admin shall list user packages

The system MUST allow an admin with `MANAGE_PACKAGE` permission to query a paginated list of user-purchased package instances. The list MUST support filtering by status, package mode, expiry date range, and keyword (package number / user / coach).

#### Scenario: List packages successfully

- **GIVEN** admin M is logged in with `MANAGE_PACKAGE` permission
- **AND** multiple package instances exist in the system
- **WHEN** admin M calls `POST /api/admin/package/list`
- **THEN** the API returns HTTP 200 with a paginated list
- **AND** each item contains package number, user, coach, package mode, status, remaining hours, and expire time

### Requirement: REQ-002 Admin shall view package detail

The system MUST allow an admin to view the full detail of a package instance, including purchase snapshot, purchase time, expire time, consumed hours with usage records, frozen reason, related orders, class records, and operation logs.

#### Scenario: View package detail successfully

- **GIVEN** admin M is logged in with `MANAGE_PACKAGE` permission
- **AND** package P1 exists with `created_at`, `expire_at`, and consumed class records
- **WHEN** admin M calls `POST /api/admin/package/detail` with `packageId`
- **THEN** the API returns HTTP 200 with full detail
- **AND** the response contains purchase snapshot, purchase time, expire time, consumed hours with usage records, frozen reason, and related orders

### Requirement: REQ-003 Admin shall freeze an active package

The system MUST allow an admin to freeze an active package, select a reason, release reserved hours, and cancel upcoming bookings. The system MUST only cancel bookings whose status is `已预约` or `待上课`（未上课）；bookings already completed / cancelled / marked absent are not affected.

#### Scenario: Freeze active package successfully

- **GIVEN** admin M is logged in with `MANAGE_PACKAGE` permission
- **AND** package P1 is active with `reserved_count = 2` and `available_count = 3`
- **WHEN** admin M freezes P1 with reasonDetail "投诉处理中"
- **THEN** `package.status` is updated to `frozen`
- **AND** `frozen_reason` is set to `admin_frozen`
- **AND** `reserved_count` becomes `0` and `available_count` becomes `5`
- **AND** upcoming bookings with status ∈ {已预约, 待上课} are cancelled with `cancel_reason = 6`（套餐冻结）
- **AND** one `audit_log` entry with `action = 'ADMIN_FREEZE_PACKAGE'` and `remark = "投诉处理中"` is created
- **AND** the API returns HTTP 200 with message "套餐已冻结"

### Requirement: REQ-004 Admin shall unfreeze a frozen package

The system MUST allow an admin to unfreeze a frozen package and restore it to active status.

#### Scenario: Unfreeze frozen package successfully

- **GIVEN** admin M has package management permission
- **AND** package P1 is frozen with `frozen_reason = "admin_frozen"`
- **WHEN** admin M unfreezes P1
- **THEN** `package.status` is updated to `active`
- **AND** `frozen_reason` is cleared
- **AND** one `audit_log` entry with `action = 'ADMIN_UNFREEZE_PACKAGE'` is created
- **AND** the API returns HTTP 200 with message "套餐已解冻"

### Requirement: REQ-005 Admin shall extend an active or expired package

The system MUST allow an admin to extend a package whose status is `active` or `expired` and whose `available_count + reserved_count > 0`. The new `expire_at` MUST be later than the current time.

#### Scenario: Extend expired package successfully

- **GIVEN** admin M has package management permission
- **AND** package P1 is expired with `available_count = 3`, `reserved_count = 0`, `expire_at = "2026-08-01"`
- **WHEN** admin M extends P1 with newExpireAt "2026-09-01T23:59:59"
- **THEN** `package.status` is updated to `active`
- **AND** `expire_at` is updated to "2026-09-01T23:59:59"
- **AND** one `audit_log` entry with `action = 'ADMIN_EXTEND_PACKAGE'` is created
- **AND** the API returns HTTP 200 with message "套餐已延期"

### Requirement: REQ-006 Admin shall request a refund for an active package

The system MUST allow an admin to request a refund for an active package when `refund_enabled = true` and within `refund_valid_days`. The system MUST create a refund order (`order.type = 'refund'`, `status = 'refund_pending'`), create a `refund_record`, and set the package status to `frozen` with `frozen_reason = 'refund_pending'`.

#### Scenario: Request refund successfully

- **GIVEN** admin M has package management permission
- **AND** package P1 is active with `refund_enabled = true` and within refund validity period
- **AND** the original purchase order O1 exists
- **AND** no pending refund order exists for P1
- **WHEN** admin M requests refund for P1 with reason "协商退款"
- **THEN** a refund order O2 is created with `type = 'refund'`, `status = 'refund_pending'`, `purchase_order_id` pointing to O1
- **AND** a `refund_record` is created linked to O2
- **AND** `package.status` is updated to `frozen` with `frozen_reason = 'refund_pending'`
- **AND** the API returns HTTP 200 with message "退款订单已生成，请前往订单管理审批"

### Requirement: REQ-007 System shall enforce package management permission

The system MUST reject package management operations from admins without the required permission.

#### Scenario: Admin without permission is denied

- **GIVEN** admin M2 is logged in but does not have `MANAGE_PACKAGE` permission
- **WHEN** admin M2 calls any package management API
- **THEN** the API returns HTTP 403 with error code `ADMIN_PERMISSION_DENIED`
- **AND** package status remains unchanged

### Requirement: REQ-008 System shall validate package status for freeze

The system MUST only allow freezing packages that are currently active.

#### Scenario: Freeze non-active package fails

- **GIVEN** admin M has package management permission
- **AND** package P1 is already frozen
- **WHEN** admin M calls the freeze API for P1
- **THEN** the API returns HTTP 409 with error code `PACKAGE_NOT_ACTIVE`
- **AND** package status remains frozen

### Requirement: REQ-009 System shall validate package status for unfreeze

The system MUST only allow unfreezing packages that are currently frozen.

#### Scenario: Unfreeze non-frozen package fails

- **GIVEN** admin M has package management permission
- **AND** package P1 is active
- **WHEN** admin M calls the unfreeze API for P1
- **THEN** the API returns HTTP 409 with error code `PACKAGE_NOT_FROZEN`
- **AND** package status remains active

### Requirement: REQ-010 System shall validate package status for extend

The system MUST only allow extending packages whose status is `active` or `expired` and whose `available_count + reserved_count > 0`.

#### Scenario: Extend non-extendable package fails

- **GIVEN** admin M has package management permission
- **AND** package P1 is exhausted
- **WHEN** admin M calls the extend API for P1
- **THEN** the API returns HTTP 409 with error code `PACKAGE_NOT_EXTENDABLE`
- **AND** package status remains exhausted

### Requirement: REQ-011 System shall validate refund eligibility

The system MUST reject refund requests when the package is not active, `refund_enabled = false`, or outside `refund_valid_days`.

#### Scenario: Refund ineligible package fails

- **GIVEN** admin M has package management permission
- **AND** package P1 is active with `refund_enabled = false`
- **WHEN** admin M requests refund for P1
- **THEN** the API returns HTTP 409 with error code `PACKAGE_NOT_REFUNDABLE`
- **AND** package status remains active

### Requirement: REQ-012 System shall prevent duplicate refund requests

The system MUST reject a new refund request when a pending refund order already exists for the same package.

#### Scenario: Duplicate refund request fails

- **GIVEN** admin M has package management permission
- **AND** package P1 is frozen with `frozen_reason = 'refund_pending'`
- **WHEN** admin M requests refund for P1 again
- **THEN** the API returns HTTP 409 with error code `REFUND_PENDING_EXISTS`
- **AND** package status remains frozen

## MODIFIED Requirements

- 无

## REMOVED Requirements

- 无
