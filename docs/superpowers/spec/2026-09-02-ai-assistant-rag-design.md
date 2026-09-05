# AI-Service RAG + Function Calling 设计规格

> **文档性质**：MVP 阶段技术设计规格
> **版本**：v1.0
> **创建日期**：2026-09-02
> **对应需求**：[docs/tech/AI-assistant-requirements/rag-function-calling-requirements.md](../../tech/AI-assistant-requirements/rag-function-calling-requirements.md)

---

## 1. 设计目标

在现有 AI 助理推荐能力基础上，引入 RAG（检索增强生成）+ Function Calling：
- 后台管理员可维护游泳知识库
- 用户询问安全/急救/训练等知识类问题时，优先检索知识库
- 知识库无匹配时，通过 Tavily 联网搜索兜底

---

## 2. 架构决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 向量库存放位置 | `ai-service` 内部 | RAG 检索与 LLM 调用在同一进程，链路最短 |
| 向量库实现 | Chroma 本地文件 | MVP 成本最低，后续可抽象为 Qdrant |
| 文档格式 | 纯文本 / Markdown | MVP 范围最小，PDF/Word 后续扩展 |
| 联网搜索 | Tavily API | 专为 LLM RAG 设计，返回已清洗摘要 |
| Embedding 模型 | OpenAI 兼容 API | 与现有 LLM 配置方式一致 |

---

## 3. 组件设计

### 3.1 新增组件

| 组件 | 位置 | 职责 |
|------|------|------|
| 知识库管理页面 | `web-admin` | 管理员上传、查看、删除知识库文档 |
| 管理端上传 API | `backend` | 接收文件/文本，校验权限，转发给 `ai-service` |
| 向量索引服务 | `ai-service` | 文档分块、Embedding、写入 Chroma |
| 知识检索工具 | `ai-service` | `query_knowledge` function calling 工具 |
| 联网搜索工具 | `ai-service` | `web_search` function calling 工具（Tavily） |

### 3.2 ai-service 新增模块

```
ai-service/app/
├── services/
│   ├── chat_service.py          # 现有：扩展意图判断和工具调用
│   └── knowledge_service.py     # 新增：向量库操作、文档索引
├── tools/
│   ├── recommendation_tools.py  # 现有
│   └── knowledge_tools.py       # 新增：query_knowledge, web_search
├── clients/
│   ├── java_client.py           # 现有
│   └── tavily_client.py         # 新增： Tavily 搜索客户端
├── models/
│   └── schemas.py               # 扩展：新增 Knowledge 相关 schema
└── stores/
    └── vector_store.py          # 新增：VectorStore 抽象 + Chroma 实现
```

---

## 4. 数据模型

### 4.1 MySQL 表：`ai_knowledge_document`

```sql
CREATE TABLE `ai_knowledge_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
  `category` VARCHAR(50) NOT NULL COMMENT '分类：safety/technique/emergency',
  `content_type` VARCHAR(20) NOT NULL COMMENT '内容类型：text/markdown',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源：manual/file',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=启用 1=禁用',
  `created_by` BIGINT NOT NULL COMMENT '上传管理员ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档元数据';
```

### 4.2 Chroma Collection

- Collection name: `knowledge_base`
- 每个 chunk 作为一条 Document
- Metadata 结构：
  ```json
  {
    "document_id": 123,
    "title": "野泳安全指南",
    "category": "safety",
    "chunk_index": 0
  }
  ```

---

## 5. API 设计

### 5.1 Backend 管理端 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/knowledge/add` | POST | 手动输入新增知识库文档 |
| `/api/admin/knowledge/upload` | POST | 文件上传新增知识库文档（multipart/form-data） |
| `/api/admin/knowledge/list` | POST | 文档列表分页查询 |
| `/api/admin/knowledge/toggle` | POST | 启用/禁用文档 |
| `/api/admin/knowledge/delete` | POST | 删除文档 |

#### POST /api/admin/knowledge/add
- **鉴权**：ADMIN
- **Request**:
  ```json
  {
    "title": "野泳安全指南",
    "category": "safety",
    "content": "# 野泳安全...",
    "contentType": "markdown"
  }
  ```
- **Response**:
  ```json
  {
    "code": 0,
    "message": "success",
    "data": { "documentId": 123 }
  }
  ```

#### POST /api/admin/knowledge/upload
- **鉴权**：ADMIN
- **Content-Type**: `multipart/form-data`
- **Fields**: `file` (.txt / .md), `title`, `category`
- **Response**: 同 `/api/admin/knowledge/add`

#### POST /api/admin/knowledge/list
- **鉴权**：ADMIN
- **Request**:
  ```json
  {
    "page": 1,
    "pageSize": 20,
    "category": "safety",
    "keyword": "",
    "status": 0
  }
  ```
- **Response**:
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

#### POST /api/admin/knowledge/toggle
- **鉴权**：ADMIN
- **Request**:
  ```json
  {
    "documentId": 123,
    "status": 1
  }
  ```
- **Response**: 通用成功响应

#### POST /api/admin/knowledge/delete
- **鉴权**：ADMIN
- **Request**:
  ```json
  { "documentId": 123 }
  ```
- **Response**: 通用成功响应

### 5.2 ai-service Internal API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/internal/ai/knowledge/ingest` | POST | 接收文档内容，构建向量索引 |
| `/api/internal/ai/knowledge/delete` | POST | 按 documentId 删除向量 |
| `/api/internal/ai/knowledge/rebuild` | POST | 按 documentId 重建索引 |

#### POST /api/internal/ai/knowledge/ingest
- **鉴权**：X-Internal-Token
- **Request**:
  ```json
  {
    "documentId": 123,
    "title": "野泳安全指南",
    "category": "safety",
    "content": "# 野泳安全...",
    "contentType": "markdown"
  }
  ```
- **Response**:
  ```json
  {
    "documentId": 123,
    "chunksCount": 5,
    "status": "success"
  }
  ```

#### POST /api/internal/ai/knowledge/delete
- **鉴权**：X-Internal-Token
- **Request**:
  ```json
  { "documentId": 123 }
  ```

#### POST /api/internal/ai/knowledge/rebuild
- **鉴权**：X-Internal-Token
- **Request**: 同 ingest

---

## 6. Function Calling 工具设计

### 6.1 `query_knowledge`

```python
@tool
async def query_knowledge(query: str, top_k: int = 3) -> list[dict]:
    """
    从游泳知识库中检索与用户问题相关的知识片段。
    当用户询问游泳安全、急救、训练知识等非业务问题时调用。
    """
```

### 6.2 `web_search`

```python
@tool
async def web_search(query: str, max_results: int = 3) -> list[dict]:
    """
    当知识库没有相关内容时，联网搜索补充信息。
    """
```

### 6.3 调用决策流程

```
用户提问
    │
    ▼
LLM 意图判断
    │
    ├── 教练/课程/套餐推荐 ──▶ 现有 recommendation_tools
    │
    └── 游泳知识类问题 ──▶ query_knowledge
                │
                ▼
        检索结果最高 score >= 0.7?
                │
        是 ──▶ 用知识库结果生成回答
                │
        否 ──▶ web_search ──▶ 用搜索结果生成回答
                    │
                    失败 ──▶ 返回"暂时无法获取该知识"
```

---

## 7. 关键配置

```python
# ai-service .env
LEYO_TAVILY_API_KEY=...
LEYO_TAVILY_MAX_RESULTS=3
LEYO_KNOWLEDGE_SIMILARITY_THRESHOLD=0.7
LEYO_KNOWLEDGE_CHUNK_SIZE=500
LEYO_KNOWLEDGE_CHUNK_OVERLAP=100
LEYO_KNOWLEDGE_COLLECTION_NAME=knowledge_base
LEYO_EMBEDDING_MODEL=text-embedding-3-small
LEYO_EMBEDDING_BASE_URL=...
LEYO_EMBEDDING_API_KEY=...
```

---

## 8. 错误处理

| 场景 | 行为 |
|------|------|
| 向量库查询失败 | 记录日志，降级到 web_search |
| Tavily 搜索失败 | 返回友好提示，不阻塞对话 |
| Embedding 失败 | 直接走 web_search |
| 文档分块失败 | 返回 500，不入库 |
| 删除向量失败 | 记录日志，后台告警 |

---

## 9. 测试策略

### 9.1 单元测试
- `knowledge_service.chunk_document`
- `knowledge_service.extract_text`
- `knowledge_tools.query_knowledge` (mock Chroma)
- `knowledge_tools.web_search` (mock Tavily)

### 9.2 集成测试
- 文档上传 → 索引 → 检索完整链路
- 无匹配 → Tavily fallback
- 禁用文档 → 检索排除

### 9.3 E2E 测试
- web-admin 上传文档
- 小程序提问获得知识库答案
- 小程序提问触发联网搜索

---

## 10. 部署注意事项

- `ai-service` 单副本部署，Chroma 数据通过挂载卷持久化
- 生产环境如需多副本，需迁移至 Qdrant 等独立向量库
- Tavily API Key 通过环境变量注入
- Embedding 服务需与 LLM 服务网络可达
