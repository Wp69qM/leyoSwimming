> **OpenSpec Tasks | 映射自 `docs/stories/US-063-管理员-管理知识库文档/test-plan.md`**
> 每个 Task 严格遵循 RED → GREEN → COMMIT 循环。

## Task 1: 后端知识库文档列表查询 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/entity/AiKnowledgeDocument.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/repository/AiKnowledgeDocumentMapper.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/dto/response/AdminKnowledgeListResponse.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/dto/response/AdminKnowledgeListItemResponse.java`
- Test: `backend/src/test/java/com/leyoswimming/repository/AiKnowledgeDocumentMapperTest.java`

**Spec coverage:** REQ-001 / 管理员查看知识库列表

- [ ] **RED:** Write failing test — `AiKnowledgeDocumentMapper.list` 返回分页结果
- [ ] **GREEN:** Implement dynamic SQL list query with category/status/keyword filters and pagination — maps to REQ-001
- [ ] **COMMIT:** `feat(us-063): add ai knowledge document list query mapper`

## Task 2: 后端管理端列表接口 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/controller/admin/AdminKnowledgeController.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/dto/request/AdminKnowledgeListRequest.java`
- Test: `backend/src/test/java/com/leyoswimming/controller/admin/AdminKnowledgeControllerIT.java`

**Spec coverage:** REQ-001 / 非 ADMIN 访问列表被拒绝

- [ ] **RED:** Write failing tests — ADMIN 访问返回分页列表；非 ADMIN 返回 403
- [ ] **GREEN:** Implement `POST /api/admin/knowledge/list` with ADMIN auth — maps to REQ-001
- [ ] **COMMIT:** `feat(us-063): add admin knowledge list endpoint`

## Task 3: 后端禁用文档并删除向量 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/controller/admin/AdminKnowledgeController.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/client/AiServiceClient.java`
- Test: `backend/src/test/java/com/leyoswimming/service/AiKnowledgeDocumentServiceTest.java`

**Spec coverage:** REQ-002 / 管理员禁用启用中的文档

- [ ] **RED:** Write failing test — 禁用 status=0 文档后 status=1 并调用 ai-service delete
- [ ] **GREEN:** Implement `POST /api/admin/knowledge/toggle` for disable and invoke ai-service delete — maps to REQ-002
- [ ] **COMMIT:** `feat(us-063): add disable knowledge document and delete vectors`

## Task 4: 后端启用文档并重建索引 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/controller/admin/AdminKnowledgeController.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/client/AiServiceClient.java`
- Test: `backend/src/test/java/com/leyoswimming/service/AiKnowledgeDocumentServiceTest.java`

**Spec coverage:** REQ-002 / 管理员启用已禁用的文档

- [ ] **RED:** Write failing test — 启用 status=1 文档后 status=0 并调用 ai-service rebuild
- [ ] **GREEN:** Implement `POST /api/admin/knowledge/toggle` for enable and invoke ai-service rebuild — maps to REQ-002
- [ ] **COMMIT:** `feat(us-063): add enable knowledge document and rebuild index`

## Task 5: 后端删除文档并清理向量 [P0]

**Files:**
- Create/Update: `backend/src/main/java/com/leyoswimming/controller/admin/AdminKnowledgeController.java`
- Create/Update: `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java`
- Test: `backend/src/test/java/com/leyoswimming/controller/admin/AdminKnowledgeControllerIT.java`

**Spec coverage:** REQ-003 / 管理员删除文档成功、删除时 ai-service 不可用仍清理元数据

- [ ] **RED:** Write failing tests — 删除后 MySQL 记录不存在且调用 ai-service delete；ai-service 失败时仍删除并记录日志
- [ ] **GREEN:** Implement `POST /api/admin/knowledge/delete` with MySQL deletion and ai-service cleanup — maps to REQ-003
- [ ] **COMMIT:** `feat(us-063): add delete knowledge document and cleanup vectors`

## Task 6: ai-service 向量删除能力 [P0]

**Files:**
- Create/Update: `ai-service/app/stores/vector_store.py`
- Create/Update: `ai-service/app/services/knowledge_service.py`
- Create/Update: `ai-service/app/main.py`
- Test: `ai-service/tests/test_knowledge_service.py`

**Spec coverage:** REQ-002 / 管理员禁用启用中的文档，REQ-003 / 管理员删除文档成功

- [ ] **RED:** Write failing test — `delete_by_document_id(123)` 后 Chroma 无对应 chunk
- [ ] **GREEN:** Implement `VectorStore.delete_by_document_id` and expose `POST /api/internal/ai/knowledge/delete` — maps to REQ-002 / REQ-003
- [ ] **COMMIT:** `feat(ai-service): add delete knowledge vectors endpoint`

## Task 7: ai-service 向量重建能力 [P0]

**Files:**
- Create/Update: `ai-service/app/services/knowledge_service.py`
- Create/Update: `ai-service/app/stores/vector_store.py`
- Create/Update: `ai-service/app/main.py`
- Test: `ai-service/tests/test_knowledge_service.py`

**Spec coverage:** REQ-002 / 管理员启用已禁用的文档

- [ ] **RED:** Write failing test — rebuild 后 Chroma 包含新 chunk
- [ ] **GREEN:** Implement chunk/document rebuild and expose `POST /api/internal/ai/knowledge/rebuild` — maps to REQ-002
- [ ] **COMMIT:** `feat(ai-service): add rebuild knowledge index endpoint`

## Task 8: web-admin 知识库管理列表页 [P0]

**Files:**
- Create/Update: `web-admin/src/views/knowledge-management/KnowledgeManagementView.vue`
- Create/Update: `web-admin/src/views/knowledge-management/KnowledgeDeleteModal.vue`
- Create/Update: `web-admin/src/api/knowledgeManagement.ts`
- Test: `web-admin/src/views/knowledge-management/__tests__/KnowledgeManagementView.spec.ts`

**Spec coverage:** REQ-001 / 管理员查看知识库列表，REQ-002 / 管理员禁用启用中的文档，REQ-003 / 管理员删除文档成功

- [ ] **RED:** Write failing tests — 列表渲染、筛选分页、禁用/删除二次确认
- [ ] **GREEN:** Build knowledge list page with filters, pagination, toggle and delete confirmation — maps to REQ-001 / REQ-002 / REQ-003
- [ ] **COMMIT:** `feat(web-admin): add knowledge management list page`
