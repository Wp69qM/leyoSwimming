# US-042 管理员管理用户账号测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 30% | Jest | 唯一性校验、乐观锁、用户资料校验 |
| 集成测试 | 55% | Jest + Supertest | 用户列表、详情、新建、编辑、封禁 API |
| E2E 测试 | 10% | Playwright | 管理后台用户管理页 |
| 性能测试 | 5% | k6 | 用户列表查询 |

> **范围说明**：本测试计划覆盖 `user` 表（C 端用户/学员）的管理接口。管理员账号（`admin_user` 表）的测试由 US-057「管理员管理管理员账号」负责。

---

## 2. TDD 任务清单

### Task 1：用户列表与详情

- [ ] **1.1 RED**：编写用户列表分页与筛选测试
- [ ] **1.2 RED**：编写用户详情返回完整档案测试
- [ ] **1.3 GREEN**：实现列表与详情接口
- [ ] **1.4 REFACTOR**：封装 admin user DTO
- [ ] **1.5 COMMIT**：`feat(us-042): admin user list and detail`

**对应**：user-story.md §6.1 查看列表/详情 / tech-design §4.1、§4.2

### Task 2：手动新建用户

- [ ] **2.1 RED**：编写管理员新建用户成功测试
- [ ] **2.2 RED**：编写新建用户手机号已存在失败测试
- [ ] **2.3 RED**：编写未成年人缺少监护人信息失败测试
- [ ] **2.4 GREEN**：实现新建用户接口（含手机号唯一性校验、source='ADMIN_CREATED'）
- [ ] **2.5 REFACTOR**：封装用户资料校验逻辑
- [ ] **2.6 COMMIT**：`feat(us-042): admin create user`

**对应**：user-story.md §6.3、§6.4 / tech-design §4.3

### Task 3：编辑用户资料

- [ ] **3.1 RED**：编写编辑用户资料成功测试
- [ ] **3.2 RED**：编写未成年人缺少监护人信息失败测试
- [ ] **3.3 RED**：编写并发编辑失败测试
- [ ] **3.4 GREEN**：实现编辑资料接口（含乐观锁）
- [ ] **3.5 COMMIT**：`feat(us-042): admin update user profile`

**对应**：user-story.md §6.2 / tech-design §4.4

### Task 4：封禁/解封账号

- [ ] **4.1 RED**：编写封禁账号更新 status 与 audit_log 测试
- [ ] **4.2 RED**：编写解封账号测试
- [ ] **4.3 RED**：编写无权限封禁失败测试
- [ ] **4.4 GREEN**：实现 ban/unban 接口
- [ ] **4.5 COMMIT**：`feat(us-042): admin ban and unban user`

**对应**：user-story.md §6.5 / tech-design §4.5、§4.6、§8.2

### Task 5：审计日志

- [ ] **5.1 RED**：编写新建/编辑/封禁操作写入 audit_log 测试
- [ ] **5.2 GREEN**：实现审计日志统一记录
- [ ] **5.3 COMMIT**：`feat(us-042): admin user audit log`

**对应**：user-story.md §6.3-6.6 / tech-design §4.3-4.6

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 管理员查看用户详情及档案 | 6.1 | `test_admin_user_detail_success` | 返回完整档案字段，HTTP 200 |
| 2 | 管理员编辑用户资料 | 6.2 | `test_admin_update_profile_success` | user.name / user.swim_years 更新，audit_log +1 |
| 3 | 管理员手动新建用户 | 6.3 | `test_admin_create_user_success` | user 创建，source='ADMIN_CREATED'，audit_log +1 |
| 4 | 管理员手动新建未成年人用户 | 6.4 | `test_admin_create_minor_user_success` | user 创建，guardian 信息写入 |
| 5 | 管理员封禁用户账号 | 6.5 | `test_admin_ban_user_success` | user.status=2，audit_log +1 |
| 6 | 管理员解封用户账号 | 6.6 | `test_admin_unban_user_success` | user.status=0，audit_log +1 |
| 7 | 无权限管理员操作失败 | 6.7 | `test_admin_no_permission_denied` | HTTP 403，ADMIN_PERMISSION_DENIED |
| 8 | 目标用户不存在 | 6.8 | `test_admin_user_not_found` | HTTP 404，USER_NOT_FOUND |
| 9 | 新建用户时手机号已被占用 | 6.9 | `test_admin_create_user_phone_exists` | HTTP 409，PHONE_ALREADY_EXISTS |
| 10 | 新建未成年人用户时缺少监护人信息 | 6.10 | `test_admin_create_minor_missing_guardian` | HTTP 400，INVALID_GUARDIAN_INFO |
| 11 | 编辑未成年人用户时缺少监护人信息 | 6.11 | `test_admin_update_minor_missing_guardian` | HTTP 400，INVALID_GUARDIAN_INFO |
| 12 | 并发编辑同一用户资料 | 8.1 | `test_admin_update_profile_concurrent` | 后到请求 409 |
| 13 | 批量导出用户列表 | 8.2 | `test_export_user_list_async` | 异步任务创建成功 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 审计日志覆盖所有敏感操作
- [ ] 无 TBD/TODO 遗留
