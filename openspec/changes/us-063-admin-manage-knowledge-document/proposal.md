## Why

后台管理员需要控制哪些知识库文档可被 AI 助理引用，以便及时下线错误、过期或不合规的内容，保证用户获得准确、可控的游泳知识回答。

## What Changes

- 管理后台新增「AI 助理 → 知识库管理」页面
- 管理员可查看所有知识库文档列表，支持按分类、状态、关键词筛选和分页
- 管理员可对文档执行启用/禁用操作：
  - 禁用后，`ai-service` 向量库中对应 chunk 被删除，用户检索不到该文档
  - 启用后，`ai-service` 重新为该文档建立向量索引
- 管理员可删除文档，删除后同时清理 MySQL 元数据和向量库 chunk
- 所有操作仅限 ADMIN 角色；非 ADMIN 访问返回 403
- 删除或禁用过程中若 `ai-service` 不可用，MySQL 状态已更新/记录已删除，并记录日志、触发后台告警

## Capabilities

### New Capabilities

- `admin-manage-knowledge-document`: 管理员查看、启用/禁用、删除知识库文档

### Modified Capabilities

- 无

## Impact

- **数据表**：查询/更新/删除 `ai_knowledge_document`
- **API**：
  - 新增 `POST /api/admin/knowledge/list`
  - 新增 `POST /api/admin/knowledge/toggle`
  - 新增 `POST /api/admin/knowledge/delete`
  - 新增内部接口 `POST /api/internal/ai/knowledge/delete`
  - 新增内部接口 `POST /api/internal/ai/knowledge/rebuild`
- **状态机**：文档启用状态 `status=0`（启用）⇄ `status=1`（禁用）；文档存在 → 已删除
- **前端**：新增 `web-admin` 知识库管理列表页
- **依赖**：US-053（管理员登录认证）、US-062（上传知识库文档）
- **影响**：为 US-064 用户从知识库获取答案提供可控的内容源
