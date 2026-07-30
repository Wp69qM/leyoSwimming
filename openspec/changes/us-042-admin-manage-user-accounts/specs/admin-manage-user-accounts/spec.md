## ADDED Requirements

### Requirement: REQ-001 Admin shall list and view user accounts

The system MUST provide an admin interface to list and view user accounts with filtering by identity and registration time.

#### Scenario: List users with filters

- **GIVEN** admin M is logged in with `MANAGE_USER_ACCOUNT` permission
- **WHEN** admin M opens the user account list with identity filter "学员"
- **THEN** the system returns paginated users matching the filter
- **AND** each item shows user ID, phone, identity, and registration time

### Requirement: REQ-002 Admin shall reset user password

The system MUST allow an admin to reset a user's password and force a password change on next login.

#### Scenario: Reset password successfully

- **GIVEN** admin M has user management permission
- **AND** user U has phone "13800138000"
- **WHEN** admin M clicks "重置密码" for user U
- **THEN** `user.password_hash` is updated with a new random hash
- **AND** `user.force_change_password` is set to `true`
- **AND** one `audit_log` entry with `action = 'ADMIN_RESET_PASSWORD'` is created
- **AND** the API returns HTTP 200 with message "密码已重置，用户首次登录需修改密码"

### Requirement: REQ-003 Admin shall update user phone

The system MUST allow an admin to update a user's phone number after verifying uniqueness.

#### Scenario: Update phone successfully

- **GIVEN** admin M has user management permission
- **AND** user U has phone "13800138000"
- **AND** phone "13900139000" is not used by any other user
- **WHEN** admin M updates user U's phone to "13900139000"
- **THEN** `user.phone` is updated to "13900139000"
- **AND** one `audit_log` entry with `action = 'ADMIN_UPDATE_PHONE'` is created
- **AND** the API returns HTTP 200 with message "手机号已更新"

### Requirement: REQ-004 System shall enforce admin permission

The system MUST reject user management operations from admins without the required permission.

#### Scenario: Admin without permission is denied

- **GIVEN** admin M2 is logged in but does not have `MANAGE_USER_ACCOUNT` permission
- **WHEN** admin M2 calls the user list or reset-password API
- **THEN** the API returns HTTP 403 with error code `ADMIN_PERMISSION_DENIED`
- **AND** no user data is modified

### Requirement: REQ-005 System shall validate target user existence and phone uniqueness

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
