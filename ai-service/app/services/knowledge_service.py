from typing import Any

from app.config import Settings, get_settings
from app.stores.vector_store import ChromaVectorStore, VectorStore
from app.utils.logger import get_logger

logger = get_logger(__name__)

DEFAULT_TOP_K = 3
DEFAULT_THRESHOLD = 0.2


class KnowledgeService:
    """知识库业务服务：封装向量存储的增删改查。"""

    def __init__(
        self,
        vector_store: VectorStore | None = None,
        settings: Settings | None = None,
    ) -> None:
        self.settings = settings or get_settings()
        self.vector_store = vector_store or ChromaVectorStore(self.settings)

    async def ingest(
        self,
        document_id: str,
        title: str,
        category: str,
        content: str,
        content_type: str = "text",
    ) -> dict[str, Any]:
        """新增文档到知识库。

        Args:
            document_id: 文档唯一标识
            title: 文档标题
            category: 分类（如 safety / technique / emergency）
            content: 文档原始内容
            content_type: 内容类型（text / markdown），仅用于日志/扩展
        """
        logger.info(
            "knowledge_ingest_start",
            document_id=document_id,
            title=title,
            category=category,
            content_type=content_type,
        )
        self.vector_store.add_document(
            document_id=document_id,
            title=title,
            category=category,
            content=content,
        )
        logger.info(
            "knowledge_ingest_success",
            document_id=document_id,
            title=title,
        )
        return {
            "document_id": document_id,
            "status": "success",
        }

    async def delete(self, document_id: str) -> dict[str, Any]:
        """按 document_id 删除知识库文档。"""
        logger.info("knowledge_delete_start", document_id=document_id)
        self.vector_store.delete_document(document_id)
        logger.info("knowledge_delete_success", document_id=document_id)
        return {
            "document_id": document_id,
            "status": "deleted",
        }

    async def rebuild(
        self,
        document_id: str,
        title: str,
        category: str,
        content: str,
        content_type: str = "text",
    ) -> dict[str, Any]:
        """重建指定文档的向量索引。"""
        logger.info(
            "knowledge_rebuild_start",
            document_id=document_id,
            title=title,
            category=category,
            content_type=content_type,
        )
        self.vector_store.rebuild_document(
            document_id=document_id,
            title=title,
            category=category,
            content=content,
        )
        logger.info(
            "knowledge_rebuild_success",
            document_id=document_id,
            title=title,
        )
        return {
            "document_id": document_id,
            "status": "success",
        }

    async def query(
        self,
        query: str,
        top_k: int = DEFAULT_TOP_K,
        threshold: float = DEFAULT_THRESHOLD,
    ) -> list[dict[str, Any]]:
        """检索与问题相关的知识片段。"""
        logger.info(
            "knowledge_query",
            query=query,
            top_k=top_k,
            threshold=threshold,
        )
        return self.vector_store.query(
            query=query,
            top_k=top_k,
            threshold=threshold,
        )
