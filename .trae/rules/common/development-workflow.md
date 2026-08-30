# Development Workflow

> This file extends [common/git-workflow.md](./git-workflow.md) with the full feature development process that happens before git operations.

The Feature Implementation Workflow describes the development pipeline: research, planning, TDD, code review, and then committing to git.

## Feature Implementation Workflow

0. **Research & Reuse** _(mandatory before any new implementation)_
   - **GitHub code search first:** Run `gh search repos` and `gh search code` to find existing implementations, templates, and patterns before writing anything new.
   - **Library docs second:** Use Context7 or primary vendor docs to confirm API behavior, package usage, and version-specific details before implementing.
   - **Exa only when the first two are insufficient:** Use Exa for broader web research or discovery after GitHub search and primary docs.
   - **Check package registries:** Search npm, PyPI, crates.io, and other registries before writing utility code. Prefer battle-tested libraries over hand-rolled solutions.
   - **Search for adaptable implementations:** Look for open-source projects that solve 80%+ of the problem and can be forked, ported, or wrapped.
   - Prefer adopting or porting a proven approach over writing net-new code when it meets the requirement.

1. **Plan First**
   - Use **planner** agent to create implementation plan
   - Generate planning docs before coding: PRD, architecture, system_design, tech_doc, task_list
   - **API 设计必须遵循 [api-convention.md](../../../docs/tech/api-convention.md)**：统一使用 `POST`，URL 按 `/list`、`/detail`、`/add`、`/update`、`/delete`、业务动作命名，参数通过 JSON body 传递
   - Identify dependencies and risks
   - Break down into phases

2. **TDD Approach（强制 Gate）**
   - 任何新功能、Bug 修复、重构开始前，**必须**先调用 **tdd-guide** agent
   - 在没有对应测试覆盖的情况下，禁止编写业务实现代码
   - Write tests first (RED)
   - Implement to pass tests (GREEN)
   - Refactor (IMPROVE)
   - Verify 80%+ coverage
   - 如跳过 tdd-guide，视为流程违规，必须在提交前补测试并由 code-reviewer 确认

3. **Code Review（强制 Gate）**
   - 代码编写或修改完成后，**必须**立即调用 **code-reviewer** agent
   - code-reviewer 必须检查 [testing.md](./testing.md) §执行检查清单 与 §code-reviewer 强制检查项
   - 未通过测试/覆盖率 Gate 的代码，不得进入 Commit & Push 阶段
   - Address CRITICAL and HIGH issues
   - Fix MEDIUM issues when possible

4. **Commit & Push**
   - Detailed commit messages
   - Follow conventional commits format
   - See [git-workflow.md](./git-workflow.md) for commit message format and PR process

5. **Pre-Review Checks**
   - Verify all automated checks (CI/CD) are passing
   - Resolve any merge conflicts
   - Ensure branch is up to date with target branch
   - Only request review after these checks pass
