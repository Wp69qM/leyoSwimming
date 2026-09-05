> **OpenSpec Tasks | 映射自 `docs/stories/US-065-系统-联网搜索兜底/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: Tavily 搜索客户端 [P0]

**Files:**
- Create: `ai-service/app/clients/tavily_client.py`
- Test: `ai-service/tests/clients/test_tavily_client.py`

**Spec coverage:** REQ-001 / REQ-003

- [ ] **RED:** Write failing tests — Tavily 成功返回 3 条结果；API Key 错误返回异常；超时触发 `TimeoutException`
- [ ] **GREEN:** Implement `TavilyClient.search()` with configurable `base_url`, `api_key`, `max_results`, and 5s timeout — maps to REQ-001
- [ ] **GREEN:** Map Tavily response to `{title, content, url}` list and handle empty `results` — maps to REQ-001
- [ ] **COMMIT:** `feat(ai-service): add Tavily search client`

## Task 2: web_search Function Calling Tool [P0]

**Files:**
- Update: `ai-service/app/tools/knowledge_tools.py`
- Test: `ai-service/tests/tools/test_web_search.py`

**Spec coverage:** REQ-001 / REQ-003

- [ ] **RED:** Write failing tests — `web_search("最新游泳规则")` 返回结果列表；Tavily 失败返回 `{"error": "搜索服务暂时不可用"}`；超时返回 `{"error": "搜索超时"}`
- [ ] **GREEN:** Implement `@tool async def web_search(query, max_results=3)` wrapping `TavilyClient` and swallowing exceptions — maps to REQ-001
- [ ] **GREEN:** Distinguish timeout vs. other failures in returned error payload — maps to REQ-003 Scenario "Tavily 调用超时降级"
- [ ] **COMMIT:** `feat(ai-service): add web_search function calling tool`

## Task 3: ChatService 触发联网搜索逻辑 [P0]

**Files:**
- Update: `ai-service/app/services/chat_service.py`
- Test: `ai-service/tests/services/test_chat_service_web_search_trigger.py`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — `query_knowledge` 空结果时调用 `web_search`；最高 score < 0.7 时调用 `web_search`；score >= 0.7 时不调用
- [ ] **GREEN:** Add threshold check after `query_knowledge` and call `web_search` on empty or low-score results — maps to REQ-001 Scenario "知识库最高相似度低于阈值触发 web_search"
- [ ] **GREEN:** Use `LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD` from env, default 0.7 — maps to REQ-001
- [ ] **COMMIT:** `feat(ai-service): trigger web_search when knowledge base misses`

## Task 4: 基于搜索结果生成带来源标注的回答 [P0]

**Files:**
- Update: `ai-service/app/services/chat_service.py`
- Update: `ai-service/app/prompts/knowledge_prompts.py`
- Test: `ai-service/tests/services/test_chat_service_web_search_answer.py`

**Spec coverage:** REQ-002

- [ ] **RED:** Write failing tests — Tavily 有结果时 LLM 收到搜索结果；最终回答末尾包含"以下内容来自网络，仅供参考"
- [ ] **GREEN:** Build prompt that feeds Tavily `title/content/url` results to LLM and requests concise answer — maps to REQ-002 Scenario "Tavily 返回有效结果并生成标注回答"
- [ ] **GREEN:** Append source note exactly once at the end of the final answer — maps to REQ-002 Scenario "回答中仅标注一次来源"
- [ ] **COMMIT:** `feat(ai-service): generate web search answer with source label`

## Task 5: Tavily 失败/超时/无结果优雅降级 [P0]

**Files:**
- Update: `ai-service/app/services/chat_service.py`
- Update: `ai-service/app/tools/knowledge_tools.py`
- Test: `ai-service/tests/services/test_chat_service_web_search_degradation.py`

**Spec coverage:** REQ-003

- [ ] **RED:** Write failing tests — Tavily HTTP 失败返回"暂时无法获取该知识，请换个方式提问"；超时返回"搜索超时，请稍后再试"；无结果返回"这个问题我暂时无法回答"
- [ ] **GREEN:** Handle `web_search` error payload and map to corresponding friendly message — maps to REQ-003 Scenario "Tavily API 调用失败降级"
- [ ] **GREEN:** Handle `web_search` empty result list and return "这个问题我暂时无法回答" — maps to REQ-003 Scenario "Tavily 返回空结果降级"
- [ ] **GREEN:** Ensure no exception propagates and next user message is processed normally — maps to REQ-003 Scenario "降级后继续后续对话"
- [ ] **COMMIT:** `feat(ai-service): add graceful degradation for Tavily fallback`

## Task 6: 配置项与启动校验 [P1]

**Files:**
- Update: `ai-service/app/config.py`
- Update: `ai-service/.env.example`
- Test: `ai-service/tests/test_config.py`

**Spec coverage:** REQ-001 / REQ-003

- [ ] **RED:** Write failing tests — config reads `LEYO_TAVILY_API_KEY`, `LEYO_TAVILY_MAX_RESULTS`, `LEYO_TAVILY_TIMEOUT_SECONDS`, `LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD`
- [ ] **GREEN:** Add config fields with defaults `max_results=3`, `timeout=5`, `threshold=0.7` — maps to REQ-001
- [ ] **GREEN:** Log warning on startup when `LEYO_TAVILY_API_KEY` is missing, skip web_search fallback — maps to REQ-003
- [ ] **COMMIT:** `chore(ai-service): add Tavily and knowledge threshold config`

## Task 7: 集成测试：知识库无匹配 → Tavily 兜底 [P1]

**Files:**
- Create: `ai-service/tests/integration/test_knowledge_to_web_search.py`

**Spec coverage:** REQ-001 / REQ-002 / REQ-003

- [ ] **RED:** Write failing integration test — upload unrelated doc → ask out-of-scope question → assert `web_search` called and answer contains source note
- [ ] **GREEN:** Wire `query_knowledge`, `web_search`, and LLM answer generation through `ChatService` end-to-end — maps to REQ-001 / REQ-002
- [ ] **GREEN:** Add integration test for Tavily timeout returning friendly message — maps to REQ-003
- [ ] **COMMIT:** `test(ai-service): add knowledge-to-web-search integration tests`

## Task 8: 验证

- [ ] **8.1** Run unit and integration tests for all 7 GWT scenarios
- [ ] **8.2** Run `pytest ai-service/tests` and ensure coverage for new files ≥ 80%
- [ ] **8.3** Manually verify with Tavily API key that out-of-scope question returns answer labeled "以下内容来自网络，仅供参考"
