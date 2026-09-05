import asyncio
from typing import Any

from langchain_core.tools import tool
from tavily import TavilyClient

from app.config import Settings, get_settings
from app.services.knowledge_service import KnowledgeService
from app.stores.vector_store import ChromaVectorStore
from app.utils.logger import get_logger

logger = get_logger(__name__)

DEFAULT_TOP_K = 3
DEFAULT_THRESHOLD = 0.2
DEFAULT_MAX_RESULTS = 3

_knowledge_service: KnowledgeService | None = None


def _get_knowledge_service(settings: Settings | None = None) -> KnowledgeService:
    global _knowledge_service
    if _knowledge_service is None:
        cfg = settings or get_settings()
        _knowledge_service = KnowledgeService(
            vector_store=ChromaVectorStore(cfg),
            settings=cfg,
        )
    return _knowledge_service


def build_tools() -> list[Any]:
    """构建 RAG / 联网搜索相关的 function calling 工具。"""

    @tool
    async def query_knowledge(query: str, top_k: int = DEFAULT_TOP_K) -> list[dict[str, Any]]:
        """从游泳知识库中检索与用户问题相关的知识片段。

        当用户询问游泳安全、急救、训练技巧、健康等知识类问题时调用。
        返回每条知识的原文内容、来源标题、分类和相似度分数。

        Args:
            query: 用户问题或检索关键词
            top_k: 最多返回几条知识片段，默认 3
        """
        logger.info("tool_query_knowledge", query=query, top_k=top_k)
        service = _get_knowledge_service()
        raw_results = await service.query(
            query=query,
            top_k=top_k,
            threshold=DEFAULT_THRESHOLD,
        )
        return [
            {
                "content": r["content"],
                "source": r["title"],
                "source_type": "knowledge_base",
                "category": r["category"],
                "score": r["score"],
            }
            for r in raw_results
        ]

    @tool
    async def web_search(query: str, max_results: int = DEFAULT_MAX_RESULTS) -> list[dict[str, Any]]:
        """当游泳知识库没有相关内容时，通过联网搜索补充信息。

        仅当 query_knowledge 返回空或结果相似度不足时调用。
        返回搜索结果的标题、摘要和链接。

        Args:
            query: 搜索关键词
            max_results: 最多返回几条结果，默认 3
        """
        logger.info("tool_web_search", query=query, max_results=max_results)
        settings = get_settings()
        if not settings.tavily_api_key:
            logger.warning("tavily_api_key_missing")
            return []

        try:
            client = TavilyClient(api_key=settings.tavily_api_key)
            response = await asyncio.to_thread(client.search, query=query, max_results=max_results)
        except Exception as exc:
            logger.error("tavily_search_failed", query=query, error=str(exc))
            return []

        raw_results = response.get("results", []) if isinstance(response, dict) else []
        results: list[dict[str, Any]] = []
        for item in raw_results[:max_results]:
            if not isinstance(item, dict):
                continue
            results.append(
                {
                    "title": item.get("title", ""),
                    "content": item.get("content", ""),
                    "url": item.get("url", ""),
                    "source_type": "web_search",
                }
            )
        logger.info("tool_web_search_finished", query=query, result_count=len(results))
        return results

    return [query_knowledge, web_search]
