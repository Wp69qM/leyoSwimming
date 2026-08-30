# leyoSwimming 多 Agent 协同开发规范

> **文档定位**：第一批功能（登录注册相关页面）进入多 Agent 并行开发前的协作标准与执行手册。  
> **目标读者**：主 Agent、各端 Sub-Agent、后续接手的开发工程师。  
> **配套文档**：
> - [frontend-backend-setup-guide.md](./frontend-backend-setup-guide.md) — 技术栈与目录结构
> - [AGENTS.md](../../AGENTS.md) — 项目级硬约束（SDD / TDD / OpenSpec / Git 规范）
> - `docs/stories/US-*/` — 用户故事、技术设计、测试计划
> - `docs/figma/page-spec/` — 页面规格与设计稿

---

## 1. 多 Agent 组织架构

本批次开发采用「1 主 Agent + N 个专业 Sub-Agent」模式。

| Agent | 职责 | 工作目录 | 负责 US 范围（示例） |
|-------|------|----------|---------------------|
| **主 Agent（Master）** | 任务拆解、API 契约管理、跨端协调、冲突解决、每日联调组织、最终验收 | 全仓库 | 全部 |
| **用户端 Agent** | `miniapp-user/` 的页面、样式、交互、与后端接口联调 | `miniapp-user/` | US-004、US-005、US-006、US-052 等 |
| **教练端 Agent** | `miniapp-coach/` 的页面、样式、交互、与后端接口联调 | `miniapp-coach/` | US-009、US-010、US-051、US-054 等 |
| **Web 后台 Agent** | `web-admin/` 的页面、样式、RBAC、与后端接口联调 | `web-admin/` | US-053 等 |
| **后端 Agent** | `backend/` 的 API、业务逻辑、数据库、单元/集成测试、OpenAPI 文档 | `backend/` | 全部登录注册相关 US |

> **红线**：Sub-Agent 只修改自己负责目录下的文件；跨目录引用必须通过 `shared/` 或 backend API。

---

## 2. 协作流程（API-First）

```
Day 0：主 Agent 发布本批次页面清单 + API 契约草案
   ↓
Day 1：后端 Agent 产出正式 OpenAPI 文档 → 主 Agent 评审并冻结 v1
   ↓
Day 2~N：四端 Agent 并行开发
   - 后端 Agent：写测试 → 实现 API → 本地自测 → 更新 OpenAPI
   - 前端 Agent：按 OpenAPI 写 Mock → 实现页面 → 联调真实 API
   ↓
每日 17:00：主 Agent 组织联调会议（异步/同步均可）
   - 检查当日 API 变更
   - 解决字段/路径/状态码不一致
   - 更新 API 契约版本
   ↓
联调通过：主 Agent 运行全端验收检查单
   ↓
本地 commit（不 push）
```

### 2.1 API 契约版本管理

- 所有 API 文档统一放在 `shared/openapi/`。
- 后端 Agent 每完成一个 API，必须更新 `shared/openapi/leyo-swimming-v1.yaml`。
- 变更 API 时必须升级版本号：`v1.0.0` → `v1.0.1`，并在 `shared/openapi/CHANGELOG.md` 中记录。
- 前端 Agent 只消费已冻结版本的 OpenAPI，禁止直接按自己理解调用未文档化的接口。

### 2.2 Mock 机制

- 后端 API 未就绪前，前端 Agent 使用 `shared/openapi/leyo-swimming-v1.yaml` 生成 Mock Server（推荐 `msw` 或 `prism`）。
- Mock 数据必须覆盖：成功、失败、加载、空态四种状态。
- 后端 API 就绪后，前端 Agent 切换为真实后端地址，保留 Mock 开关便于本地开发。

### 2.3 跨 Agent 通信协议

- 所有跨 Agent 问题必须在 `docs/tech/dev-log/` 下以日期命名创建日志，例如 `2026-08-05-login-batch.md`。
- 每个 Sub-Agent 每日结束时向主 Agent 汇报：
  - 已完成 Task
  - 阻塞点
  - 需要的 API/设计稿/字段
  - 预计明日完成 Task

---

## 3. 开发纪律

### 3.1 强制遵循 AGENTS.md

- **TDD**：任何功能/修复编码前必须先调用 `test-driven-development` skill。
- **调试**：测试失败/运行异常时先调用 `systematic-debugging` skill。
- **代码审查**：完成重要功能后调用 `requesting-code-review` skill。
- **计划**：跨端改动/复杂功能前调用 `writing-plans` skill。
- **Git**：不主动 push GitHub，仅本地 commit；每 Task 1 commit。

### 3.2 代码目录边界

| 目录 | 允许内容 | 禁止内容 |
|------|---------|---------|
| `miniapp-user/` | 学员端页面、组件、API 封装、状态管理 | 直接调用教练端/后台代码；引入后端框架依赖 |
| `miniapp-coach/` | 教练端页面、组件、API 封装、状态管理 | 直接调用用户端/后台代码；引入后端框架依赖 |
| `web-admin/` | 管理后台页面、组件、API 封装、RBAC | 直接调用小程序代码；引入小程序框架依赖 |
| `backend/` | REST API、业务逻辑、数据访问、测试 | 直接引用前端代码；在 service 层写 UI 逻辑 |
| `shared/` | TypeScript 类型、常量、纯工具函数、OpenAPI | 引入 UI/后端框架依赖 |
| `deploy/` | Docker、Nginx、CI/CD、环境变量模板 | 生产密钥 |
| `tools/` | 脚本、生成器、迁移辅助 | 直接操作生产数据库 |

### 3.3 提交规范

- 每个 Task 结束后必须 commit。
- 提交信息格式：`feat(scope): message`，例如：
  - `feat(miniapp-user/auth): add wechat login page`
  - `feat(backend/auth): implement POST /api/v1/auth/wechat-login`
  - `test(backend/auth): add wechat login integration tests`
- 禁止在 commit 中提交 `.env`、密钥、node_modules、构建产物。

---

## 4. 第一批开发范围（登录注册相关）

本批次聚焦「身份认证与授权」能力，涉及的 US 如下：

### 4.1 用户端（miniapp-user）

| US | 页面 | 说明 |
|----|------|------|
| US-004 | U-微信授权登录页 | 微信一键登录、协议勾选、手机号授权 |
| US-005 | U-完善个人资料页 | 昵称、头像、性别、泳龄等 |
| US-006 | U-手机号验证码登录页 | 短信验证码登录 |
| US-052 | U-退出登录入口 | 我的页面退出 |

### 4.2 教练端（miniapp-coach）

| US | 页面 | 说明 |
|----|------|------|
| US-009 | C-隐私协议与用户须知授权页 | 协议勾选 |
| US-010 | C-入驻资料填写页 | 实名、资质、教学履历、参考单价 |
| US-051 | C-微信授权登录页 | 教练端登录、状态分流 |
| US-054 | C-手机号验证码登录页 | 教练短信验证码登录 |

### 4.3 Web 后台（web-admin）

| US | 页面 | 说明 |
|----|------|------|
| US-053 | A-管理员账号密码登录页 | 管理员登录、RBAC token |

### 4.4 后端（backend）

本批次后端需支撑上述全部 US 的 API：

| 领域 | API 示例 |
|------|---------|
| 微信登录 | `POST /api/v1/auth/wechat-login` |
| 手机登录 | `POST /api/v1/auth/phone-code-login` |
| 短信验证码 | `POST /api/v1/auth/send-phone-code` |
| 管理员登录 | `POST /api/v1/admin/auth/login` |
| 教练入驻 | `POST /api/coach/application` |
| 教练入驻查询 | `GET /api/coach/application` |
| 协议配置 | `GET /api/v1/config/terms` |

> **待用户确认**：最终第一批页面清单以用户整理为准；本表为初始参考。

---

## 5. 外部依赖与环境清单

### 5.1 必装环境

| # | 依赖 | 版本 | 用途 | 安装命令（参考） |
|---|------|------|------|----------------|
| 1 | Node.js | 20 LTS | 前端三件套 + shared + tools | `fnm use 20` / `nvm use 20` |
| 2 | yarn | 9.x | monorepo 包管理 | `npm i -g yarn` |
| 3 | Java JDK | 21 LTS | Spring Boot 后端 | 下载 Oracle/OpenJDK 21 |
| 4 | Maven | 3.9+ | Java 构建 | `choco install maven` |
| 5 | MySQL | 8.0 | 主数据库 | Docker 或官方 Installer |
| 6 | Redis | 7.x | 缓存 / 会话 / 验证码 | Docker 或官方 Installer |
| 7 | 微信开发者工具 | 稳定版 | 小程序预览/调试 | 微信官方下载 |
| 8 | Chrome / Edge | 最新版 | Web 后台调试 | — |
| 9 | Docker Desktop | 最新版 | 本地 MySQL/Redis/Nginx | Docker 官网下载 |

### 5.2 外部服务与账号

| # | 服务 | 用途 | 是否必须 | 申请/配置项 |
|---|------|------|---------|------------|
| 1 | 微信公众平台 | 用户端小程序 AppID / AppSecret | 必须 | 注册小程序，**第一版不认证**，用体验版跑通 |
| 2 | 微信公众平台 | 教练端小程序 AppID / AppSecret | 必须 | 注册第二个小程序，**第一版不认证**，用体验版跑通 |
| 3 | 微信开放平台（可选） | 同一自然人 user/coach 账号打通 | 可选 | 第一版不做账号打通，可暂不申请 |
| 4 | 短信服务 | 发送手机验证码 | 必须 | 阿里云短信 / 腾讯云短信 / 网易云信（可用测试/沙箱模式） |
| 5 | 对象存储 / CDN | 头像、证书图片上传 | 推荐 | 开发阶段可用后端本地存储或 MinIO 替代 |
| 6 | 微信支付（可选） | 后续支付订单 | 本批次不需要 | 体验版无法测试真实支付，正式上线前再申请 |

### 5.3 开发工具与 VS Code 插件

| # | 工具/插件 | 用途 |
|---|----------|------|
| 1 | VS Code + Trae / Cursor | IDE |
| 2 | ESLint / Prettier / Stylelint | 前端代码规范 |
| 3 | Spring Boot Extension Pack | 后端开发 |
| 4 | MySQL / Redis 客户端 | Navicat / DBeaver / RedisInsight |
| 5 | Postman / Insomnia / Bruno | API 调试 |
| 6 | Git | 版本控制 |

---

## 6. 目录初始化清单

开发前，主 Agent 需确保以下目录已创建并初始化：

```
leyoSwimming/
├── miniapp-user/              # 学员端小程序（Taro + React + TS）
│   ├── src/
│   ├── tests/
│   ├── config/
│   ├── package.json
│   ├── tsconfig.json
│   ├── project.config.json
│   └── AGENT.md
├── miniapp-coach/             # 教练端小程序（Taro + React + TS）
│   ├── src/
│   ├── tests/
│   ├── config/
│   ├── package.json
│   ├── tsconfig.json
│   ├── project.config.json
│   └── AGENT.md
├── web-admin/                 # 管理后台（Vue3 + TS + Vite）
│   ├── public/
│   ├── src/
│   ├── tests/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── AGENT.md
├── backend/                   # 后端（Spring Boot 3 + Java 21）
│   ├── src/main/java/...
│   ├── src/test/java/...
│   ├── src/main/resources/
│   ├── pom.xml
│   ├── Dockerfile
│   └── AGENT.md
├── shared/                    # 跨端共享（TypeScript 类型 + OpenAPI）
│   ├── src/types/
│   ├── src/constants/
│   ├── src/utils/
│   ├── openapi/
│   ├── package.json
│   ├── tsconfig.json
│   └── AGENT.md
├── deploy/                    # Docker / Nginx / CI/CD
│   ├── docker-compose.yml
│   ├── nginx.conf
│   └── AGENT.md
└── tools/                     # 脚本与生成器
    ├── scripts/
    └── AGENT.md
```

---

## 7. 自动联调机制

### 7.1 本地开发环境一键启动

`deploy/docker-compose.yml` 需包含：

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: leyo_root
      MYSQL_DATABASE: leyo_dev
  redis:
    image: redis:7-alpine
  backend:
    build: ../backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
```

启动命令：

```bash
cd deploy
docker-compose up -d
```

### 7.2 前端代理配置

- `miniapp-user/config/dev.js`：Taro 开发服务器代理到 `http://localhost:8080`。
- `miniapp-coach/config/dev.js`：同上。
- `web-admin/vite.config.ts`：配置 `/api` 代理到 `http://localhost:8080`。

### 7.3 API 联调检查单

每个 API 联调时，主 Agent 必须确认：

- [ ] OpenAPI 文档与后端实现一致
- [ ] 前端请求 URL / 方法 / 参数与 OpenAPI 一致
- [ ] 成功响应字段与页面需求一致
- [ ] 错误码与页面提示文案一致
- [ ] 加载态 / 空态 / 错误态 UI 已覆盖
- [ ] 跨域 / Token / Cookie 配置正确

---

## 8. 主 Agent 管理 checklist

### 8.1 每日开始

- [ ] 检查昨日 dev-log，确认阻塞点已解决
- [ ] 发布今日 API 契约版本
- [ ] 同步设计稿变更（如有）

### 8.2 每日结束

- [ ] 收集各端 Sub-Agent 日报
- [ ] 更新 API 契约与 dev-log
- [ ] 组织联调，解决不一致
- [ ] 确认当日 commit 质量

### 8.3 批次结束

- [ ] 运行全端 lint / test
- [ ] 执行登录注册批次 E2E 流程
- [ ] 更新 `docs/tech/dev-log/` 批次总结
- [ ] 确认无敏感文件提交
- [ ] 告知用户开发完成，等待用户决策是否 push GitHub

---

## 9. 自检清单（开发前）

- [ ] 本规范文档已生成并评审
- [ ] 7 个代码目录已创建
- [ ] 每个代码目录已创建 `AGENT.md`
- [ ] 用户已确认第一批页面清单
- [ ] Docker / MySQL / Redis 已安装并可启动
- [ ] Node.js / yarn / Java / Maven 已安装
- [ ] 微信开发者工具已安装
- [ ] 小程序 AppID / AppSecret 已获取（或已申请测试号）
- [ ] 短信服务账号已申请（开发环境可用沙箱/测试模式）
- [ ] OpenAPI 初始文件已创建
- [ ] 后端 Agent 已产出第一批 API 契约 v1.0.0

---

## 10. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-08-05 | AI Agent | 初稿：多 Agent 组织架构、API-First 流程、开发纪律、环境清单、目录初始化、联调机制 |
