## Why

在 AI 助理问答流程中，当游泳知识库无法命中有效内容时，用户问题将无法得到回答。通过接入 Tavily 联网搜索作为兜底能力，`ai-service` 可以在知识库无匹配时自动搜索互联网补充最新信息，提升回答覆盖率与用户满意度，同时明确标注答案来源，避免误导用户。

## What Changes

- `ai-service` 新增 `TavilyClient`，封装 Tavily Search API 调用，默认超时 5 秒
- `ai-service` 新增 `web_search` function calling 工具，供 LLM/业务逻辑在知识库无匹配时调用
- `ChatService` 在 `query_knowledge` 返回空列表或最高相似度低于 `0.7` 时，自动触发 `web_search`
- 基于 Tavily 返回的摘要结果，由 LLM 生成最终回答，并在回答末尾追加标注"以下内容来自网络，仅供参考"
- Tavily 调用失败、超时或无结果时，返回预设的友好降级提示，不阻塞后续对话
- 新增环境变量配置：`LEYO_TAVILY_API_KEY`、`LEYO_TAVILY_MAX_RESULTS`、`LEYO_TAVILY_TIMEOUT_SECONDS`

## Capabilities

### New Capabilities

- `web-search-fallback`: 当知识库无匹配时，AI 助理通过 Tavily 联网搜索兜底

### Modified Capabilities

- 无

## Impact

- **数据表**：无新增或修改
- **API**：无新增后端 API；本 US 为 `ai-service` 内部 function calling 工具能力
- **状态机**：无
- **前端**：无新增页面；复用 AI 助理聊天界面展示来自网络的答案与降级提示
- **依赖**：US-064（用户-从知识库获取游泳知识）提供 `query_knowledge` 触发条件
- **影响**：扩展 AI 助理对知识库外问题的回答能力，明确标注网络来源并保证失败时不阻断对话
