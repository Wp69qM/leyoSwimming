> **OpenSpec Tasks | 映射自 `docs/stories/US-009-用户-隐私协议与用户须知授权/test-plan.md`**

## Task 1: 当前协议查询接口 [P0]

- RED: 测试存在当前《用户须知》返回版本内容；存在当前《隐私协议》返回版本内容；无当前协议返回 404
- GREEN: 实现 `POST /api/common/terms/current` 与 `POST /api/common/privacy/current`
- COMMIT: `feat(terms-privacy): add current terms and privacy policy endpoints`

## Task 2: 用户协议同意状态查询 [P0]

- RED: 测试游客访问返回 401；未同意返回 `status='none'`；已同意返回 `status='agreed'` 与版本号
- GREEN: 实现 `POST /api/user/terms/status` 与 `POST /api/user/privacy/status`
- COMMIT: `feat(terms-privacy): add user terms and privacy status endpoints`

## Task 3: 同意协议记录接口 [P0]

- RED: 测试游客调用返回 401；同意当前版本返回 200 并写入记录；非当前版本返回 `VERSION_MISMATCH`
- GREEN: 实现 `POST /api/user/terms/consent` 与 `POST /api/user/privacy/consent`
- COMMIT: `feat(terms-privacy): add terms and privacy consent agreement endpoints`

## Task 4: 登录页协议浮层弹窗 [P0]

- RED: E2E 测试点击勾选框仅切换勾选状态且不唤起浮层；点击「我已阅读并同意」这几个字仅切换勾选状态且不唤起浮层；点击「《用户须知》」/「《隐私协议》」协议名唤起浮层；切换 Tab；《用户须知》默认展示；点击「同意」后勾选框变为已勾选；点击「不同意」后勾选框变为未勾选；点击浮层外部关闭浮层且状态不变
- GREEN: 实现登录页协议勾选区与浮层弹窗组件，精确区分勾选框/外层文字点击与协议名点击，双 Tab 展示协议内容
- COMMIT: `feat(miniapp): add terms/privacy popup on login page with precise tap areas`

## Task 5: 登录后协议查看页 [P1]

- RED: E2E 测试进入「我的 → 设置 → 用户须知」展示内容且无撤回按钮；进入「我的 → 设置 → 隐私协议」展示内容且无撤回按钮
- GREEN: 实现设置页用户须知/隐私协议查看页面
- COMMIT: `feat(miniapp): add terms and privacy view pages in settings`

## Execution Discipline

- 严格顺序：Task 1 → 2 → 3 → 4 → 5
- 每 Task = 1 commit
- 禁止 placeholder（TBD / TODO / "实现 later"）

## GWT Coverage Matrix

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 |
|---------|--------|--------|--------|--------|--------|
| §6.1 登录页同意协议后成功登录 | — | ✅ | ✅ | ✅ | — |
| §6.2 登录页不同意协议导致无法登录 | — | — | — | ✅ | — |
| §6.3 登录后查看用户须知和隐私协议 | ✅ | — | — | — | ✅ |
| §6.4 教练端登录页同意协议后进入教练端 | — | ✅ | ✅ | ✅ | — |
| §6.5 点击勾选框或外层文字直接切换勾选状态 | — | — | — | ✅ | — |
| §6.6 点击浮层外部关闭浮层且状态不变 | — | — | — | ✅ | — |
