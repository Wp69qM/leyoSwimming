## Context

本 US 实现管理员向 AI 知识库上传文档的能力。管理员可通过 web-admin 手动输入文本/Markdown，或上传 `.txt`/`.md` 文件；`backend` 校验权限与标题唯一性后将元数据写入 `ai_knowledge_document`，再调用 `ai-service` 构建向量索引。若索引失败，必须回滚 MySQL 记录。

## Goals / Non-Goals

**Goals:**
- `ADMIN` 角色可通过手动输入新增知识库文档
- `ADMIN` 角色可通过文件上传新增知识库文档（仅 `.txt`/`.md`）
- 文档标题全局唯一，重复时返回明确错误
- 上传成功后 `ai-service` 完成分块、Embedding、写入 Chroma
- `ai-service` 索引失败时回滚 MySQL 写入

**Non-Goals:**
- 不支持 PDF/Word 等富文档格式（后续扩展）
- 不实现文档编辑、禁用、删除（由 US-063 负责）
- 不实现用户侧知识检索（由 US-064 负责）

## Data Model

### 新增表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `ai_knowledge_document` | 新增 | `id`, `title` (UK), `category` (`safety`/`technique`/`emergency`), `content_type` (`text`/`markdown`), `source_type` (`manual`/`file`), `status` (0=启用/1=禁用), `created_by`, `created_at`, `updated_at` |

### 索引

- `ai_knowledge_document.title` 唯一索引
- `ai_knowledge_document(category, status)` 复合索引

### 向量库 Collection

- Collection name: `knowledge_base`
- 每个 chunk 作为一条 Document，metadata 包含 `document_id`、`title`、`category`、`chunk_index`

## API Design

> 统一使用 POST，URL 按动作命名，参数通过 JSON body 传递；文件上传使用 multipart/form-data。

### POST /api/admin/knowledge/add

- **鉴权**：管理员 JWT（仅 `ADMIN`）
- **请求体**：
  ```json
  {
    "title": "野泳安全指南",
    "category": "safety",
    "content": "# 野泳安全\n...",
    "contentType": "markdown"
  }
  ```
- **业务规则**：
  - `title` 必填，1-200 字符，全局唯一
  - `category` 必填，仅允许 `safety`/`technique`/`emergency`
  - `contentType` 必填，仅允许 `text`/`markdown`
  - `content` 必填
  - `source_type` 记为 `manual`
  - 写入 MySQL 后调用 `ai-service` ingest；失败回滚
- **响应 200**：`{ "code": 0, "message": "success", "data": { "documentId": 123 } }`
- **错误码**：`ADMIN_PERMISSION_DENIED`（403）、`KNOWLEDGE_TITLE_ALREADY_EXISTS`（400）、`VALIDATION_ERROR`（400）、`KNOWLEDGE_INGEST_FAILED`（500）

### POST /api/admin/knowledge/upload

- **鉴权**：管理员 JWT（仅 `ADMIN`）
- **Content-Type**: `multipart/form-data`
- **Fields**: `file`（仅 `.txt`/`.md`）、`title`、`category`
- **业务规则**：
  - 文件扩展名必须是 `.txt` 或 `.md`
  - 文件内容按 UTF-8 解码；Markdown 移除标记后保留纯文本
  - `content_type` 根据扩展名映射为 `text` 或 `markdown`
  - `source_type` 记为 `file`
  - 其余校验与回滚逻辑同 `add`
- **响应 200**：同 `add`
- **错误码**：`KNOWLEDGE_FILE_TYPE_NOT_SUPPORTED`（400），其余同 `add`

### POST /api/internal/ai/knowledge/ingest

- **鉴权**：`X-Internal-Token`
- **请求体**：
  ```json
  {
    "documentId": 123,
    "title": "野泳安全指南",
    "category": "safety",
    "content": "...",
    "contentType": "markdown"
  }
  ```
- **业务规则**：
  - 解析文本，按 500 字符/100 重叠分块
  - 调用 Embedding 模型生成向量
  - 写入 `knowledge_base` collection，metadata 包含 `document_id`、`title`、`category`、`chunk_index`
- **响应 200**：`{ "documentId": 123, "chunksCount": 5, "status": "success" }`
- **错误码**：内部错误返回 500，由 backend 捕获后回滚

## State Machine

无。

## Error Handling

| 场景 | 行为 |
|------|------|
| 标题重复 | 返回 `KNOWLEDGE_TITLE_ALREADY_EXISTS`，不入库 |
| 文件格式不支持 | 返回 `KNOWLEDGE_FILE_TYPE_NOT_SUPPORTED`，不入库 |
| 字段校验失败 | 返回 `VALIDATION_ERROR` |
| `ai-service` 索引失败 | backend 删除已写入的 MySQL 记录，返回 `KNOWLEDGE_INGEST_FAILED` |
| 无权限 | 返回 `ADMIN_PERMISSION_DENIED` |

## Performance

- `POST /api/admin/knowledge/add` P99 < 500ms（不含 ai-service 索引耗时）
- `POST /api/admin/knowledge/upload` P99 < 800ms（不含 ai-service 索引耗时）
- 文件大小限制 5MB

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-053 | 被本 US 依赖 | 管理员登录态与 `ADMIN` 角色校验 |
| US-063 | 反向依赖 | 依赖本 US 产生的 `ai_knowledge_document` 记录 |
| US-064 | 反向依赖 | 依赖本 US 构建的向量索引 |
