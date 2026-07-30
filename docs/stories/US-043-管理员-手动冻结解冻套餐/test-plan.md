# US-043 管理员手动冻结/解冻套餐测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 30% | Jest | 状态机、reserved 释放计算 |
| 集成测试 | 60% | Jest + Supertest | 冻结/解冻 API、并发 |
| E2E 测试 | 5% | Playwright | 管理后台套餐管理页 |
| 性能测试 | 5% | k6 | 冻结接口并发 |

---

## 2. TDD 任务清单

### Task 1：冻结接口

- [ ] **1.1 RED**：编写 active package 冻结成功测试
- [ ] **1.2 RED**：编写冻结释放 reserved 课时测试
- [ ] **1.3 RED**：编写非 active 冻结失败测试
- [ ] **1.4 GREEN**：实现 freeze 接口
- [ ] **1.5 REFACTOR**：封装 package freeze service
- [ ] **1.6 COMMIT**：`feat(us-043): admin freeze package`

**对应**：user-story.md 场景 1、4 / tech-design §4.1

### Task 2：解冻接口

- [ ] **2.1 RED**：编写 frozen package 解冻成功测试
- [ ] **2.2 RED**：编写解冻清除 frozen_reason 测试
- [ ] **2.3 RED**：编写非 frozen 解冻失败测试
- [ ] **2.4 GREEN**：实现 unfreeze 接口
- [ ] **2.5 COMMIT**：`feat(us-043): admin unfreeze package`

**对应**：user-story.md 场景 2、5 / tech-design §4.2

### Task 3：取消未上课 booking

- [ ] **3.1 RED**：编写冻结时取消已预约 booking 测试
- [ ] **3.2 RED**：编写不取消已上课 booking 测试
- [ ] **3.3 GREEN**：实现 booking 取消逻辑
- [ ] **3.4 COMMIT**：`feat(us-043): cancel bookings on freeze`

**对应**：user-story.md 场景 1 / tech-design §5.2

### Task 4：权限与并发

- [ ] **4.1 RED**：编写无权限操作失败测试
- [ ] **4.2 RED**：编写并发冻结/解冻测试
- [ ] **4.3 GREEN**：实现权限校验与乐观锁
- [ ] **4.4 COMMIT**：`feat(us-043): permission and concurrency guard`

**对应**：user-story.md 场景 3、边界场景 1 / tech-design §8

### Task 5：审计与缓存

- [ ] **5.1 RED**：编写 audit_log 写入测试
- [ ] **5.2 RED**：编写缓存失效测试
- [ ] **5.3 GREEN**：实现审计日志与缓存失效
- [ ] **5.4 COMMIT**：`feat(us-043): audit log and cache invalidation`

**对应**：tech-design §6、§8

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 管理员手动冻结套餐 | 6.1 | `test_admin_freeze_package_success` | status=frozen，reserved=0，available 增加 |
| 2 | 管理员手动解冻套餐 | 6.2 | `test_admin_unfreeze_package_success` | status=active，frozen_reason 清空 |
| 3 | 无权限管理员操作失败 | 6.3 | `test_admin_freeze_permission_denied` | HTTP 403 |
| 4 | 冻结非 active 套餐失败 | 6.4 | `test_freeze_not_active_package` | HTTP 409，PACKAGE_NOT_ACTIVE |
| 5 | 解冻非 frozen 套餐失败 | 6.5 | `test_unfreeze_not_frozen_package` | HTTP 409，PACKAGE_NOT_FROZEN |
| 6 | 并发冻结/解冻 | 8.1 | `test_concurrent_freeze_package` | 仅一次成功 |
| 7 | 冻结时取消未上课 booking | 8.2 | `test_freeze_cancels_future_bookings` | 未来 booking 取消 |
| 8 | 学员端实时展示 frozen 状态 | 8.3 | `test_frozen_status_visible_to_student` | 缓存失效后最新状态 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 状态机转换 100% 覆盖（active→frozen、frozen→active）
- [ ] 无 TBD/TODO 遗留
