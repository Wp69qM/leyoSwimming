# US-040 教练重新入驻测试计划

---

## 1. 测试策略

| 层级 | 占比 | 工具 | 说明 |
|------|------|------|------|
| 单元测试 | 35% | Jest | 状态机转换、可见性过滤逻辑 |
| 集成测试 | 50% | Jest + Supertest | 重新入驻、管理员审核 API 端到端 |
| E2E 测试 | 10% | 微信小程序自动化 | 教练端重新入驻入口与状态页 |
| 性能测试 | 5% | k6 | 提交接口并发 |

---

## 2. TDD 任务清单

### Task 1：数据模型扩展

- [ ] **1.1 RED**：编写 `coach_application.is_reapply` 字段缺失测试
- [ ] **1.2 RED**：编写 `coach_rating.is_visible_to_new` 字段缺失测试
- [ ] **1.3 GREEN**：创建 Knex migration 扩展两张表
- [ ] **1.4 COMMIT**：`feat(us-040): extend application and rating tables for reapply`

**对应**：user-story.md 场景 2 / tech-design §3

### Task 2：教练发起重新入驻

- [ ] **2.1 RED**：编写 `POST /api/coach/v1/reapply` 成功测试
- [ ] **2.2 RED**：编写 status ≠ 3 时 403 测试
- [ ] **2.3 RED**：编写重复发起 409 测试
- [ ] **2.4 GREEN**：实现重新入驻接口与幂等去重
- [ ] **2.5 REFACTOR**：封装 reapply service
- [ ] **2.6 COMMIT**：`feat(us-040): coach reapply endpoint`

**对应**：user-story.md 场景 1、3、4 / tech-design §4.1

### Task 3：管理员审核重新入驻

- [ ] **3.1 RED**：编写通过审核 `0 → 1` 测试
- [ ] **3.2 RED**：编写拒绝审核 `0 → 3` 测试
- [ ] **3.3 RED**：编写非 pending 申请不可审核测试
- [ ] **3.4 GREEN**：实现管理员 approve/reject 接口
- [ ] **3.5 REFACTOR**：复用 US-011 审核公共逻辑
- [ ] **3.6 COMMIT**：`feat(us-040): admin reapply review endpoints`

**对应**：user-story.md 场景 2、5 / tech-design §4.3、§4.4

### Task 4：历史评分可见性

- [ ] **4.1 RED**：编写新学员查看主页不展示历史评分测试
- [ ] **4.2 RED**：编写老学员仍可查看历史评分测试
- [ ] **4.3 GREEN**：实现评分查询的可见性过滤
- [ ] **4.4 COMMIT**：`feat(us-040): rating visibility filter for reapply`

**对应**：user-story.md 场景 2、边界场景 3 / tech-design §3

### Task 5：缓存与审计

- [ ] **5.1 RED**：编写 coach.status 缓存失效测试
- [ ] **5.2 RED**：编写 audit_log 写入测试
- [ ] **5.3 GREEN**：实现缓存失效与审计日志
- [ ] **5.4 COMMIT**：`feat(us-040): cache invalidation and audit log`

**对应**：tech-design §6、§8

---

## 3. 测试用例映射

| # | 用例名称 | 对应 GWT 场景 | 测试方法 | 期望结果 |
|---|---------|--------------|---------|---------|
| 1 | 已离职教练发起重新入驻 | 6.1 | `test_reapply_success` | coach.status=0，生成 pending 申请 |
| 2 | 管理员通过重新入驻 | 6.2 | `test_approve_reapply_success` | coach.status=1，application approved |
| 3 | 非已离职教练无法重新入驻 | 6.3 | `test_not_resigned_cannot_reapply` | 入口隐藏或接口 403 |
| 4 | 重复发起重新入驻 | 6.4 | `test_reapply_duplicate` | HTTP 409，错误码 REAPPLY_ALREADY_PENDING |
| 5 | 重新入驻审核被拒绝 | 6.5 | `test_reject_reapply_success` | coach.status=3，application rejected |
| 6 | 并发发起幂等 | 8.1 | `test_reapply_idempotent` | 仅 1 条申请记录 |
| 7 | 老学员 frozen package 不自动恢复 | 8.2 | `test_frozen_package_not_auto_active` | package.status 仍为 frozen |
| 8 | 历史评分对新学员隐藏 | 8.3 | `test_rating_hidden_for_new_students` | 新学员主页无历史评分 |

---

## 4. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 状态机转换 100% 覆盖（3→0、0→1、0→3）
- [ ] 无 TBD/TODO 遗留
