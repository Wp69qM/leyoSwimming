# US-037 教练维护学员信息（含未成年人）测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 50% | Jest | Service 层字段校验、加密、XSS 转义 |
| 集成测试 | 35% | Jest + Supertest | API 端到端、DB 交互、关联关系校验 |
| E2E 测试 | 10% | Playwright / 微信小程序自动化 | 教练端页面交互 |
| 性能测试 | 5% | k6 / JMeter | 列表/保存接口 P99 |

---

## 2. TDD 任务清单

### Task 1：数据模型与关联查询

- [ ] **1.1 RED**：编写 `coach_student_profile` 模型缺失测试（字段、唯一索引）
- [ ] **1.2 GREEN**：创建 Knex migration 与模型
- [ ] **1.3 REFACTOR**：抽取 coach-student 关联校验公共函数
- [ ] **1.4 COMMIT**：`feat(us-037): add coach_student_profile table`

**对应**：user-story.md 场景 1-5、tech-design §3

### Task 2：列表与详情接口

- [ ] **2.1 RED**：编写 `GET /api/coach/v1/students` 返回关联学员列表的失败测试
- [ ] **2.2 RED**：编写 `GET /api/coach/v1/students/{id}/profile` 返回切片详情的失败测试
- [ ] **2.3 GREEN**：实现两个 GET 接口及关联校验
- [ ] **2.4 REFACTOR**：统一列表/详情的脱敏输出
- [ ] **2.5 COMMIT**：`feat(us-037): coach student list and profile detail`

**对应**：user-story.md 场景 1、场景 5、tech-design §4.1 / §4.2

### Task 3：更新接口与字段校验

- [ ] **3.1 RED**：编写正常更新成人学员的失败测试
- [ ] **3.2 RED**：编写未成年人缺监护人 / 手机号非法的失败测试
- [ ] **3.3 GREEN**：实现 PUT 接口、未成年人 guardian_phone 必填校验、手机号正则
- [ ] **3.4 REFACTOR**：将校验逻辑抽到 validator 中间件
- [ ] **3.5 COMMIT**：`feat(us-037): update student profile with validation`

**对应**：user-story.md 场景 1-4、tech-design §4.3

### Task 4：越权与幂等

- [ ] **4.1 RED**：编写非关联学员 403 失败测试
- [ ] **4.2 RED**：编写并发保存幂等测试
- [ ] **4.3 GREEN**：实现 `idempotency_key` 去重与越权拦截
- [ ] **4.4 REFACTOR**：封装 idempotency 中间件
- [ ] **4.5 COMMIT**：`feat(us-037): authorization and idempotency for profile update`

**对应**：user-story.md 场景 5、边界场景 1、tech-design §8

### Task 5：安全与审计

- [ ] **5.1 RED**：编写 guardian_phone AES 加密存储测试
- [ ] **5.2 RED**：编写 notes XSS 转义测试
- [ ] **5.3 RED**：编写 audit_log 写入测试
- [ ] **5.4 GREEN**：实现加密、转义、审计日志
- [ ] **5.5 REFACTOR**：统一敏感字段加密工具函数
- [ ] **5.6 COMMIT**：`feat(us-037): encryption, xss escape and audit log`

**对应**：user-story.md 边界场景 2-3、tech-design §8

### Task 6：性能与缓存

- [ ] **6.1 RED**：编写列表/详情缓存命中测试（mock Redis）
- [ ] **6.2 RED**：编写保存后缓存失效测试
- [ ] **6.3 GREEN**：实现 Redis 缓存读写与失效
- [ ] **6.4 REFACTOR**：封装 coach-student 缓存键生成器
- [ ] **6.5 COMMIT**：`perf(us-037): add redis cache for student list and profile`

**对应**：tech-design §6、§7

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 正常更新成人学员信息 | 6.1 | `test_update_adult_student_profile_success` | HTTP 200，DB 字段更新，audit_log +1 |
| 2 | 正常更新未成年人并填写监护人 | 6.2 | `test_update_minor_student_profile_success` | HTTP 200，guardian_phone 加密存储 |
| 3 | 未成年人未填监护人手机号 | 6.3 | `test_update_minor_missing_guardian` | HTTP 400，错误码 GUARDIAN_PHONE_REQUIRED，DB 不变 |
| 4 | 监护人手机号格式非法 | 6.4 | `test_update_invalid_guardian_phone` | HTTP 400，错误码 INVALID_PHONE，DB 不变 |
| 5 | 教练维护非关联学员 | 6.5 | `test_update_non_associated_student_forbidden` | HTTP 403，错误码 NOT_ASSOCIATED_STUDENT |
| 6 | 并发编辑幂等 | 8.1 | `test_update_profile_idempotent` | 两次请求均 200，仅 1 条 DB 记录 / 1 条审计 |
| 7 | XSS 脚本转义 | 8.2 | `test_notes_xss_escaped` | 数据库存储转义后文本，前端无脚本执行 |
| 8 | 注销学员脱敏展示 | 8.3 | `test_list_deleted_student_masked` | 列表显示脱敏姓名与"已注销"标签 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 覆盖率：service 层 ≥ 80%，validator 层 100%
- [ ] 性能测试：列表 P99 < 200ms，保存 P99 < 300ms
- [ ] 无 TBD/TODO 遗留
