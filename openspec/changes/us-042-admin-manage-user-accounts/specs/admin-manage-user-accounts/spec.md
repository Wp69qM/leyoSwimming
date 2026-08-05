## ADDED Requirements

### Requirement: REQ-001 Admin shall list and view user accounts

The system MUST provide an admin interface to list and view user accounts with filtering by identity and registration time.

#### Scenario: List users with filters

- **GIVEN** admin M is logged in with `MANAGE_USER_ACCOUNT` permission
- **WHEN** admin M opens the user account list with identity filter "学员"
- **THEN** the system returns paginated users matching the filter
- **AND** each item shows user ID, phone, identity, and registration time

### Requirement: REQ-002 Admin shall update user phone

The system MUST allow an admin to update a user's phone number after verifying uniqueness and ownership. Ownership MUST be verified by an SMS code sent to the original phone number; if verification is not possible, the admin MUST select "force change" and record the reason in `audit_log.remark`.

#### Scenario: Update phone with ownership verification

- **GIVEN** admin M has user management permission
- **AND** user U has phone "13800138000"
- **AND** phone "13900139000" is not used by any other user
- **AND** admin M has verified ownership via SMS code sent to "13800138000"
- **WHEN** admin M updates user U's phone to "13900139000"
- **THEN** `user.phone` is updated to "13900139000"
- **AND** one `audit_log` entry with `action = 'ADMIN_UPDATE_PHONE'` is created
- **AND** the API returns HTTP 200 with message "手机号已更新"

#### Scenario: Force update phone when original phone is unreachable

- **GIVEN** admin M has user management permission
- **AND** user U has phone "13800138000" but the original SMS cannot be delivered
- **AND** phone "13900139000" is not used by any other user
- **WHEN** admin M selects "force change" and enters reason "原手机号已停机"
- **THEN** `user.phone` is updated to "13900139000"
- **AND** one `audit_log` entry with `action = 'ADMIN_UPDATE_PHONE'` and `remark = "原手机号已停机（强制变更）"` is created
- **AND** the API returns HTTP 200 with message "手机号已强制更新"

### Requirement: REQ-003 System shall enforce admin permission

The system MUST reject user management operations from admins without the required permission. The system MUST also prevent `admin` roles from modifying `super_admin` accounts or granting the `super_admin` role.

#### Scenario: Admin without permission is denied

- **GIVEN** admin M2 is logged in with role `admin` but does not have `USER:WRITE` permission
- **WHEN** admin M2 calls the update-phone API
- **THEN** the API returns HTTP 403 with error code `ADMIN_PERMISSION_DENIED`
- **AND** no user data is modified

#### Scenario: Admin cannot modify super_admin account

- **GIVEN** admin M2 is logged in with role `admin`
- **AND** user S is a `super_admin`
- **WHEN** admin M2 calls the ban API for user S
- **THEN** the API returns HTTP 403 with error code `ADMIN_PERMISSION_DENIED`
- **AND** user S.status remains unchanged

### Requirement: REQ-004 System shall validate target user existence and phone uniqueness

The system MUST return clear errors when the target user does not exist or the new phone is already taken.

#### Scenario: User not found

- **GIVEN** admin M has user management permission
- **AND** user ID 999999 does not exist
- **WHEN** admin M queries or updates that user
- **THEN** the API returns HTTP 404 with error code `USER_NOT_FOUND`

#### Scenario: Phone already exists

- **GIVEN** admin M has user management permission
- **AND** user U has phone "13800138000"
- **AND** phone "13900139000" is already used by user U2
- **WHEN** admin M updates user U's phone to "13900139000"
- **THEN** the API returns HTTP 409 with error code `PHONE_ALREADY_EXISTS`
- **AND** `user.phone` remains "13800138000"

### Requirement: REQ-005 Admin shall ban and unban user accounts

The system MUST allow an admin with `USER:BAN` permission to ban or unban a user account. The system MUST record the reason in `audit_log.remark`.

#### Scenario: Ban user account successfully

- **GIVEN** admin M has role `admin` and permission `USER:BAN`
- **AND** user U has status "正常" (0)
- **WHEN** admin M bans user U with reason "涉嫌违规"
- **THEN** `user.status` is updated to 2 (封禁)
- **AND** one `audit_log` entry with `action = 'ADMIN_BAN_USER'` and `remark = "涉嫌违规"` is created
- **AND** the API returns HTTP 200 with message "账号已封禁"

#### Scenario: Unban user account successfully

- **GIVEN** admin M has role `admin` and permission `USER:BAN`
- **AND** user U has status 2 (封禁)
- **WHEN** admin M unbans user U with reason "申诉通过"
- **THEN** `user.status` is updated to 0 (正常)
- **AND** one `audit_log` entry with `action = 'ADMIN_UNBAN_USER'` and `remark = "申诉通过"` is created
- **AND** the API returns HTTP 200 with message "账号已解封"
