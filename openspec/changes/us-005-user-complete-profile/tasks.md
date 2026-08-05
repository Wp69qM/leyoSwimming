> **OpenSpec Tasks | 映射自 `docs/stories/US-005-用户-完善个人资料/test-plan.md`**

## Task 1 资料完善接口基础结构

- RED: 测试 `PUT /api/user/profile` 未登录返回 401；已登录提交完整必填资料返回 200 且 `profile_completed=true`
- GREEN: 实现 controller skeleton、auth middleware、route
- COMMIT: `feat(user): add PUT /api/user/profile endpoint skeleton`

## Task 2 必填项校验与监护人信息联动

- RED: 测试缺失头像/姓名/手机号/年龄/性别时返回 400；字段格式错误返回对应错误码；age < 18 时缺失 guardian_name / guardian_phone 返回 400；age ≥ 18 时提交 guardian 字段被忽略
- GREEN: 实现字段级校验（头像 URL、姓名长度、手机号格式、年龄范围、性别枚举、监护人信息条件必填）
- COMMIT: `feat(user): add profile field validation including guardian conditional fields`

## Task 3 手机号唯一性校验

- RED: 测试手机号已被其他用户绑定返回 `PHONE_ALREADY_BOUND`
- GREEN: 实现 `UserRepository.findByPhone` 并集成
- COMMIT: `feat(user): add phone uniqueness check for profile`

## Task 4 姓名敏感词校验

- RED: 测试敏感姓名返回 `SENSITIVE_NAME`
- GREEN: 实现敏感词过滤服务
- COMMIT: `feat(user): add name sensitive word filter`

## Task 5 头像上传

- RED: 测试合规图片上传返回 `avatar_url`；非法文件返回 `INVALID_AVATAR`
- GREEN: 实现 `POST /api/upload/avatar`
- COMMIT: `feat(user): add avatar upload endpoint`

## Task 6 资料完成状态转换

- RED: 测试首次完善后 `profile_completed` 从 false 变为 true；编辑资料时保持 true
- GREEN: 实现状态转换逻辑
- COMMIT: `feat(user): implement profile_completed state transition`

## Task 7 游泳基础联动与选填字段

- RED: 测试 `has_swim_basis=false` 时忽略 `swim_strokes` / `swim_years`；`has_swim_basis=true` 时允许保存
- GREEN: 实现联动校验与存储
- COMMIT: `feat(user): add swim basis conditional fields`

## Task 8 幂等性处理

- RED: 测试相同 Idempotency-Key 重复提交返回首次结果
- GREEN: 实现 Redis 幂等键
- COMMIT: `feat(user): add idempotency for profile update`

## Task 9 小程序完善个人资料页（首次完善入口）

- RED: E2E 测试微信授权登录后 `profile_completed=false` 强制跳转资料页；必填项缺失阻止提交；保存成功后跳转首页；未成年人（age < 18）时动态展示监护人信息区且未填写阻止提交
- GREEN: 实现 `miniapp-user/src/pages/complete-profile/index.tsx`，含年龄与监护人信息联动
- COMMIT: `feat(miniapp): add complete profile page with guardian conditional fields`

## Task 10 小程序「我的 → 编辑资料」入口

- RED: E2E 测试从「我的」进入编辑资料页回显当前内容；修改后保存返回「我的」；`profile_completed` 保持 true；年龄从成年人改为未成年人时展示监护人信息区，反之隐藏
- GREEN: 实现「我的」页面编辑入口与资料页编辑模式
- COMMIT: `feat(miniapp): add edit profile entry from mine page with guardian fields`

---

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 | Task 6 | Task 7 | Task 8 | Task 9 | Task 10 |
|---------|--------|--------|--------|--------|--------|--------|--------|--------|--------|---------|
| §6.1 成年人首次完善资料 | ✅ | ✅ | — | — | ✅ | ✅ | — | ✅ | ✅ | — |
| §6.2 未成年人首次完善资料并填写监护人 | ✅ | ✅ | — | — | ✅ | ✅ | — | ✅ | ✅ | — |
| §6.3 从「我的」编辑资料 | ✅ | ✅ | — | — | — | ✅ | — | ✅ | — | ✅ |
| §6.4 手机号已被其他账号绑定 | ✅ | — | ✅ | — | — | — | — | — | — | — |
| §6.5 必填项缺失 | — | ✅ | — | — | — | — | — | — | ✅ | ✅ |
| §6.6 姓名含敏感词 | — | — | — | ✅ | — | — | — | — | ✅ | ✅ |
| §6.7 未成年人未填写监护人信息 | — | ✅ | — | — | — | — | — | — | ✅ | ✅ |
| §8.5 年龄从成年人改为未成年人 | — | ✅ | — | — | — | — | — | — | — | ✅ |
| §8.6 年龄从未成年人改为成年人 | — | ✅ | — | — | — | — | — | — | — | ✅ |
