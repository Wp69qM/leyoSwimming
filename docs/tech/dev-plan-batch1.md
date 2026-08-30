# leyoSwimming 第一批次开发规划

> **文档定位**：第一批次进入多 Agent 并行开发前的总规划与前置准备清单。  
> **目标读者**：主 Agent、各端 Sub-Agent、后续接手的开发工程师。  
> **配套文档**：
> - [environment-setup-checklist.md](./environment-setup-checklist.md) — 环境安装与外部账号
> - [frontend-backend-setup-guide.md](./frontend-backend-setup-guide.md) — 技术栈、目录结构、工程化
> - [multi-agent-dev-guide.md](./multi-agent-dev-guide.md) — 多 Agent 协作流程
> - [api-convention.md](./api-convention.md) — API 接口规范
> - [AGENTS.md](../../AGENTS.md) — 项目级硬约束
> - `docs/stories/US-*/` — 已 [APPROVED] 的用户故事
> - `docs/figma/page-spec/` — 已关联 Calicat 的页面规格

---

## 1. 本批次目标

完成「登录注册 + 用户管理 + 教练入驻/审核/重新入驻 + 后台账号管理」能力，覆盖 19 个已批准的 US：

| 端 | US |
|----|----|
| 用户端小程序 | US-004 / US-005 / US-006 / US-007 / US-009 / US-052 |
| 教练端小程序 | US-010 / US-011 / US-012 / US-039 / US-040 / US-051 / US-054 / US-056 |
| Web 管理后台 | US-041 / US-042 / US-053 / US-055 / US-057 |

> 注：US-011（管理员审核教练入驻资质）虽然由后台端操作，但依赖教练端提交的资料快照，属于跨端联调重点。

---

## 2. 前置准备总览

前置准备分为四类：**环境/工具**、**外部账号/服务**、**工程初始化**、**API 契约**。当前状态如下：

| 类别 | 项目 | 当前状态 | 阻塞开发？ | 参考文档 |
|------|------|----------|-----------|----------|
| 环境 | Node.js 20 LTS | 已安装 | 否 | [environment-setup-checklist.md §2](./environment-setup-checklist.md) |
| 环境 | yarn 1.22.22 | 已安装 | 否 | [environment-setup-checklist.md §2.1](./environment-setup-checklist.md) |
| 环境 | Java JDK 21 | 已验证可用 | 否 | [environment-setup-checklist.md §2.2](./environment-setup-checklist.md) |
| 环境 | Maven 3.9+ | 已验证可用 | 否 | [environment-setup-checklist.md §2.3](./environment-setup-checklist.md) |
| 环境 | Docker Desktop | 因奇安信拦截，开发期暂不使用 | 否 | [environment-setup-checklist.md §2.4](./environment-setup-checklist.md) |
| 环境 | MySQL 8 | 已安装并创建 leyo_dev 数据库 | 否 | — |
| 环境 | Redis 5+ | 已验证可用 | 否 | — |
| 环境 | 微信开发者工具 | 用户称已安装 | 否 | [environment-setup-checklist.md §2.5](./environment-setup-checklist.md) |
| 账号 | 用户端小程序 AppID / AppSecret | 已申请，已写入 `deploy/.env` | 否 | [environment-setup-checklist.md §3.1](./environment-setup-checklist.md) |
| 账号 | 教练端小程序 AppID / AppSecret | 已申请，已写入 `deploy/.env` | 否 | [environment-setup-checklist.md §3.1](./environment-setup-checklist.md) |
| 账号 | 短信服务（验证码） | **开发期用 mock，暂不接入真实短信** | 否 | 见 §2.1 短信 mock 方案 |
| 工程 | `miniapp-user/` 目录及脚手架 | 已初始化 Taro 4 + React 18 + TypeScript 脚手架 | 否 | [frontend-backend-setup-guide.md §4](./frontend-backend-setup-guide.md) |
| 工程 | `miniapp-coach/` 目录及脚手架 | 已初始化 Taro 4 + React 18 + TypeScript 脚手架 | 否 | [frontend-backend-setup-guide.md §4](./frontend-backend-setup-guide.md) |
| 工程 | `web-admin/` 目录及脚手架 | 已初始化 Vue 3 + Vite 5 + Element Plus 脚手架 | 否 | [frontend-backend-setup-guide.md §5](./frontend-backend-setup-guide.md) |
| 工程 | `backend/` 目录及脚手架 | 已初始化 Spring Boot 3.2 + Java 21 + Maven 脚手架 | 否 | [frontend-backend-setup-guide.md §6](./frontend-backend-setup-guide.md) |
| 工程 | `shared/` 目录及类型定义 | 目录存在，已有 `openapi/leyo-swimming-v1.yaml` 初稿 | 否 | [frontend-backend-setup-guide.md §7](./frontend-backend-setup-guide.md) |
| 工程 | `deploy/` Docker Compose | 已存在 `docker-compose.yml`、`.env.example`、`nginx.conf` | 否 | [frontend-backend-setup-guide.md §8](./frontend-backend-setup-guide.md) |
| 工程 | `tools/` 脚本 | 目录存在，已有若干 Calicat 上传脚本 | 否 | — |
| 工程 | 各目录 `AGENT.md` | 已存在 | 否 | [frontend-backend-setup-guide.md §9](./frontend-backend-setup-guide.md) |
| 契约 | OpenAPI v1.0.0 | 已按 [api-convention.md](./api-convention.md) 评审修正并冻结 | 否 | [multi-agent-dev-guide.md §2.1](./multi-agent-dev-guide.md) |

**结论**：Phase 0（环境/工具/工程初始化）与 Phase 1（API 契约冻结）已完成，可以进入 Phase 2 后端骨架 + 第一个 US 实现。

**开发规范**：
- 前端：[frontend-development-standards.md](./frontend-development-standards.md)，适用于 `miniapp-user/`、`miniapp-coach/`、`web-admin/`。
- 后端：[backend-development-standards.md](./backend-development-standards.md)，适用于 `backend/`。

---

## 2.1 短信服务 mock 方案（开发期）

第一批次可先不接入真实短信平台，采用以下 mock 策略：

| 环境 | 行为 |
|------|------|
| 本地开发 / 体验版 | `POST /api/v1/auth/send-phone-code` 返回固定验证码 `123456`，并写入 Redis TTL=5 分钟；后端日志同时打印手机号与验证码，便于前端联调。 |
| 测试环境 | 仍可使用固定码或接入短信沙箱。 |
| 生产环境 | 切换为真实短信服务（阿里云 / 腾讯云）。 |

**配置方式**：
- `deploy/.env` 中新增 `SMS_MOCK_ENABLED=true`
- 后端读取该开关：为 true 时跳过真实短信发送，直接返回 mock 验证码。
- 该开关默认在 dev profile 开启，prod profile 强制关闭。

> 这样第一批次开发不需要真实短信账号，也不影响后续切换。

---

## 3. 建议执行顺序

### Phase 0：准备期（1-2 天）

1. **安装环境**（按 [environment-setup-checklist.md §2](./environment-setup-checklist.md)）：
   - yarn / JDK 21 / Maven / Docker Desktop / 微信开发者工具
2. **注册外部账号**（按 [environment-setup-checklist.md §3](./environment-setup-checklist.md)）：
   - 两个小程序（不认证，体验版）
   - 短信服务（可用沙箱模式）
3. **初始化工程目录与脚手架**（按 [frontend-backend-setup-guide.md §12](./frontend-backend-setup-guide.md)）：
   - 创建 7 个目录：`miniapp-user/`、`miniapp-coach/`、`web-admin/`、`backend/`、`shared/`、`deploy/`、`tools/`
   - 每个目录初始化最小可运行脚手架 + `AGENT.md`
4. **创建 `deploy/.env`**（复制 `.env.example` 后填写真实 AppID / AppSecret / 短信密钥）
5. **启动本地基础设施**：`cd deploy && docker-compose up -d`

### Phase 1：API 契约冻结（0.5-1 天）

1. 后端 Agent 汇总 19 个 US 的 API 影响表，输出 `shared/openapi/leyo-swimming-v1.yaml`
2. 主 Agent 组织评审，重点核对：
   - 是否符合 [api-convention.md](./api-convention.md)（RPC over HTTP、统一 POST、无路径参数）
   - 用户端/教练端登录接口是否复用同一套微信 OAuth 但分别写入 `user` / `coach` 表
   - 教练入驻/审核/重新入驻是否按 `coach_application` 快照表方案设计
3. 冻结 v1.0.0 后，前端 Agent 据此生成 Mock Server

### Phase 2：后端骨架 + 第一个 US（2-3 天）

1. 后端搭建 Spring Boot 骨架：统一响应、全局异常、JWT、MyBatis-Plus、Redis、Flyway
2. 实现首个 US 的完整后端（建议 US-004 用户微信登录 或 US-053 管理员登录），跑通 TDD 全流程
3. 建立后端测试基线（单元 + 集成测试），确保 CI 可通过

### Phase 3：三端并行开发（按 US 分组）

按依赖关系分组，每组内部优先完成后端接口，再并行三端实现：

| 分组 | 优先完成后端 | 并行三端 |
|------|-------------|----------|
| G1：登录授权 | US-004 / US-006 / US-051 / US-054 / US-053 | 用户端/教练端/后台登录页 |
| G2：协议与资料 | US-009 / US-005 | 协议页、完善资料页 |
| G3：教练入驻/审核/重新入驻 | US-010 / US-011 / US-040 | 入驻资料页、审核队列/详情页、重新入驻状态 |
| G4：账号生命周期 | US-007 / US-039 / US-041 | 注销页、离职申请页、离职审批页 |
| G5：管理后台账号 | US-042 / US-055 / US-057 | 用户/教练/管理员管理列表与弹窗 |
| G6：个人主页与退出 | US-012 / US-052 / US-056 | 个人主页编辑、参考单价、退出登录 |

### Phase 4：联调与验收（2-3 天）

1. 每日 17:00 主 Agent 组织联调，更新 `docs/tech/dev-log/`
2. 跑通登录注册关键 E2E 流程
3. 全端 lint / test / build 通过
4. 输出批次总结，等待用户决策是否 push

---

## 4. 关键决策点（开发前必须确认）

| # | 决策 | 建议 | 影响 |
|---|------|------|------|
| 1 | 管理后台 UI 库 | Element Plus 或 Ant Design Vue | 影响 `web-admin/` 脚手架初始化 |
| 2 | 后端 ORM | MyBatis-Plus（文档已推荐） | 影响 `backend/` 脚手架与目录结构 |
| 3 | 状态管理 | 小程序 Zustand，后台 Pinia | 影响前端脚手架依赖 |
| 4 | 图片上传 | 开发期用后端本地存储 / MinIO，还是直接上 OSS/COS？ | 影响头像/证书上传实现 |
| 5 | monorepo 工具 | yarn workspace 还是 Turborepo？ | 影响根目录配置 |
| 6 | 短信服务商 | 阿里云 / 腾讯云 / 网易云信 | 影响短信接口封装 |
| 7 | 是否引入 RabbitMQ | 第一批次消息量小，建议不引入 | 影响架构复杂度 |
| 8 | 是否使用 Flyway | 建议用，便于后续迁移 | 影响 `backend/src/main/resources/db/migration/` |

---

## 5. 开发前最终确认清单

- [x] yarn / JDK 21 / Maven 安装并验证版本（Docker Desktop 因奇安信拦截暂不使用）
- [x] 微信开发者工具安装
- [x] 用户端、教练端小程序注册并获取 AppID / AppSecret
- [x] 短信 mock 开关已配置（`SMS_MOCK_ENABLED=true`，开发期不需要真实短信账号）
- [x] 7 个代码目录已创建并初始化最小脚手架
- [x] 每个代码目录已创建 `AGENT.md`
- [ ] `deploy/.env` 已配置（复制自 `.env.example`）
- [ ] `docker-compose up -d` 可正常启动 MySQL + Redis（开发期改用本地 MySQL/Redis）
- [x] 后端骨架可运行，统一响应、JWT、异常处理已就位
- [x] 前端脚手架可运行，设计 token 已与 Figma 对齐
- [x] `shared/openapi/leyo-swimming-v1.yaml` 已创建并冻结 v1.0.0
- [ ] 第一个 US 的后端 + 前端已走通 TDD 全流程

---

## 6. Calicat 设计稿还原偏差根因分析

> **背景**：第一批次开发完成后，管理端教练离职审批页、用户端小程序启动页与 Calicat 设计稿存在明显差异；虽已反复强调"Calicat 为视觉最高优先级"，但实现仍出现偏差。本章节复盘根因并制定预防措施。

### 6.1 直接原因

| # | 偏差表现 | 实际设计稿要求 | 实现时问题 |
|---|---------|---------------|-----------|
| 1 | 管理端离职审批队列页摘要条变成 4 张独立统计卡片 | 单一蓝底待办摘要条（`#E6F7FF` 背景，文字"待审批：N 条 · 超 3 工作日未处理：K 条"） | 直接套用 Element Plus 的统计卡片组件，未按 Calicat 图层结构还原 |
| 2 | 管理端详情页多出「登记」按钮 | 审批记录时间轴 + 检查清单，底部固定审批操作栏，无「登记」按钮 | 把业务动作与审批动作混淆，按 Element Plus 默认表单布局实现 |
| 3 | 用户端启动页 originally 只显示标题 | 全屏 `gradient-water` 渐变、居中 96×96 半透明 Logo、加载指示器、自动跳转 | 未读取 Calicat 图层数据，仅用占位文本实现 |
| 4 | 颜色/间距/字号与稿不符 | 严格使用全局 Token（`gradient-water`、`color-primary`、`space-md` 等） | 依赖组件库默认主题，未做主题覆盖或 token 映射 |

### 6.2 根因分析

1. **规范优先级理解错误**
   - 虽然项目规则写明"Calicat 设计稿为视觉最高优先级，page-spec 仅作交互与业务规则参考"，但开发时 Sub-Agent 仍把 page-spec 中的尺寸/颜色描述当作视觉来源。
   - page-spec 中的数值是文字描述，无法精确还原图层间距、透明度、阴影等细节；而 Calicat 图层数据包含真实像素级信息。

2. **开发顺序颠倒：先写代码，后补设计稿**
   - 多 Agent 并行开发时，Sub-Agent 为追求进度，先基于 Element Plus / NutUI 默认样式搭建页面，完成后再"对照"设计稿微调。
   - 这种"先实现后还原"的方式导致大量样式债务，返工成本远高于"先拉图层数据再实现"。

3. **组件库默认样式的惯性依赖**
   - 管理端使用 Element Plus，小程序端使用 NutUI-React-Taro，组件默认主题与 Calicat 设计系统存在差异。
   - 开发时未先建立"Token 映射 + 组件覆盖"层，导致按钮、卡片、表格、表单项直接使用库默认样式。

4. **缺少视觉还原检查点**
   - 原流程只有代码审查（code-reviewer），没有视觉还原审查（visual-review）环节。
   - 没有将"逐图层对比 Calicat"作为 PR 合并前的强制检查项。

5. **设计资产导出流程缺失**
   - Logo、图标、插画等视觉元素未按规则从 Calicat 导出为 PNG/SVG/WebP，而是临时用 CSS 绘制或占位符替代。

### 6.3 预防措施

| # | 措施 | 责任方 | 检查点 |
|---|------|--------|--------|
| 1 | 每个前端页面开发前，必须先用 Calicat MCP 拉取对应 Frame 的图层数据并输出 `design-tokens.json` | 前端 Sub-Agent | 开工前提交图层数据摘要 |
| 2 | 建立 `web-admin/src/styles/calicat-overrides.scss` 与 `miniapp-user/src/styles/calicat-overrides.scss`，将颜色、字号、间距、圆角、阴影映射到全局 Token | 主 Agent / 前端 Lead | 代码审查时检查是否使用 Token |
| 3 | 页面开发完成后，必须导出 Calicat 截图与本地实现截图并排对比，差异项登记为 issue | 前端 Sub-Agent | PR 描述必须包含对比图 |
| 4 | 新增 `visual-review` Agent，专门检查 UI 还原度（颜色误差 ≤1%、间距误差 ≤2px、字体字号一致） | 主 Agent | 合并前必须通过 |
| 5 | Logo、图标、插画必须从 Calicat 导出，禁止 CSS 重绘；导出文件统一放到 `src/assets/calicat/` | 前端 Sub-Agent | 审查时检查 asset 来源 |
| 6 | page-spec 仅用于交互流程、业务规则、状态机；任何视觉尺寸以 Calicat 图层为准 | 全员 | 发现用 page-spec 当视觉来源时立即纠正 |

### 6.4 本次已修复内容

- 管理端 `ResignationApprovalQueueView.vue`：按 Calicat 调整摘要条、表格列、状态标签、进度条、分页布局。
- 管理端 `ResignationTicketDetailView.vue`：按 Calicat 调整审批记录时间轴、检查清单、审批操作区，移除多余的「登记」按钮。
- 用户端 `miniapp-user/src/pages/index/index.tsx` + `index.scss`：按 Calicat 实现全屏渐变、Logo、Slogan、加载指示器、错误态、自动跳转逻辑。
- 新增 `miniapp-user/src/assets/logo-ribbon.svg`：从 Calicat 设计稿导出 Logo 图形。
- 修复 `authStore` 最小化用户信息持久化，避免本地存储 PII，并补充测试覆盖。

---

## 7. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-06 | AI Agent | 初版：第一批次 19 个 US 开发规划、前置准备清单、执行顺序、关键决策点 |
| v1.1 | 2026-08-11 | AI Agent | 新增 §6 Calicat 设计稿还原偏差根因分析，总结偏差表现、根因、预防措施与已修复内容 |
