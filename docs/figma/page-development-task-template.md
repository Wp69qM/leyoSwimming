# 前端页面开发任务模板

> **定位**：每个前端页面开发任务的「入口清单源文件」。
> **作用**：Agent 在任何新会话中收到前端页面开发任务后，第一步必须 Read 本文件，并按下方模板生成 `TodoWrite` 检查清单；未生成清单前禁止写代码。
> **适用范围**：`miniapp-user/`、`miniapp-coach/`、`web-admin/` 中所有新增或修改的页面。
> **配套规则**：`.trae/rules/figma/calicat-mcp-hooks.md`、`.trae/rules/figma/page-dev-must-checklist.md`。

---

## 1. 使用方式

1. Agent 收到任务后，先 `Read` 本文件。
2. 根据任务替换下方占位符：
   - `PAGE_SPEC_PATH` → 实际 page-spec 文件路径，如 `docs/figma/page-spec/U-package-list-page.md`
   - `PAGE_NAME` → 页面中文名，如 `U-套餐列表页`
   - `FILE_ID` → Calicat file_id
   - `MAIN_FRAME_NODE_ID` → 主 Frame node-id
   - `STATE_FRAME_NODE_IDS` → 其他状态 Frame node-id 列表（空状态、加载中、错误状态、成功状态等）
3. 调用 `TodoWrite` 生成本次任务的检查清单。
4. 按清单顺序执行，完成一项勾一项。

---

## 2. 任务信息占位

开发前必须明确并填写：

| 占位项 | 值 |
|--------|-----|
| 页面名称 | `<PAGE_NAME>` |
| page-spec 路径 | `<PAGE_SPEC_PATH>` |
| 归属 US | `<US-XXX>` |
| Calicat file_id | `<FILE_ID>` |
| 主 Frame node-id | `<MAIN_FRAME_NODE_ID>` |
| 其他状态 Frame node-id | `<STATE_FRAME_NODE_IDS>` |
| 前端项目路径 | `<PROJECT_PATH>` |

---

## 3. Must Do 检查清单（生成 TodoWrite 时使用）

### Phase 1：开工前（Must Do）

- [ ] 读取 page-spec，确认归属 US、业务状态、四态设计、跳转矩阵、元素清单
- [ ] 从 page-spec 中提取 Calicat file_id 与所有关键 Frame 的 node-id
- [ ] 调用 `mcp_calicat:get_canvas_list` 确认 Frame 所在 canvas
- [ ] 调用 `mcp_calicat:get_design_page_list` 核对 Frame 名称与 node-id 是否一致
- [ ] 调用 `mcp_calicat:get_design_data` 拉取主 Frame 完整图层数据
- [ ] 调用 `mcp_calicat:get_meta_data` 获取 Frame 结构骨架
- [ ] 调用 `mcp_calicat:get_screenshots` 导出设计稿截图到 `src/assets/calicat/screenshots/`
- [ ] 梳理 Logo、图标、插画、复杂视觉元素清单，规划从 Calicat 导出为 PNG/SVG/WebP
- [ ] 输出/更新 `design-tokens.json`（颜色、字号、间距、圆角、阴影）
- [ ] 确认或创建 `calicat-overrides.scss`，准备用 Token 覆盖组件库默认样式

### Phase 2：开发中（Must Do）

- [ ] 禁止从 page-spec 文字中推断颜色、宽度、高度、边距、字体、阴影等视觉值
- [ ] 按 Calicat 图层自上而下依次实现，不得跳过或合并图层
- [ ] 先实现全局容器、NavBar/状态栏、TabBar（如适用），再实现业务区块
- [ ] 复杂视觉元素使用 Calicat 导出的静态资源，禁止用 CSS 重绘
- [ ] 四态设计完整实现：空状态、加载中、错误状态、成功状态
- [ ] 跨页面共用组件从同一基础组件派生，禁止各页面独立写样式
- [ ] 交互状态（hover/active/disabled/选中态）按 Calicat 实现
- [ ] H5 桌面端（`>=768px`）内容区固定 375px 居中，固定标题栏/底部栏/浮层同步居中
- [ ] Icon 尺寸使用 `font-size + line-height: 1` 控制；无法使用字体图标时改用导出的 `Image`
- [ ] 普通文本、标题、价格、状态、提示优先使用 `<View>`，避免 `<Text>` 默认样式干扰
- [ ] Button 禁用态使用 `className` 控制视觉 + JS 拦截点击，不使用 `disabled` 属性

### Phase 3：自测与提交前（Must Do）

- [ ] 本地运行页面，截取实现截图
- [ ] 将本地实现截图与 Calicat 设计稿截图并排对比，差异项登记并修复
- [ ] 必须包含 H5 桌面端截图，验证 375px 居中 1:1 还原，固定栏/浮层未偏离内容区
- [ ] 调用 `code-reviewer` Agent 审查代码质量
- [ ] 调用 `visual-review` Agent 检查视觉还原（颜色误差 ≤1%、间距误差 ≤2px、字体字号一致、四态完整）
- [ ] 两个 Agent 均通过后方可提交
- [ ] PR/提交说明包含：Calicat 截图、本地实现截图、差异说明、`design-tokens.json` 路径、导出静态资源清单

---

## 4. 禁止事项（Hard No）

| 禁止行为 | 正确替代 |
|---------|---------|
| 用 page-spec 当视觉稿 | 以 Calicat 图层为准 |
| CSS 绘制 Logo/图标/插画 | 从 Calicat 导出静态资源 |
| 组件库默认样式直接使用 | 先映射到 Calicat Token，再覆盖 |
| 只实现成功态 | 必须实现四态 |
| 先写代码后补设计稿 | 先拉图层数据再实现 |
| 跳过图层或合并多个图层 | 按 Calicat 图层自上而下逐个实现 |
| H5 桌面端内容区/固定栏全宽拉伸 | `>=768px` 时固定 375px 并居中 |
| 用 `width/height` 控制 `Icon` 组件尺寸 | 使用 `font-size + line-height: 1`；非字体图标用 `Image` |
| 普通文本大量使用 `<Text>` | 优先使用 `<View>` 并通过样式控制 |
| 使用 `<Button disabled>` 控制禁用态 | 使用 `className` 控制样式 + JS 拦截点击 |

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-15 | AI | 创建：前端页面开发任务模板，固化 Calicat 开工检查清单 |
| v1.1 | 2026-08-25 | AI | Phase 2 / Phase 3 增加 H5 响应式、Icon 尺寸、View/Text、Button 禁用态检查项；Hard No 表补充对应禁止事项 |
