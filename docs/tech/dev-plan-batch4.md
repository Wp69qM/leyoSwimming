# 第四批次开发设计文档 - AI 助理知识库（RAG + 联网搜索）

> **批次范围**：US-062 ~ US-065
> **批次目标**：实现 AI 助理的向量知识库能力，支持后台上传/管理知识文档、用户侧基于知识库回答、无匹配时 Tavily 联网搜索兜底。
> **状态**：需求设计已 APPROVAL，进入开发阶段
> **创建日期**：2026-09-03

---

## 1. 涉及用户故事

| US | 标题 | 状态 | 估时 | 优先级 |
|----|------|------|------|--------|
| US-062 | 管理员-上传知识库文档 | [APPROVAL] | 1 人天 | MVP |
| US-063 | 管理员-管理知识库文档 | [APPROVAL] | 0.8 人天 | MVP |
| US-064 | 用户-从知识库获取游泳知识 | [APPROVAL] | 1.5 人天 | MVP |
| US-065 | 系统-联网搜索兜底 | [APPROVAL] | 1 人天 | MVP |

---

## 2. 开发顺序与依赖关系

```
US-062 上传知识库文档
    │
    ▼
US-063 管理知识库文档  ───────┐
    │                         │
    ▼                         │
US-064 从知识库获取游泳知识    │
    │                         │
    ▼                         │
US-065 联网搜索兜底 ◄─────────┘
```

### 2.1 推荐的开发顺序

1. **US-062 先行**：先建立 `ai_knowledge_document` 表、ai-service 向量索引链路，为后续 US 提供数据基础。
2. **US-063 随后**：基于 US-062 的实体和索引能力，补齐列表、启用/禁用、删除。
3. **US-064 与 US-065 并行或串行**：
   - US-064 实现 `query_knowledge` 工具与意图判断
   - US-065 实现 `web_search` 工具与兜底策略
   - 两者都依赖 US-062 的向量库，建议 US-064 先完成后再做 US-065

### 2.2 开发分组建议

| 分组 | 负责内容 | 可并行 |
|------|----------|--------|
| Backend 组 | 管理端 API（add/upload/list/toggle/delete）、MySQL 实体、ai-service 调用 | 是 |
| ai-service 组 | 向量库、Embedding、ingest/delete/rebuild、query_knowledge、web_search | 是 |
| web-admin 组 | 知识库管理页、上传弹窗 | 是 |
| 测试组 | 单元/集成/E2E 测试 | 依赖前三组 |

---

## 3. 数据表变更

### 3.1 新增表

**`ai_knowledge_document`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 文档 ID |
| title | VARCHAR(200) NOT NULL UNIQUE | 文档标题 |
| category | VARCHAR(50) NOT NULL | 分类：safety / technique / emergency / other |
| content_type | VARCHAR(20) NOT NULL | text / markdown |
| source_type | VARCHAR(20) NOT NULL | manual / file |
| status | TINYINT NOT NULL DEFAULT 0 | 0=启用，1=禁用 |
| created_by | BIGINT NOT NULL | 上传管理员 ID |
| created_at | DATETIME NOT NULL | 创建时间 |
| updated_at | DATETIME NOT NULL | 更新时间 |

**索引建议**：
- `idx_category_status` (category, status)
- `idx_created_by`
- `idx_status`

### 3.2 不修改现有表

本批次不修改 `ai_chat_session`、`ai_chat_message` 等现有表结构，仅复用 `/api/ai-assistant/chat` 接口。

---

## 4. API 清单

### 4.1 Backend 管理端 API

| 接口 | 方法 | 鉴权 | 归属 US | 说明 |
|------|------|------|---------|------|
| `/api/admin/knowledge/add` | POST | ADMIN | US-062 | 手动输入新增文档 |
| `/api/admin/knowledge/upload` | POST | ADMIN | US-062 | 文件上传新增文档（multipart/form-data） |
| `/api/admin/knowledge/list` | POST | ADMIN | US-063 | 文档列表分页查询，返回 `data.list`（见 §4.5） |
| `/api/admin/knowledge/toggle` | POST | ADMIN | US-063 | 启用/禁用文档 |
| `/api/admin/knowledge/delete` | POST | ADMIN | US-063 | 删除文档 |

### 4.2 ai-service Internal API

| 接口 | 方法 | 鉴权 | 归属 US | 说明 |
|------|------|------|---------|------|
| `/api/internal/ai/knowledge/ingest` | POST | X-Internal-Token | US-062 | 文档分块、Embedding、写入 Chroma |
| `/api/internal/ai/knowledge/delete` | POST | X-Internal-Token | US-063 | 按 documentId 删除向量 |
| `/api/internal/ai/knowledge/rebuild` | POST | X-Internal-Token | US-063 | 按 documentId 重建向量索引 |

### 4.3 复用的用户侧 API

| 接口 | 方法 | 鉴权 | 归属 US | 说明 |
|------|------|------|---------|------|
| `/api/ai-assistant/chat` | POST | 用户 Token | US-064 / US-065 | 复用现有 AI 助理对话接口，内部增强知识库/联网搜索能力 |

### 4.4 新增 function calling 工具

| 工具名 | 归属 US | 说明 |
|--------|---------|------|
| `query_knowledge` | US-064 | 从 Chroma 检索相关知识片段 |
| `web_search` | US-065 | 调用 Tavily API 联网搜索兜底 |

### 4.5 管理端分页响应规范

`/api/admin/knowledge/list` 返回统一分页结构，字段名与项目其他列表接口保持一致：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [],
    "total": 0,
    "page": 1,
    "pageSize": 10
  }
}
```

> 注意：不使用 `data.items`，统一使用 `data.list`，与 `AdminAccountListResponse`、`AdminUserListResponse` 等项目既有列表响应保持一致。

---

## 5. 关键实现点

### 5.1 ai-service 向量库模块

**新增文件结构**：

```
ai-service/app/
├── services/
│   └── knowledge_service.py       # 文档分块、Embedding、Chroma 读写
├── stores/
│   └── vector_store.py            # VectorStore 抽象层（便于后续切 Qdrant）
├── tools/
│   ├── knowledge_tools.py         # query_knowledge, web_search
│   └── recommendation_tools.py    # 现有工具（保持不变）
├── routers/
│   └── internal_knowledge.py      # /api/internal/ai/knowledge/* 路由
├── schemas/
│   └── knowledge.py               # Pydantic 模型
└── config.py                      # 增加 Chroma/Tavily/Embedding 配置
```

**核心逻辑**：
1. **分块**：使用 `RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=100)`
2. **Embedding**：`OpenAIEmbeddings(model="text-embedding-3-small")` 或兼容 API
3. **存储**：Chroma `knowledge_base` collection，metadata 包含 `documentId`、`title`、`category`、`chunkIndex`
4. **检索**：`similarity_search_with_score`，过滤 `status=0`，阈值 0.7
5. **兜底**：top-k 结果均低于阈值 → 调用 Tavily

### 5.2 Backend Java 实现

**新增/修改文件**：

```
backend/src/main/java/com/leyoswimming/
├── controller/admin/
│   └── KnowledgeAdminController.java
├── service/
│   ├── KnowledgeAdminService.java
│   └── AiAssistantGatewayService.java   # 增加内部调用方法
├── repository/
│   └── AiKnowledgeDocumentRepository.java
├── entity/
│   └── AiKnowledgeDocumentEntity.java
└── dto/
    └── knowledge/
        ├── KnowledgeAddRequest.java
        ├── KnowledgeUploadRequest.java
        ├── KnowledgeListRequest.java
        ├── KnowledgeToggleRequest.java
        ├── KnowledgeDeleteRequest.java
        └── KnowledgeListResponse.java
```

**关键逻辑**：
1. **add/upload**：写入 MySQL → 调用 ai-service `/ingest` → 失败则回滚
2. **list**：分页查询，支持 category/status/keyword 过滤
3. **toggle**：更新 MySQL status → 禁用调用 `/delete`，启用调用 `/rebuild`
4. **delete**：删除 MySQL → 调用 ai-service `/delete`

### 5.3 web-admin 前端实现

**新增/修改文件**：

```
web-admin/src/
├── views/
│   └── knowledge/
│       ├── index.vue              # 知识库管理页
│       └── components/
│           └── UploadModal.vue    # 新增文档弹窗
├── api/
│   └── knowledge.ts               # 管理端 API 封装
└── router/
    └── index.ts                   # 新增 /knowledge-management 路由
```

**权限控制**：菜单仅对 ADMIN 角色可见，接口调用带 ADMIN token。

### 5.4 System Prompt 调整

在 `ai-service/app/...` 的 System Prompt 中增加：

```
当用户询问游泳安全、急救、训练技巧、游泳健康知识时：
- 优先调用 query_knowledge 从知识库获取答案。
- 如果 query_knowledge 返回空或相关性低于 0.7，调用 web_search。
- 基于工具返回内容生成回答，不编造。
如果答案来自网络，提示用户"以下内容来自网络，仅供参考"。
```

---

## 6. 测试策略

### 6.1 单元测试

| 测试对象 | 归属 | 说明 |
|----------|------|------|
| `extract_text` / `chunk_document` | ai-service | 文本解析与分块 |
| `VectorStore.add/delete/query` | ai-service | Chroma 读写 |
| `query_knowledge` | ai-service | Mock Chroma |
| `web_search` | ai-service | Mock Tavily |
| `KnowledgeAdminService` | backend | 业务逻辑与回滚 |

### 6.2 集成测试

| 场景 | 说明 |
|------|------|
| 完整入库 → 检索 | backend → ai-service → Chroma → 用户问答 |
| 无匹配 → Tavily 兜底 | 相似度低于阈值触发 web_search |
| 禁用文档 → 检索不到 | 状态同步到向量库 |
| 删除文档 → 向量清理 | MySQL 与 Chroma 一致性 |

### 6.3 E2E 测试

| 场景 | 说明 |
|------|------|
| 管理员上传文档 | web-admin → backend → ai-service |
| 用户提问获得知识库答案 | 小程序/用户端 → backend → ai-service |
| 用户提问触发联网搜索 | 知识库无匹配时 Tavily 兜底 |

---

## 7. 环境变量与配置

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `OPENAI_API_KEY` / `OPENAI_BASE_URL` | Embedding 与 LLM | - |
| `CHROMA_PERSIST_DIRECTORY` | Chroma 持久化目录 | `./data/chroma` |
| `TAVILY_API_KEY` | Tavily 搜索 API Key | - |
| `TAVILY_MAX_RESULTS` | Tavily 最大结果数 | `3` |
| `KNOWLEDGE_SIMILARITY_THRESHOLD` | 相似度阈值 | `0.7` |
| `INTERNAL_AUTH_TOKEN` | ai-service 内部接口鉴权 | - |

---

## 8. 风险与回滚方案

| 风险 | 影响 | 缓解/回滚措施 |
|------|------|---------------|
| Chroma 多副本无法共享 | 高（未来扩展） | MVP 单副本；VectorStore 已抽象，后续可切 Qdrant |
| Tavily API 成本/稳定性 | 中 | 配置开关；失败时返回友好提示 |
| Embedding 调用延迟 | 中 | 异步索引；检索使用本地向量库 |
| ai-service 索引失败 | 中 | backend 写入 MySQL 后调用 ai-service，失败回滚 |
| 回答幻觉 | 中 | 严格基于检索/搜索结果生成；System Prompt 约束 |

### 回滚方案

- **数据库**：本批次仅有新增表 `ai_knowledge_document`，回滚可直接 DROP TABLE。
- **向量库**：删除 Chroma `knowledge_base` collection 即可清空。
- **代码**：功能开关控制，若线上异常可关闭 knowledge tools 调用，回退到原有推荐-only 行为。

---

## 9. 验收标准

- [ ] US-062：管理员可成功上传手动输入和文件两种形式的文档，标题重复被拒绝，ai-service 失败回滚
- [ ] US-063：管理员可列表查看、启用/禁用、删除文档，状态同步到向量库
- [ ] US-064：用户询问游泳知识类问题，AI 优先从知识库生成答案；推荐类问题不走知识库
- [ ] US-065：知识库无匹配时自动调用 Tavily，回答标注来源；Tavily 失败时优雅降级

---

## 10. 相关文档

### 10.1 需求与设计

- 需求文档：[docs/tech/AI-assistant-requirements/rag-function-calling-requirements.md](./AI-assistant-requirements/rag-function-calling-requirements.md)
- 设计规格：[docs/superpowers/spec/2026-09-02-ai-assistant-rag-design.md](../superpowers/spec/2026-09-02-ai-assistant-rag-design.md)
- 批次梳理：[docs/figma/第四批次页面梳理.md](../figma/%E7%AC%AC%E5%9B%9B%E6%AC%A1%E7%94%B5%E9%A1%B5%E9%9D%A2%E6%A2%B3%E7%90%86.md)

### 10.2 用户故事

- US-062：[docs/stories/US-062-管理员-上传知识库文档/user-story.md](../stories/US-062-%E7%AE%A1%E7%90%86%E5%91%98-%E4%B8%8A%E4%BC%A0%E7%9F%A5%E8%AF%86%E5%BA%93%E6%96%87%E6%A1%A3/user-story.md)
- US-063：[docs/stories/US-063-管理员-管理知识库文档/user-story.md](../stories/US-063-%E7%AE%A1%E7%90%86%E5%91%98-%E7%AE%A1%E7%90%86%E7%9F%A5%E8%AF%86%E5%BA%93%E6%96%87%E6%A1%A3/user-story.md)
- US-064：[docs/stories/US-064-用户-从知识库获取游泳知识/user-story.md](../stories/US-064-%E7%94%A8%E6%88%B7-%E4%BB%8E%E7%9F%A5%E8%AF%86%E5%BA%93%E8%8E%B7%E5%8F%96%E6%B8%B8%E6%B3%B3%E7%9F%A5%E8%AF%86/user-story.md)
- US-065：[docs/stories/US-065-系统-联网搜索兜底/user-story.md](../stories/US-065-%E7%B3%BB%E7%BB%9F-%E8%81%94%E7%BD%91%E6%90%9C%E7%B4%A2%E5%85%9C%E5%BA%95/user-story.md)

### 10.3 页面规格

- 知识库管理页：[docs/figma/page-spec/A-knowledge-management-page.md](../figma/page-spec/A-knowledge-management-page.md)
- 新增知识库文档弹窗：[docs/figma/page-spec/A-knowledge-upload-modal.md](../figma/page-spec/A-knowledge-upload-modal.md)
- AI 助理页（复用）：[docs/figma/page-spec/U-AI-assistant-page.md](../figma/page-spec/U-AI-assistant-page.md)

### 10.4 OpenSpec Changes

- `openspec/changes/us-062-admin-upload-knowledge-document/`
- `openspec/changes/us-063-admin-manage-knowledge-document/`
- `openspec/changes/us-064-user-query-knowledge-base/`
- `openspec/changes/us-065-web-search-fallback/`
