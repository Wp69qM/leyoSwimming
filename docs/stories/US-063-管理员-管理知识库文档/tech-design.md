# Tech Design: US-063 管理员-管理知识库文档

## 1. 总体架构

复用 US-062 的 `AiKnowledgeDocumentService` 和 `AdminKnowledgeController`，增加列表查询、状态切换、删除能力。

## 2. 新增/修改文件

| 文件 | 说明 |
|------|------|
| `AdminKnowledgeController.java` | 新增 list/toggle/delete 接口 |
| `AiKnowledgeDocumentService.java` | 新增列表、启用/禁用、删除逻辑 |
| `ai-service/app/services/knowledge_service.py` | 新增 delete/rebuild 方法 |
| `ai-service/app/stores/vector_store.py` | 新增按 documentId 删除向量 chunk 方法 |

## 3. API 设计

见 [设计规格 §5.1](../../../superpowers/spec/2026-09-02-ai-assistant-rag-design.md)

## 4. 关键实现

### 4.1 禁用文档

1. backend 更新 MySQL status=1
2. backend 调用 ai-service `/api/internal/ai/knowledge/delete`
3. ai-service 从 Chroma 删除所有 `document_id` 对应的 chunk

### 4.2 启用文档

1. backend 更新 MySQL status=0
2. backend 调用 ai-service `/api/internal/ai/knowledge/rebuild`
3. ai-service 重新解析该文档内容并写入 Chroma

### 4.3 删除文档

1. backend 删除 MySQL 记录
2. backend 调用 ai-service delete
3. 若 ai-service 失败，记录日志，后台告警

## 5. 前端页面

见 Figma 规格 [A-knowledge-management-page.md](../../../figma/page-spec/A-knowledge-management-page.md)
