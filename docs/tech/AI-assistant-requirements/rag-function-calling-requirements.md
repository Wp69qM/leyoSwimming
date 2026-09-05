# AI-Service RAG + Function Calling 需求文档

> **文档性质**：AI 助理知识库功能的需求规格说明书
> **版本**：v1.0
> **创建日期**：2026-09-02
> **适用范围**：MVP 阶段

---

## 1. 背景与目标

### 1.1 背景
当前 `ai-service` 已支持基于 function calling 的教练/课程推荐。但用户可能询问游泳安全、急救、训练知识等**非业务数据类问题**，现有系统无法回答。

### 1.2 目标
- 后台管理员可上传游泳知识文档到向量知识库
- 用户侧 AI 助理优先从知识库检索答案
- 知识库无匹配时，自动调用 Tavily 联网搜索兜底
- 不改变现有推荐类 function calling 行为

---

## 2. 需求范围

### 2.1 In Scope（MVP）
- 支持 **纯文本 / Markdown** 文档上传
- 文档分块、Embedding、向量存储
- 用户提问时向量检索 + 联网搜索兜底
- 后台管理页面：上传、列表、删除、启用/禁用
- 用户侧 AI 助理对话自然接入，无需改动小程序界面

### 2.2 Out of Scope（MVP 不做）
- PDF/Word 解析（后续扩展）
- 多轮对话中的知识库上下文增强
- 文档编辑、版本管理
- 多语言知识库
- 用户反馈/点赞知识库答案

---

## 3. 用户故事

### US-062：管理员-上传知识库文档
> 作为后台管理员，我希望能上传游泳安全知识文本/Markdown，这样 AI 助理就能基于这些内容回答用户问题。

**验收标准**
- 支持输入标题、分类、文本内容
- 支持上传 `.md`、`.txt` 文件
- 上传成功后返回文档 ID
- 相同标题文档不允许重复上传
- 上传失败的文档不进入向量库

### US-063：管理员-管理知识库文档
> 作为后台管理员，我希望能查看、启用/禁用、删除已上传的知识文档，这样我可以控制 AI 助理能引用哪些内容。

**验收标准**
- 列表展示文档标题、分类、状态、上传时间
- 支持分页
- 禁用后文档不再被检索
- 删除后向量和元数据都被清理
- 只有 ADMIN 角色可访问管理页面

### US-064：用户-从知识库获取游泳知识
> 作为用户，当我问"野泳的注意事项"时，AI 助理能从知识库找到相关内容并给出准确回答。

**验收标准**
- 知识库有匹配内容时，回答基于知识库内容
- 回答中隐含引用来源文档标题
- 不编造知识库中没有的信息
- 对教练/课程类问题仍走现有推荐工具

### US-065：系统-联网搜索兜底
> 作为用户，当知识库没有相关内容时，AI 助理能联网搜索最新信息并告诉我。

**验收标准**
- 向量检索 top-k 相似度均低于阈值时触发联网搜索
- Tavily 搜索失败时优雅降级，不阻塞对话
- 联网搜索结果作为补充参考，回答需标注"来自网络"
- 联网搜索耗时过长时返回超时提示

---

## 4. 架构设计

### 4.1 整体架构

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   web-admin     │────▶│     backend      │────▶│   ai-service    │
│ 知识库管理页面   │     │ 管理端上传 API    │     │ 向量索引/检索服务 │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                              MySQL (元数据)              Chroma (向量)
                                                              ↑
                                                       Embedding Model
                                                              ↑
                                                       Tavily (fallback)
```

### 4.2 核心数据流

**文档入库流**
1. 管理员在 `web-admin` 填写/上传文档
2. `backend` 接收请求，校验 ADMIN 权限
3. `backend` 将文档元数据写入 MySQL，原文转发给 `ai-service`
4. `ai-service` 解析文本 → 分块 → Embedding → 写入 Chroma
5. 返回入库结果

**用户问答流**
1. 用户发送问题到 `backend` `/api/ai-assistant/chat`
2. `backend` 转发给 `ai-service`
3. `ai-service` LLM 判断意图
4. 若是知识类问题 → 调用 `query_knowledge`
5. 若检索结果有效 → 用检索结果生成回答
6. 若检索结果无效 → 调用 `web_search`
7. 返回最终回答

---

## 5. 数据模型

### 5.1 MySQL 表：`ai_knowledge_document`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 文档 ID |
| title | VARCHAR(200) | 文档标题 |
| category | VARCHAR(50) | 分类，如 safety、technique、emergency |
| content_type | VARCHAR(20) | `text` 或 `markdown` |
| source_type | VARCHAR(20) | `manual`（手动输入）或 `file`（文件上传） |
| status | TINYINT | 0=启用，1=禁用 |
| created_by | BIGINT | 上传管理员 ID |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 5.2 Chroma Collection

- Collection name: `knowledge_base`
- Document: 原始文本块
- Metadata:
  - `document_id`: 对应 MySQL 文档 ID
  - `title`: 文档标题
  - `category`: 分类
  - `chunk_index`: 块序号

---

## 6. API 设计

### 6.1 Backend 管理端 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/knowledge/add` | POST | 手动输入新增文档 |
| `/api/admin/knowledge/upload` | POST | 文件上传新增文档（multipart/form-data） |
| `/api/admin/knowledge/list` | POST | 文档列表分页查询 |
| `/api/admin/knowledge/toggle` | POST | 启用/禁用文档（body 传 documentId） |
| `/api/admin/knowledge/delete` | POST | 删除文档（body 传 documentId） |

### 6.2 ai-service Internal API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/internal/ai/knowledge/ingest` | POST | 接收文档内容，构建向量索引（body 传 documentId） |
| `/api/internal/ai/knowledge/delete` | POST | 按 documentId 删除向量 |
| `/api/internal/ai/knowledge/rebuild` | POST | 按 documentId 重建索引 |

---

## 7. Function Calling 工具设计

### 7.1 新增工具：`query_knowledge`

```python
@tool
async def query_knowledge(query: str, top_k: int = 3) -> list[dict]:
    """
    从游泳知识库中检索与用户问题相关的知识片段。
    当用户询问游泳安全、急救、训练知识等非业务问题时调用。
    
    Args:
        query: 用户的问题
        top_k: 最多返回几条片段，默认 3
    """
```

返回结构：
```json
[
  {
    "content": "野泳前应了解水域深浅、水流、水草情况...",
    "source": "野泳安全指南",
    "category": "safety",
    "score": 0.89
  }
]
```

### 7.2 新增工具：`web_search`

```python
@tool
async def web_search(query: str, max_results: int = 3) -> list[dict]:
    """
    当知识库没有相关内容时，联网搜索补充信息。
    
    Args:
        query: 搜索关键词
        max_results: 最多返回几条结果，默认 3
    """
```

返回结构：
```json
[
  {
    "title": "...",
    "content": "...",
    "url": "..."
  }
]
```

### 7.3 System Prompt 调整

在现有 `SYSTEM_PROMPT` 中增加：

```
14. 当用户询问游泳安全、急救、训练技巧、游泳健康知识时：
    - 优先调用 query_knowledge 从知识库获取答案。
    - 如果 query_knowledge 返回空或相关性很低，调用 web_search 联网搜索。
    - 基于工具返回的内容生成回答，不要编造。
15. 如果答案来自知识库，保持专业、简洁；如果来自网络，提示用户"以下内容来自网络，仅供参考"。
```

---

## 8. Web-admin 页面设计

### 8.1 页面路径
`/knowledge-management`

### 8.2 页面结构

**顶部操作区**
- 标题：知识库管理
- 按钮：新增文档

**新增文档弹窗**
- 表单字段：标题、分类（下拉选择）、内容类型（文本/Markdown）
- 内容输入：文本域
- 文件上传：支持 `.txt`、`.md`

**列表区**
- 表格列：标题、分类、状态、上传人、上传时间、操作
- 操作按钮：启用/禁用、删除

---

## 9. 关键实现细节

### 9.1 文本分块策略
- 使用 LangChain `RecursiveCharacterTextSplitter`
- chunk_size: 500
- chunk_overlap: 100
- 按段落优先分割，保持语义完整

### 9.2 Embedding 模型
- 使用 OpenAI 兼容的 Embedding API（如 `text-embedding-3-small`）
- 通过 `langchain-openai` 的 `OpenAIEmbeddings` 封装
- embedding 配置与 LLM 配置分离，便于后续切换

### 9.3 相似度阈值
- 默认阈值：0.7（cosine similarity）
- 低于阈值的检索结果视为无匹配，触发 web_search
- 阈值可配置

### 9.4 错误降级
- 向量库查询失败 → 记录日志 → 尝试 web_search
- Tavily 失败 → 返回"暂时无法获取该知识，请换个方式提问"
- Embedding 服务不可用 → 直接走 web_search

---

## 10. 测试策略

### 10.1 单元测试
- `extract_text` 解析函数
- `chunk_document` 分块函数
- `query_knowledge` 工具（Mock Chroma）
- `web_search` 工具（Mock Tavily）

### 10.2 集成测试
- 完整入库 → 检索链路
- 无匹配 → Tavily fallback 链路
- 禁用文档 → 检索不到

### 10.3 端到端测试
- 管理员上传文档
- 用户提问获得知识库答案
- 用户提问触发联网搜索

---

## 11. 风险与限制

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Chroma 多副本无法共享 | 高（未来扩展） | MVP 单副本；抽象 VectorStore 接口，后续切 Qdrant |
| Tavily API 成本/稳定性 | 中 | 配置开关；失败优雅降级 |
| Embedding 模型调用延迟 | 中 | 文档分块异步索引；检索使用本地向量库 |
| 回答幻觉 | 中 | 严格基于检索结果生成；System Prompt 约束 |
