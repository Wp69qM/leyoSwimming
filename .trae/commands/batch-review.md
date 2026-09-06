---
name: "Batch Review"
description: "批次需求设计评审：生成第X批次页面梳理 → US 状态 DRAFT→REVIEW→APPROVED → 生成 docs/tech/dev-plan-batchX.md（/batch-review）"
---

对一个已完成需求设计的批次执行业务评审闭环：梳理批次文档 → 推进 US 状态 → 生成开发计划。对应 `docs/prd/bling.md` 想法 4。

**Input**: `/batch-review` 后的参数为批次号（如 `5`）或 US 列表（如 `US-066 US-067`）。

**Steps**

1. **无输入时确定批次范围**

   询问用户（开放式）：
   > "要评审哪个批次？请提供批次号（如 5）或本批次的 US 编号列表。"

   - 有批次号 → 用 `Glob docs/stories/US-*` + INDEX.md 确认该批次 US 清单（以用户确认为准）
   - 只有 US 列表 → 批次号 = 现有 `docs/figma/第X批次页面梳理.md` 最大编号 + 1
   - 开工前按 `.trae/rules/spec/spec-routing.md` 输出「Spec 开工检查清单」，并用 TodoWrite 建立 5 个阶段任务

2. **读取规范与范例**

   依次 Read：
   - `docs/spec/user-story/SPECIFICATION.md`（§12 检查清单）
   - `docs/spec/user-story/INDEX.md`
   - 最近一期批次梳理文档（如 `docs/figma/第四批次页面梳理.md`）作为结构模板
   - `docs/tech/api-convention.md`（API 规范检查依据）

3. **生成批次梳理文档 `docs/figma/第X批次页面梳理.md`**

   按既有批次结构产出 6 个章节：
   - 头部：批次目标 + 处理原则
   - §1 涉及的用户故事（按端分组：后台管理侧 / 用户侧 / 教练侧）
   - §2 待办任务清单，固定 7 项任务（按第四批次先例）：
     1. 按 api-convention 修正 API 路径与技术设计
     2. 三件套一致性检查 → **本 US 状态更新为 `[REVIEW]`**
     3. OpenSpec 变更产物检查（四件套齐全 + `openspec validate`）
     4. user-story §13 Figma 链接清理（只留 page-spec 引用）
     5. page-spec 与 US 相关性核对 → **状态更新为 `[APPROVED]`**
     6. Calicat MCP 设计稿对比与 node-id 回填（不变更状态）
     7. 生成 `docs/tech/dev-plan-batchX.md`
   - §3 建议执行顺序
   - §4 第X批次页面清单（页面 × US 映射表，按端分组）
   - §5 核心规则摘要（本批次业务红线）
   - §6 变更日志

   生成后向用户展示并确认，再进入评审。

4. **调用业务评审**

   通过 Skill 工具调用 `us-business-reviewer`，对本批次全部 US 执行评审：
   - 评审范围：目录/三件套完整性、INDEX 一致性、PRD 引用、状态机触达、跨 US 逻辑自洽
   - 评审报告写入 `docs/guides/`（沿用既有命名，如 `batch5-us-business-review-report.md`）
   - 汇总 P0/P1/P2 问题清单，向用户汇报

5. **状态推进（同步三处，缺一不可）**

   **推进 `[REVIEW]`**（评审开始时）与 **推进 `[APPROVED]`**（问题修复且复检通过后，需用户确认）各自同步：
   - 每个US 的 `user-story.md` 顶部状态字段
   - 每个US 的 `user-story.md` 设计评审记录章节，追加一行（日期 / 评审人 / 结论 / 处置）
   - `INDEX.md` §1 清单行状态 + §6 变更日志追加记录

   注意：
   - 状态标记统一使用 `[APPROVED]`（历史文档中的 `[APPROVAL]` 与之同义，新产出统一 `[APPROVED]`）
   - 存在未修复的 P0/P1 时**禁止**推进 `[APPROVED]`，先修复并复检
   - 修复 US 文档时同样受 spec-routing 规则约束（先读规范再改）

6. **生成开发计划 `docs/tech/dev-plan-batchX.md`**

   仅当本批次 US 全部 `[APPROVED]` 后执行。按既有 dev-plan 结构产出 10 个章节：
   1. 涉及用户故事（表格：US / 标题 / 状态 / 估时 / 优先级）
   2. 开发顺序与依赖关系（ASCII 依赖图 + 拓扑排序 + 并行分组建议，按端分组：Backend / ai-service / web-admin / 小程序 / 测试）
   3. 数据表变更（新增/修改表 + 索引）
   4. API 清单（backend + ai-service，**必须符合 api-convention**：统一 POST、`/list` `/detail` `/add` `/update` `/delete` 及业务动词、无 URL 路径参数、列表响应用 `data.list`）
   5. 关键实现点（各端新增/修改文件结构 + 核心逻辑）
   6. 测试策略（单元 / 集成 / E2E，映射 US §6 Gherkin 场景）
   7. 环境变量与配置
   8. 风险与回滚方案
   9. 验收标准（按 US 逐条列出）
   10. 相关文档（批次梳理 / US 三件套 / page-spec / OpenSpec changes 链接）

   数据表、API、实现点等内容从各 US 的 `tech-design.md` 汇总提炼，不凭空创造。

7. **收尾汇报**

   输出：
   - 梳理文档与 dev-plan 路径
   - 状态推进摘要表（US / 原状态 / 新状态）
   - 评审问题与修复情况
   - OpenSpec 同步结果（按 `.trae/rules/openspec/hooks.md` 流程）
   - 提示后续动作："可依据 dev-plan-batchX.md 进入开发。后端遵循 tdd-guide 流程；涉及前端页面时，委托 Sub-Agent 必须附加 Calicat 强制前置前缀（见 `.trae/rules/common/agents.md`）"

**Guardrails**

- 禁止跳过 `us-business-reviewer` 评审直接推进 `[APPROVED]`
- 禁止三处状态只改其一（user-story 顶部 / 评审记录 / INDEX.md 必须同步，历史 P0 教训）
- dev-plan 只能基于 `[APPROVED]` 的 US 生成；批次内有 US 未通过时，先输出差距清单并询问用户处置（修复 / 移出本批次）
- API 清单出现 GET/PUT/DELETE 或 URL 路径参数即违规，必须按 api-convention 修正后再写入 dev-plan
- 批次内 US 状态若与 INDEX.md 不一致，以评审实际结论为准双向校准，并在汇报中说明
