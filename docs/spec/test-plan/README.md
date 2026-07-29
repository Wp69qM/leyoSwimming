# 测试计划 / 实施任务规范

> **本目录用途**：定义 `docs/stories/US-XXX-.../test-plan.md` 的**双重角色**标准格式。
> **文档状态**：✅ 已生效
> **最后更新**：2026-07-30

---

## 0. 双重角色说明

`test-plan.md` 在项目中承担**双重角色**，避免文件膨胀：
- **TDD 任务清单**（superpowers writing-plans 风格）：每个 task = 2-5 分钟可执行单元
- **测试计划**：覆盖 [user-story.md](../user-story/SPECIFICATION.md) 中所有 GWT 场景

> **为什么不单独建 tasks.md？**
> 4 件套（user-story / tech-design / test-plan / figma）保持精简，避免文件碎片化。
> test-plan 本身已包含可执行单元（task），把"实现步骤"也纳入即可承担 tasks 角色。

---

## 1. 必填章节（标准结构）

每个 `test-plan.md` 必须按以下顺序包含：

| # | 章节 | 必填 | 内容 |
|---|------|------|------|
| 0 | 双重角色说明 | ✅ | 解释 test-plan 承担 tasks + 测试计划双重角色 |
| 1 | Task 概览表 | ✅ | Task 编号 / 标题 / 优先级（P0/P1） / 对应 GWT 场景 |
| 2 | 实施任务 | ✅ | 每个 Task 含 Files / Step 1-5 / 完整代码 |
| 3 | 任务执行纪律 | ✅ | 顺序 / RED-GREEN-COMMIT / 禁止 placeholder / P0/P1 区分 |
| 4 | 上下游引用 | ✅ | user-story / tech-design / figma 链接 |
| 5 | 变更日志 | ✅ | 版本 / 日期 / 作者 / 变更 |

---

## 2. Task 概览表模板（必填）

```markdown
## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | [组件名 + 责任] | P0 | [user-story.md §6.x](./user-story.md#6x-...) |
| 2 | ... | ... | ... |
```

**规则**：
- Task 按**实现顺序**编号（自底向上：Repository → Service → Controller → UI）
- 每个 Task 头部明确「对应 GWT 场景」编号
- P0 / P1 标签必填；MVP 阶段 P0 必做，P1 视进度

---

## 3. 实施任务模板（核心）

每个 Task 严格按以下 5 步执行：

````markdown
### Task N: [组件名 + 责任] [P0/P1]

**Files:**
- Create: `exact/path/to/file.ts`
- Modify: `exact/path/to/existing.ts:123-145`
- Test: `tests/exact/path/to/test.ts`

**对应 GWT**：[user-story.md §6.x 场景名](./user-story.md#6x-...)

- [ ] **Step 1: 写失败测试**

```typescript
// tests/exact/path/to/test.ts
describe('Component.action', () => {
  it('returns X when Y', () => {
    // 完整测试代码
    expect(result).toBe(expected);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- <test_file>`
Expected: FAIL with `<具体错误信息>`

- [ ] **Step 3: 写最小实现**

```typescript
// exact/path/to/file.ts
export class Component {
  action() {
    // 完整实现代码
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- <test_file>`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add <files>
git commit -m "<type>(<scope>): <description>"
```
````

---

## 4. 强制纪律（红线）

| # | 纪律 | 违反后果 |
|---|------|----------|
| 1 | **严格 5 步循环**：Step 1 → 2 → 3 → 4 → 5 一个都不能少 | 测试沦为"补单" |
| 2 | **不允许 placeholder**：禁用 "TBD" / "TODO" / "实现 later" / "类似 Task N" | 工程师拿不到可执行内容 |
| 3 | **完整代码块**：每个 Step 必须有完整代码 / 命令 / 期望输出 | 不可执行 |
| 4 | **每 Task = 1 commit**：禁止跨 Task 累积 commit | git 历史无法回溯 |
| 5 | **每个 Task 明确对应 GWT**：头部必须引用 user-story.md 场景编号 | 规则与实现脱节 |
| 6 | **P0 必做 / P1 选做**：MVP 阶段 Task 列表中 P0 占 80%+ | 范围蔓延 |

---

## 5. 与其他规范的关系

| 维度 | user-story.md | tech-design.md | **test-plan.md（本文）** | user-story.md §13-15 |
|------|---------------|----------------|-------------------------|-------------------------|
| 角色 | PM 业务级 GWT + Figma 链接 | 开发 API/数据/状态机 | **开发 TDD 任务 + QA 测试** | 设计 UI 状态截图 + 页面决策 |
| 视角 | 业务（WHAT/WHY）| 设计（HOW）| **实现（DO）** | 视觉（LOOK）|
| 产出 | Gherkin 场景 | 数据模型 + API 契约 | **可执行 task 列表** | 链接 + 截图 + 评审记录 |
| SDD 映射 | proposal.md | plan.md / design.md | **tasks.md** | （嵌入 proposal）|

---

## 6. 命名约定

- **文件命名**：`test-plan.md`（固定，不改名）
- **Task 命名**：`Task N: [组件名 + 责任]`（如 `Task 3: GET /coaches 列表 API 端点`）
- **commit 类型**：`feat` / `fix` / `refactor` / `test` / `docs`（遵循 Conventional Commits）

---

## 7. 适用技术栈

本规范**不绑死**具体技术栈，但代码示例假设：
- **Backend**：TypeScript + Koa + MySQL（项目实际栈见 [docs/prd/prd.md](../../../prd/prd.md)）
- **Frontend**：Taro + React（项目实际栈见 `agent.md §10.2`）
- **测试**：Jest（前端） + supertest（后端 HTTP）

如使用其他技术栈，**保持 5 步结构**即可，具体命令 / 代码相应替换。

---

## 8. 上下游引用

- **上游**：[docs/spec/user-story/SPECIFICATION.md](../user-story/SPECIFICATION.md) 约束 US 业务级 GWT
- **上游**：[docs/spec/tech-design/](../tech-design/) 约束技术设计（test-plan 依赖其 API/数据模型）
- **下游产物**：`docs/stories/US-XXX-.../test-plan.md`（每 US 一份）

---

## 9. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-07-30 | QA | 初版占位（纯测试用例表格） |
| v1.0 | 2026-07-30 | Dev | 升级为双重角色规范（TDD 任务清单 + 测试计划）；US-001 完整示范 5 个 task |
