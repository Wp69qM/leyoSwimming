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

### 4.1 UI 还原原则：能导出就不手写

还原 Calicat / Figma 设计稿时，遵循以下优先级：

1. **Logo、图标、插图、装饰图形、Banner 等视觉元素，优先从设计稿导出为 PNG / SVG / WebP**，放入 `assets/` 目录引用。
2. **禁止用 CSS 强行绘制**设计稿中的复杂图形、Logo、渐变背景、阴影装饰、异形图案等图层。
3. 只有纯粹的布局框、分割线、背景色块、圆角卡片等简单元素，才允许用 CSS 实现。
4. 不要为了「像素级还原」而写出大量 hack 式 CSS，应在理解设计意图的基础上，用整洁合理的布局重构。

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

- **小程序**：按 iPhone 6/7/8 的 375px 宽度为基准设计，使用 `rpx` 或等比例单位，确保在 320px ~ 430px 宽度的手机上正常显示。
- **管理后台**：使用 Flex / Grid 布局，页面主体宽度自适应，最小支持 1280px 宽度，主流笔记本（1366px 及以上）正常显示。
- 禁止使用固定宽度导致在小屏设备上出现横向滚动或元素被截断。
- 关键区域（按钮、输入框、卡片）在 hover / focus / 点击态下保持良好的可用性。

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

---

## 13. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-09 | PM | 初版：定义三端前端开发规范 |
