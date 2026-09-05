> **OpenSpec Spec | 映射自 `docs/stories/US-065-系统-联网搜索兜底/user-story.md` §6**

## Capability

当知识库无匹配时，AI 助理通过 Tavily 联网搜索兜底。

## ADDED Requirements

### Requirement: REQ-001 知识库无匹配时触发联网搜索

系统 MUST 在 `query_knowledge` 返回空列表，或返回结果中最高相似度低于配置阈值时，自动调用 `web_search` 工具补充信息。阈值 MUST 可配置，默认值为 0.7。

#### Scenario: 知识库无匹配触发 web_search

- **GIVEN** 用户输入"2026 年最新游泳世锦赛规则"
- **AND** 知识库中不存在相关内容
- **WHEN** `query_knowledge` 返回空列表
- **THEN** `ChatService` 自动调用 `web_search` 工具
- **AND** 传入的 `query` 为用户原始问题
- **AND** `max_results` 使用配置默认值 3

#### Scenario: 知识库最高相似度低于阈值触发 web_search

- **GIVEN** 用户输入"自由泳换气技巧"
- **AND** 知识库中存在 1 条相关内容，但相似度为 0.55
- **AND** 配置阈值 `LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD=0.7`
- **WHEN** `query_knowledge` 返回最高 score 为 0.55
- **THEN** `ChatService` 自动调用 `web_search` 工具
- **AND** 不直接使用知识库低相关结果生成回答

#### Scenario: 知识库高相关时不触发 web_search

- **GIVEN** 用户输入"抽筋时如何自救"
- **AND** 知识库中存在相似度为 0.85 的相关内容
- **WHEN** `query_knowledge` 返回最高 score 为 0.85
- **THEN** 系统直接使用知识库结果生成回答
- **AND** 不调用 `web_search` 工具

### Requirement: REQ-002 基于 Tavily 结果生成回答并标注来源

系统 MUST 在 `web_search` 返回有效结果后，基于 Tavily 返回的摘要内容生成自然语言回答，并 MUST 在回答中明确标注"以下内容来自网络，仅供参考"。

#### Scenario: Tavily 返回有效结果并生成标注回答

- **GIVEN** 用户输入"2026 年最新游泳世锦赛规则"
- **AND** `query_knowledge` 返回空列表
- **AND** `web_search` 调用 Tavily 返回 2 条有效结果
- **WHEN** LLM 基于 Tavily 结果生成回答
- **THEN** 回答内容包含搜索结果中的关键信息
- **AND** 回答末尾标注"以下内容来自网络，仅供参考"

#### Scenario: 回答中仅标注一次来源

- **GIVEN** 用户输入任意知识库外问题
- **AND** `web_search` 返回有效结果
- **WHEN** 系统生成最终回答
- **THEN** 回答中"以下内容来自网络，仅供参考"仅出现一次
- **AND** 标注位于回答末尾

### Requirement: REQ-003 Tavily 失败/超时/无结果时优雅降级

系统 MUST 在 Tavily 调用失败、超时或无结果时返回友好提示，并 MUST 保证不抛出未处理异常、不阻塞后续对话。

#### Scenario: Tavily API 调用失败降级

- **GIVEN** 用户输入知识类问题
- **AND** `query_knowledge` 返回空列表
- **AND** Tavily API 当前不可用（返回 HTTP 500）
- **WHEN** `web_search` 调用 Tavily
- **THEN** 系统不抛出异常
- **AND** 返回"暂时无法获取该知识，请换个方式提问"

#### Scenario: Tavily 调用超时降级

- **GIVEN** 用户输入知识类问题
- **AND** `query_knowledge` 返回空列表
- **AND** Tavily API 响应超过 5 秒
- **WHEN** `web_search` 调用 Tavily
- **THEN** 系统触发超时降级
- **AND** 返回"搜索超时，请稍后再试"

#### Scenario: Tavily 返回空结果降级

- **GIVEN** 用户输入知识类问题
- **AND** `query_knowledge` 返回空列表
- **AND** Tavily 返回 0 条结果
- **WHEN** `web_search` 处理 Tavily 响应
- **THEN** 系统返回"这个问题我暂时无法回答"

#### Scenario: 降级后继续后续对话

- **GIVEN** 用户已收到 Tavily 失败降级提示"暂时无法获取该知识，请换个方式提问"
- **WHEN** 用户继续输入下一个问题
- **THEN** 系统正常处理下一轮对话
- **AND** 不保留上一轮的异常状态
