> **OpenSpec Tasks | 映射自 `docs/stories/US-062-管理员-上传知识库文档/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 数据表与实体定义 [P0]

**Files:**
- Create/Update: `backend/src/main/resources/db/migration/Vxxx__create_ai_knowledge_document.sql`
- Create/Update: `backend/src/main/java/com/leyoswimming/entity/AiKnowledgeDocument.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/repository/AiKnowledgeDocumentMapper.java`
- Test: `backend/src/test/java/com/leyoswimming/repository/AiKnowledgeDocumentMapperTest.java`

**Spec coverage:** REQ-001 / REQ-003

- [ ] **RED:** Write failing test — 插入文档后可通过 `title` 唯一索引查询到记录
- [ ] **GREEN:** Create `ai_knowledge_document` table with UK on `title` and map with MyBatis Plus entity/mapper
- [ ] **COMMIT:** `feat(us-062): add ai_knowledge_document table and entity`

## Task 2: 后端手动输入接口 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/controller/admin/AdminKnowledgeController.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/dto/request/AdminKnowledgeAddRequest.java`
- Test: `backend/src/test/java/com/leyoswimming/controller/admin/AdminKnowledgeControllerTest.java`

**Spec coverage:** REQ-001

- [ ] **RED:** Write failing tests — 手动输入成功返回 documentId；参数校验失败返回 400
- [ ] **GREEN:** Implement `POST /api/admin/knowledge/add` with field validation and persistence
- [ ] **COMMIT:** `feat(us-062): add admin knowledge manual add API`

## Task 3: 后端文件上传接口 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/controller/admin/AdminKnowledgeController.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/dto/request/AdminKnowledgeUploadRequest.java`
- Test: `backend/src/test/java/com/leyoswimming/controller/admin/AdminKnowledgeUploadTest.java`

**Spec coverage:** REQ-002

- [ ] **RED:** Write failing tests — `.md` 上传成功；`.pdf` 上传返回 `KNOWLEDGE_FILE_TYPE_NOT_SUPPORTED`
- [ ] **GREEN:** Implement `POST /api/admin/knowledge/upload` with `.txt`/`.md` restriction and content parsing
- [ ] **COMMIT:** `feat(us-062): add admin knowledge file upload API`

## Task 4: ai-service 向量索引内部接口 [P0]

**Files:**
- Create/Update: `ai-service/app/services/knowledge_service.py`
- Create/Update: `ai-service/app/stores/vector_store.py`
- Create/Update: `ai-service/app/models/schemas.py`
- Create/Update: `ai-service/app/main.py` (register router)
- Test: `ai-service/tests/test_knowledge_service.py`

**Spec coverage:** REQ-001 / REQ-002

- [ ] **RED:** Write failing tests — ingest endpoint returns `chunksCount` and stores vectors with correct metadata
- [ ] **GREEN:** Implement `POST /api/internal/ai/knowledge/ingest` with chunking, embedding, and Chroma write
- [ ] **COMMIT:** `feat(us-062): add ai-service knowledge ingest endpoint`

## Task 5: 标题唯一性与 ai-service 失败回滚 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiAssistantGatewayService.java` (add internal ingest call)
- Test: `backend/src/test/java/com/leyoswimming/service/AiKnowledgeDocumentServiceTest.java`

**Spec coverage:** REQ-003

- [ ] **RED:** Write failing tests — 标题重复抛 `BusinessException`；ai-service 500 时 MySQL 记录被删除
- [ ] **GREEN:** Enforce title uniqueness check and wrap MySQL write + ai-service call in rollback-on-failure logic
- [ ] **COMMIT:** `feat(us-062): enforce title uniqueness and ingest rollback`

## Task 6: web-admin 知识库上传弹窗 [P0]

**Files:**
- Create/Update: `web-admin/src/views/knowledge/KnowledgeUploadModal.vue`
- Create/Update: `web-admin/src/api/knowledge.ts`
- Create/Update: `web-admin/src/router/index.ts` (add route if needed)
- Test: `web-admin/tests/unit/views/knowledge/KnowledgeUploadModal.spec.ts`

**Spec coverage:** REQ-001 / REQ-002

- [ ] **RED:** Write failing tests — 手动输入表单提交、文件选择、保存按钮禁用态
- [ ] **GREEN:** Build knowledge upload modal with manual/file tabs, calling backend APIs
- [ ] **COMMIT:** `feat(web-admin): add knowledge upload modal`

## Task 7: 集成与 E2E 验证 [P0]

**Files:**
- Create/Update: `backend/src/test/java/com/leyoswimming/integration/AiKnowledgeIntegrationTest.java`
- Create/Update: `web-admin/tests/e2e/knowledge-upload.spec.ts`

**Spec coverage:** REQ-001 / REQ-002 / REQ-003

- [ ] **RED:** Write failing integration test — 手动输入 → MySQL + Chroma 都有数据；ai-service 失败 → MySQL 无数据
- [ ] **GREEN:** Wire backend and ai-service integration, verify rollback behavior end-to-end
- [ ] **COMMIT:** `test(us-062): add knowledge upload integration tests`

## Task 8: 验证

- [ ] **8.1** Run all unit and integration tests for the 6 GWT scenarios
- [ ] **8.2** Run `./mvnw test` for backend and `pytest` for ai-service
- [ ] **8.3** Run `openspec validate us-062-admin-upload-knowledge-document` and fix issues
