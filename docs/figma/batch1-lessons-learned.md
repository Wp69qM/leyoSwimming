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

### 2.5 前端实现细节类问题（H5 / 小程序通用）

| # | 偏差表现 | 正确做法 | 根因 |
|---|---------|---------|------|
| 17 | H5 桌面端顶部标题栏 / 底部操作栏宽度铺满屏幕，与 375px 内容区不对齐 | 固定元素在 `>=768px` 时居中固定为 `375px`，`<768px` 全宽 | 未按项目响应式断点处理固定定位元素 |
| 18 | Icon 图标大小用 `width/height` 控制，实际渲染尺寸与设计稿不符 | Icon 组件渲染的是字体图标，尺寸应使用 `font-size + line-height: 1`；无法使用字体图标时改用 `Image` | 把字体图标当作普通图片处理 |
| 19 | 大量使用 `<Text>` 标签承载普通文本，导致默认样式干扰还原 | 普通文本优先使用 `<View>` 标签，通过样式精确控制；仅在需要文本特殊行为时使用 `<Text>` | 未理解 Taro 中 View/Text 的默认样式差异 |
| 20 | 用 `<Button disabled>` 控制按钮禁用态 | 禁用态通过 `className` 控制视觉样式，并通过 JS 拦截点击逻辑；不依赖原生 `disabled` 属性 | 原生 disabled 在不同端样式不一致，且事件行为不可控 |
| 21 | H5 上设计稿还原比例失真，元素被拉伸或缩放异常 | 以 375px 为基准，H5 桌面端保持 1:1 居中展示，禁止全局缩放或依赖视口宽度拉伸 | 未建立统一的 H5 视口约束 |

### 2.6 AI 消息展示与工程管理类问题

| # | 偏差表现 | 正确做法 | 根因 |
|---|---------|---------|------|
| 22 | AI 返回的 Markdown 文本无换行、加粗、列表样式 | AI 消息气泡组件实现基础 Markdown 渲染（加粗、无序/有序列表、换行） | 直接将 LLM 文本当纯文本渲染 |
| 23 | 底部输入框绑定 Enter 发送后，发送成功输入框未清空 | 发送成功后主动清空输入状态 | 事件处理只触发发送，未重置输入值 |
| 24 | 项目根目录堆积临时脚本、diff 文件等中间产出物 | 调试/分析产生的临时文件统一移入 `tmp/`，避免污染根目录 | 缺乏中间产出物整理意识 |

### 2.7 后端与测试工程类问题（batch5：MCP 能力暴露）

> batch5 为纯后端批次（ai-service 的 FastMCP + Streamable HTTP + 4 个只读工具 + Trae 注册验证），无前端页面、无数据表变更，visual-review 检查项按 N/A 处理。以下问题以后端开发/测试/集成为主。

| # | 偏差表现/问题 | 正确做法 | 根因 |
|---|---|---|---|
| 25 | 带 `--cov` 运行测试时批量 401/429 失败，不带 cov 全部通过，出现诡异的"cov 依赖"假象 | conftest 中所有测试环境变量**强制赋值**而非 `setdefault`，并写明根因注释；新项目 conftest 一律强制赋值；"只在某运行模式下失败"优先排查进程环境被提前污染 | pytest-cov 启动时 coverage 提前导入 app 包 → 触发 config.py 模块级 `load_dotenv()` → 本地 .env 真实 token/限流值写入进程环境，早于 conftest 执行，`setdefault` 保留真实值 |
| 26 | pytest-cov 下报 `KeyError: 'pydantic.root_model'` | conftest 顶部 `import pydantic.root_model` 预导入；coverage 报导入期 KeyError 时优先怀疑延迟加载模块，预导入规避 | pytest-cov 的 trace 干扰 pydantic 延迟加载机制 |
| 27 | `StreamableHTTPSessionManager` 每实例只能 `run()` 一次，测试需多次进入/退出时报错 | 测试在进入前 + 退出后重置 `_has_started` 标志（stateless 模式下 SDK 已清理内部状态，重置安全）；生产 lifespan 全程一次不受影响 | SDK 设计为单次 `run()` 生命周期，测试反复启停与该设计冲突 |
| 28 | host 为 127.0.0.1 时自定义 Host（如 `http://test`）被 421 拒绝 | 已有独立 Bearer token + IP 限流前提下显式禁用：`TransportSecuritySettings(enable_dns_rebinding_protection=False)`，注释写明安全论证 | FastMCP DNS rebinding 保护默认 allowed_hosts 仅 localhost，与测试自定义 Host 冲突 |
| 29 | FastMCP 把工具的 list 返回值展开为多个 content 块，破坏单 JSON 块响应 | 工具必须返回 `{"list": [...]}` 包装结构（与项目 API 约定 `data.list` 天然对齐） | 未了解 FastMCP 对 list 返回值的默认展开行为 |
| 30 | `hmac.compare_digest` 的 str 重载遇非 ASCII token 抛 TypeError，恶意请求得 500 而非 401，且产生错误日志噪音（code-review 发现） | 安全敏感比较一律 bytes 化：`token.encode("utf-8")` 后比较（两处中间件同模式修复 + 2 个回归测试） | 使用 str 重载进行安全比较，未考虑非 ASCII 输入 |
| 31 | MCP SDK 连接错误包装在 ExceptionGroup 中，直接 `str(exc)` 输出不可读 | 验证脚本增加 `flatten_exception` 解包出根因（ConnectError/401），并输出排查提示（服务未启动/token 不一致/限流） | ExceptionGroup 语义下顶层异常信息不含根因 |
| 32 | 本机 Trae 的 node utility 进程占用 8000 端口，`/health` 返回 200 造成"服务在运行"假象，实际 POST `/mcp-server/mcp` 返回 404 暴露真相 | 诊断顺序：进程命令行 → 决定性端点探测（404 vs 401）→ 再怀疑代码；无 `--reload` 的服务改代码后必须手动重启；端口被占时果断换端口 | 非决定性探测（`/health` 任何服务都可返回 200）误判 + 旧代码进程未重启 |
| 33 | `knowledge_similarity_threshold` 代码默认值 0.7→0.2 修正时，deploy/.env.example 残留 0.7（code-review MEDIUM 发现），生产按模板部署会阻断召回 | 调优型配置改默认值时全量 grep 所有配置源（代码 / config.example / deploy 模板 / compose fallback） | 配置默认值分散在多个源，缺乏同步检查机制 |

---

## 3. 根因总结（7 条）

1. **规范优先级理解错误**：Sub-Agent 仍把 page-spec 文字描述当视觉来源，未真正落实"Calicat 为唯一最高优先级"。
2. **开发顺序颠倒**：先搭页面后还原设计稿，导致样式债务累积。
3. **组件库默认样式依赖**：Element Plus / NutUI 默认主题与 Calicat 设计系统存在差异，未先建 Token 覆盖层。
4. **缺少视觉还原检查点**：原流程只有 code-reviewer，无 visual-review。
5. **设计资产导出流程缺失**：Logo、图标、插画未按规则从 Calicat 导出。
6. **中间产出物缺乏整理**：调试脚本、diff、临时文件散落在根目录，影响代码库整洁与后续排查。
7. **运行环境时序与多源配置的隐式耦合**（batch5 新增）：测试工具链（pytest-cov）在 conftest 之前导入业务包，模块级副作用（`load_dotenv()`、延迟加载）提前污染进程环境，造成"只在某运行模式下失败"的诡异现象；配置默认值散落在代码、模板、compose 等多个源头，改一处漏多处，直到生产部署或 code-review 才暴露。此类隐式耦合无法靠肉眼发现，必须在检查清单中显式打断（conftest 强制赋值、配置源全量同步）。

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
- [ ] AI 消息气泡支持 Markdown 基础渲染（加粗、无序/有序列表、换行）。
- [ ] 底部输入框发送成功后主动清空输入状态。

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
| 直接渲染 LLM 文本为纯文本 | AI 消息气泡实现 Markdown 基础样式 |
| 临时脚本/diff 散落在根目录 | 统一移入 `tmp/` |

### 4.5 H5 / 小程序前端实现细节规范

#### 4.5.1 顶部标题栏与底部操作栏必须响应式

所有 `position: fixed` 的顶部标题栏和底部操作栏，必须按以下规则处理：

- **断点**：`768px`。
- **手机/平板（`< 768px`）**：`left: 0; right: 0`，跟随移动端宽度全宽展示。
- **桌面/Web（`>= 768px`）**：宽度固定为 `375px`，水平居中，与内容区保持对齐：

  ```scss
  .fixed-header,
  .fixed-bottom-bar {
    position: fixed;
    left: 0;
    right: 0;

    @media (width >=768px) {
      left: 50%;
      right: auto;
      width: 375px;
      transform: translateX(-50%);
    }
  }
  ```

- 高度必须使用设计 token（如 `$calicat-bottom-bar-height`），禁止写死超大值。

#### 4.5.2 Icon 图标尺寸控制

- 项目内 `Icon` 组件基于字体图标（`remixicon`）实现，尺寸**必须使用 `font-size` + `line-height: 1`** 控制。
- **禁止**用 `width/height` 控制 `Icon` 组件的大小。
- 若设计稿中的图标无法通过字体图标表达（如复杂插画、品牌 Logo），应导出为 PNG/SVG/WebP，使用 `Image` 组件展示。

#### 4.5.3 View 与 Text 标签使用原则

- **普通文本、标题、标签、价格、状态、提示**等，优先使用 `<View>` 标签，并通过 `className` 精确控制样式。
- **尽量减少 `<Text>` 标签使用**，因为 Taro 的 `<Text>` 在不同端带有默认行高、换行、继承等样式，容易干扰 1:1 还原。
- 仅在需要文本特定行为（如 `selectable`、`numberOfLines`、`decode`）时使用 `<Text>`。

#### 4.5.4 Button 禁用态控制

- **禁止**使用 `<Button disabled>` 或组件库的 `disabled` 属性作为禁用态主要控制手段。
- 禁用态应通过以下方式实现：
  1. 视觉：给按钮添加禁用样式 `className`（如 `--disabled`）。
  2. 逻辑：在点击事件处理函数中判断状态，未满足条件时直接 `return`，不执行业务逻辑。
- 这样可以保证禁用态视觉与交互在项目内完全一致，避免原生 `disabled` 在不同端的行为差异。

#### 4.5.5 H5 保持 1:1 设计稿还原

- H5 页面以 **375px** 为视觉基准宽度。
- 桌面端浏览器中，页面内容区域固定为 **375px 并居中**，禁止拉伸、缩放或依赖视口宽度自适应。
- 所有固定定位元素（标题栏、底部栏、弹窗、浮层）必须同步遵循 4.5.1 的居中规则，避免在宽屏上偏离内容区。
- 视觉还原检查必须包含 H5 桌面端截图，与设计稿 375px 画布对比。

#### 4.5.6 AI 消息 Markdown 渲染

- AI 消息气泡组件必须支持 Markdown 基础样式：
  - 加粗：`**文本**`
  - 无序列表：`- 项目` / `* 项目`
  - 有序列表：`1. 项目`
  - 换行：按 `\n` 分段渲染
- 实现时优先使用 `<View>` 标签承载段落与列表项，通过 `className` 控制样式。
- 不要依赖 `<Text>` 标签的默认换行与加粗行为，避免不同端表现不一致。

### 4.6 后端与测试工程检查项（batch5 新增）

适用于后端/测试类批次（如 ai-service MCP 能力暴露）；纯前端批次可跳过本小节。

- [ ] conftest 测试环境变量用强制赋值，禁止 `setdefault`（防 pytest-cov + `load_dotenv` 时序污染）。
- [ ] 安全 token 比较必须 encode 后 bytes 比较（`compare_digest`），并附非 ASCII 输入回归测试。
- [ ] 配置默认值调整后全量同步所有配置源（代码 / config.example / deploy 模板 / compose fallback）。
- [ ] 端到端验证前确认服务进程为新代码（无 `--reload` 须手动重启）；端口冲突时用决定性端点探测（404 vs 401）区分目标服务，再怀疑代码。
- [ ] 每个 TDD Task GREEN 后立即 commit，保证批次复盘数据完整。
- [ ] 纯后端批次：visual-review 检查项按 N/A 处理，但 code-reviewer 审查 + 覆盖率 ≥80% + E2E 验证记录归档仍为强制项。
- [ ] 新协议接入（如 MCP JSON-RPC over Streamable HTTP）豁免项目 REST 三段式 API 约定时，豁免依据必须在 user-story / 技术文档中显式留痕。
- [ ] 含真实 token 的本地配置文件（如 `.trae/mcp.json`）：`.gitignore` 防线 + example 模板入库；注意 `git update-index --skip-worktree` 对未跟踪文件无效，勿再推荐。

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
| v1.2 | 2026-08-25 | AI | 新增 §2.5 前端实现细节类问题与 §4.5 H5/小程序前端实现细节规范：固定栏响应式、Icon 尺寸、View/Text 使用、Button 禁用态、H5 1:1 还原 |
| v1.3 | 2026-08-28 | AI | 新增 §2.6 AI 消息展示与工程管理类问题、§3 第 6 条根因、§4.2/§4.4/§4.5.6 对应检查清单与 Markdown 渲染规范 |
| v1.4 | 2026-09-07 | AI | 新增 §2.7 后端与测试工程类问题（batch5：MCP 能力暴露 9 项）、§3 第 7 条根因（运行环境时序/多源配置隐式耦合）、§4.6 后端与测试工程检查项（8 条） |
