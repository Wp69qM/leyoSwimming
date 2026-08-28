import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app
from app.services.chat_service import ChatService


TEST_TOKEN = "test-internal-token-for-ai-service-123"


class FakeRedis:
    def __init__(self):
        self._data: dict[str, list[str]] = {}

    async def rpush(self, key: str, value: str) -> None:
        self._data.setdefault(key, []).append(value)

    async def lrange(self, key: str, start: int, end: int) -> list[str]:
        items = self._data.get(key, [])
        if end < 0:
            return items[start:]
        return items[start : end + 1]

    async def ltrim(self, key: str, start: int, end: int) -> None:
        items = self._data.get(key, [])
        py_start = start if start >= 0 else len(items) + start
        py_end = end + 1 if end >= 0 else len(items) + end + 1
        self._data[key] = items[py_start:py_end]

    async def expire(self, key: str, seconds: int) -> None:
        pass


@pytest.fixture
def fake_redis(monkeypatch):
    instance = FakeRedis()

    async def get_redis(self):
        return instance

    monkeypatch.setattr(ChatService, "_get_redis", get_redis)
    return instance


@pytest.mark.asyncio
async def test_create_session(fake_redis):
    transport = ASGITransport(app=app)
    async with AsyncClient(
        transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
    ) as client:
        response = await client.post("/api/ai-assistant/session/create")
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"].startswith("sess_")
    assert "leyo" in data["welcome_message"]


@pytest.mark.asyncio
async def test_session_detail(fake_redis):
    transport = ASGITransport(app=app)
    async with AsyncClient(
        transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
    ) as client:
        create_resp = await client.post("/api/ai-assistant/session/create")
        session_id = create_resp.json()["session_id"]

        detail_resp = await client.post(
            "/api/ai-assistant/session/detail",
            json={"session_id": session_id},
        )
    assert detail_resp.status_code == 200
    data = detail_resp.json()
    assert data["session_id"] == session_id
    assert len(data["messages"]) == 1


@pytest.mark.asyncio
async def test_chat_mock_mode(fake_redis, monkeypatch):
    transport = ASGITransport(app=app)

    async def mock_run_agent(self, tools, messages, session_id, user_hash):
        return (
            "为你推荐王教练，他有 8 年教学经验。追问：\n1. 这个教练能约什么时候？\n2. 一对二课程多少钱？",
            [],
            [],
        )

    monkeypatch.setattr(ChatService, "_run_agent", mock_run_agent)

    async with AsyncClient(
        transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
    ) as client:
        session_resp = await client.post("/api/ai-assistant/session/create")
        session_id = session_resp.json()["session_id"]

        response = await client.post(
            "/api/ai-assistant/chat",
            json={
                "session_id": session_id,
                "message": "推荐一个自由泳教练",
            },
        )
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"] == session_id
    assert data["message_id"].startswith("msg_")
    assert "reply" in data
    assert len(data["reply"]["suggested_questions"]) > 0


@pytest.mark.asyncio
async def test_unauthorized_request():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/api/ai-assistant/session/create")
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


@pytest.mark.asyncio
async def test_invalid_token_request():
    transport = ASGITransport(app=app)
    async with AsyncClient(
        transport=transport,
        base_url="http://test",
        headers={"X-Internal-Token": "wrong-token"},
    ) as client:
        response = await client.post("/api/ai-assistant/session/create")
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"
