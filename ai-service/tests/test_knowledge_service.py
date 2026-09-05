from typing import Any
from unittest.mock import MagicMock

import pytest

from app.services.knowledge_service import KnowledgeService


@pytest.fixture
def mock_store():
    return MagicMock()


@pytest.fixture
def service(mock_store) -> KnowledgeService:
    return KnowledgeService(vector_store=mock_store)


@pytest.mark.asyncio
async def test_ingest_delegates_to_store(service: KnowledgeService, mock_store):
    await service.ingest(
        document_id="doc-1",
        title="野泳安全指南",
        category="safety",
        content="野外游泳很危险。",
    )
    mock_store.add_document.assert_called_once_with(
        document_id="doc-1",
        title="野泳安全指南",
        category="safety",
        content="野外游泳很危险。",
    )


@pytest.mark.asyncio
async def test_delete_delegates_to_store(service: KnowledgeService, mock_store):
    await service.delete(document_id="doc-1")
    mock_store.delete_document.assert_called_once_with("doc-1")


@pytest.mark.asyncio
async def test_rebuild_delegates_to_store(service: KnowledgeService, mock_store):
    await service.rebuild(
        document_id="doc-1",
        title="新标题",
        category="emergency",
        content="新内容。",
    )
    mock_store.rebuild_document.assert_called_once_with(
        document_id="doc-1",
        title="新标题",
        category="emergency",
        content="新内容。",
    )


@pytest.mark.asyncio
async def test_query_delegates_to_store(service: KnowledgeService, mock_store):
    mock_store.query.return_value = [
        {
            "document_id": "doc-1",
            "title": "野泳安全指南",
            "category": "safety",
            "content": "不要单独野泳。",
            "score": 0.85,
        }
    ]
    results = await service.query("野泳安全吗")
    mock_store.query.assert_called_once_with(query="野泳安全吗", top_k=3, threshold=0.2)
    assert len(results) == 1
    assert results[0]["document_id"] == "doc-1"


@pytest.mark.asyncio
async def test_query_allows_custom_top_k_and_threshold(service: KnowledgeService, mock_store):
    mock_store.query.return_value = []
    await service.query("游泳", top_k=5, threshold=0.5)
    mock_store.query.assert_called_once_with(query="游泳", top_k=5, threshold=0.5)


@pytest.mark.asyncio
async def test_ingest_passes_optional_content_type(service: KnowledgeService, mock_store):
    await service.ingest(
        document_id="doc-2",
        title="Markdown 文档",
        category="technique",
        content="# 标题\n内容",
        content_type="markdown",
    )
    mock_store.add_document.assert_called_once()
    call_kwargs = mock_store.add_document.call_args.kwargs
    assert call_kwargs["document_id"] == "doc-2"
    assert "# 标题" in call_kwargs["content"]
