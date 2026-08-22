# Figma/Calicat 前端开发强制 Hooks

> **规则定位**：强制前端 Sub-Agent 在开发/修改任何页面前，先通过 MCP Calicat 读取设计稿，将视觉信息作为唯一最高优先级来源。
> **适用对象**：所有生成或修改 `miniapp-user/`、`miniapp-coach/`、`web-admin/` 前端页面的 Agent/Sub-Agent。
> **配套文档**：
> - [docs/figma/batch1-lessons-learned.md](../../../docs/figma/batch1-lessons-learned.md)
> - [docs/figma/page-spec/TEMPLATE.md](../../../docs/figma/page-spec/TEMPLATE.md)
> - [docs/tech/frontend-development-standards.md](../../../docs/tech/frontend-development-standards.md)

---

## 1. 核心原则

1. **Calicat 设计稿是视觉实现的唯一最高优先级来源。**
2. **page-spec 只负责交互逻辑、业务规则、状态流转，不负责视觉样式。**
3. **任何前端页面开工前，必须先读取 Calicat 设计稿；未读取设计稿不得开始写代码。**
4. **Logo、图标、插画、复杂视觉元素必须从 Calicat 导出为静态资源，禁止用 CSS 重绘。**

---

## 2. 触发条件

以下操作**必须**触发本规则：

- 新增任何页面组件/视图文件（`.vue`、`.tsx`、`.jsx`、`.scss` 等）。
- 修改现有页面的结构、样式、布局、配色、图标、图片。
- 修改 `page-spec` 中 Calicat 链接或 node-id 后，同步调整前端实现。
- `code-reviewer` / `visual-review` 发现视觉还原偏差并要求修复时。

---

## 3. 前置强制检查（开工前 Must Do）

**硬性要求**：Agent 在写任何前端代码之前，必须先在当前会话中输出以下「Calicat 开工检查清单」，逐项勾选并填写真实值。**未输出并勾选完成前，禁止执行任何 `Write`/`Edit` 前端文件操作。**

输出模板（必须原样输出到当前会话，并替换 `<...>` 为实际值）：

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

**执行规则**：
- 若 page-spec 中没有 Calicat 链接或 node-id，必须暂停开发，先补齐 page-spec，不得自行设计视觉。
- 若 `src/assets/calicat/screenshots/` 中缺少当前页面截图，必须先调用 `mcp_calicat:get_screenshots` 导出。
- 完成清单输出后，方可进入 §4 MCP 调用流程与代码实现。

---

## 4. MCP 调用流程（标准操作序列）

### 4.1 获取画布与页面列表

当不确定目标 Frame 所在画布时，先调用：

```
mcp_calicat:get_canvas_list
  args: { file_id: "<file_id>" }
```

### 4.2 获取页面图层列表

```
mcp_calicat:get_design_page_list
  args: { file_id: "<file_id>", canvas_id: "<canvas_id>" }
```

### 4.3 获取 Frame 结构骨架

```
mcp_calicat:get_meta_data
  args: { file_id: "<file_id>", selected_layer_id: "<frame_node_id>" }
```

### 4.4 获取完整图层数据（含样式、嵌套结构）

```
mcp_calicat:get_design_data
  args: { file_id: "<file_id>", selected_layer_id: "<layer_id>" }
```

### 4.5 获取交互设计数据

```
mcp_calicat:get_interaction_design_data
  args: { file_id: "<file_id>", selected_layer_id: "<layer_id>" }
```

### 4.6 导出设计稿截图

```
mcp_calicat:get_screenshots
  args: {
    file_id: "<file_id>",
    layer_ids: ["<frame_node_id>", "<empty_state_node_id>", "<loading_state_node_id>"]
  }
```

> **限制**：`get_screenshots` 单次最多 10 个 layer_id，超出需分批调用。

---

## 5. 设计资产导出规范

### 5.1 必须导出的资产

| 资产类型 | 格式优先级 | 存放路径 | 命名规则 |
|---------|-----------|---------|---------|
| Logo | SVG > WebP > PNG | `src/assets/calicat/logo/` | `logo-{usage}.{ext}` |
| 图标 | SVG > WebP | `src/assets/calicat/icons/` | `icon-{name}.{ext}` |
| 插画/空状态图 | WebP > PNG | `src/assets/calicat/illustrations/` | `illust-{scenario}.{ext}` |
| 营销图片/Banner | WebP > PNG | `src/assets/calicat/images/` | `img-{name}.{ext}` |
| 设计稿截图 | PNG | `src/assets/calicat/screenshots/` | `{page-name}-{state}.png` |

### 5.2 导出方式

1. 通过 `mcp_calicat:get_screenshots` 获取完整页面截图。
2. 对于可复用的矢量图标/Logo，优先要求设计师在 Calicat 中导出为 SVG 并提供 URL；若无法直接导出，可在实现阶段用截图替代并标记为 `TODO: replace with SVG`。
3. 所有导出的静态资源必须在代码中以相对路径引用，禁止硬编码外链。

---

## 6. 图层实现顺序

读取到 Calicat 图层数据后，按以下顺序实现：

1. **全局容器**：页面背景、画板尺寸、内边距、滚动区域。
2. **NavBar / 状态栏**：标题、返回按钮、自定义导航样式。
3. **TabBar**（如适用）：图标、文字、选中态。
4. **按垂直层级自上而下**实现各业务区块：
   - 状态提示条
   - 主信息卡
   - 内容列表/表单
   - 底部固定操作区
5. **四态覆盖**：为同一 Frame 的空状态、加载中、错误状态、成功状态分别实现。
6. **交互状态**：hover、active、disabled、选中态。

**禁止行为**：
- 跳过图层或把多个图层合并为一个 `div`。
- 用组件库默认组件替代 Calicat 自定义结构。
- 从 page-spec 文字推断尺寸/颜色。

---

## 7. Token 映射与样式覆盖

### 7.1 必须建立的覆盖文件

- `web-admin/src/styles/calicat-overrides.scss`
- `miniapp-user/src/styles/calicat-overrides.scss`
- `miniapp-coach/src/styles/calicat-overrides.scss`

### 7.2 Token 输出格式

每个页面开发前，根据 Calicat 图层数据输出 `design-tokens.json`：

```json
{
  "file_id": "2083742072257646592",
  "frame_id": "b4179121-e6a9-427a-8333-a751d5784f68",
  "page_name": "U-套餐详情页",
  "colors": {
    "primary": "#3B8BFF",
    "experience": "#FF8C3A",
    "background": "#F5F7FA"
  },
  "typography": {
    "title": { "fontSize": 18, "fontWeight": 600, "lineHeight": 24 },
    "body": { "fontSize": 14, "fontWeight": 400, "lineHeight": 20 }
  },
  "spacing": {
    "xs": 4, "sm": 8, "md": 16, "lg": 24, "xl": 32
  },
  "radius": {
    "card": 12, "button": 8, "tag": 4
  },
  "shadows": {
    "card": "0 2px 8px rgba(0,0,0,0.08)"
  }
}
```

### 7.3 组件库默认样式禁止直接使用

Element Plus、NutUI-React-Taro 等组件默认主题必须与 Calicat Token 对齐，未覆盖的默认样式不得使用。

---

## 8. 视觉还原检查点

### 8.1 自测要求

页面开发完成后，Agent 必须：

1. 在本地运行页面，截取实现截图。
2. 与 Calicat 截图并排对比。
3. 输出差异清单，登记为当前会话 issue 或直接修复。

### 8.2 visual-review Agent

合并前必须通过 `visual-review` Agent，检查项：

- [ ] 颜色误差 ≤1%（HEX 值完全一致或视觉上无法区分）。
- [ ] 间距误差 ≤2px。
- [ ] 字体字号一致。
- [ ] 主要元素位置、尺寸、对齐一致。
- [ ] Logo/图标/插画来源为 Calicat 导出资源。
- [ ] 四态设计完整。

### 8.3 PR 描述要求

每个前端页面 PR 必须包含：

- Calicat 设计稿截图
- 本地实现截图
- 差异说明（无差异则写"无可见差异"）
- `design-tokens.json` 路径
- 导出静态资源清单

---

## 9. 与现有规则的衔接

- [common/hooks.md](../common/hooks.md)：通用 Hook 类型与自动接受权限说明。
- [react/hooks.md](../react/hooks.md)：React 组件与 Hooks 规范。
- [react/coding-style.md](../react/coding-style.md)：组件命名、Props、JSX 规范。
- [openspec/hooks.md](../openspec/hooks.md)：需求文档变更后同步 OpenSpec。
- [common/code-review.md](../common/code-review.md)：代码审查 checklist。

---

## 10. 违规处理

若发现前端页面未按本规则读取 Calicat 即开始开发：

1. 立即暂停该页面开发。
2. 要求 Agent 回退到"读取 Calicat"步骤。
3. 已写代码按 visual-review 结果决定是否重写或修复。
4. 在 [docs/figma/batch1-lessons-learned.md](../../../docs/figma/batch1-lessons-learned.md) 中登记本次违规，用于下一轮复盘。

---

## 11. 批次结束强制复盘（自提升闭环）

> 本节与 [docs/ops/rules-hooks-scripts-closed-loop.md](../../../docs/ops/rules-hooks-scripts-closed-loop.md) 配套阅读。

每个前端批次（batch1 / batch2 / ...）开发完成后，Agent 必须执行复盘并更新经验手册，形成「本批次问题 → 经验手册 → 下一批次预防」的自循环。

### 11.1 触发条件

- 批次规划文档（如 `docs/figma/第二批次页面梳理.md`）声明本批次开发完成。
- 用户或 Agent 说出「本批次开发完成」、「进入复盘」、「更新经验手册」等语义。
- 任何 Agent 准备进入下一批次前端开发前。

### 11.2 批次结束强制复盘检查清单

Agent 必须在当前会话输出并逐项勾选，**未完成前不得开始下一批次开发**：

```markdown
### Batch End 强制复盘检查清单
- [ ] 本批次所有页面均已通过 `visual-review` Agent（无 HIGH / Block 问题）
- [ ] 已运行 `.\scripts\update-batch-lessons.ps1 -Batch batch{N} -StartDate <start> -EndDate <end>`
- [ ] 已生成 `tmp/batch-retrospective-input-batch{N}.md`
- [ ] 已调用 `batch-retrospective` Agent 读取复盘输入文件
- [ ] `docs/figma/batch1-lessons-learned.md` 已追加新增问题与检查项
- [ ] 批次规划文档 §6 / 变更日志已记录「复盘完成」
- [ ] 未发现未登记的视觉还原偏差或流程违规
```

### 11.3 执行流程

```powershell
# Step 1：生成本批次复盘原始数据
.\scripts\update-batch-lessons.ps1 -Batch batch2 -StartDate 2026-08-15 -EndDate 2026-08-31

# Step 2：调用 batch-retrospective Agent
# 输入：tmp/batch-retrospective-input-batch2.md
# 输出：更新 docs/figma/batch1-lessons-learned.md
```

### 11.4 为什么不用 Git Hook

批次结束没有天然 Git 事件，因此无法用 Git hook 自动触发。强制手段采用：

1. **规则强制**：本文件与 `.trae/rules/common/agents.md` 声明 `batch-retrospective` 为 mandatory agent。
2. **清单强制**：批次结束检查清单必须勾选完成。
3. **规划文档强制**：批次梳理文档明确「未完成复盘不得进入下一批次开发」。

---

## 12. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-14 | AI | 初版：定义 Calicat 强制读取触发条件、MCP 调用流程、资产导出规范、图层实现顺序、视觉还原检查点 |
| v1.1 | 2026-08-15 | AI | 新增 §11 批次结束强制复盘检查清单与自提升闭环说明 |
