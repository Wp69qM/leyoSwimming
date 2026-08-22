# 第一批次页面开发经验手册

> **文档定位**：汇总第一批次（登录注册 + 用户管理）页面开发中暴露的系统性问题，形成可复用到第二批次及后续批次的检查清单与 SOP。
> **适用范围**：`miniapp-user/`、`miniapp-coach/`、`web-admin/` 所有前端页面开发。
> **配套文档**：
> - [dev-plan-batch1.md](../tech/dev-plan-batch1.md) §6 Calicat 设计稿还原偏差根因分析
> - [page-spec/TEMPLATE.md](./page-spec/TEMPLATE.md)
> - [CROSS-BATCH-PRINCIPLES.md](./page-spec/CROSS-BATCH-PRINCIPLES.md)
> - [A-CROSS-BATCH-PRINCIPLES.md](./page-spec/A-CROSS-BATCH-PRINCIPLES.md)

---

## 1. 本手册要解决的痛点

第一批次开发完成后，通过 `code-reviewer` 与人工走查发现：**管理端、用户端、教练端均有多处页面实现与 Calicat 设计稿不一致**。这些偏差并非偶发，而是反映了规范优先级、开发顺序、工具使用、检查机制上的系统性问题。本手册将这些问题沉淀为"事前预防 + 事中检查 + 事后复盘"的闭环，避免在第二批次重复踩坑。

---

## 2. 问题分类与典型案例

### 2.1 视觉还原类问题（最高频）

| # | 偏差表现 | 对应设计稿要求 | 根因 | 后果 |
|---|---------|---------------|------|------|
| 1 | 管理端离职审批队列页出现 4 张独立统计卡片 | 单一蓝底待办摘要条 | 直接套用 Element Plus 统计卡片，未读取 Calicat 图层 | 页面结构完全走样 |
| 2 | 管理端详情页多出「登记」按钮 | 底部固定审批操作栏，无登记按钮 | 把业务动作与审批动作混淆，按默认表单布局实现 | 多余操作、用户困惑 |
| 3 | 用户端启动页仅显示标题 | 全屏渐变 + 居中 Logo + 加载指示器 | 未读取 Calicat 图层，仅用占位文本 | 启动页品牌感缺失 |
| 4 | 颜色/间距/字号与稿不符 | 严格使用全局 Token | 依赖组件库默认主题，未做主题覆盖 | 整体视觉风格不统一 |

### 2.2 规范理解类问题

| # | 偏差表现 | 正确理解 | 根因 |
|---|---------|---------|------|
| 5 | 将 page-spec 中的尺寸/颜色描述当作视觉来源 | page-spec 只描述「有什么元素」和「交互/状态」，视觉以 Calicat 为准 | 规范优先级理解错误 |
| 6 | 用 CSS 绘制 Logo、图标、插画 | 必须从 Calicat 导出 PNG/SVG/WebP | 设计资产导出流程缺失 |
| 7 | 四态设计只实现成功态 | 必须实现空/加载/错误/成功四态 | 未把四态作为交付标准 |
| 8 | 跨页面组件样式不一致 | 同一组件在不同页面应使用同一基础组件/Token | 缺少组件库与 Token 映射 |

### 2.3 流程执行类问题

| # | 偏差表现 | 正确做法 | 根因 |
|---|---------|---------|------|
| 9 | 先写代码，后补设计稿 | 先拉取 Calicat 图层数据，再按图层实现 | 开发顺序颠倒 |
| 10 | 只有代码审查，没有视觉还原审查 | 合并前必须通过 visual-review Agent 逐图层对比 | 缺少视觉检查点 |
| 11 | 未导出设计稿截图作为实现参照 | 每个页面开工前导出 Calicat 截图，PR 附对比图 | 缺少可视化参照 |
| 12 | 设计稿链接缺失或 node-id 过期 | page-spec 中必须维护准确的 Calicat file_id 与 node-id | 维护不及时 |

### 2.4 跨端一致性类问题

| # | 偏差表现 | 正确做法 | 影响 |
|---|---------|---------|------|
| 13 | 管理端表格列与 page-spec 不一致 | 按 page-spec 元素清单 + Calicat 图层同步 | 后台操作字段缺失/多余 |
| 14 | 用户端协议浮层类型错误 | 按 page-spec 区分抽屉/弹窗/页面 | 交互形态错误 |
| 15 | 教练端「我的」页面缺少状态入口 | status=4 时应显示「查看离职申请」 | 状态驱动 UI 遗漏 |
| 16 | 小程序 TabBar/NavBar 与设计稿不符 | 按 Calicat 全局规范实现自定义导航 | 导航体验不一致 |

---

## 3. 根因总结（5 条）

1. **规范优先级理解错误**：Sub-Agent 仍把 page-spec 文字描述当视觉来源，未真正落实"Calicat 为唯一最高优先级"。
2. **开发顺序颠倒**：先搭页面后还原设计稿，导致样式债务累积。
3. **组件库默认样式依赖**：Element Plus / NutUI 默认主题与 Calicat 设计系统存在差异，未先建 Token 覆盖层。
4. **缺少视觉还原检查点**：原流程只有 code-reviewer，无 visual-review。
5. **设计资产导出流程缺失**：Logo、图标、插画未按规则从 Calicat 导出。

---

## 4. 预防措施与检查清单

### 4.1 开工前（Must Do）

- [ ] 已读取对应 page-spec，确认归属 US、业务状态、四态设计、跳转矩阵。
- [ ] 已从 page-spec 中提取 Calicat file_id 与关键 Frame 的 node-id。
- [ ] 已调用 `mcp_calicat:get_design_data` / `get_meta_data` 拉取图层数据。
- [ ] 已调用 `mcp_calicat:get_screenshots` 导出设计稿截图到 `src/assets/calicat/screenshots/`。
- [ ] 已将 Logo、图标、插画从 Calicat 导出为 PNG/SVG/WebP，放到 `src/assets/calicat/`。
- [ ] 已输出 `design-tokens.json`（颜色、字号、间距、圆角、阴影）。
- [ ] 已确认存在全局样式覆盖文件（`calicat-overrides.scss`）并引用 Token。

### 4.2 开发中（Must Do）

- [ ] 禁止从 page-spec 文字中推断颜色、宽度、高度、边距、字体、阴影等视觉值。
- [ ] 按 Calicat 图层自上而下依次实现，不得跳过或合并图层。
- [ ] 复杂视觉元素必须导出为静态资源，禁止用 CSS 重绘。
- [ ] 四态设计必须完整实现：空状态、加载中、错误状态、成功状态。
- [ ] 跨页面共用组件必须从同一基础组件派生，禁止各页面独立写样式。

### 4.3 自测与提交前（Must Do）

- [ ] 本地运行页面，与设计稿截图并排对比，差异项登记为 issue。
- [ ] 使用 `visual-review` Agent 检查颜色误差 ≤1%、间距误差 ≤2px、字体字号一致。
- [ ] `code-reviewer` 与 `visual-review` 均通过后方可提交。
- [ ] PR 描述必须包含：Calicat 截图、本地实现截图、差异说明。

### 4.4 禁止事项（Hard No）

| 禁止行为 | 正确替代 |
|---------|---------|
| 用 page-spec 当视觉稿 | 以 Calicat 图层为准 |
| CSS 绘制 Logo/图标/插画 | 从 Calicat 导出静态资源 |
| 组件库默认样式直接使用 | 先映射到 Calicat Token，再覆盖 |
| 只实现成功态 | 必须实现四态 |
| 先写代码后补设计稿 | 先拉图层数据再实现 |

---

## 5. 落地到后续批次的 SOP

### Step 1：page-spec 与 Calicat 对齐

每个 page-spec 必须包含准确的 Calicat 链接与 node-id：

```markdown
## 1.1 Calicat 设计稿链接

| 页面/状态 | Calicat Frame | 链接 |
|----------|---------------|------|
| 主页面 | U-套餐详情页 | https://www.calicat.cn/design/{file_id}?node-id={node-id} |
| 空状态 | U-套餐详情页-empty | https://www.calicat.cn/design/{file_id}?node-id={node-id} |
```

### Step 2：前端 Agent 开工 checklist

Agent 收到前端页面任务后，必须先执行：

1. 读取 page-spec。
2. 提取 Calicat file_id 与 node-id。
3. 调用 `mcp_calicat:get_design_data`) 获取图层数据。
4. 调用 `mcp_calicat:get_screenshots` 导出截图。
5. 读取 [docs/figma/page-development-task-template.md](./page-development-task-template.md)，按模板生成 `TodoWrite` 检查清单。
6. 生成 `design-tokens.json` 与 `layer-implementation-order.md`。
7. 按图层顺序实现，禁止跳过。

### Step 3：引入 visual-review Agent

在原有 `code-reviewer` 之后增加 `visual-review` Agent，输入：

- Calicat 截图
- 本地实现截图
- page-spec 元素清单
- design-tokens.json

输出：颜色/间距/字号/布局差异报告，差异 ≤ 阈值方可通过。

### Step 4：建立 Token 与资产仓库

- `web-admin/src/styles/calicat-overrides.scss`
- `miniapp-user/src/styles/calicat-overrides.scss`
- `miniapp-coach/src/styles/calicat-overrides.scss`
- `src/assets/calicat/`：存放导出的 Logo、图标、插画
- `src/assets/calicat/screenshots/`：存放设计稿截图

### Step 5：批次收尾复盘

每个批次开发完成后，更新本手册：

- 新增发现的问题。
- 更新检查清单。
- 修订 SOP。

---

## 6. 与本项目其他规则的衔接

- [frontend-development-standards.md](../tech/frontend-development-standards.md)：前端工程化、组件化、响应式规范。
- [page-spec/TEMPLATE.md](./page-spec/TEMPLATE.md)：page-spec 元素清单不写视觉样式。
- [CROSS-BATCH-PRINCIPLES.md](./page-spec/CROSS-BATCH-PRINCIPLES.md)：跨批次全局设计原则。
- [.trae/rules/figma/calicat-mcp-hooks.md](../../.trae/rules/figma/calicat-mcp-hooks.md)：强制读取 Calicat 的 hooks 规则。

---

## 7. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-14 | AI | 初版：汇总第一批次 16 项典型问题、5 条根因、4 阶段预防措施与 SOP |
| v1.1 | 2026-08-15 | AI | SOP Step 2 增加读取 page-development-task-template.md 并生成 TodoWrite 检查清单，与新增规则文件对齐 |
