# US-058 用户-AI助理推荐教练与套餐

> **状态**：[DRAFT]（初稿）
> **优先级**：[MVP]
> **估时**：4 人天
> **作者**：AI　|　**最后更新**：2026-08-15
> **配套文档**：Figma：[U-AI助理页](../../figma/page-spec/U-AI-assistant-page.md)　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)　·　可行性分析：[docs/tech/AI-assistant-requirements/README.md](../../tech/AI-assistant-requirements/README.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-058 |
| **标题** | 用户-AI助理推荐教练与套餐 |
| **角色（Actor）** | 游客（主）、注册用户/学员（主）、系统（辅）|
| **业务价值（Why）** | 降低用户选择教练和套餐的决策门槛，通过自然语言对话快速匹配需求，提升游客转化率和学员体验 |
| **优先级** | [MVP] |
| **估时** | 4 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划 / 可行性分析（链接见上方）|

---

## 2. 触发条件

- **触发方**：游客或已登录用户
- **触发动作**：用户点击底部 TabBar「leyo」入口，进入 AI 助理页
- **触发时机**：主动进入

---

## 3. 前置条件

- [x] 游客可访问基础推荐功能（不依赖登录）
- [x] 已登录用户可访问个性化推荐（依赖 US-004 / US-005）
- [x] 后端已搭建 Python AI Service（FastAPI + LangChain）并接入 LLM
- [x] 第二批次相关 US（US-019 浏览套餐、US-020 购买套餐、US-021 查看我的套餐、US-045 配置套餐模板、US-043 套餐管理）已完成，数据库中有 mock 教练/套餐/用户数据

---

## 4. 业务流程

### 4.1 主路径

1. 用户从底部 TabBar 点击「leyo」进入 AI 助理页
2. 页面初始化展示 leyo 欢迎语和快捷查询标签
3. 用户通过以下任一方式与 leyo 交互：
   - 点击快捷标签（如「推荐自由泳教练」「预算3000的套餐」）
   - 手动输入自然语言问题
4. 前端调用 `/api/ai-assistant/chat`，携带 `sessionId` 和 `message`
5. Java API Gateway 鉴权后转发给 Python AI Service
6. Python LangChain Agent 解析用户意图，决定调用 `@tool`：
   - `query_coaches`：召回候选教练
   - `query_packages`：召回候选套餐
   - `get_user_profile`：获取已登录用户画像
   - `get_user_packages`：获取已购套餐，避免重复推荐
   - `get_hot_recommendations`：获取热门推荐（冷启动/游客）
7. Python AI Service 通过内部接口 `/api/internal/ai/*` 调用 Java 业务服务获取数据
8. Java 业务服务返回脱敏后的教练/套餐/用户数据
9. LLM 根据 Tool 返回结果生成自然语言回复和推荐理由
10. Python AI Service 返回结构化回复给 Java Gateway
11. Java Gateway 返回给前端，前端展示 leyo 回复文本 + 推荐卡片 + 建议追问
12. 用户可继续多轮对话，或点击推荐卡片跳转教练详情/套餐详情

### 4.2 新建会话

1. 用户点击顶部「新会话」按钮
2. 前端清空当前聊天区域，重置为初始欢迎状态
3. 调用 `/api/ai-assistant/session/create` 创建新会话
4. 页面展示新的 `sessionId` 对应欢迎语和快捷标签

### 4.3 查看历史会话

1. 用户点击顶部「历史」按钮
2. 从右侧滑出历史会话抽屉
3. 调用 `/api/ai-assistant/session/list` 获取历史会话列表
4. 用户点击某条历史会话
5. 抽屉关闭，前端切换到该会话，调用 `/api/ai-assistant/session/detail` 加载消息记录

### 4.4 异常分支

- **分支 1**：游客询问需要登录才能个性化的问题（如「我的套餐」）→ 回复引导登录，不提供个人数据
- **分支 2**：LLM 调用失败或超时 → 前端展示错误提示，支持重试
- **分支 3**：Python AI Service 调用 Java 内部接口失败 → 返回通用服务错误，不暴露内部异常
- **分支 4**：用户输入敏感/违规内容 → leyo 拒绝回答并给出友好提示

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 游客可访问基础推荐；已登录用户可个性化推荐 | 本 US §3 |
| 2 | 教练公开信息（姓名、头像、评分、教龄、参考价格、擅长泳姿）可进入 LLM Prompt 和前端展示 | [AI-assistant-requirements §8.1](../../tech/AI-assistant-requirements/README.md) |
| 3 | 用户敏感信息（手机号、openid、union_id、用户真实姓名）需脱敏，不进入 LLM Prompt | [AI-assistant-requirements §8.1](../../tech/AI-assistant-requirements/README.md) |
| 4 | AI 助理推荐结果需基于当前数据库真实教练/套餐数据 | [AI-assistant-requirements §6.2](../../tech/AI-assistant-requirements/README.md) |
| 5 | 推荐日志需记录用户输入、Tool 调用链、LLM 回复，用于审计和效果分析 | [AI-assistant-requirements §8.2](../../tech/AI-assistant-requirements/README.md) |
| 6 | 多轮对话需维护会话上下文，避免重复询问已知信息 | [AI-assistant-requirements §6.4](../../tech/AI-assistant-requirements/README.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：游客使用快捷标签获取教练推荐

```gherkin
Given 用户未登录
When  用户进入 AI 助理页
And   用户点击快捷标签「自由泳教练」
Then  leyo 回复推荐教练列表
And   每个教练卡片展示姓名、头像、评分、教龄、参考价格、擅长泳姿、推荐理由
And   推荐结果来自当前数据库中的 coach 记录
And   接口返回 HTTP 200
```

### 6.2 场景 2：已登录用户获取个性化套餐推荐

```gherkin
Given 用户已登录
And   用户画像：年龄 30，目标泳姿自由泳，预算 3000
And   用户无已购套餐
When  用户输入「预算3000，想学自由泳，推荐个套餐」
Then  leyo 调用 get_user_profile 获取画像
And   leyo 调用 query_packages 召回符合条件的套餐
And   leyo 回复推荐套餐列表
And   套餐卡片展示名称、课时、价格、单课时价格、有效期、推荐理由
And   接口返回 HTTP 200
```

### 6.3 场景 3：避免重复推荐已购套餐

```gherkin
Given 用户已登录
And   用户已购买「成人自由泳 10 节私教」套餐且 status=active
When  用户输入「推荐自由泳套餐」
Then  leyo 调用 get_user_packages 获取已购套餐
And   leyo 不重复推荐相同或高度相似的套餐
And   leyo 优先推荐其他未购买套餐、续课方案或自定义套餐
```

### 6.4 场景 4：推荐自定义套餐

```gherkin
Given 用户已登录
And   用户输入「我想上 12 节自由泳一对一，但标准套餐里没有合适的」
When  leyo 调用 query_packages 查询标准套餐后发现无完全匹配
Then  leyo 推荐「自定义套餐」选项
And   推荐卡片展示教练参考单价、自定义课时数、预估总价、班级规模
And   用户点击卡片跳转 U-套餐详情页的自定义套餐购买流程
```

### 6.5 场景 5：新建会话

```gherkin
Given 用户正在 AI 助理页进行多轮对话
When  用户点击顶部「新会话」按钮
Then  当前聊天区域清空
And   页面恢复为 leyo 欢迎语和快捷标签
And   系统创建新的 sessionId
```

### 6.6 场景 6：查看历史会话

```gherkin
Given 用户已登录且有 3 条历史会话
When  用户点击顶部「历史」按钮
Then  右侧滑出历史会话抽屉
And   展示 3 条会话标题和最后消息时间
When  用户点击其中一条
Then  抽屉关闭
And   页面加载该会话的历史消息记录
```

### 6.7 场景 7：LLM 服务失败降级

```gherkin
Given 用户输入有效问题
And   Python AI Service 因网络超时无法连接 LLM
When  前端调用 /api/ai-assistant/chat
Then  Java Gateway 返回错误状态
And   前端展示「leyo 暂时走神了，请重试」提示
And   展示重试按钮
```

### 6.8 场景 8：敏感输入处理

```gherkin
Given 用户输入包含个人隐私或违规内容
When  leyo 接收到该输入
Then  leyo 不调用任何业务 Tool
And   leyo 回复「这个问题我暂时无法回答，换个问题试试吧」
And   不将敏感内容记录到推荐日志的明细中
```

### 6.9 场景 9：推荐卡片跳转教练详情

```gherkin
Given leyo 已推荐王教练
When  用户点击王教练推荐卡片
Then  跳转 U-教练详情页（携带 coach_id）
```

### 6.10 场景 10：自定义套餐推荐卡片跳转详情

```gherkin
Given leyo 已推荐自定义「12 节自由泳一对一」套餐
When  用户点击自定义套餐推荐卡片
Then  跳转 U-套餐详情页（携带 coach_id、hours、class_size 等自定义参数）
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `coach` | 读取 | 教练公开信息：姓名、头像、评分、教龄、参考价格、擅长泳姿、班级规模 |
| 2 | `package_template` | 读取 | 套餐模板信息：名称、模式（standard/experience/custom）、课时、价格、有效期、泳姿 |
| 3 | `package` | 读取 | 已登录用户的已购套餐，用于去重和续课推荐 |
| 4 | `user` / `user_profile` | 读取 | 用户画像：年龄、目标泳姿、游泳基础等 |
| 5 | `ai_chat_session` | 新增 | 会话主表：session_id、user_id、title、create_time、update_time |
| 6 | `ai_chat_message` | 新增 | 消息记录表：message_id、session_id、role、content、recommendations、create_time |
| 7 | `ai_recommendation_log` | 新增 | 推荐日志表：记录输入、Tool 调用、LLM 回复、耗时 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/ai-assistant/chat` | POST | 新增 | 用户发送消息，返回 leyo 回复 |
| 2 | `/api/ai-assistant/session/list` | POST | 新增 | 已登录用户历史会话列表 |
| 3 | `/api/ai-assistant/session/create` | POST | 新增 | 创建新会话 |
| 4 | `/api/ai-assistant/session/detail` | POST | 新增 | 获取会话历史消息 |
| 5 | `/api/internal/ai/coaches/query` | POST | 新增 | 内部接口：召回教练 |
| 6 | `/api/internal/ai/packages/query` | POST | 新增 | 内部接口：召回套餐 |
| 7 | `/api/internal/ai/user/profile` | POST | 新增 | 内部接口：获取用户画像 |
| 8 | `/api/internal/ai/user/packages` | POST | 新增 | 内部接口：获取已购套餐 |
| 9 | `/api/internal/ai/recommendations/hot` | POST | 新增 | 内部接口：热门推荐 |

### 7.3 状态机影响

（本 US 只读推荐，不触发业务状态转换）

---

## 8. 边界场景

### 8.1 边界场景 1：LLM 返回格式异常

- **触发条件**：LLM 未按预期返回 Tool 调用或最终回复
- **预期行为**：Python AI Service 捕获异常，返回通用错误
- **用户可见反馈**：「leyo 理解错了，请重试」

### 8.2 边界场景 2：无匹配教练/套餐

- **触发条件**：数据库中无符合用户条件的教练或套餐
- **预期行为**：leyo 回复未找到合适推荐，建议放宽条件或联系客服
- **用户可见反馈**：友好提示 + 建议追问

### 8.3 边界场景 3：会话消息过多

- **触发条件**：单会话消息超过 50 条
- **预期行为**：触发上下文压缩或提示用户新建会话
- **用户可见反馈**：提示「会话太长了，建议新建会话重新开始」

### 8.4 边界场景 4：游客访问个性化功能

- **触发条件**：游客点击「我的套餐」等需登录标签
- **预期行为**：leyo 回复引导登录，不调用 get_user_profile
- **用户可见反馈**：「登录后我可以为你推荐更精准的内容哦」

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）
- [x] US-005（用户补充注册资料）
- [x] US-019（学员浏览正价套餐）
- [x] US-020（学员购买正价套餐）
- [x] US-045（管理员配置标准与自定义套餐）
- [x] US-043（管理员查看与管理用户套餐，用于数据维护）

### 9.2 后续 US（依赖本故事）

- [ ] 暂无

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 可独立交付，依赖仅为数据准备
- [x] **N**egotiable（可协商）- 推荐策略、快捷标签、LLM 模型可协商
- [x] **V**aluable（有价值）- 提升用户转化和体验
- [x] **E**stimable（可估算）- 4 人天明确
- [x] **S**mall（足够小）- 聚焦 AI 推荐对话和会话管理
- [x] **T**estable（可测试）- 10 个 GWT 场景覆盖核心路径

---

## 11. 完整性检查

- [x] 15 章齐全
- [x] 10 个 GWT 场景
- [x] ≥3 边界场景
- [x] PRD / 可行性分析引用明确

---

## 12. 备注

- **性能要求**：`/api/ai-assistant/chat` P99 < 5s（含 LLM 调用）
- **安全要求**：所有进入 LLM Prompt 的数据必须经过脱敏；内部接口需 IP 白名单 + Token 限制
- **LLM 选型**：MVP 阶段优先接入通义千问 / Kimi / DeepSeek 等支持 function calling 的国产模型
- **推荐日志**：用于后续分析推荐转化率、LLM 调用成功率、用户满意度

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 | 状态 |
|---|------|------|------|
| 1 | AI 助理页 page-spec | [U-AI-assistant-page.md](../../figma/page-spec/U-AI-assistant-page.md) | 📝 |
| 2 | 可交互原型 | [docs/tech/AI-assistant-requirements/prototype.html](../../tech/AI-assistant-requirements/prototype.html) | ✅ |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **AI 助理页** | 🔲 | 🔲 | 🔲 | 🔲 | |

---

## 14. 页面级设计决策

### 14.1 入口位置

- **选项**：
  - A. 底部 TabBar 新增「leyo」独立入口
  - B. 放在「我的」页面内作为功能入口
- **结论**：选择 A，独立 TabBar 入口，符合 AI 助理高频使用定位
- **影响范围**：需更新 `miniapp-user/src/app.config.ts` TabBar 配置

### 14.2 游客模式

- **结论**：游客可直接使用基础推荐，不强制登录；只有涉及个人隐私的功能才引导登录
- **影响范围**：`/api/ai-assistant/chat` 游客可访问；`/api/ai-assistant/session/list` 需登录

### 14.3 推荐卡片交互

- **结论**：教练卡片点击跳转 U-教练详情页，套餐卡片点击跳转 U-套餐详情页
- **影响范围**：需与 US-019、US-020 页面衔接

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 🔲 | 🔲 | 🔲 | 🔲 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-15 | AI | 初版：基于可行性分析报告和交互原型编写 |
| v1.1 | 2026-08-17 | AI | 补充自定义套餐推荐场景与跳转规则 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
