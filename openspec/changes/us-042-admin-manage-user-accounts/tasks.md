## 1. Setup

- [ ] 1.1 Verify `user` table has `version` and `force_change_password` columns
- [ ] 1.2 Add `admin-users` route module in backend
- [ ] 1.3 Add「用户账号」menu item in web-admin user management section

## 2. User List & Detail

- [ ] 2.1 Implement `GET /api/admin/v1/users` with filtering and pagination — maps to REQ-001 / Scenario: List users with filters
- [ ] 2.2 Implement `GET /api/admin/v1/users/{user_id}` returning user details and roles — maps to REQ-001

## 3. Password Reset

- [ ] 3.1 Implement `POST /api/admin/v1/users/{user_id}/reset-password` — maps to REQ-002 / Scenario: Reset password successfully
- [ ] 3.2 Generate cryptographically secure random password hash
- [ ] 3.3 Set `force_change_password = true`

## 4. Phone / Email Update

- [ ] 4.1 Implement `PUT /api/admin/v1/users/{user_id}/phone` with uniqueness and version check — maps to REQ-003 / Scenario: Update phone successfully and REQ-005
- [ ] 4.2 Implement `PUT /api/admin/v1/users/{user_id}/email` with uniqueness check

## 5. Role Assignment

- [ ] 5.1 Implement `PUT /api/admin/v1/users/{user_id}/roles`
- [ ] 5.2 Validate role IDs exist
- [ ] 5.3 Update `user_role` association table atomically
- [ ] 5.4 Prevent `admin` from granting `super_admin` role

## 6. Ban/Unban Account

- [ ] 6.1 Implement `POST /api/admin/v1/users/{user_id}/ban` — maps to REQ-006
- [ ] 6.2 Implement `POST /api/admin/v1/users/{user_id}/unban` — maps to REQ-006
- [ ] 6.3 Validate current status and record reason to audit_log
- [ ] 6.4 Prevent `admin` from banning `super_admin`

## 7. Security & Audit

- [ ] 7.1 Verify granular permissions (`USER:READ` / `USER:WRITE` / `USER:PASSWORD_RESET` / `USER:ROLE_ASSIGN` / `USER:BAN`) on endpoints — maps to REQ-004
- [ ] 7.2 Prevent modification of super-admin critical fields
- [ ] 7.3 Write audit_log for reset-password, phone/email updates, role changes, and ban/unban
- [ ] 7.4 Invalidate user detail and login caches after updates

## 8. Frontend

- [ ] 8.1 Build user account list page with filters
- [ ] 8.2 Build user detail/edit page
- [ ] 8.3 Build reset-password confirmation modal
- [ ] 8.4 Build role assignment multi-select component
- [ ] 8.5 Build ban/unban confirmation modal with reason input

## 9. Verification

- [ ] 9.1 Run unit tests for password generation and uniqueness validation
- [ ] 9.2 Run integration tests for all 7 GWT scenarios
- [ ] 9.3 Run concurrency tests for optimistic lock
- [ ] 9.4 Run `openspec validate us-042-admin-manage-user-accounts --json` and fix issues
