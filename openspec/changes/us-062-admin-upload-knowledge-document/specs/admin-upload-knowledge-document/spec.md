> **OpenSpec Spec | 映射自 `docs/stories/US-062-管理员-上传知识库文档/user-story.md` §6**

## Capability

管理员上传游泳知识文档到向量知识库

## ADDED Requirements

### Requirement: REQ-001 手动输入上传知识库文档

系统 MUST 允许 `ADMIN` 角色通过手动输入新增知识库文档。系统 MUST 校验标题、分类、内容、内容类型的合法性，校验标题全局唯一，并在 `ai-service` 索引成功后完成入库。

#### Scenario: 手动输入 Markdown 文档成功上传
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 系统中不存在标题为 "野泳安全指南" 的文档
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/add` 传入 `{ "title": "野泳安全指南", "category": "safety", "content": "# 野泳安全\n...", "contentType": "markdown" }`
- **THEN** 系统返回 `code=0` 及 `documentId`
- **AND** `ai_knowledge_document` 表新增 1 条记录，`title="野泳安全指南"`、`category="safety"`、`content_type="markdown"`、`source_type="manual"`
- **AND** `ai-service` 的 `knowledge_base` collection 中存在该文档的分块向量

#### Scenario: 手动输入参数校验失败
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/add` 传入 `{ "title": "", "category": "safety", "content": "内容", "contentType": "markdown" }`
- **THEN** 系统返回 HTTP 400，错误码 `VALIDATION_ERROR`
- **AND** `ai_knowledge_document` 表无新增记录
- **AND** `ai-service` 未收到索引请求

### Requirement: REQ-002 文件上传知识库文档

系统 MUST 允许 `ADMIN` 角色通过上传 `.txt` 或 `.md` 文件新增知识库文档。系统 MUST 拒绝其他格式，并将文件内容解析后送入向量索引流程。

#### Scenario: 上传 .md 文件成功
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 本地存在文件 `swimming_safety.md`，内容为 Markdown 格式
- **AND** 系统中不存在标题为 "游泳安全须知" 的文档
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/upload` 传入 `title="游泳安全须知"`、`category="safety"`、`file=swimming_safety.md`
- **THEN** 系统解析文件内容并返回 `code=0` 及 `documentId`
- **AND** `ai_knowledge_document` 表新增 1 条记录，`content_type="markdown"`、`source_type="file"`
- **AND** `ai-service` 的 `knowledge_base` collection 中存在该文档的分块向量

#### Scenario: 上传不支持的文件格式被拒绝
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 本地存在文件 `swimming_safety.pdf`
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/upload` 传入 `title="游泳安全须知"`、`category="safety"`、`file=swimming_safety.pdf`
- **THEN** 系统返回 HTTP 400，错误码 `KNOWLEDGE_FILE_TYPE_NOT_SUPPORTED`
- **AND** `ai_knowledge_document` 表无新增记录
- **AND** `ai-service` 未收到索引请求

### Requirement: REQ-003 标题唯一性与 ai-service 索引失败回滚

系统 MUST 保证知识库文档标题全局唯一，并在 `ai-service` 索引失败时回滚已写入的 MySQL 记录，确保向量库与元数据库保持一致。

#### Scenario: 标题重复被拒绝
- **GIVEN** 系统中已存在标题为 "野泳安全指南" 的文档
- **AND** 管理员 M 已登录且角色为 ADMIN
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/add` 传入 `{ "title": "野泳安全指南", "category": "safety", "content": "新内容", "contentType": "text" }`
- **THEN** 系统返回 HTTP 400，错误码 `KNOWLEDGE_TITLE_ALREADY_EXISTS`
- **AND** `ai_knowledge_document` 表不会新增重复记录
- **AND** `ai-service` 未收到索引请求

#### Scenario: ai-service 索引失败触发回滚
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** `ai-service` 当前不可用或 `/api/internal/ai/knowledge/ingest` 返回 500
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/add` 传入有效文档内容
- **THEN** `backend` 不向 `ai_knowledge_document` 保留记录（写入后回滚）
- **AND** 系统返回 HTTP 500，错误码 `KNOWLEDGE_INGEST_FAILED`
- **AND** `ai-service` 的 `knowledge_base` collection 中不存在该文档的任何分块向量
