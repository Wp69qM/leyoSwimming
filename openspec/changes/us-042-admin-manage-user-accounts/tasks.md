## 1. Setup

- [ ] 1.1 Verify `user` table has `version` column
- [ ] 1.2 Add `admin-users` route module in backend
- [ ] 1.3 Add「用户账号」menu item in web-admin user management section

## 2. User List & Detail

- [ ] 2.1 Implement `GET /api/admin/v1/users` with filtering and pagination — maps to REQ-001 / Scenario: List users with filters
- [ ] 2.2 Implement `GET /api/admin/v1/users/{user_id}` returning user details and roles — maps to REQ-001

## 3. Phone / Email Update

- [ ] 3.1 Implement `PUT /api/admin/v1/users/{user_id}/phone` with uniqueness and version check — maps to REQ-002 / Scenario: Update phone successfully and REQ-004
- [ ] 3.2 Implement `PUT /api/admin/v1/users/{user_id}/email` with uniqueness check

## 4. Role Assignment

- [ ] 4.1 Implement `PUT /api/admin/v1/users/{user_id}/roles`
- [ ] 4.2 Validate role IDs exist
- [ ] 4.3 Update `user_role` association table atomically
- [ ] 4.4 Prevent `admin` from granting `super_admin` role

## 5. Ban/Unban Account

- [ ] 5.1 Implement `POST /api/admin/v1/users/{user_id}/ban` — maps to REQ-005
- [ ] 5.2 Implement `POST /api/admin/v1/users/{user_id}/unban` — maps to REQ-005
- [ ] 5.3 Validate current status and record reason to audit_log
- [ ] 5.4 Prevent `admin` from banning `super_admin`

## 6. Security & Audit

- [ ] 6.1 Verify granular permissions (`USER:READ` / `USER:WRITE` / `USER:ROLE_ASSIGN` / `USER:BAN`) on endpoints — maps to REQ-003
- [ ] 6.2 Prevent modification of super-admin critical fields
- [ ] 6.3 Write audit_log for phone/email updates, role changes, and ban/unban
- [ ] 6.4 Invalidate user detail and login caches after updates

## 7. Frontend

- [ ] 7.1 Build user account list page with filters
- [ ] 7.2 Build user view modal (read-only profile with guardian info for minors)
- [ ] 7.3 Build user edit modal (avatar, name, gender, age, swim profile, guardian info)
- [ ] 7.4 Build role assignment multi-select component
- [ ] 7.5 Build ban/unban confirmation modal with reason input

## 8. Verification

- [ ] 8.1 Run unit tests for uniqueness validation
- [ ] 8.2 Run integration tests for all 7 GWT scenarios
- [ ] 8.3 Run concurrency tests for optimistic lock
- [ ] 8.4 Run `openspec validate us-042-admin-manage-user-accounts --json` and fix issues
