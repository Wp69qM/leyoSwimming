## Context

本 US 实现管理后台的知识库文档管理功能。管理员可分页查看文档列表、按分类/状态/关键词筛选，并可启用/禁用或删除文档。禁用/删除会同步清理 `ai-service` 向量库，启用会重新触发索引重建。

## Goals / Non-Goals

**Goals:**
- 管理员可分页查询知识库文档列表
- 列表支持按分类、状态、关键词筛选
- 管理员可启用/禁用文档并同步向量库
- 管理员可删除文档并清理 MySQL 与向量库
- 非 ADMIN 访问返回 403

**Non-Goals:**
- 不上传/新增知识库文档（由 US-062 负责）
- 不实现用户侧检索（由 US-064 负责）
- 不实现联网搜索兜底（由 US-065 负责）

## Data Model

### 复用/写入的表

| 表 | 操作 | 关键字段 |
|---|---|---|
| `ai_knowledge_document` | 查询/更新/删除 | `id`, `title`, `category`, `content_type`, `source_type`, `status` (0=启用/1=禁用), `created_by`, `created_at`, `updated_at` |

### 索引

- `ai_knowledge_document.title` 唯一索引
- `ai_knowledge_document(category, status)` 复合索引

## API Design

> 统一使用 POST，URL 按动作命名，参数通过 JSON body 传递。

### POST /api/admin/knowledge/list

- **鉴权**：管理员 JWT（仅 ADMIN）
- **请求体**：`{ "page": 1, "pageSize": 20, "category": "", "keyword": "", "status": null }`
- **响应 200**：
  ```json
  {
    "code": 0,
    "data": {
      "total": 1,
      "list": [
        {
          "documentId": 123,
          "title": "野泳安全指南",
          "category": "safety",
          "status": 0,
          "createdBy": 1,
          "createdAt": "2026-09-02T10:00:00"
        }
      ]
    }
  }
  ```
- **业务规则**：
  - 分页默认 20 条
  - `category` 为空/空字符串时不筛选
  - `status` 为 `null` 时不筛选
  - `keyword` 模糊匹配 `title`

### POST /api/admin/knowledge/toggle

- **鉴权**：管理员 JWT（仅 ADMIN）
- **请求体**：`{ "documentId": 123, "status": 1 }`
- **响应 200**：通用成功响应
- **业务规则**：
  - `status` 只能为 `0` 或 `1`
  - `status=1`（禁用）时调用 `ai-service` `/api/internal/ai/knowledge/delete` 删除向量
  - `status=0`（启用）时调用 `ai-service` `/api/internal/ai/knowledge/rebuild` 重建向量
  - 若 `ai-service` 调用失败，MySQL 状态已更新，记录错误日志并触发告警

### POST /api/admin/knowledge/delete

- **鉴权**：管理员 JWT（仅 ADMIN）
- **请求体**：`{ "documentId": 123 }`
- **响应 200**：通用成功响应
- **业务规则**：
  - 删除 MySQL 记录
  - 调用 `ai-service` `/api/internal/ai/knowledge/delete` 删除向量
  - 若 `ai-service` 调用失败，MySQL 记录仍删除，记录错误日志并触发告警

### POST /api/internal/ai/knowledge/delete

- **鉴权**：`X-Internal-Token`
- **请求体**：`{ "documentId": 123 }`
- **响应 200**：
  ```json
  { "documentId": 123, "deleted": true }
  ```
- **业务规则**：从 Chroma `knowledge_base` collection 删除所有 `document_id` 对应的 chunk

### POST /api/internal/ai/knowledge/rebuild

- **鉴权**：`X-Internal-Token`
- **请求体**：
  ```json
  {
    "documentId": 123,
    "title": "野泳安全指南",
    "category": "safety",
    "content": "# 野泳安全...",
    "contentType": "markdown"
  }
  ```
- **响应 200**：
  ```json
  { "documentId": 123, "chunksCount": 5, "status": "success" }
  ```
- **业务规则**：先删除旧 chunk，再重新分块、Embedding 并写入 Chroma

## State Machine

| 状态维度 | 状态值 | 转换说明 |
|---|---|---|
| 文档启用状态 | `status=0`（启用） | 可被检索；启用操作后重建向量 |
| 文档启用状态 | `status=1`（禁用） | 不可被检索；禁用后删除向量 |
| 文档存在性 | 存在 | 已上传/新增 |
| 文档存在性 | 已删除 | 删除后 MySQL 记录与向量均清理 |

## Cache Strategy

- 知识库列表不缓存（管理操作实时性要求高）

## Performance

- `POST /api/admin/knowledge/list` P99 < 200ms
- 状态切换/删除 P99 < 500ms（含 `ai-service` 调用）

## Cross-US Dependencies

| US | 方向 | 说明 |
|---|---|---|
| US-053 | 被本 US 依赖 | 管理员登录态与 ADMIN 角色校验 |
| US-062 | 被本 US 依赖 | 复用 `ai_knowledge_document` 表与基础服务 |
| US-064 | 反向依赖 | 用户知识库检索依赖本 US 维护的启用状态与向量存在性 |
