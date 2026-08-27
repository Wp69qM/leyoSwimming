# 前端开发规范

> **适用范围**：`miniapp-user/`、`miniapp-coach/`、`web-admin/`
> **目标**：以专业前端工程师标准进行组件化、可维护、可复用的开发。

---

## 1. 核心原则

### 1.1 组件化开发

- **每个页面即一个组件**，页面内部再拆分为业务组件和展示组件。
- 组件职责单一，一个组件只干一件事。
- 禁止在页面中直接写大段重复逻辑，必须下沉到 `utils` / `hooks` / `components`。

### 1.2 DRY（不要重复自己）

- 任何出现 **2 次及以上** 的代码，必须考虑抽离：
  - 纯函数逻辑 → `utils/`
  - 与 React/Vue 生命周期相关的逻辑 → `hooks/` / `composables/`
  - UI 片段 → `components/`
  - 跨页面业务逻辑 → `common/` 或 `stores/`

### 1.3 优先使用公共组件库

- 小程序端：优先使用 **NutUI** / **Taro UI** 等官方推荐组件。
- 管理后台：优先使用 **Element Plus** 组件。
- 仅在组件库无法满足业务需求时，才手写组件。
- 自定义组件必须明确其复用价值，避免为了一次性场景封装。

---

## 2. 目录组织

### 2.1 通用目录结构

```
src/
├── api/              # 按领域拆分的接口层
├── assets/           # 静态资源：图片、字体、图标
├── components/       # 公共组件
│   ├── common/       # 通用 UI 组件（Button、Empty、Skeleton 等）
│   └── business/     # 业务组件（CoachCard、PackageCard 等）
├── constants/        # 常量、枚举、错误码映射
├── hooks/            # 自定义 Hooks（React）
├── composables/      # 可复用逻辑（Vue）
├── pages/ 或 views/  # 页面级组件
├── stores/           # 状态管理
├── styles/           # 全局样式、SCSS 变量、mixins
├── types/            # TypeScript 类型（优先引用 shared/）
└── utils/            # 纯工具函数
```

### 2.2 组件目录命名

- 组件文件使用 **PascalCase**：`CoachCard.tsx`、`UserForm.vue`
- 组件目录与组件同名：
  ```
  components/
  └── CoachCard/
      ├── index.tsx
      ├── CoachCard.module.scss
      └── CoachCard.test.tsx
  ```
- 单文件组件适用于简单展示型组件。

---

## 3. 样式规范

### 3.1 使用 SCSS

- 统一使用 **SCSS** 作为样式语言。
- 每个组件配套一个样式文件（CSS Modules 或 BEM）。
- 全局样式只放：重置、设计 token、通用工具类。

### 3.2 设计 Token

- 颜色、字号、间距、圆角、阴影等必须引用 `styles/variables.scss` 中的变量。
- 禁止在组件中写死色值，如 `#1890FF`，应使用 `$color-primary`。

### 3.3 布局规范

- 页面布局必须结构清晰、层级合理。
- 管理后台统一布局结构：
  ```
  <AppLayout>
    <Sidebar />
    <Layout>
      <Header />
      <Breadcrumb />
      <MainContainer>
        <!-- 页面内容组件 -->
      </MainContainer>
      <Footer />
    </Layout>
  </AppLayout>
  ```
- 小程序页面布局使用 Flexbox，避免滥用 `position: absolute`。

---

## 4. 静态资源处理

### 4.1 UI 还原原则：Calicat 为唯一视觉来源

**Calicat 设计稿是视觉实现的最高优先级来源。** `docs/figma/page-spec/` 中的描述仅对交互逻辑、业务规则、状态流转负责；样式（颜色、尺寸、间距、阴影、圆角、字体、图标、插图、装饰）必须以 Calicat 图层数据为准。

开发前必须确认：

1. 页面规格文档中已提供 Calicat 链接。
2. 通过 Calicat MCP 工具拉取对应 Frame 的完整图层数据。
3. 若某页面规格**没有 Calicat 链接**，则**暂停该页面开发**，不得自行发挥视觉样式。

还原 Calicat / Figma 设计稿时，遵循以下优先级：

1. **Logo、图标、插图、装饰图形、Banner 等视觉元素，优先从设计稿导出为 PNG / SVG / WebP**，放入 `assets/` 目录引用。
2. **禁止用 CSS 强行绘制**设计稿中的复杂图形、Logo、渐变背景、阴影装饰、异形图案等图层。
3. 只有纯粹的布局框、分割线、背景色块、圆角卡片等简单元素，才允许用 CSS 实现。
4. 不要为了「像素级还原」而写出大量 hack 式 CSS，应在理解设计意图的基础上，用整洁合理的布局重构。
5. **禁止使用 Element Plus / NutUI 等组件库默认样式作为最终视觉。** 组件库仅提供交互基础，最终样式必须覆盖为 Calicat 定义的值。
6. **H5 桌面端必须保持 1:1 还原比例**：
   - 以 375px 为视觉基准宽度，桌面端浏览器中内容区域固定为 375px 并居中。
   - 禁止通过全局缩放、vw/vh 拉伸、或依赖视口宽度自适应来「填满」大屏。
   - 所有固定定位元素（标题栏、底部栏、弹窗、浮层）必须在桌面端同步居中，避免偏离内容区。
   - 视觉还原检查必须包含 H5 桌面端截图，并与 Calicat 375px 画布并排对比。

### 4.2 导出规范

- 纯色图标优先用 **SVG**（可缩放、体积小）。
- 照片、复杂插图、Logo、Banner 用 **PNG** 或 **WebP**。
- 小程序中注意控制图片体积，单张不宜超过 200KB。
- 导出图片统一放入 `assets/images/`，并按模块分子目录：
  ```
  assets/images/
  ├── common/        # Logo、空态图、通用图标
  ├── auth/          # 登录相关
  ├── coach/         # 教练相关
  └── admin/         # 后台相关
  ```

### 4.3 引用方式

```tsx
// 小程序
import logoPng from '@/assets/images/logo.png';
<Image src={logoPng} className={styles.logo} />
```

```vue
<!-- 管理后台 -->
<img src="@/assets/images/logo.png" class="logo" />
```

### 3.4 响应式与适配

#### 通用断点

- **H5 / 小程序**：以 **375px** 为视觉基准宽度。
  - 视口宽度 `< 768px`（手机/平板）：页面内容跟随移动端宽度全宽等比展示。
  - 视口宽度 `>= 768px`（Web/桌面）：页面内容区域固定为 **375px 并居中**，禁止拉伸或依赖视口宽度自适应。
- **管理后台**：使用 Flex / Grid 布局，页面主体宽度自适应，最小支持 1280px 宽度，主流笔记本（1366px 及以上）正常显示。
- 禁止使用固定宽度导致在小屏设备上出现横向滚动或元素被截断。
- 关键区域（按钮、输入框、卡片）在 hover / focus / 点击态下保持良好的可用性。

#### 固定定位元素（顶部标题栏 / 底部操作栏）

所有 `position: fixed` 的顶部标题栏、底部操作栏、浮层，必须同步遵循响应式规则：

```scss
.fixed-element {
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
- 底部操作栏高度禁止超过设计稿规范，避免占用过多可视区域。

### 3.5 Icon 与字体图标规范

- 项目内 `Icon` 组件基于字体图标（`remixicon`）实现，渲染为 `<Text className="leyo-icon">`。
- 控制图标尺寸时，**必须使用 `font-size` + `line-height: 1`**，禁止使用 `width/height`：

  ```scss
  .my-icon {
    font-size: 24px;
    line-height: 1;
  }
  ```

- 若设计稿中的图标无法通过字体图标表达（如复杂插画、品牌 Logo、渐变图形），必须从 Calicat 导出为 PNG/SVG/WebP，使用 `Image` 组件展示。
- 图标颜色通过 `color` 属性或 CSS `color` 控制，优先引用设计 token。

### 3.6 View 与 Text 标签使用规范（Taro）

- **普通文本、标题、标签、价格、状态、提示**等，优先使用 `<View>` 标签，并通过 `className` 精确控制样式。
- **尽量减少 `<Text>` 标签使用**，因为 Taro 的 `<Text>` 在不同端（H5、小程序、RN）带有默认行高、换行、继承等样式，容易干扰 1:1 视觉还原。
- 仅在需要文本特定行为时使用 `<Text>`，例如：
  - `selectable`（可选中复制）
  - `numberOfLines` / `ellipsis`（文本截断省略）
  - `decode`（解码 HTML 实体）
  - 嵌套在 `<Text>` 内部需要特殊行内样式的片段

### 3.7 Button 禁用态规范

- **禁止**使用 `<Button disabled>` 或组件库的 `disabled` 属性作为禁用态主要控制手段。
- 禁用态应通过以下方式实现：
  1. **视觉**：给按钮添加禁用样式 `className`（如 `.button--disabled`）。
  2. **逻辑**：在点击事件处理函数中判断状态，未满足条件时直接 `return`，不执行业务逻辑。

  ```tsx
  <View
    className={`action-button ${!canSubmit ? 'action-button--disabled' : ''}`}
    onClick={handleSubmit}
  >
    提交
  </View>

  function handleSubmit() {
    if (!canSubmit) return;
    // 提交逻辑
  }
  ```

- 这样可以保证禁用态视觉与交互在项目内完全一致，避免原生 `disabled` 在不同端的行为差异（如事件冒泡、样式不可控、点击穿透等）。

### 4.4 禁止直接写死设计稿尺寸

- 不要直接照抄设计稿中的绝对像素值堆砌布局。
- 应理解设计意图后，使用响应式、Flex 布局、百分比、设计 token 重构。

---

## 5. 逻辑复用规范

### 5.1 工具函数（utils）

- 纯函数、与框架无关的逻辑放 `utils/`。
- 按功能分组：
  ```
  utils/
  ├── date.ts
  ├── phone.ts
  ├── validate.ts
  └── storage.ts
  ```

### 5.2 自定义 Hooks / Composables

- 与组件生命周期、状态相关的可复用逻辑抽成 `hooks/` 或 `composables/`。
- 例如：`useAuth`、`useCountdown`、`useForm`、`useListQuery`

### 5.3 业务公共逻辑（common）

- 多个页面共享的业务流程（如登录状态判断、权限校验、支付流程）放 `common/` 或 `stores/`。
- 保持业务逻辑与 UI 分离。

---

## 6. 状态管理

### 6.1 状态提升最小化

- 优先局部状态（`useState` / `ref`）。
- 仅在多个远距离组件需要共享时，才使用全局状态。
- 全局状态用：
  - 小程序：`Zustand`
  - 管理后台：`Pinia`

### 6.2 状态定义规范

- 每个 store 一个文件，按领域命名：`authStore`、`userStore`、`coachStore`
- store 中只放状态与 Action，不放 UI 逻辑。

---

## 7. 接口请求规范

### 7.1 统一封装

- 所有请求必须通过统一的 `request.ts` 封装。
- 统一处理：Base URL、Token、Loading、错误提示、超时。

### 7.2 按领域拆分 API 文件

```
api/
├── auth.ts
├── user.ts
├── coach.ts
└── admin.ts
```

### 7.3 类型安全

- 请求参数和响应数据必须定义 TypeScript 类型。
- 优先从 `shared/types/` 引用类型。

---

## 8. 代码质量

### 8.1 命名规范

- 组件：PascalCase
- 函数/变量：camelCase
- 常量：SCREAMING_SNAKE_CASE
- 布尔变量：使用 `is`、`has`、`should`、`can` 前缀

### 8.2 类型定义

- 优先使用 `type` 定义组件 Props。
- 仅在需要声明合并时使用 `interface`。

### 8.3 注释

- 不写无意义的注释。
- 复杂业务逻辑必须说明「为什么」，而非「做了什么」。

---

## 9. 管理后台布局专项规范

管理后台必须采用 **Layout 组装式** 开发：

```
layouts/
├── AppLayout.vue       # 最外层布局
├── Sidebar.vue         # 侧边栏（含菜单）
├── Header.vue          # 顶部导航
├── Breadcrumb.vue      # 面包屑
├── MainContainer.vue   # 内容区容器
└── Footer.vue          # 页脚
```

- `AppLayout.vue` 负责组装 Sidebar + Header + Breadcrumb + MainContainer + Footer。
- 每个页面只负责 `MainContainer` 内部的内容组件。
- 菜单、用户信息、面包屑由布局组件统一提供。

### 9.1 页面内容区间距规范

为兼顾 1280px ~ 1920px 屏幕的显示效率，内容区采用紧凑但不拥挤的间距体系：

| 元素 | 规范 | 说明 |
|------|------|------|
| 内容区内边距 | `padding: 16px 20px` | 四边统一，避免左右留白过大 |
| 面包屑高度 | `height: 32px` | 仅容纳一行文字，不再额外加 padding |
| 面包屑下边距 | `margin-bottom: 12px` | 与下方标题保持紧凑 |
| 页面标题字号 | `font-size: 18px` | 页面级标题统一 18px，加粗 |
| 页面标题下边距 | `margin-bottom: 16px` | 标题与搜索区/卡片间距 |
| 搜索表单下边距 | `margin-bottom: 12px` | 一行或两行表单均统一 |
| 卡片内边距 | `padding: 16px` | 列表、表单卡片统一 |
| 卡片间距 | `gap: 12px` | 多个卡片/模块之间间距 |
| 表格行高 | `height: 44px` | 保证操作按钮区域可用性 |

### 9.2 页面内容区标准结构

每个后台页面按以下顺序组装：

```vue
<template>
  <div class="page-wrapper">
    <!-- 面包屑 -->
    <Breadcrumb />

    <!-- 页面标题 -->
    <h1 class="page-title">页面标题</h1>

    <!-- 搜索/筛选区 -->
    <SearchCard>
      <SearchForm />
    </SearchCard>

    <!-- 数据/操作区 -->
    <DataCard>
      <Toolbar />
      <DataTable />
    </DataCard>
  </div>
</template>
```

### 9.3 间距实现方式

- 所有间距必须引用 `styles/variables.scss` 中的设计 token，禁止在组件里写死。
- 推荐 token 命名：
  ```scss
  $space-xs: 4px;
  $space-sm: 8px;
  $space-md: 12px;
  $space-lg: 16px;
  $space-xl: 20px;
  $space-xxl: 24px;

  $breadcrumb-height: 32px;
  $page-title-size: 18px;
  $card-padding: 16px;
  $table-row-height: 44px;
  ```
- 搜索表单一行时，不需要为了「撑满」而额外增加下边距，统一使用 `$space-md`（12px）。

### 9.4 查询表单与操作按钮布局

列表页查询/筛选区与操作按钮必须采用上下布局：

- **禁止**把「新增、导出」等操作按钮与查询表单放在同一行做左右弹性布局（`space-between`）。
- 标准结构：
  1. 第一行：查询/筛选表单（含基础筛选项、查询/重置按钮）。
  2. 第二行：操作按钮区，按钮左对齐，与上方表单一一对应模块关系清晰。
- 展开高级筛选时，高级筛选表单项应插入在基础筛选项之后、**查询/重置按钮之前**，保持从上到下阅读顺序：
  `基础筛选 → 高级筛选 → 查询/重置`。
- 查询、重置按钮始终与筛选表单处于同一行，位于该行末尾。

### 9.5 表格列宽自适应

表格必须在大屏下占满内容区宽度，操作列始终固定在最右侧：

- 数据列不使用固定 `width`，统一使用 `min-width`。
- 操作列必须设置 `fixed="right"`，并配置合适的 `min-width`。
- 表格外层容器宽度设为 `100%`，左侧数据列自适应撑开剩余空间。
- 手机号、名称、描述等不确定长度内容列，给较大 `min-width`，让内容列随页面宽度变化。

操作列单元格样式必须统一为 flex 居中对齐：

```scss
:deep(.el-table__cell.operation-cell .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  height: 100%;
}
```

同时覆盖 Element Plus 按钮默认 `margin-left`：

```scss
:deep(.el-table__cell.operation-cell .cell) {
  .el-button + .el-button {
    margin-left: 0;
  }
}
```

### 9.6 操作列与「更多」按钮

操作列中超过 3 个操作时，优先把次要操作收进「更多」下拉菜单：

- 「更多」下拉触发器统一使用 Element Plus 按钮样式，**禁止**用自定义 `<span>` / `<div>` 并手写高度/对齐：

  ```vue
  <el-dropdown trigger="click" @command="handleCommand">
    <el-button link type="primary">
      更多
      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="view">查看日志</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
  ```

- 操作列按钮之间保持 8px 间距，使用 `gap` 而非按钮默认 `margin`。
- 操作列文字链接按钮统一使用 `link` 类型，保持视觉高度一致。

### 9.7 图片上传交互

后台图片上传统一使用 `el-upload` 组件，通过 `http-request` 自定义上传逻辑：

- **前端校验**：必须限制文件类型（`image/jpeg, image/png, image/webp`）和文件大小，不符合时给出明确提示。
- **上传状态**：上传过程中显示 loading/「上传中…」状态，并禁用上传区域，防止重复提交。
- **单图上传**：使用正方形预览框，hover 时显示遮罩提示「点击上传/上传中」；已上传图片支持删除。
- **多图上传**：已上传图片以缩略图列表展示，支持预览与删除；上传触发按钮置于列表末尾，使用 Element Plus `Plus` 图标：

  ```vue
  <el-upload :show-file-list="false" :http-request="handleUpload">
    <div class="image-upload-btn">
      <el-icon><Plus /></el-icon>
      <span>上传图片</span>
    </div>
  </el-upload>
  ```

- **数量限制**：达到最大数量后隐藏上传触发按钮。
- 删除按钮统一使用 `type="danger" link size="small"` 样式，位于预览图下方或右上角。

---

## 10. 小程序开发专项规范

### 10.1 页面组件

- 每个页面对应 `pages/page-name/index.tsx`。
- 页面内部拆分为：
  - 状态管理区
  - 副作用区（hooks）
  - 渲染区
  - 事件处理区

### 10.2 图片与图标

- 优先使用设计稿导出的 PNG/SVG。
- 小程序图标建议统一尺寸：16×16、24×24、32×32、48×48。
- 头像类图片使用 `80×80px` 或 `100×100px`。

### 10.3 表单处理

- 使用表单库减少手写状态：
  - 小程序：`React Hook Form`
  - 后台：`vee-validate`

---

## 11. 禁止事项

- ❌ 在页面中写大段重复逻辑
- ❌ 用 CSS 强行绘制 Logo、复杂图标、渐变背景
- ❌ 写死色值、字号、间距
- ❌ 全局状态滥用
- ❌ 直接调用 `Taro.request` / `axios`，不经过封装
- ❌ 图片直接引用线上临时地址
- ❌ 忽略 TypeScript 类型，使用 `any`

---

## 12. 检查清单

提交代码前确认：

- [ ] 可复用逻辑已抽到 `utils` / `hooks` / `composables`
- [ ] 可复用 UI 已抽到 `components`
- [ ] 样式使用 SCSS 并引用设计 token
- [ ] 图片已从设计稿导出并放入 `assets/`
- [ ] 布局结构清晰合理
- [ ] 管理后台使用统一 Layout 组件
- [ ] 无硬编码色值、尺寸
- [ ] TypeScript 类型完整
- [ ] 页面规格中 Calicat 链接存在，且已拉取图层数据核对
- [ ] 所有视觉样式（颜色、尺寸、间距、阴影、图标）与 Calicat 一致
- [ ] H5 桌面端保持 375px 居中 1:1 还原，固定栏/浮层同步居中
- [ ] Icon 尺寸使用 `font-size` + `line-height: 1` 控制，非字体图标使用 `Image`
- [ ] 普通文本优先使用 `<View>`，避免 `<Text>` 默认样式干扰
- [ ] Button 禁用态使用 `className` + JS 拦截，不使用 `disabled` 属性
- [ ] 未自行发挥 page-spec 未覆盖的视觉元素

---

## 13. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-09 | PM | 初版：定义三端前端开发规范 |
| v1.1 | 2026-08-22 | AI | 新增 §9.4~§9.7：管理后台列表页查询表单、表格列宽自适应、操作列/更多按钮、图片上传交互规范 |
| v1.2 | 2026-08-25 | AI | 更新 §3.4 响应式断点与固定元素居中规则；新增 §3.5 Icon 字体图标规范、§3.6 View/Text 标签规范、§3.7 Button 禁用态规范；§4.1 补充 H5 1:1 还原要求；§12 检查清单补充对应条目 |
