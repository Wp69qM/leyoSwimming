# leyoSwimming 代码开发前置准备建议

> **文档定位**：设计稿完成后、进入 TDD 代码开发前的技术栈选型、目录结构与工程化建议。
> **目标读者**：后续接手的 AI Agent / 开发工程师。
> **生效状态**：待评审。

---

## 1. 项目背景与约束

- 业务：游泳私教预约平台，三端（学员小程序、教练小程序、Web 管理后台）。
- 设计稿：已完成 50 个用户故事对应的 Figma Page Spec（U/C/A 三套）。
- 当前代码目录：仅有 `cursor-talk-to-figma-mcp/`，需新建 `miniapp-user/`、`miniapp-coach/`、`web-admin/`、`backend/`、`shared/`、`deploy/`、`tools/`。
- 项目级硬约束：见根目录 `AGENTS.md` —— SDD 七步流水线、TDD 强制、`openspec validate` 通过前不得归档、不主动 push GitHub。

---

## 2. 总体技术架构

```
leyoSwimming/
├── miniapp-user/      Taro 4 + React 18 + TypeScript（学员端微信小程序）
├── miniapp-coach/     Taro 4 + React 18 + TypeScript（教练端微信小程序）
├── web-admin/         Vue 3 + TypeScript + Vite + Element Plus / Ant Design Vue（管理后台）
├── backend/           Spring Boot 3 + Java 21 + Maven + MyBatis-Plus / JPA
├── shared/            OpenAPI / TypeScript 类型 / 常量 / 工具（跨端共享）
├── deploy/            Docker Compose / K8s / CI/CD / Nginx 配置
└── tools/             脚本、代码生成器、数据迁移辅助工具
```

**通信模式**：
- 小程序 ↔ 后端：HTTPS + JWT（Bearer Token）。
- 后台 ↔ 后端：同源或 CORS + JWT + RBAC。
- 后端 ↔ 数据库：TCP + 连接池。
- 后端 ↔ 微信：服务端调用微信 API（code2session、统一支付、退款）。

---

## 3. 数据库选型：MySQL 8.0 + Redis 7

### 3.1 MySQL 8.0（主数据库）

**推荐理由**：
- 团队对关系型数据熟悉，Spring Boot + MyBatis-Plus 生态成熟。
- 业务强事务场景多（订单、套餐、预约、退款），需要 ACID。
- 复杂查询（教练排班、时段占用、套餐状态）适合 SQL 表达。
- 支持窗口函数、JSON 字段、CTE，便于后续报表。

**关键表（参考 `docs/stories/` 中的 tech-design）**：
- `users`：用户 / 学员 / 管理员基础信息。
- `coaches`：教练入驻、状态、参考单价。
- `packages`：套餐（标准 / 体验 / 赠送）。
- `orders`：订单与支付 / 退款状态。
- `bookings`：预约记录。
- `schedules`：教练可约时段。
- `announcements` / `banners` / `cards`：首页运营配置。
- `tickets`：客服工单。
- `bonus_grants`：赠送课时关联。

**索引原则**：
- 高频查询字段：`status`、`user_id`、`coach_id`、`expire_at`、`booking_date`。
- 复合索引覆盖列表页查询，如 `idx_coach_status_rating`。
- 避免在低选择性字段（如 `gender`）上单独建索引。

### 3.2 Redis 7（缓存 / 会话 / 锁）

**使用场景**：
- 短信验证码 / 登录 token 过期：String + TTL。
- 首页公告、Banner、卡片配置缓存：String / Hash。
- 预约时段锁定（防并发超卖）：Redisson 分布式锁。
- 接口限流：Redis + Bucket4j / Sentinel。
- 微信 access_token 缓存：String。

**缓存 Key 规范**：
```
leyo:{module}:{biz}:{id}
leyo:config:announcements
leyo:lock:booking:{schedule_id}
leyo:rate:api:{user_id}
```

### 3.3 不使用 MongoDB / PostgreSQL 的原因

- MongoDB：业务关系复杂，强一致性场景多，文档模型会增加心智负担。
- PostgreSQL：功能优秀，但团队若更熟悉 MySQL，且 Spring Boot + MyBatis-Plus 对 MySQL 支持更直接，可优先降低学习成本。

---

## 4. 前端：Taro React 小程序

### 4.1 技术栈

| 层级 | 选型 |
|------|------|
| 框架 | Taro 4.x |
| 视图 | React 18 + TypeScript |
| 状态管理 | Zustand（全局）+ React Query（服务端缓存） |
| 路由 | Taro 内置路由 |
| UI 组件 | 自研组件库（基于 Taro 基础组件）+ 少量 Taro UI |
| 网络请求 | axios adapter（Taro.request） |
| 表单 | React Hook Form |
| 测试 | Jest + React Testing Library + @tarojs/test-utils |
| 构建 | Taro CLI + Webpack5 / Vite |

### 4.2 目录结构（以 `miniapp-user` 为例）

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
│   ├── types/                 # 业务类型定义
│   └── utils/                 # 工具函数
├── config/
│   ├── dev.js
│   ├── prod.js
│   └── index.js
├── tests/
│   ├── unit/
│   └── e2e/
├── project.config.json
├── tsconfig.json
├── package.json
└── AGENT.md                   # 本目录 AI 协作规范
```

### 4.3 关键工程化配置

- **ESLint**：`eslint-config-taro` + `@typescript-eslint` + `react-hooks`。
- **Prettier**：统一格式化。
- **Stylelint**：SCSS 规范。
- **Husky + lint-staged**：提交前检查。
- **Commitizen**：约定式提交 `feat(scope): message`。
- **微信小程序 CI**：`miniprogram-ci` 自动上传预览版。

---

## 5. 前端：Vue3 管理后台

### 5.1 技术栈

| 层级 | 选型 |
|------|------|
| 框架 | Vue 3.4 + TypeScript |
| 构建 | Vite 5 |
| 状态管理 | Pinia |
| 路由 | vue-router 4 |
| UI 组件 | Element Plus 2.x 或 Ant Design Vue 4.x |
| 网络请求 | axios + @tanstack/vue-query |
| 表单 | vee-validate / FormKit |
| 表格 | 二次封装 ElTable + 搜索表单 |
| 测试 | Vitest + Vue Test Utils + Playwright（关键流程） |

### 5.2 目录结构

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
│   ├── types/
│   ├── utils/
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
├── vite.config.ts
├── tsconfig.json
├── package.json
└── AGENT.md
```

### 5.3 关键工程化配置

- **Vite 插件**：`unplugin-auto-import` + `unplugin-vue-components` 减少样板代码。
- **路由权限**：`meta.requiresAuth` + `meta.roles` + 全局 `beforeEach`。
- **Axios 拦截器**：统一处理 token、错误码、loading、toast。
- **持续集成**：GitHub Actions 跑 lint / test / build。

---

## 6. 后端：Spring Boot

### 6.1 技术栈

| 层级 | 选型 |
|------|------|
| 框架 | Spring Boot 3.2 + Spring Web |
| 语言 | Java 21（LTS，虚拟线程友好） |
| 构建 | Maven |
| ORM | MyBatis-Plus 3.5（复杂 SQL 可控） |
| 数据库连接 | HikariCP |
| 缓存 | Spring Data Redis + Redisson |
| 安全 | Spring Security + JWT |
| 校验 | Jakarta Validation |
| 文档 | SpringDoc OpenAPI（Knife4j 可选） |
| 任务调度 | Quartz / Spring Scheduler |
| 消息 | 初期直接使用 API 调用，必要时引入 RabbitMQ |
| 测试 | JUnit 5 + Mockito + Testcontainers（MySQL/Redis） |
| 日志 | SLF4J + Logback + MDC |
| 监控 | Spring Boot Actuator + Micrometer + Prometheus（后续） |

### 6.2 目录结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/leyoswimming/
│   │   │   ├── LeyoSwimmingApplication.java
│   │   │   ├── config/              # 配置类（Security、Redis、MyBatis）
│   │   │   ├── controller/          # REST API（薄层）
│   │   │   ├── service/             # 业务逻辑
│   │   │   ├── repository/          # 数据访问接口（MyBatis-Plus Mapper）
│   │   │   ├── entity/              # 数据库实体
│   │   │   ├── dto/                 # Request / Response DTO
│   │   │   ├── vo/                  # 视图对象
│   │   │   ├── mapper/              # MapStruct 转换
│   │   │   ├── enums/               # 状态枚举
│   │   │   ├── exception/           # 全局异常 + 错误码
│   │   │   ├── security/            # JWT / 认证 / 鉴权
│   │   │   ├── scheduler/           # 定时任务
│   │   │   └── util/                # 工具类
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── mapper/              # XML（复杂 SQL）
│   │       └── db/migration/        # Flyway / Liquibase 脚本
│   └── test/
│       ├── java/
│       └── resources/
├── Dockerfile
├── pom.xml
├── lombok.config
└── AGENT.md
```

### 6.3 后端设计原则

- **Controller 薄、Service 厚**：Controller 只做参数解析、调用 Service、返回 DTO。
- **统一响应体**：`ApiResponse<T>`，HTTP 状态码语义化。
- **错误码枚举**：按模块划分，如 `AUTH_001`、`BOOKING_003`。
- **参数校验**：`@Valid` + 分组校验。
- **幂等设计**：关键写接口（下单、退款、预约）使用幂等键 + 数据库唯一索引。
- **分布式锁**：Redis Redisson，用于预约锁定、库存扣减。
- **敏感信息**：日志脱敏、接口返回脱敏。

---

## 7. 共享层 shared

```
shared/
├── src/
│   ├── types/                 # TypeScript 类型（用户、教练、套餐、订单）
│   ├── constants/             # 错误码、枚举、配置键
│   ├── utils/                 # 纯函数工具（日期、金额、验证）
│   └── openapi/               # OpenAPI 生成产物
├── tsconfig.json
├── package.json
└── AGENT.md
```

**原则**：
- 只放纯逻辑，不放框架相关代码。
- 可被 `miniapp-user`、`miniapp-coach`、`web-admin` 引用。
- 后端 DTO 与前端类型通过 OpenAPI 生成保持同步。

---

## 8. 工程化与 DevOps 工具链

### 8.1  monorepo 管理

推荐 **Turborepo** 或 **Nx** 管理前端三件套 + shared：
- 统一缓存、任务编排、依赖图。
- 命令示例：
  ```bash
  pnpm dev          # 同时启动所有前端
  pnpm build        # 统一构建
  pnpm test         # 统一测试
  ```

若项目初期简单，也可用 **pnpm workspace** + 独立 `package.json`。

### 8.2 代码质量

| 工具 | 用途 |
|------|------|
| ESLint / Prettier | JS/TS/Vue/React 规范 |
| Stylelint | CSS/SCSS/Less |
| Spotless / Checkstyle | Java 规范 |
| Husky + lint-staged | 提交前校验 |
| Commitlint | 提交信息规范 |

### 8.3 CI/CD（GitHub Actions）

```yaml
name: CI
on: [push, pull_request]
jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v2
      - name: Install & Lint & Test
        run: |
          pnpm install
          pnpm lint
          pnpm test
      - name: Build Backend
        run: cd backend && ./mvnw test package
```

### 8.4 容器化

```
docker-compose.yml          # 本地开发环境（MySQL + Redis + Backend + Nginx）
backend/Dockerfile          # 多阶段构建 Spring Boot
web-admin/Dockerfile        # Nginx 托管
nginx.conf                  # 反向代理、静态资源缓存
```

---

## 9. 各代码目录 AGENT.md 写法建议

参考 ECC-main 的结构与当前项目 `AGENTS.md`，建议每个代码目录下放置 `AGENT.md`，内容与根目录规范互补，突出技术栈特殊要求。

### 9.1 通用模板

```markdown
# {目录名} AI 协作规范

> **适用范围**：`{path}/` 目录下所有代码。
> **上级规范**：[AGENTS.md](../../AGENTS.md)（根目录硬约束优先）。

## 1. 技术栈

- 框架：...
- 语言：...
- 状态管理：...
- 测试框架：...

## 2. 必须调用的 Skill

| 用户意图 | Skill |
|----------|-------|
| 写用户故事 | `openspec-new-change` |
| 实现 / 开发 / 编码 | `test-driven-development` |
| 修 bug / 测试失败 | `systematic-debugging` |
| 代码审查 | `requesting-code-review` |
| 新功能 | `brainstorming` → `openspec-new-change` |
| 复杂计划 | `writing-plans` |

## 3. 代码规范

- 文件大小：200-400 行典型，800 行上限。
- 函数大小：<50 行。
- 禁止深嵌套（>4 层）。
- 优先不可变更新。
- 输入在系统边界校验。

## 4. 测试要求

- 覆盖率 ≥ 80%。
- 单元 / 集成 / E2E 三类都要有。
- TDD：RED → GREEN → REFACTOR → COMMIT。

## 5. 安全红线

- 不硬编码密钥。
- 所有用户输入校验。
- 防止 SQL 注入 / XSS / CSRF。
- 错误信息不泄露内部细节。

## 6. 文件组织

```
{目录}/
├── src/...
├── tests/...
└── ...
```
```

### 9.2 各目录补充点

| 目录 | AGENT.md 补充重点 |
|------|-------------------|
| `miniapp-user/` | Taro 页面配置与文件路径映射、微信 API 调用规范、分包策略、设计稿 token 映射。 |
| `miniapp-coach/` | 教练端实时状态、排班/请假/代约特殊流程、TabBar 与权限。 |
| `web-admin/` | 管理后台 RBAC、表格/表单二次封装、路由权限守卫、后台接口分页规范。 |
| `backend/` | Java 分层规范、MyBatis-Plus Mapper 位置、DTO/VO/Entity 转换、Redisson 锁使用规范、Flyway 迁移脚本命名。 |
| `shared/` | 禁止引入 UI 框架 / 后端框架依赖；保持纯 TypeScript；变更后必须同步到前端与后端 DTO。 |
| `deploy/` | 环境变量管理、密钥不上传、Docker Compose 本地调试、Nginx 配置规范。 |
| `tools/` | 脚本语言统一（Node.js / Python）、输入参数校验、不直接操作生产数据库。 |

---

## 10. 推荐优先复用的 Skill

当前项目 `.trae/skills/` 已提供以下关键 skill，进入开发阶段后必须按 `AGENTS.md` 触发规则调用：

| Skill | 触发场景 |
|-------|----------|
| `test-driven-development` | 任何功能 / Bugfix 编码前 |
| `systematic-debugging` | 测试失败、运行时异常 |
| `writing-plans` | 复杂功能、跨端改动、重构 |
| `requesting-code-review` | 完成重要功能后 |
| `subagent-driven-development` | 计划较大时，按 task 分派子 agent |
| `verification-before-completion` | 声称完成前 |
| `using-git-worktrees` | 多 US 并行开发隔离 |

从 ECC-main 可借鉴引入的 skill 方向（若后续需要自建 `.trae/skills/`）：

| 方向 | 说明 |
|------|------|
| `api-design` | REST 规范、分页、错误响应、限流 |
| `frontend-patterns` | React / Vue 通用模式 |
| `springboot-patterns` | Java 分层、事务、锁、测试 |
| `jpa-patterns` | 实体设计、索引、查询优化 |
| `ui-to-vue` | 若后续需要将 Figma 设计稿转 Vue 代码 |
| `motion-ui` | 交互动效实现 |
| `browser-qa` | 管理后台 E2E 验证 |

---

## 11. 从 ECC-main 借鉴的最佳实践

ECC-main（同层目录）是面向多 AI 工具协作的脚手架项目，以下实践可直接迁移：

1. **Agent-First**：复杂任务优先分派给专业 agent（planner / architect / tdd-guide / code-reviewer），与本项目 `AGENTS.md` 的 skill 触发规则一致。
2. **TDD 强制**：写测试 → 看失败 → 最小实现 → 通过 → 重构 → 80% 覆盖率。
3. **不可变更新**：禁止直接修改共享状态，返回新对象。
4. **小文件原则**：200-400 行为宜，800 行上限；按领域而非技术层组织。
5. **安全 checklist**：提交前必须检查无硬编码密钥、输入校验、SQL 注入、XSS、CSRF、限流、错误脱敏。
6. **API 响应统一 envelope**：`{ data, meta, error }`，HTTP 状态码语义化。
7. **Repository 模式**：数据访问抽象，业务逻辑不依赖存储细节。
8. **Conventional Commits**：`feat(scope): message`。
9. **Skills 优先于 Commands**：新的工作流优先放到 `.trae/skills/`，而非新增命令。
10. **多语言 rules 目录**：按技术栈放置 `rules/react/`、`rules/vue/`、`rules/java/` 等持续规范文件，可被对应 IDE 读取。

---

## 12. 实施路径建议

按依赖顺序，分阶段搭建：

### Phase 1：基础设施（1-2 天）

1. 创建 `miniapp-user/`、`miniapp-coach/`、`web-admin/`、`backend/`、`shared/`、`deploy/`、`tools/` 目录。
2. 为每个目录初始化项目脚手架并提交初始 `AGENT.md`。
3. 根目录添加 `pnpm-workspace.yaml` 或 Turborepo 配置。
4. 配置 `docker-compose.yml`（MySQL 8 + Redis 7）。
5. 配置 GitHub Actions 基础 CI（lint + test）。

### Phase 2：共享层 + 后端基础（2-3 天）

1. `shared/` 定义核心类型与常量。
2. `backend/` 搭建 Spring Boot 骨架：全局异常、统一响应、JWT 认证、MyBatis-Plus、Redis、Flyway。
3. 实现第一个 US 的完整后端（建议 US-001 游客浏览教练列表与详情）。
4. 写后端单元 + 集成测试，确保 TDD 流程跑通。

### Phase 3：前端基础（2-3 天）

1. `miniapp-user/` 初始化 Taro React，配置设计 token、TabBar、网络请求封装。
2. `web-admin/` 初始化 Vue3，配置路由、布局、请求封装。
3. 实现 US-001 对应的小程序页面 + 管理后台页面。
4. 配置前端测试框架并写出首批测试。

### Phase 4：按 US 批次开发

遵循 `AGENTS.md` 的 SDD + TDD 流水线，每 US 按 `tech-design.md` → `test-plan.md` → 代码 → 自测 → 代码审查 → 本地 commit 的流程执行。

---

## 13. 自检清单

在进入具体 US 开发前，确认以下事项：

- [ ] 7 个代码目录已创建并初始化。
- [ ] 每个代码目录下有 `AGENT.md`。
- [ ] `docker-compose.yml` 可正常启动 MySQL + Redis。
- [ ] 后端骨架可运行，统一响应、JWT、异常处理已就位。
- [ ] 前端脚手架可运行，设计 token 已与 Figma 对齐。
- [ ] `shared/` 类型可被前后端引用或生成。
- [ ] CI 流水线 lint / test 通过。
- [ ] 第一个 US 的后端 + 前端已走通 TDD 全流程。

---

## 14. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-08-03 | AI Agent | 初稿：技术栈、目录结构、工程化、AGENT.md 建议 |
