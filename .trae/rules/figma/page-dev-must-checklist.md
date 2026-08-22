# 前端页面开发强制检查清单规则

> **定位**：强制 Agent 在任何前端页面开发任务开始时，必须先读取模板并生成 `TodoWrite` 检查清单。
> **适用范围**：所有生成或修改 `miniapp-user/`、`miniapp-coach/`、`web-admin/` 前端页面的 Agent/Sub-Agent。
> **配套文档**：
> - [docs/figma/page-development-task-template.md](../../../docs/figma/page-development-task-template.md)
> - [docs/figma/batch1-lessons-learned.md](../../../docs/figma/batch1-lessons-learned.md)
> - [.trae/rules/figma/calicat-mcp-hooks.md](../../../.trae/rules/figma/calicat-mcp-hooks.md)

---

## 1. 核心原则

1. **前端页面开发任务的第一步永远是生成检查清单，而不是写代码。**
2. **检查清单必须从持久化模板生成，不能凭当前会话上下文临时构造。**
3. **未输出并勾选开工前清单前，禁止执行任何 `Write`/`Edit` 前端文件操作。**

---

## 2. 触发条件

以下任何任务开始前都必须执行本规则：

- 新增任何页面组件/视图文件（`.vue`、`.tsx`、`.jsx`、`.scss` 等）。
- 修改现有页面的结构、样式、布局、配色、图标、图片。
- 修改 `page-spec` 中 Calicat 链接或 node-id 后，同步调整前端实现。
- 从 `visual-review` / `code-reviewer` 收到视觉还原偏差修复要求时。
- 用户说出「做页面」、「开发页面」、「实现页面」、「开始 XXX 页」等语义时。

---

## 3. 强制步骤

### Step 1：读取模板

Agent 必须首先调用 `Read`：

```
docs/figma/page-development-task-template.md
```

### Step 2：生成 TodoWrite

根据模板 §3 的 Must Do 清单，调用 `TodoWrite` 生成当前任务的检查清单。必须替换以下占位：

- `PAGE_NAME` → 页面中文名
- `PAGE_SPEC_PATH` → 对应 page-spec 路径
- `US_XXX` → 归属用户故事
- `FILE_ID` → Calicat file_id
- `MAIN_FRAME_NODE_ID` → 主 Frame node-id
- `PROJECT_PATH` → 前端项目路径

### Step 3：按清单执行

- 开工前 Phase 1 全部勾选后，方可进入代码实现。
- 开发中 Phase 2 每条作为约束贯穿实现过程。
- 提交前 Phase 3 全部勾选后，方可提交。

---

## 4. 清单输出模板

Agent 必须在当前会话中输出以下检查清单（可直接用于 `TodoWrite`）：

```markdown
### Calicat 开工检查清单

- [ ] 已读取对应 page-spec：`<PAGE_SPEC_PATH>`
- [ ] page-spec 中已包含 Calicat 链接与 node-id
- [ ] 已提取 file_id：`<FILE_ID>`
- [ ] 已提取主 Frame node-id：`<MAIN_FRAME_NODE_ID>`
- [ ] 已调用 `mcp_calicat:get_design_data` 获取图层数据
- [ ] 已调用 `mcp_calicat:get_screenshots` 导出设计稿截图
- [ ] 已确认需要导出的静态资源清单（Logo/图标/插画）
- [ ] 已生成或更新 `design-tokens.json`
```

---

## 5. 违规处理

若发现 Agent 未读取模板、未生成 TodoWrite 即开始前端开发：

1. 立即暂停该页面开发。
2. 要求 Agent 回退到 Step 1。
3. 已写代码按 `visual-review` 结果决定是否重写或修复。
4. 在 [docs/figma/batch1-lessons-learned.md](../../../docs/figma/batch1-lessons-learned.md) 中登记本次违规，用于下一轮复盘。

---

## 6. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-15 | AI | 创建：前端页面开发强制检查清单规则，要求每次页面任务从持久化模板生成 TodoWrite |
