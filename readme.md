# 乐游游泳约课系统（leyoSwimming）

> 解决游泳教练与学员之间沟通约课难题的一站式 SaaS 平台。

[![Status](https://img.shields.io/badge/status-MVP%20ready-brightgreen)]()
[![Phase](https://img.shields.io/badge/phase-MVP-blue)]()
[![Stack](https://img.shields.io/badge/stack-TBD-lightgrey)]()

---

## 📖 项目简介

**乐游游泳约课系统** 是为游泳教练与学员设计的一站式约课平台。核心解决"约课难、排期乱、沟通成本高"的问题，覆盖从套餐购买、时段预约、上课记录到教练费结算的完整业务闭环。

### 核心价值

- **学员**：一键查看教练可约时段、购买套餐、随时约课
- **教练**：发布排班、确认扣课时、管理学员
- **管理员**：统一管理用户、订单、套餐、5 项数据看板

### MVP 范围（10 项能力）

| # | 能力 | 端 |
|---|------|-----|
| 1 | 游客浏览（教练列表 / 详情 / 套餐 / 公告）| 微信小程序（用户端）|
| 2 | 注册登录（微信 OAuth + 手机号）| 微信小程序（用户端）|
| 3 | 教练入驻 + 可约时间管理 | 微信小程序（教练端）|
| 4 | 体验课购买 + 预约 | 微信小程序（用户端）|
| 5 | 正价套餐购买 + 学员管理 | 微信小程序（用户端）|
| 6 | 每周三 10:00 固定释放下周时段 | 系统 |
| 7 | 候补 + 关注 | 微信小程序（用户端）|
| 8 | 微信/支付宝支付（原路退回）| 微信小程序（两端）|
| 9 | 上课记录 + 课时消耗 | 微信小程序（两端）|
| 10 | 管理员：用户/订单/5 项数据看板 | Web 后台 |

详细功能见 [docs/prd/prd.md](docs/prd/prd.md)。

---

## 🏗️ 项目结构

```
leyoSwimming/
├── agent.md                      # 项目级 AI 协作规范
├── readme.md                     # 本文件（项目总览）
├── docs/                         # 产品与开发文档
│   ├── prd/                      # Master PRD（唯一最终版）
│   ├── user-story-specification.md  # US 拆分规范
│   ├── stories/                  # 用户故事库（30-50 个，待生成）
│   ├── spec/                     # SDD 规格（按 US 编号组织）
│   ├── figma/                    # Figma 原型
│   └── archive/                  # 历史版本存档
├── miniapp-user/                 # 微信小程序 - 用户端（学员）
├── miniapp-coach/                # 微信小程序 - 教练端
├── web-admin/                    # Web 后台 - 管理端
├── backend/                      # 后端 API 服务
├── shared/                       # 跨端共享代码
├── deploy/                       # 部署与运维
└── tools/                        # 工具与脚本
```

> **当前状态**：文档阶段，代码目录待 §9 SDD 工作流推进时创建。

---

## 🚀 快速开始

> **本节适用于代码开发启动后**。当前处于文档阶段，按下面 § 文档规范 进行。

### 环境要求（待定）

- Node.js ≥ 20.x
- MySQL ≥ 8.0
- 微信开发者工具（小程序开发）
- 微信支付 / 支付宝商户号（生产环境）

### 本地开发（待定）

```bash
# 1. 克隆仓库
git clone https://github.com/Wp69qM/leyoSwimming.git
cd leyoSwimming

# 2. 后端启动
cd backend
npm install
npm run dev

# 3. 用户端小程序
# 用微信开发者工具打开 miniapp-user/

# 4. Web 后台
cd web-admin
npm install
npm run dev
```

---

## 📚 文档导航

| 文档 | 路径 | 说明 |
|------|------|------|
| **Master PRD** | [docs/prd/prd.md](docs/prd/prd.md) | 唯一最终产品需求（v11 落档版）|
| **US 拆分规范** | [docs/user-story-specification.md](docs/user-story-specification.md) | 用户故事拆分标准与检查清单 |
| **AI 协作规范** | [agent.md](agent.md) | §9 SDD 工作流、§8 页面发布规范、§10 目录结构 |
| **历史版本** | [docs/archive/](docs/archive/) | v1-v10 PRD 演进记录 |

### 关键概念

- **状态机驱动**：用户身份、套餐、教练、预约、订单 5 大状态机贯穿全系统
- **教练离职非学员原因 100% 退款**：参考行业判例，consumed 不退
- **强隐私规则**：教练离职信息仅在业务必需处显示，列表/搜索/详情完全隐藏
- **frozen 套餐状态**：用于处理教练离职、管理员手动冻结等中间态场景

---

## 🛠️ 技术栈（待定）

| 端 | 推荐方案 | 备选 |
|----|---------|------|
| 用户端小程序 | 微信原生 | Taro / uni-app |
| 教练端小程序 | 微信原生 | Taro / uni-app |
| Web 后台 | React + Ant Design | Vue + Element Plus |
| 后端 | Node.js (Express/NestJS) | Go (Gin) / Java (Spring Boot) |
| 数据库 | MySQL 8.0 | PostgreSQL |
| 缓存 | Redis | - |
| 部署 | Docker + k8s | 阿里云 / 腾讯云 |

---

## 📋 开发规范

### 工作流（§9 SDD + TDD + Figma MCP）

```
[1] 拆分用户故事      →  docs/stories/US-XXX-*.md
[2] US 评审通过       →  Gherkin Given-When-Then
[3] SDD 规格编写      →  docs/spec/US-XXX/ (spec.md + plan.md + tasks.md)
[4] Figma MCP 原型    →  docs/figma/US-XXX/
[5] TDD 实现          →  miniapp-* / web-admin / backend
[6] 一致性体检        →  /analyze + 覆盖率 ≥ 80%
[7] 触发 §8 上线检查  →  PRD → UI → 联调 → 测试 四步全过后合并
```

### 关键纪律

- ✅ SDD 与 TDD 严禁并行
- ✅ TDD 必须 RED → GREEN → REFACTOR
- ✅ PM 只写 WHAT/WHY，技术细节在 plan.md
- ✅ Section-by-Section 页面实现
- ✅ 测试覆盖率 ≥ 80%，状态机 100%
- ❌ 不主动同步 GitHub（除非明确指令）

详见 [agent.md](agent.md) §8-§10。

---

## 🗓️ 项目里程碑

| 阶段 | 内容 | 状态 |
|------|------|------|
| **v1-v6** | PRD 初版到可执行版 | ✅ 存档于 [archive/](docs/archive/) |
| **v7** | 业务流程 + 70 条决策 | ✅ 存档 |
| **v8** | MVP 范围冻结 + 状态机配套 | ✅ 存档 |
| **v9** | 8 处规则冲突澄清 + ERD | ✅ 存档 |
| **v10** | 用户身份重构（状态机驱动）| ✅ 存档 |
| **v11** | 全部合并落档（最终 PRD）| ✅ [docs/prd/prd.md](docs/prd/prd.md) |
| **US 拆分** | 30-50 个用户故事 | 🔜 进行中 |
| **SDD 规格** | 按 US 编号 | ⏳ 待启动 |
| **MVP 开发** | 10 项能力 | ⏳ 待启动 |

---

## 🤝 贡献指南

本项目由产品经理 + AI Agent 协作开发，规范详见 [agent.md](agent.md)。

### 提交规范

- **commit message**：`{type}: {scope} - {description}` 格式
  - `docs: 修订 PRD 教练离职方案`
  - `feat: US-001 学员单次预约`
  - `fix: 修复预约时段校验 bug`
- **PR 前检查**：§8 页面发布规范四步检查

### 同步规范

- 默认**不**主动推送到 GitHub
- 仅在用户明确指令（如 `/sync`）时执行
- 详见 [agent.md](agent.md) §2-§3

---

## 📄 License

TBD

---

## 📮 联系方式

- 项目维护：leyoSwimming Team
- 反馈渠道：项目 Issues

---

<p align="center">
  <sub>Built with discipline by PM + AI Agent 🤖</sub>
</p>
