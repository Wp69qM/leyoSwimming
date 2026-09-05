## Why

AI 助理需要可信赖的游泳知识来源，才能准确回答安全、急救、训练等知识类问题。后台管理员必须能够将游泳知识文档（手动输入或上传文件）补充到向量知识库，作为 RAG 检索的首选数据来源。

## What Changes

- 管理后台新增「知识库管理」页面及「新增文档」弹窗，支持手动输入与文件上传两种方式
- `backend` 新增管理端接口 `POST /api/admin/knowledge/add` 与 `POST /api/admin/knowledge/upload`
- `backend` 新增内部调用 `ai-service` 的 `POST /api/internal/ai/knowledge/ingest` 转发能力
- `ai-service` 新增文档解析、分块、Embedding、写入 Chroma 的向量索引能力
- 新增 `ai_knowledge_document` 表存储文档元数据，标题全局唯一
- 上传失败的文档必须回滚 MySQL 写入，不进入向量库

## Capabilities

### New Capabilities

- `admin-upload-knowledge-document`: 管理员上传游泳知识文档到向量知识库

### Modified Capabilities

- 无

## Impact

- **数据表**：新增 `ai_knowledge_document`；向量库新增 `knowledge_base` collection
- **API**：新增 `POST /api/admin/knowledge/add`、`POST /api/admin/knowledge/upload`、`POST /api/internal/ai/knowledge/ingest`
- **状态机**：无
- **前端**：新增 web-admin 知识库上传弹窗 / 页面
- **依赖**：US-053（管理员登录认证）
- **影响**：为 US-063（管理知识库文档）和 US-064（用户从知识库获取知识）提供数据基础
