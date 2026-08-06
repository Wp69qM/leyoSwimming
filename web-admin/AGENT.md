# web-admin AI 协作规范

> **适用范围**：`web-admin/` 目录下所有代码。  
> **上级规范**：[AGENTS.md](../AGENTS.md)（根目录硬约束优先）。  
> **技术方案**：[docs/tech/frontend-backend-setup-guide.md](../docs/tech/frontend-backend-setup-guide.md)。  
> **多 Agent 规范**：[docs/tech/multi-agent-dev-guide.md](../docs/tech/multi-agent-dev-guide.md)。

---

## 1. 技术栈

- 框架：Vue 3.4 + TypeScript
- 构建：Vite 5
- 状态管理：Pinia
- 路由：vue-router 4
- UI 组件：Element Plus 2.x 或 Ant Design Vue 4.x
- 网络请求：axios + @tanstack/vue-query
- 表单：vee-validate / FormKit
- 表格：二次封装 ElTable + 搜索表单
- 测试：Vitest + Vue Test Utils + Playwright（关键流程）

---

## 2. 必须调用的 Skill

| 用户意图 | Skill |
|----------|-------|
| 写用户故事 | `openspec-new-change` |
| 实现 / 开发 / 编码 | `test-driven-development` |
| 修 bug / 测试失败 | `systematic-debugging` |
| 代码审查 | `requesting-code-review` |
| 新功能 | `brainstorming` → `openspec-new-change` |
| 复杂计划 / 跨端改动 | `writing-plans` |
| 多 US 并行开发隔离 | `using-git-worktrees` |

---

## 3. 代码规范

- 文件大小：200-400 行典型，800 行上限。
- 函数大小：<50 行。
- 禁止深嵌套（>4 层）。
- 优先不可变更新。
- 输入在系统边界校验。
- 禁止直接引用 `miniapp-user/`、`miniapp-coach/`、`backend/` 代码。
- 路由权限使用 `meta.requiresAuth` + `meta.roles` + 全局 `beforeEach`。
- 表格/表单必须二次封装，保持统一交互。

---

## 4. 测试要求

- 覆盖率 ≥ 80%。
- 单元 / 集成 / E2E 三类都要有。
- TDD：RED → GREEN → REFACTOR → COMMIT。
- 关键管理流程（登录、审核、退款、离职）必须有 Playwright E2E。

---

## 5. 安全红线

- 不硬编码密钥。
- 所有用户输入校验。
- 管理员 token 使用 HttpOnly Cookie 或安全存储。
- 错误信息不泄露内部细节。
- 路由守卫必须校验 RBAC 权限。
- 敏感操作（审核通过/拒绝、退款、离职）需要二次确认。

---

## 6. 文件组织

```
web-admin/
├── public/
├── src/
│   ├── App.vue
│   ├── main.ts
│   ├── api/                   # 按领域拆分的 API 模块
│   ├── assets/
│   ├── components/
│   │   ├── common/            # 通用 UI 组件
│   │   └── business/          # 业务组件（如 RefundStatusTag）
│   ├── composables/           # 可复用逻辑
│   ├── directives/
│   ├── layouts/               # Layout、Sidebar、Header
│   ├── router/                # 路由 + 权限守卫
│   ├── stores/                # Pinia modules
│   ├── styles/                # 主题变量、tailwind / unocss
│   ├── types/                 # 优先引用 shared
│   ├── utils/                 # 优先引用 shared
│   └── views/                 # 页面（与 router 对应）
│       ├── login/
│       ├── dashboard/
│       ├── coach/
│       ├── order/
│       └── ...
├── tests/
│   ├── unit/
│   └── e2e/
├── .env / .env.production
├── vite.config.ts             # /api 代理到 backend localhost:8080
├── tsconfig.json
├── package.json
└── AGENT.md
```
