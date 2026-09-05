from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any

from langchain_community.embeddings import DashScopeEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.config import Settings, get_settings
from app.utils.logger import get_logger

logger = get_logger(__name__)

DEFAULT_CHUNK_SIZE = 500
DEFAULT_CHUNK_OVERLAP = 100
COLLECTION_NAME = "knowledge_base"

# 占位符，用于单测中 patch `app.stores.vector_store.Chroma`。
# 首次访问 _db 时若仍为占位符，则延迟导入真正的 Chroma 实现。
Chroma: Any | None = None


class VectorStore(ABC):
    """知识库向量存储抽象基类。"""

    @abstractmethod
    def add_document(
        self,
        document_id: str,
        title: str,
        category: str,
        content: str,
    ) -> None:
        """将文档分块后写入向量库。"""

    @abstractmethod
    def delete_document(self, document_id: str) -> None:
        """按 document_id 删除向量库中的对应分块。"""

    @abstractmethod
    def rebuild_document(
        self,
        document_id: str,
        title: str,
        category: str,
        content: str,
    ) -> None:
        """删除旧文档后重新索引。"""

    @abstractmethod
    def query(
        self,
        query: str,
        top_k: int = 3,
        threshold: float = 0.2,
    ) -> list[dict[str, Any]]:
        """检索与查询最相关的知识片段。"""


class ChromaVectorStore(VectorStore):
    """基于 Chroma + DashScope Embedding 的向量存储实现。"""

    def __init__(
        self,
        settings: Settings | None = None,
        embeddings: DashScopeEmbeddings | None = None,
    ) -> None:
        self.settings = settings or get_settings()
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=DEFAULT_CHUNK_SIZE,
            chunk_overlap=DEFAULT_CHUNK_OVERLAP,
        )
        self.embeddings = embeddings or self._build_embeddings()
        self.collection_name = COLLECTION_NAME
        self.persist_directory = self.settings.chroma_persist_directory
        Path(self.persist_directory).mkdir(parents=True, exist_ok=True)
        # 延迟初始化 Chroma 客户端，避免模块导入/单测时强制加载 chromadb 重型依赖
        self._db_instance: Any | None = None
        self._db_kwargs: dict[str, Any] = {
            "collection_name": self.collection_name,
            "embedding_function": self.embeddings,
            "persist_directory": self.persist_directory,
        }

    @property
    def _db(self) -> Any:
        if self._db_instance is None:
            global Chroma
            if Chroma is None:
                from langchain_chroma import Chroma as _Chroma

                Chroma = _Chroma
            self._db_instance = Chroma(**self._db_kwargs)
        return self._db_instance

    def _build_embeddings(self) -> DashScopeEmbeddings:
        return DashScopeEmbeddings(
            model=self.settings.embedding_model,
            dashscope_api_key=self.settings.embedding_api_key,
        )

    def _chunk_content(self, content: str) -> list[str]:
        return self.text_splitter.split_text(content)

    def _build_metadata(
        self,
        document_id: str,
        title: str,
        category: str,
        chunk_index: int,
    ) -> dict[str, Any]:
        return {
            "document_id": document_id,
            "title": title,
            "category": category,
            "chunk_index": chunk_index,
        }

    def add_document(
        self,
        document_id: str,
        title: str,
        category: str,
        content: str,
    ) -> None:
        chunks = self._chunk_content(content)
        if not chunks:
            logger.warning(
                "vector_store_empty_document",
                document_id=document_id,
                title=title,
            )
            return

        metadatas = [
            self._build_metadata(document_id, title, category, i)
            for i in range(len(chunks))
        ]
        ids = [f"{document_id}_{i}" for i in range(len(chunks))]

        try:
            self._db.add_texts(texts=chunks, metadatas=metadatas, ids=ids)
            logger.info(
                "vector_store_document_added",
                document_id=document_id,
                title=title,
                chunk_count=len(chunks),
            )
        except Exception as exc:
            logger.error(
                "vector_store_add_failed",
                document_id=document_id,
                title=title,
                error=str(exc),
            )
            raise

    def delete_document(self, document_id: str) -> None:
        try:
            self._db._collection.delete(where={"document_id": document_id})
            logger.info(
                "vector_store_document_deleted",
                document_id=document_id,
            )
        except Exception as exc:
            logger.error(
                "vector_store_delete_failed",
                document_id=document_id,
                error=str(exc),
            )
            raise

    def rebuild_document(
        self,
        document_id: str,
        title: str,
        category: str,
        content: str,
    ) -> None:
        self.delete_document(document_id)
        self.add_document(document_id, title, category, content)
        logger.info(
            "vector_store_document_rebuilt",
            document_id=document_id,
            title=title,
        )

    def query(
        self,
        query: str,
        top_k: int = 3,
        threshold: float = 0.2,
    ) -> list[dict[str, Any]]:
        try:
            raw_results = self._db.similarity_search_with_score(query, k=top_k)
        except Exception as exc:
            logger.error(
                "vector_store_query_failed",
                query=query,
                error=str(exc),
            )
            return []

        results: list[dict[str, Any]] = []
        for doc, score in raw_results:
            # Chroma 默认返回的是距离（越小越相似），转换为相似度分数
            normalized_score = max(0.0, min(1.0, 1.0 - float(score)))
            if normalized_score <= threshold:
                continue
            metadata = doc.metadata or {}
            results.append(
                {
                    "document_id": metadata.get("document_id", ""),
                    "title": metadata.get("title", ""),
                    "category": metadata.get("category", ""),
                    "content": doc.page_content,
                    "score": round(normalized_score, 4),
                }
            )

        results.sort(key=lambda r: r["score"], reverse=True)
        logger.info(
            "vector_store_query",
            query=query,
            top_k=top_k,
            threshold=threshold,
            result_count=len(results),
        )
        return results
