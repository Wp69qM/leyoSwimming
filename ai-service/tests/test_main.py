import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


TEST_TOKEN = "test-internal-token-for-ai-service-123"


@pytest.mark.asyncio
async def test_list_sessions():
    transport = ASGITransport(app=app)
    async with AsyncClient(
        transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
    ) as client:
        response = await client.post(
            "/api/ai-assistant/session/list",
            json={"user_hash": "u_123", "page": 1, "size": 10},
        )
    assert response.status_code == 200
    data = response.json()
    assert data["items"] == []
    assert data["total"] == 0
    assert data["page"] == 1
    assert data["size"] == 10


@pytest.mark.asyncio
async def test_global_exception_handler(monkeypatch):
    transport = ASGITransport(app=app)

    import app.main as main_module

    original_chat = main_module.chat_service.chat

    async def boom(request):
        raise RuntimeError("boom")

    monkeypatch.setattr(main_module.chat_service, "chat", boom)
    try:
        async with AsyncClient(
            transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
        ) as client:
            response = await client.post(
                "/api/ai-assistant/chat",
                json={"session_id": "sess_123", "message": "hello"},
            )
    finally:
        main_module.chat_service.chat = original_chat

    assert response.status_code == 500
    data = response.json()
    assert data["code"] == "INTERNAL_ERROR"
