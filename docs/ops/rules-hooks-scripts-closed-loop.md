# Rules + Hooks + Scripts 强制闭环说明

> **文档定位**：说明本项目如何把 Trae workspace rules、Git hooks、PowerShell scripts、Agent orchestration 四层机制组合成可落地的强制闭环，覆盖「每日工作日志自动更新」和「前端批次视觉还原自提升」两个场景。
> **配套文件**：
> - `.trae/rules/figma/calicat-mcp-hooks.md`
> - `.trae/rules/common/agents.md`
> - `.trae/agents/visual-reviewer.md`
> - `.trae/agents/batch-retrospective.md`
> - `scripts/update-daily-log.ps1`
> - `scripts/update-batch-lessons.ps1`
> - `scripts/install-git-hooks.ps1`
> - `docs/figma/batch1-lessons-learned.md`
> - `docs/figma/第二批次页面梳理.md`

---

## 1. 三种机制的分工

| 机制 | 适用场景 | 本项目实现 | 强制程度 |
|------|---------|-----------|---------|
| **Git Hooks** | 有明确 Git 事件触发（commit / push / merge） | `post-commit` 调用 `scripts/update-daily-log.ps1` 更新 `docs/project-daily-log.md` | OS 级，hook 安装后随提交自动执行 |
| **Trae Workspace Rules + Agent Orchestration** | Agent 行为约束、开发前 / 中 / 后检查 | `.trae/rules/figma/calicat-mcp-hooks.md` 强制开工清单；`.trae/rules/common/agents.md` 强制使用 `visual-reviewer` / `batch-retrospective` | 模型级，Agent 必须读取并遵守 |
| **Scripts + Agents** | 无天然 Git 事件，需要人工 / Agent 触发 | `update-batch-lessons.ps1` 生成复盘输入，`batch-retrospective` Agent 更新经验手册 | 流程级，通过规则清单和批次规划文档强制卡住 |

---

## 2. 每日工作日志自动闭环（Git Hook）

### 2.1 触发时机

每次执行 `git commit` 后，`.git/hooks/post-commit` 自动触发。

### 2.2 数据流

```
git commit
    ↓
.git/hooks/post-commit
    ↓
scripts/update-daily-log.ps1
    ↓
读取最近一次 commit 信息
读取当天 ~/.trae-cn/memory/.../topics.md
    ↓
合并到 docs/project-daily-log.md（追加 [auto] 补充记录小节）
```

### 2.3 为什么是 Git Hook 而不是 Trae PostToolUse Hook

- `git commit` 是 shell 命令，不是 Trae 的 Tool Use，因此 `PostToolUse` hook 无法捕获。
- Git hook 是 OS 级机制，只要提交就会发生，与使用的 IDE / Agent 无关。

### 2.4 安装方式

```powershell
.\scripts\install-git-hooks.ps1
```

> Git hooks 不随仓库提交，新 clone 或 hooks 被覆盖后需重新安装。

---

## 3. 前端视觉还原自提升闭环（Rules + Agents + Scripts）

### 3.1 闭环概览

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 开工前：Calicat 开工检查清单（Rules 强制）                    │
│    → 读取 page-spec → 提取 file_id / node-id                 │
│    → 调用 mcp_calicat:get_design_data / get_screenshots       │
│    → 导出静态资源 → 生成 design-tokens.json                   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. 开发中：按图层实现 + Token 覆盖（Rules 强制）                 │
│    → 禁止从 page-spec 推断视觉值                              │
│    → 按 Calicat 图层自上而下实现                               │
│    → 四态覆盖、静态资源引用                                   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. 提交前：visual-review Agent（Agent Orchestration 强制）      │
│    → 对比 Calicat 截图与本地实现                               │
│    → 输出差异报告，HIGH 问题必须修复                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. 批次结束：batch-retrospective Agent + 经验手册更新           │
│    → 运行 update-batch-lessons.ps1 生成复盘输入               │
│    → 调用 batch-retrospective Agent                          │
│    → 更新 docs/figma/batch1-lessons-learned.md                │
└─────────────────────────────────────────────────────────────┘
                              ↓
                    下一批次复用更新后的经验手册
```

### 3.2 开工前：Calicat 开工检查清单

详见 `.trae/rules/figma/calicat-mcp-hooks.md` §3。

Agent 在写任何前端代码前，必须先在当前会话输出并逐项勾选：

```markdown
### Calicat 开工检查清单
- [ ] 已读取对应 page-spec：`<page-spec-path>`
- [ ] page-spec 中已包含 Calicat 链接与 node-id
- [ ] 已提取 file_id：`<file_id>`
- [ ] 已提取主 Frame node-id：`<node-id>`
- [ ] 已调用 `mcp_calicat:get_design_data` 获取图层数据
- [ ] 已调用 `mcp_calicat:get_screenshots` 导出设计稿截图
- [ ] 已确认需要导出的静态资源清单（Logo/图标/插画）
- [ ] 已生成或更新 `design-tokens.json`
```

### 3.3 提交前：visual-review Agent

`.trae/rules/common/agents.md` 已规定：

> Frontend page just written/modified - Use **visual-reviewer** agent after code-reviewer

`visual-reviewer` Agent 会：

1. 通过 `git diff --name-only` 定位变更页面。
2. 读取对应 `docs/figma/page-spec/*.md` 提取 Calicat 链接。
3. 检查 `src/assets/calicat/screenshots/` 是否有设计稿截图。
4. 对比实现截图，按颜色、字号、间距、布局、组件、图标、四态、响应式等维度输出差异。
5. 给出 `Approve` / `Warning` / `Block` 结论。

### 3.4 批次结束：batch-retrospective Agent + 经验手册更新

#### 为什么批次复盘不能用 Git Hook

Git hook 只能响应 Git 事件（commit / merge / push）。**一个批次结束没有天然 Git 事件**，因此无法用 hook 自动触发。本项目采用「规则 + 清单 + Agent」的方式强制卡住：

1. **批次规划文档**（如 `docs/figma/第二批次页面梳理.md` §6）明确声明：未完成复盘不得进入下一批次。
2. **Agent Orchestration Rules** 规定：批次结束时必须调用 `batch-retrospective` Agent。
3. **批次结束检查清单**必须由 Agent 输出并勾选完成后，方可继续下一批次。

#### 执行流程

```powershell
# Step 1：收集本批次复盘原始数据
.\scripts\update-batch-lessons.ps1 -Batch batch2 -StartDate 2026-08-15 -EndDate 2026-08-31

# 生成：tmp/batch-retrospective-input-batch2.md
```

```
# Step 2：调用 batch-retrospective Agent
# 输入：tmp/batch-retrospective-input-batch2.md
# 输出：更新 docs/figma/batch1-lessons-learned.md
```

#### 批次结束强制复盘检查清单

Agent 在批次结束时必须输出并勾选：

```markdown
### Batch End 强制复盘检查清单
- [ ] 本批次所有页面均已通过 `visual-review` Agent（无 HIGH / Block 问题）
- [ ] 已运行 `.\scripts\update-batch-lessons.ps1 -Batch batch{N} -StartDate <start> -EndDate <end>`
- [ ] 已生成 `tmp/batch-retrospective-input-batch{N}.md`
- [ ] 已调用 `batch-retrospective` Agent 读取复盘输入文件
- [ ] `docs/figma/batch1-lessons-learned.md` 已追加新增问题与检查项
- [ ] 批次规划文档（如 `docs/figma/第二批次页面梳理.md`）§6 / 变更日志已记录「复盘完成」
- [ ] 未发现未登记的视觉还原偏差或流程违规
```

---

## 4. 四层强制手段如何保证落地

### 4.1 第一层：规则强制（Rules）

- `.trae/rules/figma/calicat-mcp-hooks.md`：开发前必须输出开工检查清单，未勾选禁止写代码。
- `.trae/rules/common/agents.md`：前端代码修改后必须先用 `visual-reviewer`，再用 `batch-retrospective`。
- 规则文件属于 always-applied workspace rules，Agent 每次会话都会加载。

### 4.2 第二层：清单强制（Checklists）

- 开工清单：卡住「开始写代码」的动作。
- 批次结束清单：卡住「进入下一批次」的动作。
- 清单必须输出到当前会话并逐项勾选，形成可审计的记录。

### 4.3 第三层：Agent 强制（Agent Orchestration）

- `visual-reviewer` 和 `batch-retrospective` 不是可选项，是规则声明的 mandatory agent。
- Agent prompt 中明确说明 `MUST BE USED` 和触发条件。

### 4.4 第四层：脚本强制（Scripts）

- `update-daily-log.ps1`：由 Git hook 自动触发，减少人工遗漏。
- `update-batch-lessons.ps1`：生成结构化复盘输入，避免 Agent 凭空编造问题。
- `install-git-hooks.ps1`：确保 hook 在新环境也能快速安装。

---

## 5. 维护与排查

| 问题 | 检查点 | 修复方式 |
|------|--------|---------|
| 提交后没有自动更新 daily log | `.git/hooks/post-commit` 是否存在且可执行 | 重新运行 `.\scripts\install-git-hooks.ps1` |
| daily log 内容追加位置错误 | `scripts/update-daily-log.ps1` 的插入逻辑 | 检查脚本版本，确保在日期条目的 `---` 分隔线前插入 `[auto]` 小节 |
| 批次复盘没有触发 | 是否在批次规划文档中声明了强制复盘 | 在 `docs/figma/第二批次页面梳理.md` §6 等文档中保留复盘流程 |
| visual-review 报告缺失 | `tmp/visual-reviews/` 目录是否存在 | 每次前端修改后显式调用 `visual-reviewer` Agent |
| 新 clone 仓库 hook 失效 | Git hooks 不随仓库提交 | 将 `.\scripts\install-git-hooks.ps1` 加入环境初始化流程 |

---

## 6. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-15 | AI | 初版：说明 rules / hooks / scripts / agents 四层机制如何形成每日日志与批次视觉还原两个闭环 |
