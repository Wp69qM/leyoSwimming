from unittest.mock import AsyncMock, patch

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


TEST_TOKEN = "test-internal-token-for-ai-service-123"


@pytest.fixture
def mock_knowledge_service():
    return AsyncMock()


@pytest.mark.asyncio
async def test_ingest_endpoint(mock_knowledge_service):
    mock_knowledge_service.ingest.return_value = {"document_id": "doc-1", "status": "success"}
    with patch("app.routers.knowledge_router.knowledge_service", mock_knowledge_service):
        transport = ASGITransport(app=app)
        async with AsyncClient(
            transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
        ) as client:
            response = await client.post(
                "/api/ai-assistant/knowledge/ingest",
                json={
                    "documentId": "doc-1",
                    "title": "野泳安全指南",
                    "category": "safety",
                    "content": "不要单独野泳。",
                },
            )

    assert response.status_code == 200
    data = response.json()
    assert data["documentId"] == "doc-1"
    assert data["status"] == "success"
    mock_knowledge_service.ingest.assert_called_once_with(
        document_id="doc-1",
        title="野泳安全指南",
        category="safety",
        content="不要单独野泳。",
    )


@pytest.mark.asyncio
async def test_delete_endpoint(mock_knowledge_service):
    mock_knowledge_service.delete.return_value = {"document_id": "doc-1", "status": "deleted"}
    with patch("app.routers.knowledge_router.knowledge_service", mock_knowledge_service):
        transport = ASGITransport(app=app)
        async with AsyncClient(
            transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
        ) as client:
            response = await client.post(
                "/api/ai-assistant/knowledge/delete",
                json={"documentId": "doc-1"},
            )

    assert response.status_code == 200
    data = response.json()
    assert data["documentId"] == "doc-1"
    assert data["status"] == "deleted"
    mock_knowledge_service.delete.assert_called_once_with(document_id="doc-1")


@pytest.mark.asyncio
async def test_rebuild_endpoint(mock_knowledge_service):
    mock_knowledge_service.rebuild.return_value = {"document_id": "doc-1", "status": "success"}
    with patch("app.routers.knowledge_router.knowledge_service", mock_knowledge_service):
        transport = ASGITransport(app=app)
        async with AsyncClient(
            transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
        ) as client:
            response = await client.post(
                "/api/ai-assistant/knowledge/rebuild",
                json={
                    "documentId": "doc-1",
                    "title": "更新后的标题",
                    "category": "emergency",
                    "content": "更新后的内容。",
                },
            )

    assert response.status_code == 200
    data = response.json()
    assert data["documentId"] == "doc-1"
    assert data["status"] == "success"
    mock_knowledge_service.rebuild.assert_called_once_with(
        document_id="doc-1",
        title="更新后的标题",
        category="emergency",
        content="更新后的内容。",
    )


@pytest.mark.asyncio
async def test_knowledge_endpoints_require_auth():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/api/ai-assistant/knowledge/ingest",
            json={
                "documentId": "doc-1",
                "title": "野泳安全指南",
                "category": "safety",
                "content": "不要单独野泳。",
            },
        )

    assert response.status_code == 401


@pytest.mark.asyncio
async def test_ingest_validates_missing_fields():
    transport = ASGITransport(app=app)
    async with AsyncClient(
        transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
    ) as client:
        response = await client.post(
            "/api/ai-assistant/knowledge/ingest",
            json={"documentId": "doc-1"},
        )

    assert response.status_code == 422
