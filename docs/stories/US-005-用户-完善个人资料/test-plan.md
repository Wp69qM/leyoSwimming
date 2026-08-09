> **状态**：[REVIEW]
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-04

---

## 1. 测试目标

验证用户完善个人资料功能：
- 首次登录后（微信授权 / 手机号验证码）强制完善资料，必填项校验正确
- 「我的 → 编辑资料」可修改任意字段
- 手机号唯一性、姓名敏感词、头像上传等边界场景处理正确
- `profileCompleted` 状态转换正确（首次 false → true，编辑保持 true）
- 游泳基础联动显示正确

---

## 2. 测试范围

- **后端**：`POST /api/user/profile/update`、`POST /api/common/file/upload`
- **前端**：完善个人资料页（首次完善入口 + 「我的 → 编辑资料」入口）
- **状态机**：`profileCompleted: false → true`

---

## 3. TDD 任务清单

### Task 1：资料完善接口基础结构 [P0]

- **RED**：编写测试 — `POST /api/user/profile/update` 未登录返回 401；已登录提交完整必填资料返回 200 且 `profileCompleted=true`
- **GREEN**：实现 controller skeleton、auth middleware、route
- **COMMIT**：`feat(user): add POST /api/user/profile/update endpoint skeleton`

### Task 2：必填项校验 [P0]

- **RED**：编写测试 — 缺失头像/姓名/手机号/年龄/性别时返回 400；未成年人（age < 18）缺失监护人姓名/手机号时返回 400；字段格式错误时返回对应错误码
- **GREEN**：实现字段级校验（头像 URL、姓名长度、手机号格式、年龄范围、性别枚举、监护人字段条件必填）
- **COMMIT**：`feat(user): add profile field validation`

### Task 3：手机号唯一性校验 [P0]

- **RED**：编写测试 — 手机号已被其他用户绑定返回 `PHONE_ALREADY_BOUND`；未绑定允许保存
- **GREEN**：实现 `UserRepository.findByPhone` 并集成
- **COMMIT**：`feat(user): add phone uniqueness check for profile`

### Task 4：姓名敏感词校验 [P0]

- **RED**：编写测试 — 敏感姓名返回 `SENSITIVE_NAME`；正常姓名通过
- **GREEN**：实现敏感词过滤服务
- **COMMIT**：`feat(user): add name sensitive word filter`

### Task 5：头像上传 [P0]

- **RED**：编写测试 — 合规图片上传返回 `avatarUrl`；超大/非法格式图片返回 `INVALID_AVATAR`
- **GREEN**：实现 `POST /api/common/file/upload`
- **COMMIT**：`feat(common): add file upload endpoint`

### Task 6：资料完成状态转换 [P0]

- **RED**：编写测试 — 首次完善后 `profileCompleted` 从 false 变为 true；编辑资料时保持 true
- **GREEN**：实现状态转换逻辑
- **COMMIT**：`feat(user): implement profileCompleted state transition`

### Task 7：游泳基础联动与选填字段 [P1]

- **RED**：编写测试 — `hasSwimBasis=false` 时忽略 `swimStrokes` / `swimYears`；`hasSwimBasis=true` 时允许保存
- **GREEN**：实现联动校验与存储
- **COMMIT**：`feat(user): add swim basis conditional fields`

### Task 8：幂等性处理 [P0]

- **RED**：编写测试 — 相同 Idempotency-Key 重复提交返回首次结果
- **GREEN**：实现 Redis 幂等键
- **COMMIT**：`feat(user): add idempotency for profile update`

### Task 9：小程序完善个人资料页（首次完善入口）[P1]

- **RED**：编写 E2E 测试 — 微信授权登录后 `profileCompleted=false` 强制跳转资料页；必填项缺失阻止提交；保存成功后跳转首页
- **GREEN**：实现 `miniapp-user/src/pages/complete-profile/index.tsx`
- **COMMIT**：`feat(miniapp): add complete profile page for first login`

### Task 10：小程序「我的 → 编辑资料」入口 [P1]

- **RED**：编写 E2E 测试 — 从「我的」进入编辑资料页回显当前内容；修改后保存返回「我的」；`profileCompleted` 保持 true
- **GREEN**：实现「我的」页面编辑入口与资料页编辑模式
- **COMMIT**：`feat(miniapp): add edit profile entry from mine page`

---

## 4. 测试用例映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 首次完善个人资料 | `test_complete_profile_success` | 集成 |
| 从「我的」编辑资料 | `test_edit_profile_from_mine` | E2E |
| 手机号已被其他账号绑定 | `test_phone_already_bound` | 集成 |
| 必填项缺失 | `test_required_fields_missing` | E2E |
| 姓名含敏感词 | `test_sensitive_name_rejected` | 集成 |
| 头像上传成功/失败 | `test_avatar_upload` | 集成 |
| 游泳基础为无时隐藏泳姿和年限 | `test_swim_basis_hidden_fields` | E2E |
| 游泳基础为无时提交泳姿被忽略 | `test_swim_basis_false_ignores_strokes` | 单元/集成 |
| 未成年人填写监护人信息 | `test_minor_guardian_fields` | 集成 |
| 未成年人未填写监护人信息被阻止 | `test_minor_missing_guardian_blocked` | E2E |
| 快速重复提交 | `test_idempotent_profile_update` | 集成 |
| 网络中断 | `test_network_error_retry` | E2E |

---

## 5. Mock / 样本数据

- **微信授权登录**：Mock `wx.login()` 与 `wx.getPhoneNumber()`，返回测试用 `code`、`encryptedData`、`iv`
- **手机号验证码登录**：Mock 短信验证码为固定值 `123456`
- **头像上传**：Mock 对象存储返回固定 `avatarUrl`
- **敏感词库样本**：`["测试敏感姓名"]`
- **错误码断言表**：
  | 错误场景 | 错误码 | HTTP 状态 |
  |---------|-------|----------|
  | 必填项缺失 | `MISSING_REQUIRED_FIELD` | 400 |
  | 手机号格式非法 | `INVALID_PHONE` | 400 |
  | 手机号已被绑定 | `PHONE_ALREADY_BOUND` | 400 |
  | 姓名含敏感词 | `SENSITIVE_NAME` | 400 |
  | 年龄超出范围 | `INVALID_AGE` | 400 |
  | 头像上传失败 | `INVALID_AVATAR` | 400 |
  | 幂等键已使用但请求体不一致 | `IDEMPOTENCY_REUSED` | 409 |

---

## 6. 验收执行检查单

- [ ] 单元测试全部通过
- [ ] 集成测试全部通过
- [ ] 所有 GWT 场景均有对应自动化测试
- [ ] 覆盖率：service 层 ≥ 80%，validator 层 100%
- [ ] 性能测试：资料完善接口 P99 < 300ms
- [ ] 无 TBD/TODO 遗留