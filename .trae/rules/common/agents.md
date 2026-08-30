# Agent Orchestration

## Available Agents

Located in `~/.claude/agents/`:

| Agent | Purpose | When to Use |
|-------|---------|-------------|
| planner | Implementation planning | Complex features, refactoring |
| architect | System design | Architectural decisions |
| tdd-guide | Test-driven development | New features, bug fixes |
| code-reviewer | Code review | After writing code |
| visual-reviewer | Visual/UI review against Calicat | After frontend code changes, before merge |
| security-reviewer | Security analysis | Before commits |
| build-error-resolver | Fix build errors | When build fails |
| e2e-runner | E2E testing | Critical user flows |
| refactor-cleaner | Dead code cleanup | Code maintenance |
| doc-updater | Documentation | Updating docs |
| batch-retrospective | Update batch lessons-learned | End of every frontend batch |
| rust-reviewer | Rust code review | Rust projects |
| harmonyos-app-resolver | HarmonyOS app development | HarmonyOS/ArkTS projects |

## Immediate Agent Usage

No user prompt needed:
1. Complex feature requests - Use **planner** agent
2. Code just written/modified - Use **code-reviewer** agent
3. Frontend page just written/modified - Use **visual-reviewer** agent after code-reviewer
4. Bug fix or new feature - Use **tdd-guide** agent
5. Architectural decision - Use **architect** agent

## Sub-Agent Frontend Task Mandatory Prefix

任何委托给 Sub-Agent 的前端页面开发任务（涉及 `miniapp-user/`、`miniapp-coach/`、`web-admin/` 的新增或修改页面），父 Agent 必须在任务描述开头强制附加以下前缀：

```markdown
**强制前置步骤（未执行前禁止写任何前端代码）**
1. 读取 `docs/figma/page-development-task-template.md`。
2. 根据模板生成当前任务的 `TodoWrite` 检查清单，替换所有占位符（PAGE_NAME、PAGE_SPEC_PATH、US_XXX、FILE_ID、MAIN_FRAME_NODE_ID、PROJECT_PATH）。
3. 完成开工前 Phase 1 所有项目后，方可开始 `Write`/`Edit` 前端文件。
4. 开发完成后必须调用 `code-reviewer` 与 `visual-review` Agent，通过后方可返回结果。
```

本规则与 `.trae/rules/figma/page-dev-must-checklist.md` 配套使用。父 Agent 不得以「任务紧急」或「上下文已包含规则」为由省略此前缀。

## Parallel Task Execution

ALWAYS use parallel Task execution for independent operations:

```markdown
# GOOD: Parallel execution
Launch 3 agents in parallel:
1. Agent 1: Security analysis of auth module
2. Agent 2: Performance review of cache system
3. Agent 3: Type checking of utilities

# BAD: Sequential when unnecessary
First agent 1, then agent 2, then agent 3
```

## Delegation Completion Contract

Applies to every agent at every depth (parent, child, grandchild):

1. **Your final message IS the deliverable.** Never end your turn with "waiting for background agents" — a spawned task is not a completed task. Ending your turn while children are running orphans their results (completed children cannot notify a parent whose turn has ended).
2. **If you delegate, you own collection.** Wait for results, integrate them, then return. Fire-and-forget delegation is forbidden.
3. **Decompose only when the work cannot fit in one context.** Do not re-delegate a task already sized for a single agent — depth is an outcome, not a plan.

> Rationale: observed failure mode — research agents followed "Parallel Task Execution" above, spawned children, and returned "waiting" as their final answer. All children completed successfully but their results were orphaned. The parallel rule without a completion contract produces zombie tasks.

## Multi-Perspective Analysis

For complex problems, use split role sub-agents:
- Factual reviewer
- Senior engineer
- Security expert
- Consistency reviewer
- Redundancy checker
