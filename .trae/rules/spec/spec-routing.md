# 需求文档 / 用户故事 Spec 路由规则

> **定位**：强制 Agent 在创建或修改需求类文档（用户故事、技术设计、测试计划）前，先读取 `docs/spec/` 中对应的规范文档。本规则只做「路由」，规范本体始终以 `docs/spec/` 为唯一来源，规则中不复述规范内容。
> **适用范围**：所有生成或修改 `docs/stories/US-*/**`、`docs/spec/user-story/INDEX.md`，或涉及「创建用户故事 / 拆分 US / 写需求文档 / 补 tech-design / 补 test-plan」语义的 Agent/Sub-Agent。
> **配套文档**：
> - [docs/spec/user-story/SPECIFICATION.md](../../../docs/spec/user-story/SPECIFICATION.md) — US 拆分规范
> - [docs/spec/tech-design/README.md](../../../docs/spec/tech-design/README.md) — 技术设计规范
> - [docs/spec/test-plan/README.md](../../../docs/spec/test-plan/README.md) — 测试计划规范

---

## 1. 核心原则

1. **`docs/spec/` 是需求类文档规范的唯一来源**，本规则只负责「何时读哪份规范」。
2. **任何需求类文档写作任务的第一步是读规范，而不是写文档。**
3. **未输出并勾选开工前清单前，禁止执行任何针对 `docs/stories/` 的 `Write`/`Edit` 操作。**
4. 规范文档的修订走 `docs/spec/` 各文档自身的 changelog，**不修改本规则**。

---

## 2. 触发条件

以下任何任务开始前都必须执行本规则：

- 新增用户故事（创建 `docs/stories/US-XXX-[角色]-[动作]/` 三件套）
- 修改现有 `user-story.md` / `tech-design.md` / `test-plan.md` 的内容
- 更新 `docs/spec/user-story/INDEX.md` 的注册表、状态或依赖关系
- 用户说出「创建用户故事」「拆分 US」「写需求文档」「补充技术设计」「补充测试计划」等语义时
- 收到 `us-business-reviewer` 评审反馈并需要修复 US 文档时

---

## 3. 路由表（何时读哪份规范）

| 任务类型 | 必读规范 | 按需参考 |
|---------|---------|---------|
| 创建/修改 `user-story.md` | `docs/spec/user-story/SPECIFICATION.md`（15 章节 + L1/L2/L3 场景分级 + 反模式清单） | `TEMPLATE.md`（复制来源）、`EXAMPLE.md`（填写示例）、`INDEX.md`（编号/依赖/状态） |
| 创建/修改 `tech-design.md` | `docs/spec/tech-design/README.md`（11 必填章节 + 禁止项） | 本 US 的 `user-story.md` |
| 创建/修改 `test-plan.md` | `docs/spec/test-plan/README.md`（双重角色 + 5 步 TDD 模板） | 本 US 的 `user-story.md` §6、`tech-design.md` |
| 注册/更新 INDEX.md | `docs/spec/user-story/INDEX.md` §1-§4 既有结构 | — |
| US 业务评审 | `.trae/skills/us-business-reviewer/SKILL.md` | 评审报告目录 `docs/guides/` |

---

## 4. 强制步骤

### Step 1：按路由表读取规范

创建用户故事类文档前必须依次 `Read`：SPECIFICATION → TEMPLATE →（需要时）EXAMPLE；技术设计 / 测试计划任务分别读取对应 README。

### Step 2：输出开工检查清单

在当前会话输出以下清单并替换 `<...>` 为实际值，全部勾选后方可写入：

```markdown
### Spec 开工检查清单
- [ ] 已读取拆分规范：`docs/spec/user-story/SPECIFICATION.md`
- [ ] 已读取模板：`docs/spec/user-story/TEMPLATE.md`
- [ ] 已确认 US 编号（INDEX.md 下一个未占用编号）：`US-XXX`
- [ ] 已确认目录名：`docs/stories/US-XXX-<角色>-<动作>/`
- [ ] 已确认前置 US 与能力归属（INDEX.md §1/§2）
- [ ] 已按 15 章节结构生成 TodoWrite 任务清单
```

tech-design / test-plan 任务将前两项替换为对应 README，其余项保留。

### Step 3：按规范执行写入

- `user-story.md` 只能从 `TEMPLATE.md` 复制起步，**禁止自创结构**
- 新建 US 状态一律 `[DRAFT]`；状态流转 `[DRAFT] → [REVIEW] → [APPROVED]` 只能由评审流程推进（`us-business-reviewer`）
- 写入完成后同步更新 `INDEX.md`：§1 清单行、§2 能力覆盖映射、§3 状态机触达、§4 依赖关系、§6 变更日志

### Step 4：OpenSpec 同步

`docs/stories/US-*/**` 写入后，按 [openspec/hooks.md](../openspec/hooks.md) 的流程同步对应 active change；未找到匹配 change 时提示用户，不静默跳过。

---

## 5. 违规处理

若发现 Agent 未读规范即开始写需求类文档：

1. 立即暂停写入，回退到 Step 1
2. 已写文档按 `us-business-reviewer` 评审结果决定重写或修复
3. 在 [docs/figma/batch1-lessons-learned.md](../../../docs/figma/batch1-lessons-learned.md) 中登记本次违规，用于下一轮复盘

---

## 6. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-09-06 | AI | 初版：定义 spec 路由表、开工检查清单、INDEX 同步与 OpenSpec 衔接 |
