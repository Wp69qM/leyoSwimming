# Tech Design: US-064 用户-从知识库获取游泳知识

## 1. 总体架构

```
用户端小程序
    │
    ▼
backend /api/ai-assistant/chat
    │
    ▼
ai-service ChatService
    │
    ├── LLM 意图判断
    │
    └── query_knowledge 工具
            │
            ▼
        Chroma 向量检索
            │
            ▼
        LLM 生成回答
```

## 2. 新增/修改文件

| 文件 | 说明 |
|------|------|
| `ai-service/app/tools/knowledge_tools.py` | 新增 query_knowledge 工具 |
| `ai-service/app/services/chat_service.py` | 扩展 tool registry 和 system prompt |
| `ai-service/app/stores/vector_store.py` | 向量检索方法 |

## 3. Function Calling 工具

```python
@tool
async def query_knowledge(query: str, top_k: int = 3) -> list[dict]:
    results = vector_store.similarity_search(query, top_k=top_k)
    return [
        {
            "content": doc.page_content,
            "source": doc.metadata["title"],
            "category": doc.metadata["category"],
            "score": score,
        }
        for doc, score in results
        if score >= threshold
    ]
```

## 4. System Prompt 扩展

在 `SYSTEM_PROMPT` 中新增规则 14-15，明确知识类问题的处理方式和来源标注要求。

## 5. 与现有推荐工具的区分

- 用户提及"教练/课程/套餐/价格" → 调用 recommendation_tools
- 用户提及"安全/急救/技巧/健康/训练方法" → 调用 query_knowledge
- LLM 负责意图判断

## 6. 返回结构

`ChatResponse` 结构不变，`reply.text` 中自然融入知识库内容。
