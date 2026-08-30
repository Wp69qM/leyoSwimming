import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app
from app.middleware.rate_limit import RateLimitMiddleware

TEST_TOKEN = "test-internal-token-for-ai-service-123"

pytestmark = pytest.mark.rate_limit


class FakeRedisPipeline:
    def __init__(self, store: dict):
        self._store = store
        self._commands: list = []

    def incr(self, key: str):
        self._commands.append(("incr", key))
        return self

    def expire(self, key: str, seconds: int):
        self._commands.append(("expire", key, seconds))
        return self

    async def execute(self) -> list:
        results = []
        for cmd in self._commands:
            if cmd[0] == "incr":
                key = cmd[1]
                self._store[key] = self._store.get(key, 0) + 1
                results.append(self._store[key])
            elif cmd[0] == "expire":
                results.append(1)
        return results


class FakeRedis:
    def __init__(self):
        self._store: dict[str, int] = {}

    def pipeline(self):
        return FakeRedisPipeline(self._store)


@pytest.fixture
def fake_rate_limit_redis(monkeypatch):
    instance = FakeRedis()

    async def get_redis(self):
        return instance

    monkeypatch.setattr(RateLimitMiddleware, "_get_redis", get_redis)
    return instance


@pytest.mark.asyncio
async def test_rate_limit_token(fake_rate_limit_redis):
    transport = ASGITransport(app=app)
    async with AsyncClient(
        transport=transport, base_url="http://test", headers={"X-Internal-Token": TEST_TOKEN}
    ) as client:
        for i in range(2):
            response = await client.post("/api/ai-assistant/session/create")
            assert response.status_code == 200, f"request {i} should succeed"

        limited = await client.post("/api/ai-assistant/session/create")
        assert limited.status_code == 429


@pytest.mark.asyncio
async def test_public_endpoint_not_rate_limited(fake_rate_limit_redis):
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        for _ in range(5):
            response = await client.get("/health")
            assert response.status_code == 200


@pytest.mark.asyncio
async def test_is_limited_counts_per_key(fake_rate_limit_redis):
    middleware = RateLimitMiddleware(app=None)
    middleware.settings = type("Settings", (), {"rate_limit_ip_per_minute": 2})()

    assert await middleware._is_limited("key:a", 2) is False
    assert await middleware._is_limited("key:a", 2) is False
    assert await middleware._is_limited("key:a", 2) is True

    assert await middleware._is_limited("key:b", 2) is False
