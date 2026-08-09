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
| 工程 | `miniapp-user/` 目录及脚手架 | 目录存在，但仅含 `AGENT.md`，无实际脚手架 | **是** | [frontend-backend-setup-guide.md §4](./frontend-backend-setup-guide.md) |
| 工程 | `miniapp-coach/` 目录及脚手架 | 目录存在，但仅含 `AGENT.md`，无实际脚手架 | **是** | [frontend-backend-setup-guide.md §4](./frontend-backend-setup-guide.md) |
| 工程 | `web-admin/` 目录及脚手架 | 目录存在，但仅含 `AGENT.md`，无实际脚手架 | **是** | [frontend-backend-setup-guide.md §5](./frontend-backend-setup-guide.md) |
| 工程 | `backend/` 目录及脚手架 | 目录存在，但仅含 `AGENT.md`，无实际脚手架 | **是** | [frontend-backend-setup-guide.md §6](./frontend-backend-setup-guide.md) |
| 工程 | `shared/` 目录及类型定义 | 目录存在，已有 `openapi/leyo-swimming-v1.yaml` 初稿 | 否 | [frontend-backend-setup-guide.md §7](./frontend-backend-setup-guide.md) |
| 工程 | `deploy/` Docker Compose | 已存在 `docker-compose.yml`、`.env.example`、`nginx.conf` | 否 | [frontend-backend-setup-guide.md §8](./frontend-backend-setup-guide.md) |
| 工程 | `tools/` 脚本 | 目录存在，已有若干 Calicat 上传脚本 | 否 | — |
| 工程 | 各目录 `AGENT.md` | 已存在 | 否 | [frontend-backend-setup-guide.md §9](./frontend-backend-setup-guide.md) |
| 契约 | OpenAPI v1.0.0 | 已存在初稿，但部分路径/方法不符合 [api-convention.md](./api-convention.md)，需评审修正 | 是 | [multi-agent-dev-guide.md §2.1](./multi-agent-dev-guide.md) |

**结论**：还不能直接进编码。当前最大缺口是：
1. 四个代码目录（`backend/`、`miniapp-user/`、`miniapp-coach/`、`web-admin/`）只有空目录 + `AGENT.md`，需要初始化实际脚手架。
2. OpenAPI 初稿需按 api-convention 修正后冻结。

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

- [ ] yarn / JDK 21 / Maven / Docker Desktop 安装并验证版本
- [ ] 微信开发者工具安装
- [ ] 用户端、教练端小程序注册并获取 AppID / AppSecret
- [ ] 短信 mock 开关已配置（`SMS_MOCK_ENABLED=true`，开发期不需要真实短信账号）
- [ ] 7 个代码目录已创建并初始化最小脚手架
- [ ] 每个代码目录已创建 `AGENT.md`
- [ ] `deploy/.env` 已配置（复制自 `.env.example`）
- [ ] `docker-compose up -d` 可正常启动 MySQL + Redis
- [ ] 后端骨架可运行，统一响应、JWT、异常处理已就位
- [ ] 前端脚手架可运行，设计 token 已与 Figma 对齐
- [ ] `shared/openapi/leyo-swimming-v1.yaml` 已创建并冻结 v1.0.0
- [ ] 第一个 US 的后端 + 前端已走通 TDD 全流程

---

## 6. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-06 | AI Agent | 初版：第一批次 19 个 US 开发规划、前置准备清单、执行顺序、关键决策点 |
