# leyoSwimming 第二批次开发规划

> **文档定位**：第二批次进入多 Agent 并行开发前的总规划与前置准备清单。本批次聚焦「套餐浏览、购买、支付、订单、退款、用户套餐管理、教练学员管理」。
> **目标读者**：主 Agent、各端 Sub-Agent、后续接手的开发工程师。
> **配套文档**：
> - [environment-setup-checklist.md](./environment-setup-checklist.md) — 环境安装与外部账号
> - [frontend-backend-setup-guide.md](./frontend-backend-setup-guide.md) — 技术栈、目录结构、工程化
> - [multi-agent-dev-guide.md](./multi-agent-dev-guide.md) — 多 Agent 协作流程
> - [api-convention.md](./api-convention.md) — API 接口规范
> - [AGENTS.md](../../AGENTS.md) — 项目级硬约束
> - [docs/figma/第二批次页面梳理.md](../figma/第二批次页面梳理.md) — 本批次 US、页面清单、任务清单
> - [docs/figma/page-spec/](../figma/page-spec/) — 已关联 Calicat 的页面规格
> - [docs/figma/page-development-task-template.md](../figma/page-development-task-template.md) — 前端页面开发强制检查清单模板

---

## 1. 本批次目标

完成「套餐浏览与购买、订单与支付、退款处理、我的套餐/订单、教练学员管理、后台套餐/订单管理」能力，覆盖 12 个已批准的 US：

| 端 | US |
|----|----|
| 用户端小程序 | US-001 / US-017 / US-019 / US-020 / US-021 / US-025 / US-026 / US-027 |
| 教练端小程序 | US-037 |
| Web 管理后台 | US-043 / US-045 / US-046 / US-028 |

> 注：US-045（管理员配置标准与自定义套餐模板）是本批次的数据基础；US-028（管理员处理退款并原路退回）已合并进 US-046 的订单管理页，二者统一开发。

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
| 账号 | 微信支付 / 支付宝 | **MVP 用 MockPaymentGateway，不需要真实商户号** | 否 | 见 §2.1 支付 mock 方案 |
| 工程 | `miniapp-user/` 目录及脚手架 | 已初始化 Taro 4 + React 18 + TypeScript | 否 | [frontend-backend-setup-guide.md §4](./frontend-backend-setup-guide.md) |
| 工程 | `miniapp-coach/` 目录及脚手架 | 已初始化 Taro 4 + React 18 + TypeScript | 否 | [frontend-backend-setup-guide.md §4](./frontend-backend-setup-guide.md) |
| 工程 | `web-admin/` 目录及脚手架 | 已初始化 Vue 3 + Vite 5 + Element Plus | 否 | [frontend-backend-setup-guide.md §5](./frontend-backend-setup-guide.md) |
| 工程 | `backend/` 目录及脚手架 | 已初始化 Spring Boot 3.2 + Java 21 + Maven | 否 | [frontend-backend-setup-guide.md §6](./frontend-backend-setup-guide.md) |
| 工程 | `shared/openapi/leyo-swimming-v1.yaml` | 已存在 v1.0.0（第一批次） | 否 | 本批次需要新增第二批次接口 |
| 契约 | API 规范 | 已按 [api-convention.md](./api-convention.md) 评审修正 | 否 | [docs/figma/第二批次页面梳理.md §任务 1](../figma/第二批次页面梳理.md) |

**结论**：Phase 0 环境/工程已完成，Phase 1 需要补充本批次 API 契约，之后可以进入后端骨架与分组并行开发。

**开发规范**：
- 前端：[frontend-development-standards.md](./frontend-development-standards.md)，并遵守 [docs/figma/page-development-task-template.md](../figma/page-development-task-template.md) 的 Calicat 开工检查清单。
- 后端：[backend-development-standards.md](./backend-development-standards.md)。

---

## 2.1 支付 mock 方案（MVP）

第二批次涉及购买、支付、退款，但 MVP 不接入真实支付平台：

| 环境 | 行为 |
|------|------|
| 本地开发 / 体验版 | `POST /api/order/pay` 调起 MockPaymentGateway，自动返回 mock 支付参数；`POST /api/payment/mock-callback` 模拟渠道回调，3 秒内完成订单状态变更。 |
| 退款 | `POST /api/admin/order/approve` 通过后进入「退款处理中」，MockPaymentGateway 自动退款成功，订单状态变为「已退款」。 |
| 生产环境 | 后续切换为真实微信支付/支付宝。 |

**配置方式**：
- `deploy/.env` 中新增 `PAYMENT_MOCK_ENABLED=true`
- 后端读取该开关：为 true 时跳过真实支付渠道，直接返回 mock transaction_no。
- 该开关默认在 dev profile 开启，prod profile 强制关闭。

---

## 3. 建议执行顺序

### Phase 0：准备期（0.5 天）

1. 确认本地 MySQL / Redis 可连接（密码 `leyo1234`）。
2. 确认三个前端项目可 `yarn dev` / 微信开发者工具可编译。
3. 确认后端可 `./mvnw spring-boot:run`。
4. 各端负责 Agent 熟读本批次 page-spec 与对应 US 的 user-story / tech-design。

### Phase 1：API 契约冻结（0.5-1 天）

1. 后端 Agent 汇总本批次 12 个 US 的 API 影响表，输出 `shared/openapi/leyo-swimming-v1.yaml` 中第二批次接口章节。
2. 主 Agent 组织评审，重点核对：
   - 是否符合 [api-convention.md](./api-convention.md)（RPC over HTTP、统一 POST、无路径参数）
   - 套餐模板与教练的多对多关系是否在 API 中体现为 `coachIds: number[]`
   - 订单/退款状态机是否覆盖：待支付 → 已支付 / 已取消；退款审批中 → 退款处理中 → 已退款 / 退款被拒
   - 套餐实例状态机是否覆盖：active / frozen / exhausted / expired / refunded
3. 冻结本批次 API 契约后，前端 Agent 据此生成 Mock Server。

### Phase 2：后端骨架 + 基础数据域（2-3 天）

按数据依赖从底层向上实现，建议顺序：

1. **US-045 套餐模板域**（后台）
   - 数据表：`package_template`、`package_template_coach`、`package_template_image`
   - 接口：`/api/admin/package-template/list`、`/add`、`/detail`、`/update`、`/status`
   - 标准套餐模板必须关联 ≥1 名教练；自定义模板作为独立类型。
2. **US-001 教练列表/详情**（用户端）
   - 接口：`/api/coach/list`、`/api/coach/detail`
   - 为套餐详情页提供教练卡片数据。
3. **US-019 套餐浏览**（用户端）
   - 接口：`/api/package/list`、`/api/package/detail`
   - 依赖套餐模板；用户端列表/详情使用模板快照 + 教练信息。
4. **US-043 用户套餐管理**（后台）
   - 数据表：`package_instance`
   - 接口：`/api/admin/package/list`、`/detail`、`/freeze`、`/unfreeze`、`/extend`、`/refund`
   - 为后续退款、我的套餐、教练视角提供数据。

### Phase 3：三端并行开发（按业务链路分组）

每组内部**先完成后端接口，再并行三端页面**；组间按依赖顺序串行。

| 分组 | 优先完成后端 | 并行三端 |
|------|-------------|----------|
| **G1：购买链路** | US-017 / US-020 / US-025 | 用户端：套餐列表 → 套餐详情 → 订单确认 → 自定义配置 → 支付页 |
| **G2：我的资产** | US-021 / US-026 | 用户端：我的套餐列表/详情、我的订单列表/详情；后台：套餐管理页/详情弹窗 |
| **G3：退款链路** | US-027 / US-028 / US-046 | 用户端：申请退款页；后台：订单管理页/订单详情弹窗 |
| **G4：教练学员管理** | US-037 | 教练端：学员列表/详情、教练视角套餐详情 |
| **G5：后台套餐模板** | US-045 | 后台：套餐模板管理页、套餐模板编辑弹窗 |

> **执行优先级建议**：G5（模板配置） → G1（购买） → G2（我的资产） → G3（退款） → G4（教练学员）。
> G4 可与 G2/G3 并行，但教练视角套餐详情依赖 US-043 的 package instance 接口。

### Phase 4：联调与验收（2-3 天）

1. 每日 17:00 主 Agent 组织联调，更新 `docs/tech/dev-log/`。
2. 跑通关键 E2E 流程：
   - 游客浏览教练/套餐 → 购买体验课 → 支付成功 → 查看我的套餐/订单
   - 学员购买正价套餐（含自定义配置） → 支付 → 查看详情
   - 学员申请退款 → 管理员审批 → 原路退回
   - 管理员冻结/解冻/延长套餐
   - 教练查看名下学员与关联套餐
3. 全端 lint / test / build 通过。
4. 所有前端页面通过 `visual-review` Agent。
5. 输出批次总结，执行 [docs/figma/第二批次页面梳理.md §6](../figma/第二批次页面梳理.md) 的强制复盘。

---

## 4. 关键决策点（开发前必须确认）

| # | 决策 | 当前结论 | 影响 |
|---|------|----------|------|
| 1 | MVP 支付方案 | MockPaymentGateway，自动成功 | `backend/src/services/payment/` 只需要 Mock 实现 |
| 2 | 退款审批入口 | 合并到 A-订单管理页，不再独立页面 | `A-refund-approval-page.md` 已归档 |
| 3 | 套餐模板适用教练 | 多选，至少 1 名；关联表 `package_template_coach` | API 请求体用 `coachIds: number[]` |
| 4 | 自定义套餐价格 | 选中教练参考单价 × 课时数 | `U-custom-package-config-page.md` 与后端计价逻辑一致 |
| 5 | 体验课限制 | 同一用户最多 1 个 active/exhausted 体验套餐 | 购买接口需校验 |
| 6 | 退款时套餐状态 | 退款订单创建后 package.status = frozen(refund_pending) | 套餐不可再预约 |
| 7 | 教练离职标记 | active 套餐设置 `pending_handover_at`，冻结预约 | 由 US-039/US-041 第一批次实现，本批次只读取该字段 |

---

## 5. 开发前最终确认清单

- [ ] 本地 MySQL / Redis 可连接（密码 `leyo1234`）
- [ ] 三个前端项目可本地运行
- [ ] 后端可 `./mvnw spring-boot:run`
- [ ] `deploy/.env` 已配置 `PAYMENT_MOCK_ENABLED=true`
- [ ] 本批次 12 个 US 的 user-story.md 顶部状态为 `[APPROVAL]`
- [ ] 本批次 22 个 page-spec 的 Calicat file_id 与 node-id 已确认
- [ ] `shared/openapi/leyo-swimming-v1.yaml` 已补充第二批次接口章节并冻结
- [ ] 后端已完成 US-045 / US-019 / US-043 基础接口，可给前端联调
- [ ] 已建立 `design-tokens.json` 与 `calicat-overrides.scss` 基线
- [ ] 第一个 US 的后端 + 前端已走通 TDD 全流程

---

## 6. 本批次新增机制

### 6.1 前端页面开发强制检查清单

为避免再次出现「先写代码后补设计稿」的视觉还原偏差，本批次引入：

- [docs/figma/page-development-task-template.md](../figma/page-development-task-template.md)
- [.trae/rules/figma/page-dev-must-checklist.md](../../.trae/rules/figma/page-dev-must-checklist.md)
- [.trae/rules/common/agents.md §Sub-Agent Frontend Task Mandatory Prefix](../../.trae/rules/common/agents.md)

**要求**：任何 Sub-Agent 收到前端页面任务时，父 Agent 必须在任务描述开头强制附加检查清单前缀；子 Agent 未生成 `TodoWrite` 前禁止写前端代码。

### 6.2 批次结束强制复盘

第二批次前端页面开发完成后，必须执行 [docs/figma/第二批次页面梳理.md §6](../figma/第二批次页面梳理.md) 的复盘流程：

1. 运行 `update-batch-lessons.ps1` 收集数据。
2. 调用 `batch-retrospective` Agent。
3. 更新 [docs/figma/batch1-lessons-learned.md](../figma/batch1-lessons-learned.md)。
4. 未完成复盘不得进入下一批次开发。

---

## 7. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-15 | AI | 初版：第二批次 12 个 US 开发规划、前置准备、执行顺序、关键决策、新增前端检查清单机制 |
