# US-043 管理员-查看与管理用户套餐测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 20% | JUnit 5 + Mockito | 状态机、reserved 释放计算、退款金额计算、延期校验 |
| 集成测试 | 65% | JUnit 5 + MockMvc / REST Assured | 列表/详情/冻结/解冻/延期/发起退款 API、并发、权限 |
| E2E 测试 | 10% | Playwright | 管理后台套餐管理页与详情页 |
| 性能测试 | 5% | k6 | 冻结/列表接口并发 |

---

## 2. TDD 任务清单

### Task 1：列表与详情接口

- [ ] **1.1 RED**：编写套餐列表分页查询测试
- [ ] **1.2 RED**：编写按状态/模式/关键词筛选测试
- [ ] **1.3 RED**：编写套餐详情返回快照、购买时间、到期时间、课时消耗、冻结原因测试
- [ ] **1.4 GREEN**：实现 `POST /api/admin/package/list` 与 `POST /api/admin/package/detail`
- [ ] **1.5 REFACTOR**：封装查询条件构建器
- [ ] **1.6 COMMIT**：`feat(us-043): admin package list and detail`

**对应**：user-story.md 场景 6.1 / 6.2 / tech-design §4.1 / §4.2

### Task 2：冻结接口

- [ ] **2.1 RED**：编写 active package 冻结成功测试
- [ ] **2.2 RED**：编写冻结释放 reserved 课时测试
- [ ] **2.3 RED**：编写非 active 冻结失败测试
- [ ] **2.4 GREEN**：实现 `POST /api/admin/package/freeze`
- [ ] **2.5 REFACTOR**：封装 package freeze service
- [ ] **2.6 COMMIT**：`feat(us-043): admin freeze package`

**对应**：user-story.md 场景 6.3 / tech-design §4.3

### Task 3：解冻接口

- [ ] **3.1 RED**：编写 frozen package 解冻成功测试
- [ ] **3.2 RED**：编写解冻清除 frozen_reason 测试
- [ ] **3.3 RED**：编写非 frozen 解冻失败测试
- [ ] **3.4 GREEN**：实现 `POST /api/admin/package/unfreeze`
- [ ] **3.5 COMMIT**：`feat(us-043): admin unfreeze package`

**对应**：user-story.md 场景 6.4 / tech-design §4.4

### Task 4：延期接口

- [ ] **4.1 RED**：编写 expired package 延期成功测试（含延期原因记录）
- [ ] **4.2 RED**：编写 active package 延期更新 expire_at 测试
- [ ] **4.3 RED**：编写 exhausted package 延期失败测试
- [ ] **4.4 RED**：编写延期原因缺失/超长测试
- [ ] **4.5 GREEN**：实现 `POST /api/admin/package/extend`
- [ ] **4.6 COMMIT**：`feat(us-043): admin extend package`

**对应**：user-story.md 场景 6.5 / tech-design §4.5

### Task 5：发起退款接口

- [ ] **5.1 RED**：编写 active package 发起退款成功测试（含默认金额）
- [ ] **5.2 RED**：编写管理员修改退款金额成功测试（可大于系统计算金额）
- [ ] **5.3 RED**：编写退款金额为负数失败测试
- [ ] **5.4 RED**：编写修改金额未填写调整原因失败测试
- [ ] **5.5 RED**：编写 refund_enabled=false 发起退款失败测试
- [ ] **5.6 RED**：编写重复发起退款失败测试
- [ ] **5.7 GREEN**：实现 `POST /api/admin/package/refund`
- [ ] **5.8 COMMIT**：`feat(us-043): admin request package refund with editable amount`

**对应**：user-story.md 场景 6.6 / 6.12 / tech-design §4.6

### Task 6：取消未上课 booking

- [ ] **6.1 RED**：编写冻结时取消已预约 booking 测试
- [ ] **6.2 RED**：编写不取消已上课 booking 测试
- [ ] **6.3 GREEN**：实现 booking 取消逻辑
- [ ] **6.4 COMMIT**：`feat(us-043): cancel bookings on freeze`

**对应**：user-story.md 场景 6.3 / tech-design §5.2

### Task 7：权限与并发

- [ ] **7.1 RED**：编写无权限操作失败测试
- [ ] **7.2 RED**：编写并发冻结/解冻/延期同一 package 测试
- [ ] **7.3 GREEN**：实现权限校验与乐观锁
- [ ] **7.4 COMMIT**：`feat(us-043): permission and concurrency guard`

**对应**：user-story.md 场景 6.7 / 8.1 / tech-design §8

### Task 8：审计与缓存

- [ ] **8.1 RED**：编写 audit_log 写入测试
- [ ] **8.2 RED**：编写缓存失效测试
- [ ] **8.3 GREEN**：实现审计日志与缓存失效
- [ ] **8.4 COMMIT**：`feat(us-043): audit log and cache invalidation`

**对应**：tech-design §6、§8

### Task 9：退款驳回后恢复

- [ ] **9.1 RED**：编写 US-046 驳回退款订单后 package 恢复 active 测试
- [ ] **9.2 RED**：编写解冻 admin_frozen 套餐后 frozen_reason 清除测试
- [ ] **9.3 GREEN**：确保 US-046 驳回逻辑恢复 package 状态
- [ ] **9.4 COMMIT**：`feat(us-043-us-046): restore package on refund reject`

**对应**：user-story.md 边界场景 8.5 / US-046

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 管理员查看套餐列表 | 6.1 | `test_admin_package_list_success` | 返回分页列表，字段正确 |
| 2 | 管理员查看套餐详情 | 6.2 | `test_admin_package_detail_success` | 返回快照、购买时间、到期时间、消耗记录、冻结原因 |
| 3 | 管理员手动冻结套餐 | 6.3 | `test_admin_freeze_package_success` | status=frozen，reserved=0，available 增加 |
| 4 | 管理员手动解冻套餐 | 6.4 | `test_admin_unfreeze_package_success` | status=active，frozen_reason 清空 |
| 5 | 管理员手动延期套餐 | 6.5 | `test_admin_extend_package_success` | expired → active，expire_at 更新，extend_reason 记录 |
| 6 | 管理员发起退款 | 6.6 | `test_admin_request_refund_success` | 创建 refund 订单，paid_amount 为填写金额，package=frozen/refund_pending |
| 7 | 无权限管理员操作失败 | 6.7 | `test_admin_package_permission_denied` | HTTP 403 |
| 8 | 冻结非 active 套餐失败 | 6.8 | `test_freeze_not_active_package` | HTTP 409，PACKAGE_NOT_ACTIVE |
| 9 | 解冻非 frozen 套餐失败 | 6.9 | `test_unfreeze_not_frozen_package` | HTTP 409，PACKAGE_NOT_FROZEN |
| 10 | 延期不可延期套餐失败 | 6.10 | `test_extend_not_extendable_package` | HTTP 409，PACKAGE_NOT_EXTENDABLE |
| 11 | 发起退款时 package 不可退款 | 6.11 | `test_refund_not_refundable_package` | HTTP 409，PACKAGE_NOT_REFUNDABLE |
| 12 | 重复发起退款失败 | 6.12 | `test_refund_pending_exists` | HTTP 409，REFUND_PENDING_EXISTS |
| 13 | 退款金额为负数 | — | `test_refund_amount_negative` | HTTP 400，REFUND_AMOUNT_INVALID |
| 14 | 修改退款金额未填调整原因 | — | `test_refund_adjust_reason_required` | HTTP 409，REFUND_AMOUNT_INVALID 或业务校验错误 |
| 15 | 并发冻结/解冻/延期 | 8.1 | `test_concurrent_package_status_change` | 仅一次成功 |
| 16 | 冻结时取消未上课 booking | 8.2 | `test_freeze_cancels_future_bookings` | 未来 booking 取消 |
| 17 | 退款驳回后套餐恢复 active | 8.6 | `test_reject_refund_restores_package` | frozen/refund_pending → active |
| 18 | 延期原因缺失或超长 | 8.5 | `test_extend_reason_invalid` | HTTP 400，INVALID_EXTENSION_REASON |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 12 个 GWT 场景均有对应自动化测试
- [ ] 状态机转换 100% 覆盖（active↔frozen、expired→active、active→frozen/refund_pending）
- [ ] 列表/详情/冻结/解冻/延期/发起退款 API 均有权限与并发测试
- [ ] 无 TBD/TODO 遗留
