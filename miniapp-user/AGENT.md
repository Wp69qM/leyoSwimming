# miniapp-user AI 协作规范

> **适用范围**：`miniapp-user/` 目录下所有代码。  
> **上级规范**：[AGENTS.md](../AGENTS.md)（根目录硬约束优先）。  
> **技术方案**：[docs/tech/frontend-backend-setup-guide.md](../docs/tech/frontend-backend-setup-guide.md)。  
> **多 Agent 规范**：[docs/tech/multi-agent-dev-guide.md](../docs/tech/multi-agent-dev-guide.md)。

---

## 1. 技术栈

- 框架：Taro 4.x
- 视图：React 18 + TypeScript
- 状态管理：Zustand（全局）+ React Query（服务端缓存）
- 路由：Taro 内置路由
- UI 组件：自研组件库 + 少量 Taro UI
- 网络请求：axios adapter（Taro.request）
- 表单：React Hook Form
- 测试：Jest + React Testing Library + @tarojs/test-utils
- 构建：Taro CLI + Webpack5 / Vite

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
- 页面文件路径与 `app.config.ts` 中页面路径一致。
- 禁止直接引用 `miniapp-coach/`、`web-admin/`、`backend/` 代码。
- 图片/图标必须放入 `src/assets/`，禁止外链 CDN（除非设计稿指定）。

---

## 4. 测试要求

- 覆盖率 ≥ 80%。
- 单元 / 集成 / E2E 三类都要有。
- TDD：RED → GREEN → REFACTOR → COMMIT。
- 每个页面至少覆盖：加载态、空态、错误态、成功态。

---

## 5. 安全红线

- 不硬编码密钥。
- 所有用户输入校验。
- 微信小程序 `appid` / `appsecret` 只通过构建时环境变量注入。
- 错误信息不泄露内部细节。
- JWT token 存储在 `Taro.getStorageSync` 中，注意过期处理。

---

## 6. 文件组织

```
miniapp-user/
├── src/
│   ├── app.config.ts          # 全局页面配置、TabBar
│   ├── app.tsx                # 应用入口
│   ├── app.scss               # 全局样式、设计 token
│   ├── api/                   # API 请求封装（按领域拆分）
│   │   ├── auth.ts
│   │   ├── coach.ts
│   │   ├── package.ts
│   │   └── booking.ts
│   ├── assets/                # 图片、图标、字体
│   ├── components/            # 通用组件
│   │   ├── common/            # Button、Input、Empty、Skeleton
│   │   └── business/          # CoachCard、PackageCard、TimeSlotGrid
│   ├── constants/             # 枚举、错误码、配置
│   ├── hooks/                 # 自定义 Hooks
│   ├── pages/                 # 页面（与 app.config 一一对应）
│   │   ├── home/
│   │   ├── coach-list/
│   │   ├── coach-detail/
│   │   ├── coach-schedule/
│   │   └── ...
│   ├── stores/                # Zustand stores
│   ├── styles/                # 变量、mixins、主题
│   ├── types/                 # 业务类型定义（优先引用 shared）
│   └── utils/                 # 工具函数（优先引用 shared）
├── tests/
│   ├── unit/
│   └── e2e/
├── config/
│   ├── dev.js                 # 开发服务器代理到 backend localhost:8080
│   ├── prod.js
│   └── index.js
├── project.config.json
├── tsconfig.json
├── package.json
└── AGENT.md                   # 本文件
```
