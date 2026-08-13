# US-037 教练查看并维护学员资料测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 50% | JUnit 5 + Mockito | Service 层字段校验、状态标签映射、XSS 转义 |
| 集成测试 | 35% | JUnit 5 + Spring Boot Test / MockMvc | API 端到端、DB 交互、关联关系校验 |
| E2E 测试 | 10% | Playwright / 微信小程序自动化 | 教练端页面交互 |
| 性能测试 | 5% | k6 / JMeter | 列表/保存/套餐列表接口 P99 |

---

## 2. TDD 任务清单

### Task 1：数据模型与关联查询

- [ ] **1.1 RED**：编写 `coach_student_profile` 模型缺失测试（字段、唯一索引）
- [ ] **1.2 GREEN**：创建 Flyway migration 与实体
- [ ] **1.3 REFACTOR**：抽取 coach-student 关联校验公共函数
- [ ] **1.4 COMMIT**：`feat(us-037): add coach_student_profile table`

**对应**：user-story.md 场景 1-4、tech-design §3

### Task 2：列表与详情接口

- [ ] **2.1 RED**：编写 `GET /api/coach/v1/students` 返回关联学员列表的失败测试（含 US-005 头像、姓名、性别、年龄、是否未成年人）
- [ ] **2.2 RED**：编写 `GET /api/coach/v1/students/{id}/profile` 返回完整资料（US-005 自主档案 + 教练切片）的失败测试
- [ ] **2.3 GREEN**：实现两个 GET 接口及关联校验
- [ ] **2.4 REFACTOR**：统一列表/详情的脱敏输出；拆分 `user_profile` 与 `coach_slice`
- [ ] **2.5 COMMIT**：`feat(us-037): coach student list and profile detail with us005 readonly fields`

**对应**：user-story.md 场景 1、场景 4、tech-design §4.1 / §4.2

### Task 3：更新接口与字段校验

- [ ] **3.1 RED**：编写正常更新学员并添加沟通备注的失败测试
- [ ] **3.2 RED**：编写 US-005 只读字段不可修改的失败测试
- [ ] **3.3 GREEN**：实现 PUT 接口、忽略 US-005 字段
- [ ] **3.4 REFACTOR**：将校验逻辑抽到 validator
- [ ] **3.5 COMMIT**：`feat(us-037): update student profile with validation and readonly guard`

**对应**：user-story.md 场景 2、场景 3、tech-design §4.3

### Task 4：关联套餐列表接口

- [ ] **4.1 RED**：编写 `GET /api/coach/v1/students/{id}/packages` 返回关联 package 卡片的失败测试
- [ ] **4.2 RED**：编写套餐列表按购买时间倒序、状态标签映射的失败测试
- [ ] **4.3 RED**：编写套餐列表越权访问的失败测试
- [ ] **4.4 GREEN**：实现 packages 接口与关联校验
- [ ] **4.5 REFACTOR**：抽取 package 卡片 DTO 组装逻辑
- [ ] **4.6 COMMIT**：`feat(us-037): coach student package list for detail page`

**对应**：user-story.md 场景 1 套餐卡片断言、tech-design §4.4

### Task 5：越权与幂等

- [ ] **5.1 RED**：编写非关联学员 403 失败测试
- [ ] **5.2 RED**：编写并发保存幂等测试
- [ ] **5.3 GREEN**：实现 `idempotency_key` 去重与越权拦截
- [ ] **5.4 REFACTOR**：封装 idempotency 拦截器
- [ ] **5.5 COMMIT**：`feat(us-037): authorization and idempotency for profile update`

**对应**：user-story.md 场景 3、边界场景 1、tech-design §8

### Task 6：安全与审计

- [ ] **6.1 RED**：编写 notes XSS 转义测试
- [ ] **6.2 RED**：编写 audit_log 写入测试
- [ ] **6.3 GREEN**：实现转义、审计日志
- [ ] **6.4 REFACTOR**：统一敏感字段处理工具函数
- [ ] **6.5 COMMIT**：`feat(us-037): xss escape and audit log`

**对应**：user-story.md 边界场景 2-3、tech-design §8

### Task 7：性能与缓存

- [ ] **7.1 RED**：编写列表/详情/套餐列表缓存命中测试（mock Redis）
- [ ] **7.2 RED**：编写保存后缓存失效测试
- [ ] **7.3 GREEN**：实现 Redis 缓存读写与失效
- [ ] **7.4 REFACTOR**：封装 coach-student 缓存键生成器
- [ ] **7.5 COMMIT**：`perf(us-037): add redis cache for student list, profile and packages`

**对应**：tech-design §6、§7

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 教练查看学员完整资料含 US-005 字段 | 6.1 | `test_get_student_profile_with_us005_fields` | HTTP 200，返回 user_profile（头像、姓名、手机号脱敏、年龄、性别、游泳基础、是否未成年人等）与 coach_slice |
| 2 | 正常更新学员并添加备注 | 6.2 | `test_update_student_profile_with_notes` | HTTP 200，DB 字段更新，user 表不变，audit_log +1 |
| 3 | 教练维护非关联学员 | 6.3 | `test_update_non_associated_student_forbidden` | HTTP 403，错误码 NOT_ASSOCIATED_STUDENT |
| 4 | 教练切换至历史学员 Tab | 6.4 | `test_list_history_students` | HTTP 200，返回 history 学员，卡片无套餐类型标签 |
| 5 | US-005 只读字段不可修改 | 8.4 | `test_us005_fields_readonly_for_coach` | HTTP 200/400，user 表不被修改，coach_slice 正常保存 |
| 6 | 并发编辑幂等 | 8.1 | `test_update_profile_idempotent` | 两次请求均 200，仅 1 条 DB 记录 / 1 条审计 |
| 7 | XSS 脚本转义 | 8.2 | `test_notes_xss_escaped` | 数据库存储转义后文本，前端无脚本执行 |
| 8 | 注销学员脱敏展示 | 8.3 | `test_list_deleted_student_masked` | 列表显示脱敏姓名与"已注销"标签 |
| 9 | 查看关联套餐列表 | 6.1 | `test_get_student_packages_list` | HTTP 200，返回 package 卡片数组，包含套餐名称、模式、有效期、状态标签、剩余课时 |
| 10 | 关联套餐按购买时间倒序 | 6.1 | `test_student_packages_sorted_by_created_at_desc` | 返回结果按 created_at 倒序 |
| 11 | 关联套餐状态标签映射 | 6.1 | `test_student_packages_status_labels` | active+consumed=0 → 未使用；active+consumed>0 → 使用中；exhausted → 已使用；expired → 已过期；refunded → 已退款；frozen → 已冻结 |
| 12 | 关联套餐仅展示剩余课时 | 6.1 | `test_student_packages_show_remaining_hours_only` | 卡片字段不包含已用/总课时，仅展示 remaining_hours |
| 13 | 关联套餐列表越权 | 6.3 | `test_student_packages_non_associated_forbidden` | HTTP 403，错误码 NOT_ASSOCIATED_STUDENT |
| 14 | 套餐卡片跳转 US-021 教练视角详情页 | 6.1 | `test_package_card_navigates_to_us021_coach_detail` | E2E 验证点击卡片跳转至 US-021 教练视角套餐使用详情页 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 覆盖率：service 层 ≥ 80%，validator 层 100%
- [ ] 性能测试：列表 P99 < 200ms，保存 P99 < 300ms，套餐列表 P99 < 150ms
- [ ] 无 TBD/TODO 遗留

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-04 | PM | 初版 |
| v2.0 | 2026-08-12 | PM | 移除未成年人及监护人相关测试用例； coach_student_profile 不再维护 is_minor / guardian 字段 |
| v2.1 | 2026-08-12 | PM | 新增 Task 4 关联套餐列表接口 TDD 任务；新增用例 9-14 覆盖套餐卡片展示、倒序、状态标签、剩余课时、越权及跳转 US-021 教练视角详情页 |
