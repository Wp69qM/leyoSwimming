# Figma 全局设计规范

> **本目录用途**：存放**设计系统（Design System）**的全局规范，所有 US 共享。
> **文档状态**：✅ Token / 组件库 / 主题 / 视觉规格 / AI 生成规则 / Page Spec 层已定义（v1.4）
> **最后更新**：2026-08-02

---

## 0. 四层设计架构

设计相关文档分 4 层，职责严格分离：

| 层 | 位置 | 角色 | 维护者 | 频次 |
|----|------|------|--------|------|
| **L1 设计系统 (DS)** | `docs/figma/README.md`（本文） | 全局规范：token / 组件 / 状态徽标 / 4 态模板 / 文案 / 主题 | 资深设计 | 一次定义，全局复用 |
| **L1.5 页面交付规格 (Page Spec)** | `docs/figma/page-spec/*.md` | 页面级设计指令源：每个页面精确的元素清单、身份显隐矩阵、跳转矩阵、四态、AI 检查清单 | PM / 设计负责人 | 每页面一份 |
| **L2 故事级设计内容** | `docs/stories/US-XXX-.../user-story.md` §13-15 | 故事独有设计决策 + Figma 链接 + 评审记录 | 各 US 设计 | 每故事一份 |
| **L3 设计师交付物** | Figma 云端 | 实际设计稿、组件实例、原型 | 各 US 设计 | 实时 |

> **核心原则**：
> - L1 一次定义，所有 US 复用；
> - **L1.5 Page Spec 是 AI 生成 Figma 的唯一指令源**，贯通 PRD + US + L1 设计系统；
> - L2 只写本故事独有的设计决策，**不重复 L1 / L1.5**；
> - AI 生成 Figma 前**必须**先读取对应 Page Spec，再读取 L1 全局规范，最后动手。

> **Page Spec 模板与示例**：
> - 模板：`docs/figma/page-spec/TEMPLATE.md`
> - 示例：`docs/figma/page-spec/U-home-page.md`
> - 跨页自检：`docs/figma/page-spec/cross-page-check.md`

---

## 0.5 设计主题：游泳运动（Aquatic Motion）

> **本节定义项目整体视觉风格基调，所有 Token、组件、页面必须服从该主题。**

### 0.5.1 主题理念

leyoSwimming 是一款游泳教学约课产品，视觉风格必须传递**「水、运动、专业、信任」**四大关键词：

| 关键词 | 视觉表达 | 对应设计决策 |
|--------|---------|------------|
| **水** | 蓝色主色调 + 流体渐变 + 波纹纹理 | 主色采用「泳池蓝」系，按钮/卡片可用浅蓝渐变 |
| **运动** | 高对比数据色 + 动态进度条 + 能量感图标 | 数据展示用饱和度高的橙/绿，强调"动起来" |
| **专业** | 干净留白 + 信息分层 + 克制的图标线条 | 大量留白，图标用线性（outline）而非填充 |
| **信任** | 稳定的卡片布局 + 可预期的交互模式 | 卡片化布局为主，按钮位置跨页面一致 |

### 0.5.2 主题参考

- **Apple Fitness / Apple Health 深色主题**：高饱和度数据色 + 深色背景 + 大字号数据展示
- **Keep / Nike Training Club**：运动卡片 + 进度环 + 激励文案
- **泳者胜 App**：游泳专属数据可视化（SWOLF、配速、划频）的视觉语言

### 0.5.3 设计风格定调

| 维度 | 决策 |
|------|------|
| **整体风格** | 现代极简 + 运动活力（非纯极简，保留运动数据的高饱和点缀） |
| **主色调** | 泳池蓝（#1890FF 系）为主，传递"水"与"信任" |
| **强调色** | 活力橙（#FF6B35）用于运动成就/进度/CTA，与蓝色形成互补对比 |
| **背景策略** | 小程序端浅色为主（白底 + 浅灰卡片），避免深色背景在小屏的压抑感 |
| **卡片化** | 主要信息载体为卡片（圆角 + 浅阴影），呼应"泳池"的边界感 |
| **数据可视化** | 进度环、条形图、趋势线，色用主色 + 强调色 + 成功/警告/错误四色 |
| **图标风格** | 线性图标（outline）为主，线条 1.5-2px，避免填充式图标 |
| **插画风格** | 空状态用扁平运动主题插画（游泳人物、水波、奖牌），禁止写实插画 |
| **动效** | 数据加载用骨架屏，进度变化用平滑过渡（300ms ease-out），成就达成用粒子效果 |

---

## 1. 设计 Token

> **本节定义全局设计变量，所有页面必须使用，禁止硬编码颜色/字体/间距。**
> **色值依据**：游泳运动主题（§0.5）+ 微信小程序规范 + WCAG 2.1 AA 对比度。

### 1.1 颜色 Token

| Token 名称 | 色值 | 浅色变体（背景用） | 用途 |
|-----------|------|------------------|------|
| `color-primary` | `#1890FF` | `#E6F7FF` | 主色：按钮、链接、激活态（泳池蓝） |
| `color-primary-dark` | `#0050B3` | — | 主色按压态/Hover |
| `color-accent` | `#FF6B35` | `#FFF2E8` | 强调色：运动成就、进度、CTA（活力橙） |
| `color-success` | `#52C41A` | `#F6FFED` | 成功状态、成功徽标 |
| `color-warning` | `#FAAD14` | `#FFFBE6` | 警告状态、提示条 |
| `color-error` | `#FF4D4F` | `#FFF1F0` | 错误状态、错误徽标 |
| `color-text-primary` | `#262626` | — | 主要文字 |
| `color-text-secondary` | `#8C8C8C` | — | 次要文字 |
| `color-text-disabled` | `#BFBFBF` | — | 禁用文字 |
| `color-bg-page` | `#F5F7FA` | — | 页面背景（浅灰，呼应泳池瓷砖） |
| `color-bg-card` | `#FFFFFF` | — | 卡片背景 |
| `color-border` | `#E8E8E8` | — | 边框、分割线 |
| `color-divider` | `#F0F0F0` | — | 弱分割线（卡片内） |

> **色值说明**：主色 `#1890FF` 为 Ant Design / NutUI 默认主色，与所选组件库天然对齐，降低主题定制成本；强调色 `#FF6B35` 为运动领域常用活力橙，与主色形成 180° 互补对比，用于"运动成就/进度"等需要激励的场景。

### 1.2 字体 Token

| Token 名称 | 字号 / 字重 | 小程序 rpx（750rpx 基准） | 用途 |
|-----------|------------|----------------------|------|
| `font-size-display` | 32px / Bold | 64rpx | 数据大数字（如剩余课时、运动数据） |
| `font-size-h1` | 24px / Bold | 48rpx | 页面大标题 |
| `font-size-h2` | 20px / Medium | 40rpx | 卡片标题 |
| `font-size-body` | 16px / Regular | 32rpx | 正文 |
| `font-size-body-sm` | 14px / Regular | 28rpx | 次要正文、列表项 |
| `font-size-caption` | 12px / Regular | 24rpx | 辅助文字、时间戳 |
| `font-weight-bold` | 600 | — | 强调 |
| `font-weight-medium` | 500 | — | 卡片标题 |
| `font-weight-regular` | 400 | — | 常规 |

### 1.3 间距 / 圆角 / 阴影 Token

| Token 名称 | 数值 | 小程序 rpx | 用途 |
|-----------|------|-----------|------|
| `space-xs` | 4px | 8rpx | 紧凑间距（图标与文字） |
| `space-sm` | 8px | 16rpx | 小间距（卡片内元素） |
| `space-md` | 16px | 32rpx | 常规间距（卡片 padding） |
| `space-lg` | 24px | 48rpx | 大间距（卡片间距、区块间距） |
| `space-xl` | 32px | 64rpx | 超大间距（页面级区块） |
| `radius-sm` | 4px | 8rpx | 小圆角（徽标、小按钮） |
| `radius-md` | 8px | 16rpx | 中圆角（按钮、输入框） |
| `radius-lg` | 12px | 24rpx | 大圆角（卡片、模态） |
| `radius-xl` | 16px | 32rpx | 超大圆角（FAB、底部弹层） |
| `radius-pill` | 999px | — | 胶囊圆角（标签、药丸按钮） |
| `shadow-none` | `none` | — | 无阴影（扁平元素） |
| `shadow-card` | `0 2px 8px rgba(0,0,0,0.08)` | — | 卡片阴影（轻盈，常规卡片） |
| `shadow-raised` | `0 4px 12px rgba(0,0,0,0.10)` | — | 抬升阴影（可点击卡片、悬停态） |
| `shadow-float` | `0 8px 24px rgba(0,0,0,0.12)` | — | 浮动阴影（FAB、固定底栏） |
| `shadow-modal` | `0 8px 24px rgba(0,0,0,0.16)` | — | 模态阴影（Dialog、Popup） |
| `shadow-primary` | `0 4px 12px rgba(24,144,255,0.35)` | — | 主色阴影（主按钮，带蓝色光晕） |
| `shadow-accent` | `0 4px 12px rgba(255,107,53,0.35)` | — | 强调色阴影（CTA 按钮，带橙色光晕） |

### 1.4 渐变 Token

| Token 名称 | 渐变定义 | 用途 |
|-----------|---------|------|
| `gradient-primary` | `linear-gradient(135deg, #1890FF 0%, #0050B3 100%)` | 主按钮、启动页背景、头部区域 |
| `gradient-primary-soft` | `linear-gradient(180deg, #E6F7FF 0%, #FFFFFF 100%)` | 卡片浅蓝渐变背景、首页头部 |
| `gradient-accent` | `linear-gradient(135deg, #FF6B35 0%, #FA541C 100%)` | CTA 按钮、运动成就、进度强调 |
| `gradient-water` | `linear-gradient(180deg, #1890FF 0%, #096DD9 50%, #0050B3 100%)` | 游泳主题深水区背景（启动页、详情页头部） |
| `gradient-success` | `linear-gradient(135deg, #52C41A 0%, #389E0D 100%)` | 成功状态、完成进度环 |
| `gradient-mask-bottom` | `linear-gradient(180deg, rgba(245,247,250,0) 0%, rgba(245,247,250,1) 100%)` | 卡片底部内容渐隐遮罩 |

### 1.5 栅格系统

小程序端基于 375px 设计宽度：

| Token | 值 | 说明 |
|-------|-----|------|
| `grid-margin` | 16px | 页面左右边距 |
| `grid-gutter` | 8px | 列间距 |
| `grid-columns` | 4 列 | 375 - 32(左右margin) - 24(3个gutter) = 319 / 4 ≈ 79.75px/列 |
| `grid-column-width` | ~80px | 单列宽度 |

> **使用规则**：
> - 全宽卡片：左右贴 margin（16px），宽度 = 375 - 32 = 343px
> - 两列卡片：每列宽 = (375 - 32 - 8) / 2 = 167.5px
> - 内容区文字：左右各留 16px padding
> - 禁止元素超出 margin 边界（除了全宽背景色/渐变）

---

## 2. 组件库引用

> **所有 US 必须使用统一组件库，禁止自创组件样式。**
> **选型依据**：小程序端用 Taro + React 技术栈，Web 端用 Vue3 技术栈。

### 2.1 小程序端（用户端 + 教练端）：NutUI-React-Taro

| 项 | 值 |
|----|----|
| **组件库** | [NutUI-React-Taro](https://nutui.jd.com/taro/react/2x/) |
| **版本** | 2.x（React 版，适配 Taro 3+） |
| **维护方** | 京东零售前端团队（与 Taro 同厂，官方推荐） |
| **License** | MIT（全开源，无商业限制） |
| **组件数** | 80+ 高质量组件 |
| **设计规范** | 京东 APP 10.0 视觉规范，可深度定制主题 |
| **选择理由** | Taro 生态最成熟的 React 组件库；与 Taro 框架同厂维护，兼容性最佳；默认主色 `#1890FF` 与本项目 Token 天然对齐 |
| **Figma 组件库** | 🔲 待设计师在 Figma 社区搜索 "NutUI" 获取，或基于 NutUI 官网组件示例自建 Figma Components |

**常用组件映射**：

| 组件 | NutUI-React-Taro 组件 | 引用方式 |
|------|----------------------|---------|
| Button | `Button` | `<Button type="primary">` |
| Card | `Cell` + 自定义容器 | `<Cell>` |
| Modal / Dialog | `Dialog` | `<Dialog visible={...}>` |
| Toast | `Toast` | `Toast.show('...')` |
| Tabs | `Tabs` | `<Tabs>` |
| Avatar | `Avatar` | `<Avatar src="...">` |
| Badge | `Badge` | `<Badge value={5}>` |
| Empty（空状态） | `Empty` | `<Empty description="暂无数据" />` |
| Form / Input | `Form` / `Input` | `<Form><Input /></Form>` |
| Picker | `Picker` | `<Picker columns={...}>` |
| Skeleton（骨架屏） | `Skeleton` | `<Skeleton rows={3} animated />` |
| Swiper（轮播） | `Swiper` | `<Swiper>` |
| Progress（进度条） | `Progress` | `<Progress percentage={80} />` |
| Calendar | `Calendar` | `<Calendar>` |

> **开发原则**：NutUI 已有的组件直接使用；缺少的游泳业务专用组件（如课时进度环、教练卡片、排班日历）由前端自研，但**视觉样式必须遵循本文 Token**。

### 2.2 Web 端（管理后台）：Element Plus

| 项 | 值 |
|----|----|
| **组件库** | [Element Plus](https://element-plus.org/zh-CN/) |
| **版本** | 2.x（Vue 3 原生） |
| **维护方** | 饿了么大前端团队 |
| **License** | MIT |
| **组件数** | 70+ 核心组件 |
| **选择理由** | Vue 3 中后台首选；中文文档最完善；表单/表格能力最成熟，适合管理后台大量 CRUD 场景；默认主色 `#409EFF` 可通过 SCSS 变量覆盖为 `#1890FF` 对齐小程序端 |
| **Figma 组件库** | 🔲 待设计师在 Figma 社区搜索 "Element Plus" 获取，或参考 [Element Plus 官网](https://element-plus.org/zh-CN/) 组件示例自建 |

**常用组件映射**：

| 组件 | Element Plus 组件 | 引用方式 |
|------|------------------|---------|
| Button | `ElButton` | `<el-button type="primary">` |
| Table | `ElTable` | `<el-table :data="...">` |
| Form | `ElForm` / `ElFormItem` | `<el-form>` |
| Dialog | `ElDialog` | `<el-dialog>` |
| Pagination | `ElPagination` | `<el-pagination>` |
| Tag | `ElTag` | `<el-tag type="success">` |
| Avatar | `ElAvatar` | `<el-avatar>` |
| Empty | `ElEmpty` | `<el-empty description="暂无数据" />` |
| Skeleton | `ElSkeleton` | `<el-skeleton />` |
| Select | `ElSelect` | `<el-select>` |
| DatePicker | `ElDatePicker` | `<el-date-picker>` |
| Upload | `ElUpload` | `<el-upload>` |

### 2.3 主题对齐

两端组件库需通过主题变量对齐到统一 Token：

```scss
// 小程序端 NutUI 主题变量覆盖（variables.scss）
$primary-color: #1890FF;
$primary-color-end: #0050B3;
$success-color: #52C41A;
$warning-color: #FAAD14;
$danger-color: #FF4D4F;
$text-color: #262626;
$text-color-secondary: #8C8C8C;
$background-color: #F5F7FA;
$card-bg-color: #FFFFFF;
$border-color: #E8E8E8;

// Web 端 Element Plus 主题变量覆盖（element-variables.scss）
$--color-primary: #1890FF;
$--color-success: #52C41A;
$--color-warning: #FAAD14;
$--color-danger: #FF4D4F;
$--color-text-primary: #262626;
$--color-text-regular: #595959;
$--color-text-secondary: #8C8C8C;
$--background-color-base: #F5F7FA;
$--border-color-base: #E8E8E8;
```

> **Figma 中的 Token 实现**：设计师需在 Figma 中创建 Variables（Color / Number / String），命名与本文 §1.1-§1.3 的 Token 名称完全一致，所有组件实例引用这些 Variables，便于后续主题切换与设计稿同步。

---

## 2.5 组件视觉规格

> **本节定义核心组件的精确视觉参数**，所有页面必须使用这些规格。AI 生成设计稿时必须严格按参数创建，禁止自创样式。
> **对应 NutUI 组件**：每个组件标注 NutUI 组件名，开发时直接使用，设计稿需与 NutUI 默认样式一致（经主题变量覆盖后）。

### 2.5.1 按钮（Button）

| 变体 | 背景 | 文字 | 高度 | 圆角 | 阴影 | 用途 |
|------|------|------|------|------|------|------|
| **Primary（主按钮）** | `gradient-primary`（#1890FF→#0050B3） | #FFFFFF 16px Medium | 44px | radius-md (8px) | `shadow-primary` | 页面主操作（购买、预约、提交） |
| **Accent（CTA 按钮）** | `gradient-accent`（#FF6B35→#FA541C） | #FFFFFF 16px Medium | 44px | radius-md (8px) | `shadow-accent` | 强调转化（立即预约、体验课购买） |
| **Secondary（次按钮）** | #FFFFFF | `color-primary` #1890FF 16px Medium | 44px | radius-md (8px) | 1px 边框 `color-primary` | 次要操作（取消、返回） |
| **Text（文字按钮）** | transparent | `color-primary` #1890FF 14px Medium | auto | — | none | 行内链接（查看更多、忘记密码） |
| **Disabled（禁用态）** | #F5F5F5 | `color-text-disabled` #BFBFBF 16px | 44px | radius-md (8px) | none | 不可点击状态 |

**按钮内边距**：水平 padding 16px（大按钮可 24px）；按钮宽度：全宽按钮 343px（=375-32）。
**图标按钮**：图标 20×20px，图标与文字间距 8px。

### 2.5.2 卡片（Card）

| 属性 | 值 |
|------|-----|
| 背景 | `color-bg-card` #FFFFFF |
| 圆角 | `radius-lg` 12px |
| 阴影 | `shadow-card`（默认）/ `shadow-raised`（可点击时） |
| 内边距 | 16px（space-md）；信息密集卡片可用 12px |
| 卡片间距 | 12px（垂直） |
| 宽度 | 全宽 343px；两列 167.5px |

**卡片结构**（自上而下）：
1. **卡片头部**（可选）：标题 16px Medium + 右侧操作（文字按钮），底部 12px 间距
2. **卡片内容**：正文 14px Regular，行高 1.6
3. **卡片底部**（可选）：分割线 1px `color-divider` + 操作区

**可点击卡片**：hover/按下时阴影从 `shadow-card` 变为 `shadow-raised`，过渡 200ms。

### 2.5.3 标签（Tag）

| 变体 | 背景 | 文字 | 圆角 | 内边距 | 字号 |
|------|------|------|------|--------|------|
| Primary | `color-primary` 浅色 #E6F7FF | `color-primary` #1890FF | radius-pill (999px) | 水平 8px / 垂直 2px | 12px Regular |
| Success | `color-success` 浅色 #F6FFED | `color-success` #52C41A | radius-pill | 同上 | 12px |
| Warning | `color-warning` 浅色 #FFFBE6 | `color-warning` #FAAD14 | radius-pill | 同上 | 12px |
| Error | `color-error` 浅色 #FFF1F0 | `color-error` #FF4D4F | radius-pill | 同上 | 12px |
| Neutral | #F5F5F5 | `color-text-secondary` #8C8C8C | radius-pill | 同上 | 12px |

### 2.5.4 头像（Avatar）

| 尺寸 | 直径 | 圆角 | 用途 |
|------|------|------|------|
| xs | 24px | 50%（圆形） | 列表小头像、评论 |
| sm | 32px | 50% | 列表项、消息 |
| md | 48px | 50% | 教练卡片、详情 |
| lg | 64px | 50% | 个人中心 |
| xl | 80px | 50% | 教练详情头部 |

- 头像默认背景：`color-primary` 浅色 #E6F7FF
- 头像内文字（无图时）：首字符白色 16px Medium
- 边框（可选）：2px #FFFFFF（在深色背景上）

### 2.5.5 输入框（Input）

| 属性 | 值 |
|------|-----|
| 高度 | 44px |
| 背景 | #FFFFFF |
| 边框 | 1px `color-border` #E8E8E8；聚焦时 1px `color-primary` #1890FF |
| 圆角 | radius-md (8px) |
| 内边距 | 水平 12px |
| 字号 | 14px Regular |
| placeholder 色 | `color-text-disabled` #BFBFBF |
| 标签 | 顶部 14px Medium `color-text-primary`，与输入框间距 8px |
| 错误态 | 边框 `color-error` #FF4D4F + 下方 12px 错误文字 |

### 2.5.6 导航栏（NavBar）

| 属性 | 值 |
|------|-----|
| 总高度 | 88px（状态栏 44px + 内容区 44px） |
| 背景 | `gradient-primary`（#1890FF→#0050B3）或 #FFFFFF |
| 状态栏 | 系统接管（iOS 44px / Android 24px），Figma 中预留 44px 占位 |
| 内容区高度 | 44px |
| 标题 | 17px Medium 白色（深色背景）或 `color-text-primary`（白色背景），居中 |
| 左侧返回 | 图标 24×24px，白色，左 padding 16px |
| 右侧操作 | 文字 14px / 图标 24px，右 padding 16px |

**渐变导航栏**（用于首页、详情页等品牌页面）：背景使用 `gradient-primary`，标题/图标白色。
**白色导航栏**（用于表单页、设置页）：背景 #FFFFFF，底部 1px `color-divider`，标题/图标 `color-text-primary`。

### 2.5.7 底部标签栏（TabBar）

| 属性 | 值 |
|------|-----|
| 总高度 | 83px（内容 49px + iPhone X+ 安全区 34px；非 X 设备为 49px） |
| 背景 | #FFFFFF + 顶部 1px `color-divider` + `shadow-float` |
| Tab 数量 | 3-5 个（本项目用户端 4 个） |
| 图标尺寸 | 24×24px |
| 文字 | 10px Regular |
| 选中态 | 图标 `color-primary` #1890FF + 文字 `color-primary` |
| 未选中态 | 图标 `color-text-secondary` #8C8C8C + 文字 `color-text-secondary` |
| 图标文字间距 | 2px |
| 内容区 padding | 顶部 6px，底部 0（安全区在下方） |

**用户端 4 个 Tab**（顺序固定）：
1. 首页（home 图标）
2. 教练（search/users 图标）
3. 预约（calendar 图标）
4. 我的（user 图标）

**教练端 4 个 Tab**：
1. 工作台（grid 图标）
2. 排班（calendar 图标）
3. 学员（users 图标）
4. 我的（user 图标）

> **页面级规则**：TabBar 仅在每个 Tab 的首页显示，二级页面（详情、表单、列表）不显示 TabBar，由 NavBar 返回按钮回退。

### 2.5.8 列表项（Cell / List Item）

| 属性 | 值 |
|------|-----|
| 高度 | 56px（单行）/ 72px（两行） |
| 背景 | #FFFFFF |
| 内边距 | 水平 16px |
| 左侧图标 | 24×24px `color-text-secondary`，右间距 12px |
| 标题 | 16px Regular `color-text-primary` |
| 描述（两行时） | 12px Regular `color-text-secondary`，标题下 2px |
| 右侧值 | 14px Regular `color-text-secondary` |
| 右侧箭头 | 16×16px `color-text-disabled` |
| 分割线 | 1px `color-divider`，左侧缩进 16px（与标题对齐） |

### 2.5.9 空状态（Empty）

| 属性 | 值 |
|------|-----|
| 插画尺寸 | 120×120px（扁平运动主题插画，见 §0.5.3） |
| 文案 | 14px Regular `color-text-secondary`，距插画 16px |
| CTA 按钮（可选） | Primary 按钮，距文案 24px |
| 垂直居中 | 距顶部约 30% 屏幕高度 |

### 2.5.10 图标规范

| 属性 | 值 |
|------|-----|
| 风格 | 线性（outline），描边 2px，圆角端点（round cap/join） |
| 尺寸 | 24×24px（标准）/ 20×20px（紧凑）/ 16×16px（小） |
| 颜色 | 跟随上下文文字色（currentColor），不独立设色 |
| 来源 | NutUI 内置图标库优先；缺失图标用 [Tabler Icons](https://tabler-icons.io/)（线性风格统一） |
| 禁止 | 填充式图标、彩色图标、emoji、写实插画 |

### 2.5.11 进度环 / 进度条

| 属性 | 值 |
|------|-----|
| 进度环直径 | 80px（套餐详情）/ 48px（卡片） |
| 进度环描边 | 6px，背景轨 `color-divider`，前景 `gradient-primary` |
| 进度条高度 | 8px |
| 进度条圆角 | radius-pill |
| 进度条背景 | `color-divider` #F0F0F0 |
| 进度条前景 | `gradient-primary` 或 `gradient-accent`（运动数据） |
| 中心文字 | 24px Bold `color-text-primary`（进度环） |

---

## 2.6 AI 设计生成规则（给 AI Agent 必读）

> **本节是 AI 生成 Figma 设计稿时的强制规则**，DocumentToDesign 等 AI Agent 调用 TalkToFigma MCP 时必须遵守。不遵守将导致设计稿质量不达标。

### 2.6.1 页面骨架结构（强制）

每个小程序页面 Frame **必须**按以下骨架自上而下创建（除启动页外）：

```
Frame（375×812，背景 #F5F7FA）
├── 状态栏（44px 高，与 NavBar 同色）
├── 导航栏 NavBar（44px 高，渐变或白色，见 §2.5.6）
├── 页面内容区（可滚动，背景 #F5F7FA，padding 16px）
│   ├── 页面标题区（可选，24px Bold + 8px 副标题）
│   ├── 内容卡片/列表...
│   └── 底部留白（83px，避免内容被 TabBar 遮挡）
└── TabBar（83px 高，仅 Tab 首页包含，见 §2.5.7）
```

**关键规则**：
- 所有有 TabBar 的页面，内容区底部必须留 83px 空白
- 二级页面（详情/表单）不含 TabBar，但内容区底部留 24px
- 启动页不含 NavBar 和 TabBar，全屏渐变背景 + 居中 logo

### 2.6.2 居中对齐必须用 Auto Layout（强制）

**禁止用估算 x 坐标实现居中**（这是之前启动页文字不居中的根因）。

正确做法：
1. 创建一个 Frame 作为容器
2. 调用 `set_layout_mode` 设置为 `VERTICAL`
3. 调用 `set_axis_align` 设置 `primaryAxisAlignItems: CENTER` 和 `counterAxisAlignItems: CENTER`
4. 在容器内添加子元素
5. 容器宽度设为父级宽度，子元素自然居中

**水平居中**：父 Frame 宽度填满（343px），子元素水平对齐 CENTER。
**垂直居中**：父 Frame 高度填满（如启动页 812px），子元素垂直对齐 CENTER。

### 2.6.3 视觉层次（避免"扁平 low 感"）

每个页面**必须**包含以下视觉层次元素：

1. **阴影**：卡片用 `shadow-card`，可点击卡片用 `shadow-raised`，浮动按钮/底栏用 `shadow-float`
2. **渐变**：NavBar、启动页、详情页头部、主按钮使用渐变（§1.4），不要纯色平铺
3. **圆角**：卡片 12px、按钮 8px、标签 999px（胶囊），禁止直角矩形
4. **留白节奏**：页面 margin 16px，卡片间距 12px，卡片内 padding 16px，区块间距 24px
5. **信息层级**：标题 24px Bold → 卡片标题 16px Medium → 正文 14px Regular → 辅助 12px Regular
6. **色彩点缀**：主色用于操作元素（按钮/链接/选中态），强调色用于 CTA 和成就，不要整个页面都是蓝色

### 2.6.4 图标和占位

- **图标**：无法直接创建 SVG 图标时，用 24×24px 的圆角矩形占位（圆角 4px，背景 `color-primary` 浅色 #E6F7FF），并在旁边标注 `Icon: <icon-name>` 文字（10px 灰色），方便设计师替换
- **头像**：用圆形矩形占位（48×48px，50% 圆角，背景 #E6F7FF），内放首字符文字
- **图片**：用矩形占位（圆角 12px，背景 #F5F5F5），中间放 `Image` 12px 灰色文字
- **禁止用 emoji 或色块代替图标**

### 2.6.5 状态徽标使用

- 所有状态（教练状态、套餐状态、订单状态）**必须**使用 §3 定义的徽标样式
- 徽标作为 Tag 组件创建（见 §2.5.3），使用对应颜色变体
- 徽标位置：列表项右侧、卡片标题右侧、头像右下角

### 2.6.6 排班页特殊布局（周×时段网格）

排班页/日历页**禁止**做成"日期标签 + 卡片堆叠"，必须使用二维网格：

```
排班页 Frame
├── NavBar（渐变）
├── 周视图（高 60px，白色卡片）
│   └── 7 列（周一~周日），等宽 53.5px，选中日期蓝色背景
├── 时段网格（可滚动）
│   ├── 左列：时段标签（9:00 / 10:00 / ...），宽 48px
│   └── 右 7 列：每天的时段格
│       ├── 每格 48×48px，圆角 8px
│       ├── 可约：白底 + 蓝色边框 + 蓝色文字
│       ├── 已约：灰底 #F5F5F5 + 灰字 + 删除线
│       └── 选中：渐变蓝背景 + 白字
└── 底部操作栏（固定底部，"确认预约"按钮）
```

### 2.6.7 列表页特殊布局

教练列表/订单列表等列表页必须包含：
1. **搜索栏**（如有）：顶部固定，白色背景，搜索框 44px 高，圆角 999px，灰色背景 #F5F7FA
2. **筛选标签栏**（如有）：横向滚动，标签 32px 高，胶囊圆角，选中蓝色背景白字
3. **列表卡片**：每张卡片包含头像/图片 + 标题 + 描述 + 状态徽标 + 右侧箭头，卡片间距 12px
4. **空状态**：无数据时显示 Empty 组件（插画 + 文案 + CTA）

### 2.6.8 详情页特殊布局

详情页必须包含：
1. **头部区域**（高 200-240px）：渐变背景 + 大图/头像 + 姓名/标题 + 状态徽标
2. **Tab 切换**（可选）：如"资料 / 评价 / 套餐"，用 NutUI Tabs 样式
3. **信息卡片组**：多张卡片分组展示，每张卡片有标题
4. **底部固定操作栏**：白色背景 + 顶部阴影 + 主按钮（全宽 343px 或双按钮）

### 2.6.9 四态设计要求

每个页面 Frame 创建后，必须在其右侧（间隔 50px）再创建 3 个副本 Frame：
- `<Frame名>-empty`：空状态
- `<Frame名>-loading`：骨架屏（用灰色矩形 #F0F0F0 模拟内容块）
- `<Frame名>-error`：错误状态（插画占位 + 文案 + 重试按钮）

> 本次批次生成时可先只做 success 态，empty/loading/error 态在设计师精修阶段补充，但 Frame 命名要预留。

### 2.6.10 颜色使用纪律

- **背景**：页面背景 #F5F7FA，卡片背景 #FFFFFF，不要混用
- **主色**：#1890FF 只用于操作元素（按钮、链接、选中态、图标），不要做大面积背景（除 NavBar/启动页）
- **文字**：主文字 #262626，次文字 #8C8C8C，禁用纯黑 #000000
- **状态色**：成功绿/警告橙/错误红只用于对应状态，不做装饰色
- **渐变**：每个页面渐变使用不超过 2 处（NavBar + 1 个强调元素）

---

## 3. 状态徽标视觉定义

> **本节定义所有"状态徽标"的视觉规范**，跨 US 复用。**禁止在故事级 user-story.md §14 中重复定义。**

### 3.1 教练实时状态徽标（5 种）

> 适用于：US-001 列表/详情、US-013 教练管理实时状态、US-038 分享可约时段等

| 状态 | 文案 | 背景色 token | 文字色 token | 业务含义 | 触发现场 |
|------|------|-------------|-------------|---------|---------|
| 空闲中 | 空闲中 | `color-success` 浅色 | `color-success` | 可立即预约 | 教练点击"开始接单" |
| 上课中 | 上课中 | `color-error` 浅色 | `color-error` | 当前正在上课 | 学员签到触发 |
| 休息中 | 休息中 | `color-warning` 浅色 | `color-warning` | 临时休息 | 教练点击"休息一下" |
| 已下班 | 已下班 | `color-text-secondary` 浅色 | `color-text-secondary` | 当日不再接单 | 教练点击"下班" / 系统日切 |
| 请假中 | 请假中 | `color-warning` 浅色 + 边框 | `color-warning` | 短期请假 | 教练提交请假申请 |

> 详细业务规则见 [prd.md §5.2.2](../../../prd/prd.md)

### 3.2 套餐状态徽标

> 适用于：US-021 学员查看我的套餐、US-022 学员更换绑定教练、US-050 套餐过期处理

| 状态 | 文案 | 背景色 | 备注 |
|------|------|--------|------|
| active | 使用中 | `color-success` 浅色 |  |
| exhausted | 已耗尽 | `color-text-secondary` 浅色 |  |
| expired | 已过期 | `color-warning` 浅色 |  |
| refunded | 已退款 | `color-text-secondary` 浅色 |  |
| frozen | 已冻结 | `color-error` 浅色 | 教练离职触发 |

> 业务规则见 [prd.md §4.2](../../../prd/prd.md)

### 3.3 订单状态徽标

> 适用于：US-026 学员查看订单、US-046 管理员处理订单

| 状态 | 文案 | 背景色 |
|------|------|--------|
| pending_payment | 待支付 | `color-warning` 浅色 |
| paid | 已支付 | `color-success` 浅色 |
| canceled | 已取消 | `color-text-secondary` 浅色 |
| refund_approval | 退款审批中 | `color-warning` 浅色 |
| dispute_refund | 争议处理中 | `color-error` 浅色 |
| refunded | 已退款 | `color-text-secondary` 浅色 |
| refund_rejected | 退款已拒绝 | `color-error` 浅色 |

> 业务规则见 [prd.md §5](../../../prd/prd.md)

### 3.4 教练入职状态徽标

> 适用于：US-011 管理员审核、US-013 教练管理

| 状态 | 文案 | 背景色 |
|------|------|--------|
| 0 申请中 | 申请中 | `color-warning` 浅色 |
| 1 在职 | 在职 | `color-success` 浅色 |
| 2 休息 | 休息 | `color-text-secondary` 浅色 |
| 3 离职审批中 | 离职审批中 | `color-warning` 浅色 |
| 4 已离职 | 已离职 | `color-text-secondary` 浅色 |

---

## 4. 4 态截图模板

> **所有页面必须提供 4 种状态截图**，命名与归档统一。

| 状态 | 触发条件 | 截图要点 |
|------|---------|---------|
| **空状态 (empty)** | 数据为空（无教练、无订单、无套餐等） | 居中插画 + 提示文案 + 可选 CTA 按钮 |
| **加载状态 (loading)** | 数据请求中（首次加载 / 翻页） | Skeleton 占位（不要 spinner 居中，骨架屏更好） |
| **错误状态 (error)** | 网络断开 / 接口 5xx / 业务 4xx | 居中插画 + 错误文案 + "重试" 按钮 |
| **成功状态 (success)** | 数据正常返回 | 完整内容展示 |

**截图命名规范**：
```
<US编号>-<页面名>-<状态>.png
示例：US-001-列表页-empty.png / US-001-列表页-loading.png
```

**归档位置**：🔲 待补充（建议放 Figma 库的 `/screenshots/<US编号>/`）

---

## 5. 文案规范

> **所有用户可见文案**必须遵循本节规范，统一语气、长度、错误处理。

### 5.1 文案语气

- **简洁**：每条文案 ≤ 20 字
- **礼貌**：用"请"、"您"而非命令式
- **避免技术术语**：不出现 "404"、"COACH_NOT_FOUND"、"接口异常"等

### 5.2 通用文案模板

| 场景 | 模板 | 示例 |
|------|------|------|
| 空数据 | 暂无{对象}，{行动建议或鼓励} | "暂无教练入驻，敬请期待" |
| 加载中 | 加载中... | （通用，不分场景） |
| 网络错误 | 网络异常，请重试 | （通用） |
| 服务器错误 | 服务繁忙，请稍后重试 | （通用） |
| 操作成功 | {动作}成功 | "保存成功" |
| 操作失败 | {动作}失败，请稍后重试 | "提交失败，请稍后重试" |
| 必填校验 | 请输入{字段名} | "请输入手机号" |
| 权限拒绝 | 请先{前置动作} | "请先登录" |

### 5.3 故事级文案扩展

如果某 US 有**特殊文案**（如下单成功后的祝贺语、支付失败的详细说明），在 `docs/stories/US-XXX-.../user-story.md` 的「§14 页面级设计决策」中补充，**但不与本节冲突**。

---

## 6. 适配规范

> **所有页面必须适配目标设备**

| 维度 | 规范 |
|------|------|
| **屏幕尺寸** | iPhone SE（375×667）作为最小适配，iPhone 14 Pro Max（430×932）作为大屏 |
| **微信小程序** | rpx 单位，750rpx = 屏幕宽度 |
| **Web 后台** | 1280px / 1440px / 1920px 三档断点 |
| **字体缩放** | 跟随系统设置（用户改了字号要适配） |
| **横竖屏** | 仅竖屏（横屏提示"请使用竖屏访问"） |

---

## 7. Figma 设计交付规范

> **本节定义 Figma 设计稿的交付要求与工作流，所有设计师必须遵守。**

### 7.1 可编辑图层要求（强制）

- **必须生成可编辑的 Figma 图层**，禁止仅交付截图或导出的图片
- 所有设计元素必须使用 Figma 原生工具创建：Frame、Auto Layout、Component、Text、Shape、Vector 等
- 颜色、字体、间距必须使用 §1 定义的 Token（通过 Figma Variables 或 Styles 实现）
- 组件必须使用 §2 定义的组件库实例，禁止栅格化为图片
- 状态徽标必须使用 §3 定义的样式，作为可复用 Component

> **验收标准**：交付的 Figma 文件中，每个页面 Frame 的图层树必须可展开、可选中、可编辑属性。截图仅用于四态截图归档（§4），不作为交付物。

### 7.2 文件存放位置

- 所有新建的 Figma 文件必须创建在 **Figma Drafts** 中（设计师的个人草稿区）
- 文件命名遵循 [figma-frame-organization.md](./figma-frame-organization.md) §1 的规范：
  - `leyoSwimming-用户端小程序`
  - `leyoSwimming-教练端小程序`
  - `leyoSwimming-管理端Web`
- 设计稿评审通过后，由设计负责人将文件移动到团队项目空间

### 7.3 参考文档（创建项目与设计前必读）

设计师在创建 Figma 文件和开始设计前，**必须阅读同目录下的以下两份文件**作为项目创建和设计的参考：

| # | 文件 | 用途 |
|---|------|------|
| 1 | [figma-frame-organization.md](./figma-frame-organization.md) | Figma 文件组织、Frame 命名、55 页映射、四态模板、交付 checklist |
| 2 | [us-batch-handoff.md](./us-batch-handoff.md) | 50 个 US 按 19 批次拆分的交割清单，每批的 US/页面/依赖/Figma 路径/交付要求 |

> 这两份文件定义了「文件怎么组织」「页面怎么命名」「US 分几批交付」「每批包含哪些页面」等关键信息，是 Figma 设计的执行手册。

### 7.4 Figma 链接回填要求

每批设计完成后，设计师**必须**将 Figma 链接回填到对应 US 的 `user-story.md` §13 占位符中：

```markdown
## 13. Figma 链接

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | xxx 页 Figma file URL | https://www.figma.com/file/... | ✅ |
| 2 | xxx 页关键 frame node-id | node-id=123:456 | ✅ |
```

回填规则：
- **Figma file URL**：填写完整的 Figma 文件链接（从 Drafts 中获取）
- **node-id**：填写关键页面的 Frame node-id（右键 Copy/Paste as → Copy link to selection）
- **状态**：从 🔲 待设计填写 改为 ✅
- **§13.1 状态截图清单**：四态完成后逐项勾选

> 链接回填是设计交付的必要环节，未回填视为未完成。详见 [us-batch-handoff.md](./us-batch-handoff.md) §7 状态回填要求。

### 7.5 交付工作流

```
设计师收到 US 批次
    ↓
阅读 3 份参考文档（本文件 + figma-frame-organization.md + us-batch-handoff.md）
    ↓
阅读对应 US 的 user-story.md §1-§6（业务）+ §13-§15（设计）
    ↓
在 Figma Drafts 创建文件（命名遵循 figma-frame-organization.md §1）
    ↓
按批次清单设计页面（Frame 命名遵循 figma-frame-organization.md §2）
    ↓
每个页面完成四态（empty / loading / error / success）
    ↓
自检交付 checklist（figma-frame-organization.md §6）
    ↓
将 Figma 链接回填到各 US 的 user-story.md §13
    ↓
通知 PM/设计负责人评审
```

---

## 8. 上下游引用

- **上游业务规则**：[docs/prd/prd.md](../../prd/prd.md)（所有徽标的状态名 / 业务含义必须与 PRD 一致）
- **下游产物**：`docs/stories/US-XXX-.../user-story.md` §13-15（每 US 引用本文档，不重复定义）
- **配套**：
  - [figma-frame-organization.md](./figma-frame-organization.md)（Figma 文件组织与 Frame 命名规范）
  - [us-batch-handoff.md](./us-batch-handoff.md)（US 分批 Figma 交割清单）
  - Figma 组件库地址 🔲 待补充

---

## 9. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-07-30 | Design | 初版占位（待补 token / 组件 / 徽标 / 文案） |
| v1.0 | 2026-07-30 | PM | 重新架构：L1/L2/L3 三层分离；明确"全局规范 vs 故事级独有"的边界；初版内容覆盖 token / 组件库 / 4 类状态徽标 / 4 态截图模板 / 文案规范 / 适配 |
| v1.1 | 2026-07-31 | PM | 新增 §7 Figma 设计交付规范：可编辑图层强制要求、Drafts 存放、参考文档引用、Figma 链接回填、交付工作流；修正 §8 上下游引用的相对路径 |
| v1.2 | 2026-07-31 | PM | 新增 §0.5 游泳运动主题（Aquatic Motion）：主题理念/参考/风格定调；补齐 §1 全部 Token 色值（13 色 + 字号 9 级 + 间距/圆角/阴影）；新增 §2 组件库选型：小程序端 NutUI-React-Taro、Web 端 Element Plus，含组件映射表 + 主题对齐 SCSS 变量 |
| v1.3 | 2026-08-01 | PM | 扩展 §1.3 阴影系统（7 级阴影含主色/强调色光晕）；新增 §1.4 渐变 Token（6 种渐变）；新增 §1.5 栅格系统；新增 §2.5 组件视觉规格（按钮/卡片/标签/头像/输入框/NavBar/TabBar/列表项/空状态/图标/进度环共 11 个组件精确参数）；新增 §2.6 AI 设计生成规则（页面骨架/居中对齐/视觉层次/图标占位/排班网格/列表/详情/四态/颜色纪律共 10 条强制规则） |
| v1.4 | 2026-08-02 | PM | 架构升级为四层：新增 L1.5 Page Spec 层；建立 `docs/figma/page-spec/` 目录；新增 TEMPLATE.md / U-home-page.md / U-splash-page.md / U-announcement-page.md / U-coach-list-page.md / U-coach-detail-page.md / U-coach-schedule-page.md；新增 cross-page-check.md 跨页一致性自检报告；明确 Page Spec 是 AI 生成 Figma 的唯一指令源 |
