> **OpenSpec Spec | 映射自 `docs/stories/US-064-用户-从知识库获取游泳知识/user-story.md` §6**

## Capability

用户向 AI 助理提问时从向量知识库检索游泳知识

## ADDED Requirements

### Requirement: REQ-001 知识类问题触发 query_knowledge 并生成回答

系统 MUST 在用户提出游泳安全、急救、技巧、健康、训练方法等知识类问题时，调用 `query_knowledge` 工具从 Chroma `knowledge_base` collection 检索相关知识片段，并基于检索结果生成自然语言回答。

#### Scenario: 知识库有匹配答案
- **GIVEN** 知识库中存在"野泳安全指南"文档
- **AND** 该文档已启用，`status=0`
- **AND** 该文档已完成向量化并写入 `knowledge_base` collection
- **WHEN** 用户在 AI 助理页输入"野泳的注意事项"
- **THEN** 系统调用 `POST /api/ai-assistant/chat` 将请求转发至 `ai-service`
- **AND** `ai-service` 调用 `query_knowledge` 工具
- **AND** 返回 `score >= 0.7` 的相关知识片段
- **AND** `ai-service` 基于片段生成自然语言回答
- **AND** 回答内容隐含来源"野泳安全指南"
- **AND** 接口返回 HTTP 200

#### Scenario: 相似度低于阈值视为无匹配
- **GIVEN** 知识库中不存在与问题"野生水域游泳注意事项"高度相关的内容
- **AND** 检索结果最高 `score < 0.7`
- **WHEN** 用户在 AI 助理页输入"野生水域游泳注意事项"
- **THEN** `query_knowledge` 返回空列表
- **AND** 系统进入 US-065 联网搜索兜底流程

### Requirement: REQ-002 推荐类问题不调用 query_knowledge

系统 MUST 在用户询问教练、课程、套餐、价格等推荐类问题时，继续调用现有 `recommendation_tools`，不调用 `query_knowledge`。

#### Scenario: 用户询问教练推荐
- **GIVEN** 用户在 AI 助理页输入"推荐自由泳教练"
- **WHEN** `ai-service` 完成意图判断
- **THEN** 系统调用 `query_coaches` 等推荐工具
- **AND** 不调用 `query_knowledge`
- **AND** 返回教练推荐卡片

### Requirement: REQ-003 禁用文档排除在检索结果之外

系统 MUST 在检索知识片段时排除 `ai_knowledge_document.status=1` 的禁用文档，仅返回启用文档的内容。

#### Scenario: 禁用文档不被检索
- **GIVEN** 知识库中存在"野泳安全指南"文档
- **AND** 该文档已被禁用，`status=1`
- **WHEN** 用户在 AI 助理页输入"野泳的注意事项"
- **THEN** `query_knowledge` 不返回"野泳安全指南"的任何知识片段
- **AND** 系统进入 US-065 联网搜索兜底流程
