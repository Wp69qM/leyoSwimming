# US-042 管理员管理用户账号测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 30% | Jest | 唯一性校验、乐观锁、密码生成 |
| 集成测试 | 55% | Jest + Supertest | 用户列表、详情、修改 API |
| E2E 测试 | 10% | Playwright | 管理后台用户管理页 |
| 性能测试 | 5% | k6 | 用户列表查询 |

---

## 2. TDD 任务清单

### Task 1：用户列表与详情

- [ ] **1.1 RED**：编写用户列表分页与筛选测试
- [ ] **1.2 RED**：编写用户详情返回角色列表测试
- [ ] **1.3 GREEN**：实现列表与详情接口
- [ ] **1.4 REFACTOR**：封装 admin user DTO
- [ ] **1.5 COMMIT**：`feat(us-042): admin user list and detail`

**对应**：user-story.md 场景 1-2 / tech-design §4.1、§4.2

### Task 2：重置密码

- [ ] **2.1 RED**：编写重置密码更新 hash 与 force_change_password 测试
- [ ] **2.2 RED**：编写无权限重置失败测试
- [ ] **2.3 GREEN**：实现重置密码接口
- [ ] **2.4 COMMIT**：`feat(us-042): admin reset user password`

**对应**：user-story.md 场景 1、3 / tech-design §4.3

### Task 3：修改手机号/邮箱

- [ ] **3.1 RED**：编写修改手机号成功测试
- [ ] **3.2 RED**：编写手机号已存在失败测试
- [ ] **3.3 RED**：编写邮箱已存在失败测试
- [ ] **3.4 GREEN**：实现 phone/email 更新接口
- [ ] **3.5 COMMIT**：`feat(us-042): admin update user phone and email`

**对应**：user-story.md 场景 2、5 / tech-design §4.4、§4.5

### Task 4：角色权限配置

- [ ] **4.1 RED**：编写配置角色权限测试
- [ ] **4.2 RED**：编写非法角色 ID 失败测试
- [ ] **4.3 GREEN**：实现角色配置接口
- [ ] **4.4 COMMIT**：`feat(us-042): admin user role assignment`

**对应**：user-story.md 场景 1 / tech-design §4.6

### Task 5：并发与审计

- [ ] **5.1 RED**：编写并发修改手机号测试
- [ ] **5.2 RED**：编写 audit_log 写入测试
- [ ] **5.3 GREEN**：实现乐观锁与审计日志
- [ ] **5.4 COMMIT**：`feat(us-042): concurrent guard and audit log`

**对应**：user-story.md 边界场景 1 / tech-design §8

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 管理员重置用户密码 | 6.1 | `test_admin_reset_password_success` | password_hash 更新，force_change_password=true |
| 2 | 管理员修改用户手机号 | 6.2 | `test_admin_update_phone_success` | user.phone 更新，audit_log +1 |
| 3 | 无权限管理员操作失败 | 6.3 | `test_admin_no_permission_denied` | HTTP 403，ADMIN_PERMISSION_DENIED |
| 4 | 目标用户不存在 | 6.4 | `test_admin_user_not_found` | HTTP 404，USER_NOT_FOUND |
| 5 | 手机号已被占用 | 6.5 | `test_admin_phone_already_exists` | HTTP 409，PHONE_ALREADY_EXISTS |
| 6 | 并发修改手机号 | 8.1 | `test_admin_update_phone_concurrent` | 后到请求 409 |
| 7 | 重置密码后首次登录强制修改 | 8.2 | `test_force_change_password_on_login` | 跳转密码修改页 |
| 8 | 批量导出用户列表 | 8.3 | `test_export_user_list_async` | 异步任务创建成功 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 审计日志覆盖所有敏感操作
- [ ] 无 TBD/TODO 遗留
