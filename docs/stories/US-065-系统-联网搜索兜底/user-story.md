# US-065 系统-联网搜索兜底

> **状态**：[APPROVAL]
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：AI | **最后更新**：2026-09-03
> **配套文档**：需求：[rag-function-calling-requirements.md](../../tech/AI-assistant-requirements/rag-function-calling-requirements.md) · 设计：[2026-09-02-ai-assistant-rag-design.md](../../superpowers/spec/2026-09-02-ai-assistant-rag-design.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-065 |
| **标题** | 系统-联网搜索兜底 |
| **角色（Actor）** | 系统 |
| **业务价值（Why）** | 当知识库无法回答时，通过联网搜索补充最新信息，提升用户满意度 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |

---

## 2. 触发条件

- **触发方**：系统（由 `ai-service` 自动触发）
- **触发动作**：`query_knowledge` 返回空或最高相似度低于 0.7
- **触发时机**：用户问答流程中

---

## 3. 前置条件

- [x] 用户已发起知识类问题（US-064）
- [x] Tavily API Key 已配置
- [x] `ai-service` 可访问 Tavily API

---

## 4. 业务流程

### 4.1 主路径

1. `query_knowledge` 返回结果为空或最高 score < 0.7
2. `ai-service` 自动调用 `web_search` 工具
3. `web_search` 调用 Tavily API 搜索相关问题
4. Tavily 返回清洗后的搜索结果摘要
5. LLM 基于搜索结果生成回答
6. 回答中标注"以下内容来自网络，仅供参考"
7. 返回给用户

### 4.2 异常分支

- **分支 1**：Tavily 调用失败 → 返回友好提示"暂时无法获取该知识，请换个方式提问"
- **分支 2**：Tavily 超时 → 返回超时提示
- **分支 3**：联网搜索无结果 → 返回"这个问题我暂时无法回答"

---

## 5. 业务规则引用

| # | 规则 | 来源 |
|---|------|------|
| 1 | 仅当知识库无有效匹配时才触发联网搜索 | 设计规格 §6.3 |
| 2 | 默认使用 Tavily API，最大返回 3 条结果 | 设计规格 §7 |
| 3 | 来自网络的回答需明确标注 | 需求文档 §3 |
| 4 | Tavily 失败时优雅降级，不阻塞对话 | 需求文档 §3 |

---

## 6. 验收标准（业务级 Gherkin）

### 场景 1：知识库无匹配触发联网搜索

```gherkin
Given 用户输入"2026 年最新游泳世锦赛规则"
And   知识库中没有相关内容
When  query_knowledge 返回最高相似度 < 0.7
Then  leyo 自动调用 web_search
And   基于 Tavily 返回结果生成回答
And   回答中标注"以下内容来自网络，仅供参考"
```

### 场景 2：Tavily 失败降级

```gherkin
Given 用户输入知识类问题
And   Tavily API 当前不可用
When  leyo 尝试调用 web_search
Then  不抛出异常
And   返回"暂时无法获取该知识，请换个方式提问"
```

### 场景 3：联网搜索超时

```gherkin
Given 用户输入知识类问题
And   Tavily API 响应超过 5 秒
When  leyo 调用 web_search
Then  触发超时降级
And   返回"搜索超时，请稍后再试"
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

无。

### 7.2 API 影响

无新增后端 API。本 US 在 `ai-service` 内部通过 `web_search` function calling 工具调用 Tavily API。

### 7.3 状态机影响

无。

---

## 8. 边界场景

### 8.1 边界场景 1：Tavily API 不可用
返回友好提示，不阻塞对话。

### 8.2 边界场景 2：Tavily 返回空结果
返回"暂时无法回答该问题"。

### 8.3 边界场景 3：Tavily 超时
返回"搜索超时，请稍后再试"。

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）
- US-058 用户-AI助理推荐教练与套餐
- US-062 管理员-上传知识库文档
- US-064 用户-从知识库获取游泳知识

### 9.2 后续 US（依赖本故事）
无。

---

## 10. INVEST 自检

| 维度 | 评估 |
|------|------|
| Independent | 否，依赖 US-064 的 query_knowledge 触发条件 |
| Negotiable | 是，可配置开关/阈值 |
| Valuable | 是，扩展 AI 助理回答能力 |
| Estimable | 是，估时 1 天 |
| Small | 是，范围明确 |
| Testable | 是，有明确验收标准 |

---

## 11. 完整性检查

- [x] 业务流程覆盖 Tavily 成功、失败、超时场景
- [x] 验收标准覆盖联网搜索兜底与降级
- [x] API 影响表明确无新增后端 API
- [x] page-spec 链接完整

---

## 12. 备注

- 本 US 不新增后端 API，改动集中在 ai-service `web_search` 工具
- Tavily API Key 通过环境变量配置，需关注成本与稳定性

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
