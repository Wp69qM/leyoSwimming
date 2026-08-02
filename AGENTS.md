# AGENTS.md — leyoSwimming AI 协作规范

> **文件性质**：项目级硬约束，所有 AI Agent（Trae / Claude / Cursor / Copilot 等）进入本项目后**必须首先读取本文件**。
> **最后更新**：2026-07-30（v2.2）

---

## 0. AI Agent 启动检查（最高优先级）

> **本节优先级高于一切。AI Agent 在响应用户任何请求前，必须先完成本节检查。**

### 0.1 必读文件清单

AI Agent 首次进入本项目时，**必须**读取以下文件：

| # | 文件 | 用途 |
|---|------|------|
| 1 | `AGENTS.md`（本文件） | 项目级协作规范 |
| 2 | `docs/prd/prd.md` | Master PRD（唯一业务真相源） |
| 3 | `docs/spec/user-story/SPECIFICATION.md` | US 拆分规范（15 章硬约束） |
| 4 | `docs/spec/user-story/TEMPLATE.md` | US 模板（复制后填写） |
| 5 | `docs/spec/user-story/INDEX.md` | US 注册表（50 个 US 清单） |
| 6 | `docs/figma/README.md` | 全局设计系统规范 |

### 0.2 Skills 触发规则（强制）

本项目在 `.trae/skills/` 下安装了以下 skill 集。AI Agent **必须**在以下场景主动调用对应 skill：

| 用户意图关键词 | 必须调用的 Skill | 原因 |
|--------------|-----------------|------|
| "写用户故事" / "创建 US" / "拆分 US" / "新故事" | `openspec-new-change` | 强制走 OpenSpec 变更流程 |
| "实现" / "开发" / "编码" / "写代码" | `test-driven-development` | 强制走 TDD |
| "修 bug" / "测试失败" / "报错" | `systematic-debugging` | 强制走科学调试 |
| "审查" / "review" / "检查代码" | `requesting-code-review` | 强制走代码审查 |
| "新功能" / "新特性" / "加个功能" | `brainstorming` → `openspec-new-change` | 先头脑风暴再走 SDD |
| "合并" / "merge" / "收尾" | `finishing-a-development-branch` | 强制走分支收尾 |
| "计划" / "plan" / "方案" | `writing-plans` | 强制先写计划 |

**红线**：如果用户意图匹配上表但 AI Agent 未调用对应 skill，视为违规。

### 0.3 禁止项

| # | 禁止 | 原因 |
|---|------|------|
| 1 | 未读取本文件就执行任何任务 | 不了解项目规范 |
| 2 | 用户说"写 US"但跳过 `openspec-new-change` skill | 破坏 SDD 流程 |
| 3 | 用户说"写代码"但跳过 `test-driven-development` skill | 破坏 TDD |
| 4 | 未读 SPECIFICATION.md 就生成 US | 章节结构漂移 |
| 5 | **生成/修订 US 时未同步创建或更新 OpenSpec 4 件套** | `docs/stories/` 与 `openspec/changes/` 脱节，CLI 无法校验 |
| 6 | **OpenSpec 4 件套未通过 `openspec validate` 就视为 US 完成** | 破坏规范校验流程，可能导致 archive 失败 |

---

## 1. 目标

本文档规范 AI Agent 与本项目的协作方式，确保代码变更、文档更新、GitHub 同步行为符合预期。

---

## 2. Git 与 GitHub 规范

### 2.1 不主动同步 GitHub

- AI Agent **不得**在每次生成或修改代码、文档后自动推送到 GitHub。
- AI Agent **不得**主动创建 Git 提交、打标签或发布 Release。
- AI Agent **不得**将本地修改默认视为需要同步的状态。

### 2.2 仅响应明确指令

只有在用户明确表达以下意图时，AI Agent 才执行 GitHub 同步相关操作：

- "上传到 GitHub" / "推送到 GitHub" / "同步到 GitHub"
- "提交代码" / "发布到 GitHub"
- 其他语义等价的表达

### 2.3 快捷指令

| 命令 | 含义 |
|------|------|
| `/sync` 或 `/github-sync` | 将当前本地所有已确认修改推送到 GitHub |
| `/commit` | 仅创建本地 Git 提交，不推送 |
| `/push` | 将当前分支本地提交推送到远程 |

使用 `/sync` 前，Agent 应先确认变更范围、仓库名称、分支及提交信息。

### 2.4 TDD 例外

§9 TDD 流程中的 per-task commit（`git add` + `git commit`）不受 §2.1 约束，但**仅限本地，不 push**。TDD 每个 Task 结束时的 commit 是工作流的一部分。

---

## 2.5 OpenSpec CLI

本项目已接入 OpenSpec CLI（v1.7.0），用于规范校验和变更归档。

### 2.5.1 目录结构

```
openspec/
├── config.yaml                    # 项目配置 + 技术栈 + artifact 规则
├── changes/                       # 活跃变更
│   └── us-001-coach-browsing/     # US-001 的 OpenSpec 工作区
│       ├── .openspec.yaml
│       ├── proposal.md            # ≈ user-story.md §1-§5（Why / What / Impact）
│       ├── specs/coach-browsing/
│       │   └── spec.md            # ≈ user-story.md §6（GWT + REQ + Scenario）
│       ├── design.md              # ≈ tech-design.md（数据/API/缓存/性能）
│       └── tasks.md               # ≈ test-plan.md（TDD 任务清单）
├── specs/                         # 源真相层（archive 后自动生成）
└── archive/                       # 已归档变更历史
```

### 2.5.2 文档映射关系

| OpenSpec 产物 | 我们的文档 | 关系 |
|--------------|---------|------|
| `proposal.md` | `user-story.md` §1-§5 | OpenSpec = 精简版（Why/What/Capabilities/Impact）|
| `specs/*/spec.md` | `user-story.md` §6 GWT | OpenSpec = 独立提取（含 REQ-XXX + Scenario delta）|
| `design.md` | `tech-design.md` | 内容基本一致，OpenSpec 版删减了映射表等辅助内容 |
| `tasks.md` | `test-plan.md` | OpenSpec = 精简版（去掉完整测试代码，保留 RED/GREEN/COMMIT 步骤）|

**原则**：`docs/stories/` 是主文档（PM/开发/QA 协作），`openspec/changes/` 是 CLI 校验用的映射版。两者内容保持同步，但格式不同。

### 2.5.3 常用命令

| 命令 | 用途 |
|------|------|
| `openspec new change <name>` | 创建新变更目录 |
| `openspec validate <name> --json` | 校验变更格式（delta header / REQ / Scenario）|
| `openspec status --change <name>` | 查看变更状态 |
| `openspec archive <name>` | 归档变更到 `openspec/specs/` |

### 2.5.4 约束（强规范）

> **核心纪律**：`docs/stories/` 与 `openspec/changes/` 必须同步创建、同步更新，缺一不可。OpenSpec 不是 US 定稿后的"补做项"，而是 US 生成工作流的必要组成部分。

| # | 约束 | 违反后果 |
|---|------|----------|
| 1 | **每次生成或修订 US 时，必须同步创建/更新 `openspec/changes/us-XXX-.../` 下的 4 件套**（`.openspec.yaml` / `proposal.md` / `specs/*/spec.md` / `design.md` / `tasks.md`） | `docs/stories/` 与 OpenSpec 映射层不一致，后续 `archive` 失败 |
| 2 | **OpenSpec 4 件套生成后，必须立即运行 `openspec validate <change-name> --json` 并确保通过** | 无法进入 archive 阶段，CI 校验失败 |
| 3 | **US 状态从 `[DRAFT]` 转为 `[REVIEW]` 或 `[APPROVED]` 前，必须先通过 `openspec validate`** | 状态迁移缺乏规范校验背书 |
| 4 | `docs/stories/` 是**主文档**，`openspec/changes/` 是 CLI 校验用的**映射版** | — |
| 5 | `openspec validate` 必须通过才能 `archive` | — |
| 6 | archive 后 `openspec/specs/` 自动生成"源真相"层 | — |

**历史教训**：US-005 ~ US-014 曾因将 OpenSpec 视为"定稿后补做项"而漏生成映射层，导致 `docs/stories/` 与 `openspec/changes/` 不同步。此后严禁重复。

---

## 3. 本地工作流

1. Agent 完成代码或文档修改后，仅保留在本地工作区。
2. Agent 可向用户简要汇报已修改的文件列表，但不执行任何 Git push。
3. 用户确认需要同步后，再执行 `git push` 等操作。

---

## 4. 安全与隐私

- 同步前检查是否包含敏感文件（如 `.env`、密钥、凭证）。
- 默认排除 `node_modules`、构建产物、日志文件、IDE 配置。
- 如检测到潜在敏感文件，应提示用户确认。

---

## 5. 页面发布规范

### 5.1 适用范围

适用于 MVP 范围内所有 ★ 标记的关键页面（共 55 个）。

### 5.2 四步上线检查（缺一不可）

1. **PRD 评审**（产品）：功能点与 PRD 条款逐条对齐。
2. **UI**（设计）：设计稿覆盖空 / 加载 / 错误 / 成功 四种状态。
3. **联调**（前端 / 后端）：接口字段、状态机、边界场景全部跑通。
4. **测试**（测试）：关键路径 + 异常路径用例 100% 通过。

### 5.3 合并前检查清单

- [ ] PRD 评审通过
- [ ] UI 四态设计稿完成
- [ ] 联调覆盖接口 + 状态机 + 边界
- [ ] 测试 100% 通过

任意一个环节未通过 → **不得**合并到主分支。

---

## 6. SDD 工作流（规范驱动开发）

> 每个用户故事（User Story）必须严格按本节流程实现。

### 6.1 适用范围

- 适用于 `docs/prd/prd.md` 范围内所有 MVP 用户故事（50 个）。
- 不适用于一次性脚本、技术调研、纯重构任务。

### 6.2 七步流水线（严格串行，不可跳步）

```
[1] 拆分用户故事
    │  必须调用 skill: openspec-new-change
    │  必须读取: SPECIFICATION.md + TEMPLATE.md + EXAMPLE.md
    │  产物:
    │    - docs/stories/US-XXX-.../user-story.md
    │    - docs/stories/US-XXX-.../tech-design.md
    │    - docs/stories/US-XXX-.../test-plan.md
    │    - openspec/changes/us-XXX-.../ 4 件套（同步创建，不是后续补做）
    │  产物必须通过 `openspec validate us-XXX-... --json`
    ↓
[2] PRD 评审
    │  验收标准统一用 Gherkin (Given-When-Then)
    │  产物: 评审通过标记
    ↓
[3] 技术设计
    │  产物: docs/stories/US-XXX-.../tech-design.md
    │  规范: docs/spec/tech-design/README.md
    ↓
[4] Figma 原型
    │  设计师读 user-story.md §13-15 → 产出 Figma 原型
    │  更新 user-story.md §13 链接 + §15 评审记录
    ↓
[5] TDD 实现
    │  必须调用 skill: test-driven-development
    │  产物: docs/stories/US-XXX-.../test-plan.md（TDD 任务清单）
    │  每个 Task: RED(写失败测试) → GREEN(最小实现) → REFACTOR → COMMIT
    ↓
[6] 一致性体检
    │  GWT 场景 → tech-design 章节 → test-plan Task 三维映射
    │  覆盖率 ≥ 80%
    ↓
[7] 触发 §5 页面发布
    PRD → UI → 联调 → 测试 四步全过后才合并
```

### 6.3 关键纪律（红线）

| # | 纪律 | 违反后果 |
|---|------|----------|
| 1 | **SDD 与 TDD 严禁并行**：先 user-story + tech-design 定稿，再走 TDD | 测试与规格脱节 |
| 2 | **TDD 必须严格 RED → GREEN → REFACTOR → COMMIT** | 测试沦为"补单" |
| 3 | **PM 只写 WHAT/WHY**：user-story.md 不写技术实现 | AI 替 PM 决定架构 |
| 4 | **每 Task = 1 commit** | git 历史无法回溯 |
| 5 | **测试覆盖率**：核心业务逻辑 ≥ 80%，状态机覆盖 100% | 上线事故 |
| 6 | **不主动 push GitHub**：遵循 §2 | 违反项目硬约束 |

### 6.4 工时预算（单 US 参考）

| 步骤 | 预算 |
|------|------|
| [1] 拆分用户故事 | 0.5h |
| [2] PRD 评审 | 0.5h |
| [3] 技术设计 | 1.5h |
| [4] Figma 原型 | 1.0h |
| [5] TDD 实现 | 4.0h |
| [6] 一致性体检 | 0.5h |
| [7] 上线检查 | 1.0h |
| **合计** | **~9h** |

---

## 7. 项目目录结构

```
leyoSwimming/
├── AGENTS.md                     # 本文件（项目级 AI 协作规范）
├── docs/
│   ├── prd/prd.md                # Master PRD
│   ├── spec/                     # 规范类文档
│   │   ├── user-story/           #   US 规范（INDEX / SPECIFICATION / TEMPLATE / EXAMPLE）
│   │   ├── tech-design/          #   技术设计规范
│   │   └── test-plan/            #   测试计划规范
│   ├── figma/                    # 全局设计系统规范（L1 + L1.5 + L2 交割清单）
│   ├── stories/                  # US 库（每个 US = 1 个子目录，3 件套）
│   │   └── US-XXX-.../
│   │       ├── user-story.md     #   业务 GWT + Figma 链接 + 设计决策
│   │       ├── tech-design.md    #   API/数据/状态机
│   │       └── test-plan.md      #   TDD 任务清单 + 测试用例
│   └── archive/                  # 历史版本
├── miniapp-user/                 # 微信小程序 - 用户端
├── miniapp-coach/                # 微信小程序 - 教练端
├── web-admin/                    # Web 后台 - 管理端
├── backend/                      # 后端 API 服务
├── shared/                       # 跨应用共享代码
├── deploy/                       # 部署与运维
└── tools/                        # 工具与脚本
```

### 7.1 约束规则

| # | 规则 |
|---|------|
| 1 | 顶层目录固定为 8 个 |
| 2 | 每个代码目录可有独立 AGENT.md |
| 3 | 文档单向引用代码（docs/ → 代码路径，仅引用不修改）|
| 4 | 代码目录禁止互引用（miniapp-* / web-admin 之间不允许直接引用）|
| 5 | 跨端通过 backend（API）+ shared（类型）通信 |

---

## 8. 用户故事生成硬约束

> AI Agent **每次生成 / 拆分 / 修订用户故事时**必须强制遵守本节。

### 8.1 生成 US 的强制流程

```
用户说"写 US-XXX" 或 "创建用户故事"
    ↓
Step 1: 调用 skill: openspec-new-change
    ↓
Step 2: 读取 docs/spec/user-story/SPECIFICATION.md
    ↓
Step 3: 读取 docs/spec/user-story/TEMPLATE.md
    ↓
Step 4: 读取 docs/spec/user-story/EXAMPLE.md
    ↓
Step 5: 读取 docs/spec/user-story/INDEX.md（确认编号未占用）
    ↓
Step 6: 创建 docs/stories/US-XXX-[角色]-[动作]/ 子目录
    ↓
Step 7: 复制 TEMPLATE.md → user-story.md，填充 15 章
    ↓
Step 8: 创建 tech-design.md + test-plan.md 占位
    ↓
Step 9: 填写 user-story.md（15 章完整）
    ↓
Step 10: 同步创建 OpenSpec 4 件套
    │  创建 openspec/changes/us-XXX-[kebab-case]/ 目录
    │  生成 .openspec.yaml / proposal.md / specs/<capability>/spec.md / design.md / tasks.md
    │  内容从 docs/stories/US-XXX-.../ 三件套映射而来
    ↓
Step 11: 运行 `openspec validate us-XXX-[kebab-case] --json` 并确保通过
    │  未通过则修复文档，禁止跳过
    ↓
Step 12: 附加 Agent 自检清单（§8.5）
    ↓
Step 13: 更新 INDEX.md 注册表
    │  仅当 OpenSpec validate 通过后才允许将状态从 [DRAFT] 更新为 [REVIEW]
    ↓
Step 14: 输出覆盖性自检报告（§8.4）
```

**红线**：跳过任何一步 = 不合格。

### 8.2 章节完整性（强制 15 章）

```
1.  基本信息              → 6 字段
2.  触发条件              → 触发方 / 动作 / 时机
3.  前置条件              → checkbox 列表
4.  业务流程              → 主路径 + 异常分支
5.  业务规则引用          → 引用 prd.md §x.y.z
6.  验收标准（Gherkin）   → ≥ 1 正常 + 2 异常
7.  数据/API/状态机影响   → 新增/修改/索引
8.  边界场景              → ≥ 3 个异常场景
9.  依赖关系              → 前置 US + 后续 US
10. INVEST 自检           → 6 项 checkbox
11. 完整性检查            → 字段/规则/标准/场景 4 大类
12. 备注                  → 幂等键/事务边界/性能要求
13. Figma 链接            → Figma file URL + frame + 状态截图清单
14. 页面级设计决策         → 本故事独有决策（不重复全局规范）
15. 设计评审记录           → 评审人/反馈/处置
```

**少任一章 = 不合格。**

### 8.3 验收标准硬约束（Gherkin）

| # | 约束 |
|---|------|
| 1 | 场景数量 ≥ 3（1 正常 + 2 异常） |
| 2 | 场景用业务语义命名（禁止"测试 1"） |
| 3 | 每条 Then 必须含具体数值/状态码/错误码/DB 字段值 |
| 4 | 业务规则必须可被 GWT 验证 |

### 8.4 覆盖性检查（批次合并前必跑）

每完成一批 US 后，Agent **必须**输出：

```markdown
## 覆盖性自检报告

### 1. MVP 能力覆盖（10 项必全覆盖）
| 能力 | 涉及 US |
|------|---------|

### 2. 状态机转换触达（5 大状态机必全触达）
| 状态机 | 涉及 US |
|--------|---------|
```

### 8.5 Agent 自检清单（每次交付 US 必填）

```markdown
## Agent 自检

- [ ] 已调用 skill: openspec-new-change
- [ ] 已读取 SPECIFICATION.md
- [ ] 已读取 TEMPLATE.md
- [ ] 已参考 EXAMPLE.md
- [ ] 已读取 INDEX.md 确认编号未占用
- [ ] US 子目录 docs/stories/US-XXX-.../ 下已建立 3 件套
- [ ] 15 个章节齐全（§1-§15），顺序与模板一致
- [ ] 标题符合 [角色] + [动作] + [目标]
- [ ] 目录名符合 US-XXX-[角色]-[动作]/
- [ ] ≥ 3 个 GWT 场景（1 正常 + 2 异常）
- [ ] ≥ 3 个边界场景
- [ ] 业务规则引用 prd.md §x.y.z
- [ ] §10 INVEST 至少 5/6 通过
- [ ] **OpenSpec change 目录 `openspec/changes/us-XXX-.../` 已同步创建 4 件套**
- [ ] **`openspec validate us-XXX-... --json` 已通过（valid: true, issues: []）**
- [ ] 覆盖性自检报告已生成
```

**未勾选全部 = 不允许提交。**

### 8.6 命名规范

| # | 约束 | 正例 |
|---|------|------|
| 1 | 标题：`[角色] + [动作] + [目标]` | "学员单次预约教练时段" |
| 2 | 角色术语：仅用 `学员 / 教练 / 管理员 / 系统 / 游客` | — |
| 3 | PRD 引用：`§x.y.z` 格式 + 链接到 prd.md | "[§5.3.1](../../prd/prd.md)" |
| 4 | 估时：人天，0.5-3 天 | "1.5 人天" |
| 5 | 编号：三位数连续，删除不回收 | — |

---

## 9. 文档三件套职责边界

```
user-story.md   → PM：业务需求 + GWT + Figma 链接 + 页面级设计决策 + 评审记录
                       （"用户要什么"）
tech-design.md  → 开发：数据模型 + API + 状态机 + 缓存 + 性能 + 安全 + 跨 US 依赖
                       （"用什么技术实现"）
test-plan.md    → 开发+QA：TDD 任务清单（RED→GREEN→COMMIT）+ 测试用例
                       （"怎么一步步做出来并验证"）
```

| 内容 | 归属文件 |
|------|---------|
| API 路径 + 字段定义 | tech-design.md |
| API 的具体测试代码 | test-plan.md |
| 数据模型 / 索引 | tech-design.md |
| 缓存策略 | tech-design.md |
| 缓存相关的 TDD 步骤 | test-plan.md |
| 性能指标目标 | tech-design.md |
| 性能验证 task | test-plan.md |
| Figma 链接 + 状态截图 | user-story.md §13 |
| 页面级设计决策 | user-story.md §14 |
| 设计评审记录 | user-story.md §15 |
| 业务级 GWT | user-story.md §6 |
| TDD 逐任务执行步骤 | test-plan.md |

---

## 10. 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-27 | 初版：GitHub 同步规范 |
| v1.1 | 2026-07-28 | 新增页面发布规范（四步检查） |
| v1.2 | 2026-07-28 | 新增 SDD + TDD 七步流水线 |
| v1.3 | 2026-07-29 | US 拆分规范强制引用 |
| v1.4 | 2026-07-30 | US 规范迁移至 docs/spec/user-story/ |
| v1.5 | 2026-07-30 | 目录结构升级：3 件套子目录 |
| v1.6 | 2026-07-30 | test-plan 升级为双重角色 |
| v1.7 | 2026-07-30 | 拆分为 3 件套，删除故事级 figma.md |
| v2.0 | 2026-07-30 | **重命名为 AGENTS.md**；新增 §0 启动检查（Skills 触发规则）；收紧 US 生成流程为 12 步强制；统一 MVP 能力为 10 项 |
| v2.1 | 2026-07-30 | 接入 OpenSpec CLI v1.7.0；新增 §2.5（目录结构 + 文档映射 + 常用命令）；US-001 迁移完成并通过 `openspec validate` |
| v2.2 | 2026-07-30 | **强规范**：US 生成时必须同步创建/更新 OpenSpec 4 件套并通过 `openspec validate`；新增 §0.3 禁止项 5-6、§2.5.4 强规范约束、§6.2 流水线产物、§8.1 Step 10-11、§8.5 OpenSpec 自检项；记录 US-005~US-014 漏生成 OpenSpec 的历史教训 |
