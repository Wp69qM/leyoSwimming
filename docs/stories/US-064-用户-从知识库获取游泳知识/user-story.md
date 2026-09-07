# US-064 用户-从知识库获取游泳知识

> **状态**：[APPROVAL]
> **优先级**：[MVP]
> **估时**：1.5 人天
> **作者**：AI | **最后更新**：2026-09-03
> **配套文档**：需求：[rag-function-calling-requirements.md](../../tech/AI-assistant-requirements/rag-function-calling-requirements.md) · 设计：[2026-09-02-ai-assistant-rag-design.md](../../superpowers/spec/2026-09-02-ai-assistant-rag-design.md) · Figma：[U-AI-assistant-page.md](../../figma/page-spec/U-AI-assistant-page.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-064 |
| **标题** | 用户-从知识库获取游泳知识 |
| **角色（Actor）** | 游客、注册用户/学员 |
| **业务价值（Why）** | 提升 AI 助理的可回答范围，满足用户游泳安全/急救/训练类知识诉求 |
| **优先级** | [MVP] |
| **估时** | 1.5 人天 |

---

## 2. 触发条件

- **触发方**：用户（游客或已登录用户）
- **触发动作**：在 AI 助理页输入游泳知识类问题
- **触发时机**：主动提问

---

## 3. 前置条件

- [x] AI 助理页面可访问（US-058）
- [x] 知识库中已存在相关文档（US-062）
- [x] `ai-service` 正常运行

---

## 4. 业务流程

### 4.1 主路径

1. 用户在 AI 助理页输入问题，如"野泳的注意事项"
2. 前端调用 `/api/ai-assistant/chat`
3. `backend` 鉴权后转发给 `ai-service`
4. `ai-service` LLM 判断意图为知识类问题
5. 调用 `query_knowledge` 工具检索 Chroma
6. 返回相关 chunk（score >= 0.7）
7. LLM 基于检索结果生成自然语言回答
8. 返回答复给用户

### 4.2 异常分支

- **分支 1**：知识库无匹配（score < 0.7）→ 进入 US-065 联网搜索兜底
- **分支 2**：用户问的是教练/课程推荐 → 走现有 recommendation_tools
- **分支 3**：向量库查询失败 → 降级到 web_search

---

## 5. 业务规则引用

| # | 规则 | 来源 |
|---|------|------|
| 1 | 知识类问题优先调用 `query_knowledge` | 设计规格 §6.3 |
| 2 | 仅检索 status=0 的启用文档 | 设计规格 §4.1 |
| 3 | 相似度阈值 0.7，低于则视为无匹配 | 设计规格 §7 |
| 4 | 回答基于检索内容生成，不得编造 | 需求文档 §3 |
| 5 | 推荐类问题仍走原有业务工具 | 需求文档 §3 |

---

## 6. 验收标准（业务级 Gherkin）

### 场景 1：知识库有匹配答案

```gherkin
Given 知识库中存在"野泳安全指南"文档
And   该文档已启用
When  用户输入"野泳的注意事项"
Then  leyo 调用 query_knowledge 工具
And   返回相关度 >= 0.7 的知识片段
And   leyo 基于片段生成回答
And   回答中隐含来源"野泳安全指南"
And   接口返回 HTTP 200
```

### 场景 2：不回答推荐类问题

```gherkin
Given 用户输入"推荐自由泳教练"
When  leyo 判断意图为推荐
Then  调用 query_coaches 工具
And   不调用 query_knowledge
```

### 场景 3：禁用文档不被检索

```gherkin
Given "野泳安全指南"已被禁用
When  用户输入"野泳的注意事项"
Then  query_knowledge 不返回该文档内容
And   可能进入联网搜索兜底
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `ai_chat_session` | 读/写 | 复用现有 AI 助理会话表 |
| 2 | `ai_chat_message` | 新增 | 复用现有 AI 助理消息表 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/ai-assistant/chat` | POST | 复用/扩展 | 用户侧 AI 助理对话接口，新增知识类回答能力 |

### 7.3 状态机影响

无。

---

## 8. 边界场景

### 8.1 边界场景 1：知识库无匹配
query_knowledge 返回空或最高相关度低于 0.7，进入 US-065 联网搜索兜底。

### 8.2 边界场景 2：推荐类问题
用户询问教练/课程/套餐时，不调用 query_knowledge，仍走现有 recommendation_tools。

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）
- US-058 用户-AI助理推荐教练与套餐
- US-062 管理员-上传知识库文档

### 9.2 后续 US（依赖本故事）
- US-065 系统-联网搜索兜底

---

## 10. INVEST 自检

| 维度 | 评估 |
|------|------|
| Independent | 否，依赖 US-058 的 AI 助理会话能力和 US-062 的知识库 |
| Negotiable | 是，MVP 仅支持单轮知识问答 |
| Valuable | 是，扩展 AI 助理可回答范围 |
| Estimable | 是，估时 1.5 天 |
| Small | 是，范围明确 |
| Testable | 是，有明确验收标准 |

---

## 11. 完整性检查

- [x] 业务流程覆盖知识类问题与推荐类问题区分
- [x] 验收标准覆盖知识库匹配、推荐绕过、禁用文档过滤
- [x] API 影响表列出复用的对话接口
- [x] page-spec 链接完整

---

## 12. 备注

- 本 US 不新增后端 API，改动集中在 ai-service 工具注册与 System Prompt
- 推荐类问题必须继续走现有 recommendation_tools

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| 内容 | 链接 | 状态 |
|------|------|------|
| AI 助理页 page-spec | [U-AI-assistant-page.md](../../figma/page-spec/U-AI-assistant-page.md) | ✅ |

---

## 14. 设计评审记录

| 版本 | 日期 | 评审人 | 结论 |
|------|------|--------|------|
| v1.0 | 2026-09-02 | AI | DRAFT，待评审 |
| v1.1 | 2026-09-02 | AI | REVIEW，三件套一致性检查通过 |
| v1.2 | 2026-09-03 | AI | APPROVAL，API 已按 api-convention 规范化，OpenSpec / page-spec / Calicat 已对齐 |
