# Tech Design: US-062 管理员-上传知识库文档

## 1. 总体架构

```
web-admin
    │
    ├── 手动输入 → POST /api/admin/knowledge/add
    └── 文件上传 → POST /api/admin/knowledge/upload
              │
              ▼
        backend 校验 + MySQL 写入 ai_knowledge_document
              │
              ▼
ai-service POST /api/internal/ai/knowledge/ingest
    │
    ├── 文本解析
    ├── 分块
    ├── Embedding
    ▼
Chroma (knowledge_base collection)
```

## 2. 新增文件

| 文件 | 说明 |
|------|------|
| `backend/src/main/java/com/leyoswimming/entity/AiKnowledgeDocument.java` | 实体 |
| `backend/src/main/java/com/leyoswimming/repository/AiKnowledgeDocumentMapper.java` | MyBatis Plus Mapper |
| `backend/src/main/java/com/leyoswimming/service/AiKnowledgeDocumentService.java` | 业务逻辑 |
| `backend/src/main/java/com/leyoswimming/controller/admin/AdminKnowledgeController.java` | 管理端 API |
| `ai-service/app/services/knowledge_service.py` | 文档索引服务 |
| `ai-service/app/stores/vector_store.py` | 向量存储抽象 |
| `ai-service/app/models/schemas.py` | 扩展 KnowledgeIngestRequest 等 schema |

## 3. 数据模型

见 [设计规格 §4.1](../../../superpowers/spec/2026-09-02-ai-assistant-rag-design.md)

## 4. API 设计

见 [设计规格 §5.1](../../../superpowers/spec/2026-09-02-ai-assistant-rag-design.md)

## 5. 关键实现

### 5.1 文件解析

```python
def extract_text(content: bytes, content_type: str) -> str:
    encoding = "utf-8"
    text = content.decode(encoding)
    if content_type == "markdown":
        # 简单移除 Markdown 标记，保留纯文本
        text = re.sub(r"[#*_\[\]()`]", "", text)
    return text.strip()
```

### 5.2 分块策略

```python
from langchain.text_splitter import RecursiveCharacterTextSplitter

def chunk_document(text: str, chunk_size: int = 500, chunk_overlap: int = 100):
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separators=["\n\n", "\n", "。", " ", ""],
    )
    return splitter.split_text(text)
```

### 5.3 入库流程

1. backend 校验权限和标题唯一性
2. backend 写入 MySQL，status=0
3. backend 调用 ai-service ingest
4. ai-service 解析、分块、embedding、写入 Chroma
5. 若 ai-service 失败，backend 删除 MySQL 记录

## 6. 依赖

- `langchain`
- `langchain-community` (Chroma)
- `langchain-openai` (Embedding)
- `chromadb`
