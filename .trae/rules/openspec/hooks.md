---
paths:
  - "docs/stories/**/*.md"
  - "docs/figma/page-spec/**/*.md"
  - "docs/spec/**/*.md"
---

# OpenSpec 自动同步 Hooks

> 本规则扩展 [common/hooks.md](../common/hooks.md)，用于在修改需求/设计文档后自动同步 OpenSpec 变更产物。

## PostToolUse Hooks

### 1. 触发条件

在以下文件发生 `Write` 或 `Edit` 后触发：

- `docs/stories/US-*/user-story.md`
- `docs/stories/US-*/tech-design.md`
- `docs/stories/US-*/test-plan.md`
- `docs/figma/page-spec/*.md`
- `docs/spec/**/*.md`

> 注意：`openspec/changes/**/*.md` 的编辑**不触发**本 hook，避免递归更新。

### 2. 行为

1. 从被修改文件路径中提取 US 编号（如 `US-055`）。
2. 运行 `openspec list --json`，查找名称以 `us-055-` 开头的 active change。
3. 如果找到唯一匹配 change：
   - 运行 `openspec status --change <name> --json` 确认当前 artifacts 状态。
   - **优先调用 `openspec-update-change` skill** 将 docs/stories 的改动同步回该 change 的 artifacts；如果该 skill 不可用，则通过 `openspec status` 获取 `existingOutputPaths` 后，使用 Read/Edit 手动将关键变更（尤其是 API 路径、数据模型、错误码）同步到对应 artifact。
   - **不要直接运行不存在的 `openspec update-change` CLI 命令**；当前 OpenSpec CLI 未提供该子命令。
4. 如果找到多个匹配 change，列出候选并询问用户选择；如果未找到，跳过并提示“未找到对应 OpenSpec change”。
5. 同步完成后，简要汇报哪些 artifact 被修改、是否存在冲突或需要人工确认的地方。

### 3. 限制

- 只在单个文件编辑完成后触发；批量并行编辑时，等待最后一步完成后再统一运行一次。
- 不自动提交、不自动归档；仅做 artifacts 一致性同步。
- 如果 OpenSpec CLI 不可用或命令失败，停止并报告错误，不要静默忽略。
