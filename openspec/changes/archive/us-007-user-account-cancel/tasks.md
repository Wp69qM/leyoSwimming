> **OpenSpec Tasks | 映射自 `docs/stories/US-007-用户-账号注销/user-story.md` §? 与配套 test-plan.md**

## Task 1: 注销条件查询接口 [P0]

- RED: 测试 `GET /api/user/account/cancel/check`：无 active 套餐/订单/预约时返回 `can_cancel=true`；存在 active 套餐时返回 `can_cancel=false` 与 `reasons=['active_package']`；组合条件时返回全部原因
- GREEN: 实现查询服务，聚合 `package_purchase`、`order`、`booking` 状态
- COMMIT: `feat(user): add account cancel check api`

## Task 2: 注销执行接口 [P0]

- RED: 测试 `POST /api/user/account/cancel`：条件满足时返回成功，`user.status=1`，`deleted_at` 有值，会话被清除，审计日志写入；条件不满足返回对应 409 错误码
- GREEN: 实现注销执行服务，含条件二次校验、软删除、会话清除、日志写入
- COMMIT: `feat(user): add account cancel execution api`

## Task 3: 小程序注销确认页 [P1]

- RED: E2E 测试进入「我的 → 注销账号」展示风险提示与 checklist；条件不满足时按钮置灰；点击「确认注销」弹出二次确认弹窗；点击弹窗「确认」后跳转登录页；点击「取消」关闭弹窗
- GREEN: 实现 `miniapp-user/src/pages/account-cancel/index.tsx`，含风险提示卡片、条件 checklist、二次确认弹窗
- COMMIT: `feat(miniapp): add account cancel page with risk disclosure and checklist`

## Task 4: 幂等与并发控制 [P1]

- RED: 测试快速重复点击「确认注销」、并发请求场景下只执行一次注销，第二次返回已注销结果
- GREEN: 实现 `cancel:{user_id}:{date}` 幂等键与分布式锁
- COMMIT: `feat(user): add idempotency and concurrency control for account cancel`

---

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）
- P0 必做（Task 1-2），P1 选做（Task 3-4）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 |
|---------|--------|--------|--------|--------|
| §6.1 正常注销 | ✅ | ✅ | ✅ | ✅ |
| §6.2 存在 active 套餐 | ✅ | — | ✅ | — |
| §6.3 存在未完成订单 | ✅ | — | ✅ | — |
| §6.4 存在进行中预约 | ✅ | — | ✅ | — |
| §6.5 用户取消二次确认 | — | — | ✅ | — |
