from pathlib import Path
from typing import Any
from unittest.mock import MagicMock, patch

import pytest

from app.stores.vector_store import ChromaVectorStore, VectorStore


class FakeEmbeddings:
    """确定性嵌入模型，所有文本返回相同的单位向量，便于测试距离/相似度。"""

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [[1.0, 0.0, 0.0] for _ in texts]

    def embed_query(self, text: str) -> list[float]:
        return [1.0, 0.0, 0.0]


@pytest.fixture
def chroma_dir(tmp_path: Path) -> Path:
    return tmp_path / "chroma"


@pytest.fixture
def settings(chroma_dir: Path, monkeypatch):
    from app.config import get_settings

    settings = get_settings()
    monkeypatch.setattr(settings, "chroma_persist_directory", str(chroma_dir))
    monkeypatch.setattr(settings, "embedding_model", "text-embedding-v2")
    return settings


@pytest.fixture
def mock_chroma_class():
    with patch("app.stores.vector_store.Chroma") as mock_cls:
        yield mock_cls


@pytest.fixture
def store(settings, mock_chroma_class) -> ChromaVectorStore:
    return ChromaVectorStore(settings=settings, embeddings=FakeEmbeddings())


def test_vector_store_is_abstract():
    with pytest.raises(TypeError):
        VectorStore()


def test_add_document_creates_chunks(store: ChromaVectorStore, mock_chroma_class):
    store.add_document(
        document_id="doc-1",
        title="野泳安全指南",
        category="safety",
        content="野外游泳很危险。请勿单独前往。遇到危险保持冷静。",
    )
    db_instance = mock_chroma_class.return_value
    call_kwargs = db_instance.add_texts.call_args.kwargs
    chunks = call_kwargs["texts"]
    metadatas = call_kwargs["metadatas"]
    ids = call_kwargs["ids"]

    assert len(chunks) >= 1
    assert len(chunks) == len(metadatas) == len(ids)
    assert ids[0].startswith("doc-1_")
    assert metadatas[0]["document_id"] == "doc-1"
    assert metadatas[0]["title"] == "野泳安全指南"
    assert metadatas[0]["category"] == "safety"
    assert "chunk_index" in metadatas[0]


def test_delete_document_removes_chunks(store: ChromaVectorStore, mock_chroma_class):
    store.delete_document("doc-del")
    db_instance = mock_chroma_class.return_value
    db_instance._collection.delete.assert_called_once_with(where={"document_id": "doc-del"})


def test_rebuild_document_replaces_chunks(store: ChromaVectorStore, mock_chroma_class):
    store.rebuild_document(
        document_id="doc-rebuild",
        title="新标题",
        category="emergency",
        content="新内容，关于抽筋处理。",
    )
    db_instance = mock_chroma_class.return_value
    assert db_instance._collection.delete.call_count == 1
    assert db_instance.add_texts.call_count == 1


def test_query_respects_top_k_and_threshold(store: ChromaVectorStore, mock_chroma_class):
    db_instance = mock_chroma_class.return_value
    db_instance.similarity_search_with_score.return_value = [
        (MagicMock(page_content="内容 A", metadata={"title": "A"}), 0.1),
        (MagicMock(page_content="内容 B", metadata={"title": "B"}), 0.5),
        (MagicMock(page_content="内容 C", metadata={"title": "C"}), 0.8),
    ]

    results = store.query("游泳", top_k=3, threshold=0.5)

    db_instance.similarity_search_with_score.assert_called_once_with("游泳", k=3)
    assert len(results) == 1
    assert results[0]["content"] == "内容 A"
    assert results[0]["score"] == pytest.approx(0.9, abs=0.01)


def test_query_returns_expected_fields(store: ChromaVectorStore, mock_chroma_class):
    db_instance = mock_chroma_class.return_value
    db_instance.similarity_search_with_score.return_value = [
        (
            MagicMock(
                page_content="检查返回字段。",
                metadata={
                    "document_id": "doc-fields",
                    "title": "字段检查",
                    "category": "health",
                },
            ),
            0.2,
        )
    ]

    results = store.query("字段", top_k=1, threshold=0.0)

    assert results
    required_fields = {"document_id", "title", "category", "content", "score"}
    assert required_fields.issubset(set(results[0].keys()))
    assert results[0]["document_id"] == "doc-fields"
    assert results[0]["title"] == "字段检查"
    assert results[0]["category"] == "health"


def test_query_sorts_by_score_desc(store: ChromaVectorStore, mock_chroma_class):
    db_instance = mock_chroma_class.return_value
    db_instance.similarity_search_with_score.return_value = [
        (MagicMock(page_content="中", metadata={"title": "中"}), 0.5),
        (MagicMock(page_content="高", metadata={"title": "高"}), 0.1),
        (MagicMock(page_content="低", metadata={"title": "低"}), 0.9),
    ]

    results = store.query("排序", top_k=3, threshold=0.0)
    scores = [r["score"] for r in results]
    assert scores == sorted(scores, reverse=True)


def test_query_returns_empty_on_exception(store: ChromaVectorStore, mock_chroma_class):
    db_instance = mock_chroma_class.return_value
    db_instance.similarity_search_with_score.side_effect = RuntimeError("db error")

    results = store.query("游泳", top_k=3)
    assert results == []


def test_add_document_handles_empty_content(store: ChromaVectorStore, mock_chroma_class):
    store.add_document(
        document_id="doc-empty",
        title="空文档",
        category="safety",
        content="",
    )
    db_instance = mock_chroma_class.return_value
    db_instance.add_texts.assert_not_called()


def test_add_document_propagates_exception(store: ChromaVectorStore, mock_chroma_class):
    db_instance = mock_chroma_class.return_value
    db_instance.add_texts.side_effect = RuntimeError("chroma error")

    with pytest.raises(RuntimeError, match="chroma error"):
        store.add_document(
            document_id="doc-err",
            title="错误文档",
            category="safety",
            content="任意内容。",
        )


def test_delete_document_propagates_exception(store: ChromaVectorStore, mock_chroma_class):
    db_instance = mock_chroma_class.return_value
    db_instance._collection.delete.side_effect = RuntimeError("delete error")

    with pytest.raises(RuntimeError, match="delete error"):
        store.delete_document("doc-err")

