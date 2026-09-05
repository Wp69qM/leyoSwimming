> **OpenSpec Tasks | 映射自 `docs/stories/US-064-用户-从知识库获取游泳知识/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 实现 ai-service 向量检索能力 [P0]

**Files:**
- Create/Update: `ai-service/app/stores/vector_store.py`
- Test: `ai-service/tests/stores/test_vector_store.py`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — `similarity_search` 返回带 score 的知识片段；`score < 0.7` 的 chunk 被过滤；禁用的 `document_id` 对应的 chunk 被过滤
- [ ] **GREEN:** Implement `VectorStore.similarity_search(query, top_k=3)` using Chroma `knowledge_base` collection
- [ ] **GREEN:** Apply similarity threshold `0.7` and status filter based on `ai_knowledge_document.status=0`
- [ ] **COMMIT:** `feat(ai-service): add vector store knowledge retrieval`

## Task 2: 实现 query_knowledge Function Calling 工具 [P0]

**Files:**
- Create: `ai-service/app/tools/knowledge_tools.py`
- Test: `ai-service/tests/tools/test_knowledge_tools.py`

**Spec coverage:** REQ-001 / REQ-003

- [ ] **RED:** Write failing tests — `query_knowledge("野泳注意事项")` 返回启用文档片段；`query_knowledge("不存在的问题")` 返回空列表；禁用文档内容不返回
- [ ] **GREEN:** Implement `query_knowledge(query: str, top_k: int = 3)` decorated with `@tool`
- [ ] **GREEN:** Map retrieval results to `{content, source, category, score}` format
- [ ] **COMMIT:** `feat(ai-service): add query_knowledge tool`

## Task 3: 扩展 ChatService 意图判断与工具注册 [P0]

**Files:**
- Create/Update: `ai-service/app/services/chat_service.py`
- Test: `ai-service/tests/services/test_chat_service.py`

**Spec coverage:** REQ-001 / REQ-002

- [ ] **RED:** Write failing tests — 知识类问题触发 `query_knowledge`；推荐类问题调用 `recommendation_tools` 且不调用 `query_knowledge`
- [ ] **GREEN:** Register `query_knowledge` in the tool registry alongside existing recommendation tools
- [ ] **GREEN:** Update system prompt rules to route safety/technique/emergency questions to `query_knowledge`
- [ ] **COMMIT:** `feat(ai-service): wire query_knowledge into chat service`

## Task 4: 验证对话接口复用与返回结构 [P0]

**Files:**
- Update: `backend/src/routes/ai-assistant.ts`（如需要）
- Test: `backend/tests/routes/ai-assistant/chat.test.ts`
- Test: `ai-service/tests/integration/test_chat_knowledge.py`

**Spec coverage:** REQ-001 / REQ-002

- [ ] **RED:** Write failing integration test — `POST /api/ai-assistant/chat` 接收知识类问题后返回 HTTP 200，且 `reply.text` 包含知识库内容
- [ ] **GREEN:** Ensure `POST /api/ai-assistant/chat` forwards to `ai-service` without path changes
- [ ] **GREEN:** Verify `ChatResponse` structure remains unchanged
- [ ] **COMMIT:** `test(us-064): verify chat endpoint reuses existing API`

## Task 5: 配置环境变量与默认阈值 [P1]

**Files:**
- Update: `ai-service/.env.example`
- Update: `ai-service/app/config.py`
- Test: `ai-service/tests/test_config.py`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — `LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD` 默认值为 `0.7`；`LEYO_KNOWLEDGE_COLLECTION_NAME` 默认值为 `knowledge_base`
- [ ] **GREEN:** Add environment variables: `LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD`, `LEYO_KNOWLEDGE_COLLECTION_NAME`, `LEYO_KNOWLEDGE_TOP_K`
- [ ] **COMMIT:** `chore(ai-service): add knowledge retrieval env config`

## Task 6: 验证推荐类问题不触发 query_knowledge [P0]

**Files:**
- Test: `ai-service/tests/services/test_chat_service.py`
- Test: `ai-service/tests/e2e/test_knowledge_vs_recommendation.py`

**Spec coverage:** REQ-002

- [ ] **RED:** Write failing tests — 输入"推荐自由泳教练"仅调用 `query_coaches`，不调用 `query_knowledge`；输入"推荐课程"继续走推荐流程
- [ ] **GREEN:** Ensure recommendation intent keywords bypass `query_knowledge`
- [ ] **COMMIT:** `test(us-064): ensure recommendation questions skip knowledge retrieval`

## Task 7: 验证禁用文档过滤 [P0]

**Files:**
- Test: `ai-service/tests/tools/test_knowledge_tools.py`
- Test: `ai-service/tests/integration/test_disabled_document.py`

**Spec coverage:** REQ-003

- [ ] **RED:** Write failing tests — 禁用文档被排除在检索结果外；`query_knowledge` 返回空列表并触发 fallback
- [ ] **GREEN:** Implement status filtering by joining Chroma metadata with `ai_knowledge_document.status`
- [ ] **COMMIT:** `fix(us-064): exclude disabled documents from knowledge retrieval`

## Task 8: 验证

- [ ] **8.1** Run unit and integration tests for all 4 GWT scenarios
- [ ] **8.2** Run `pytest ai-service/tests` and confirm coverage >= 80%
