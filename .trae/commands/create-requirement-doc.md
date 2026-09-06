---
name: "Create Requirement Doc"
description: "需求全链路：写需求文档 → 更新 PRD → 拆分用户故事（/create-user-story）→ 同步 OpenSpec/INDEX → page-spec 原型规格（/create-requirement-doc）"
---

从一个需求构想到完整的需求设计产物链：需求文档 → PRD 集成 → 用户故事三件套 → OpenSpec/INDEX 同步 → page-spec 原型规格。对应 `docs/prd/bling.md` 想法 1。

本命令是**编排命令**：US 创建环节复用 `/create-user-story` 的全部规范逻辑，不重复定义；每个阶段之间设 STOP 确认点，不一口气跑完。

**Input**: `/create-requirement-doc` 后的参数为需求描述（一段话或需求名），也可来自 `docs/prd/需求池.md` 的条目编号。

**Steps**

### Phase 0：输入澄清

1. 无输入时询问（开放式）：
   > "要立项什么需求？请描述背景、目标和期望的用户价值。"
2. Read `docs/prd/需求池.md`：若需求来自需求池条目，记录条目编号，Phase 5 回写立项标记
3. 主动澄清（用 AskUserQuestion）：目标用户与角色、MVP 边界、是否涉及新状态机/数据表、是否有明确 out-of-scope
4. 按 `.trae/rules/spec/spec-routing.md` 输出「Spec 开工检查清单」，并用 TodoWrite 建立 Phase 1-5 任务

### Phase 1：撰写需求文档

1. Read 范例 `docs/tech/AI-assistant-requirements/rag-function-calling-requirements.md` 作为结构参照
2. 产出 `docs/prd/<功能名>-requirements.md`（路径默认 `docs/prd/`，用户可指定子目录），章节结构：
   - 头部：文档性质 / 版本 / 创建日期 / 适用范围
   - §1 背景与目标（1.1 背景 / 1.2 目标，目标必须是可验证的业务结果）
   - §2 需求范围（2.1 In Scope / 2.2 Out of Scope，明确 MVP 边界）
   - §3 用户故事（**草稿级**：每个 US 一句「作为<角色>，我希望<动作>，以便<价值>」+ 3-5 条简要验收标准，不展开 15 章节）
   - §4 架构设计（整体架构图 + 核心数据流，概述级，细节留给 tech-design）
   - §5 非功能需求与风险（性能/安全/成本要求，可精简）
   - 附录：变更日志
3. **STOP**：向用户展示需求文档，确认后才进入 Phase 2

### Phase 2：PRD 集成

检查需求是否引入新业务规则、状态机转换或数据实体：

- **是** → 更新 `docs/prd/prd.md` 对应章节（新规则写入对应 §5.x 角色模块 / §6.x 状态机 / 数据模型章节），并在 PRD changelog 记录。US 的 §5 业务规则引用**必须指向 prd.md 真实存在的章节**，这一步是前提
- **否**（纯复用既有规则）→ 跳过，说明理由
- **STOP**：PRD 变更需用户确认

### Phase 3：拆分用户故事

1. 将需求文档 §3 的草稿 US 按 `docs/spec/user-story/SPECIFICATION.md` §5 拆分粒度复核：估时 0.5-3 人天、单一职责、跨页面/跨状态机拆分；不合理先调整草稿
2. 展示 US 清单（编号范围、标题、复杂度等级 L1/L2/L3、估时、依赖顺序），**STOP 确认**后才批量创建
3. 编号从 INDEX.md 当前最大值起**顺序分配**，避免并发冲突
4. 对每个 US 逐一执行 `/create-user-story` 的完整流程（读 spec → 建 `docs/stories/US-XXX-[角色]-[动作]/` 三件套 → 更新 INDEX.md → OpenSpec 同步）
   - 若委派 Sub-Agent 执行，任务描述必须附加：按 `.trae/rules/spec/spec-routing.md` 输出开工清单后才能写入
5. 每完成一个 US 运行 `openspec validate --change <name>` 验证

### Phase 4：page-spec 原型规格

1. Read `docs/figma/page-spec/TEMPLATE.md` 与 `docs/figma/README.md`（设计系统规范入口）
2. 从各 US 的 §7 数据/API 影响与业务流程中提取页面清单，展示页面 × US 映射表，确认后创建
3. 为每个页面产出 `docs/figma/page-spec/<页面名>.md`：
   - 命名遵循 figma-frame-organization 惯例（前缀 U-/C-/A- 区分端）
   - **只描述元素与交互**（元素清单、显显隐规则、跳转矩阵、四态），**不写视觉样式细节**（颜色/尺寸/间距留给 Calicat 设计稿）
   - Calicat file URL 与 frame node-id 留占位，由设计完成后回填（batch-review 任务 6 处理）
4. 将 page-spec 链接回填到各 US 的 `user-story.md` §13（表格列头：内容 | 链接 | 状态）

### Phase 5：收尾汇报

输出：

- 产物清单：需求文档 / PRD 变更 diff / US 三件套列表 / INDEX.md 更新 / OpenSpec changes / page-spec 列表
- US 摘要表（编号 / 标题 / L 等级 / 估时 / 依赖）
- 若来自需求池：在 `docs/prd/需求池.md` 对应条目追加「已立项 → 链接需求文档」标记
- 后续指引："本批次需求设计完成后，运行 `/batch-review` 进入业务评审与状态流转（DRAFT → REVIEW → APPROVED），并生成 dev-plan-batchX.md"

**Guardrails**

- 每个 Phase 结束必须 STOP 等用户确认，禁止一口气跑完全链路
- 禁止跳过 spec-routing 开工清单直接写需求类文档
- US §5 禁止引用 PRD 中不存在的章节（Phase 2 未完成的规则不得进入 US）
- 草稿 US 复核不合格必须先调整需求文档，再拆分；技术任务（如「搭建 CI」）拦截并提示独立跟踪
- page-spec 禁止包含视觉样式细节；Calicat node-id 禁止编造，留占位待回填
- 新建 US 状态一律 `[DRAFT]`；本命令不推进状态，状态流转是 `/batch-review` 的职责
