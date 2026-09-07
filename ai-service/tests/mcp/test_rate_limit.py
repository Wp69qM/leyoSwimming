"""MCP 路径限流覆盖测试（US-066 Task 4 / §8.2）。

RateLimitMiddleware 需覆盖 /mcp-server/**：
- 同一 IP 高频调用 MCP 路径 → 429（US-066 §8.2）
- MCP 路径仅 IP 维度限流（X-Internal-Token token 维度不适用，MCP 鉴权独立）
- 既有 /api/ai-assistant/ 限流行为零回归（tests/test_rate_limit.py 覆盖）

注：conftest 的 autouse fixture 会 noop _is_limited，需标记 @pytest.mark.rate_limit
并 monkeypatch _get_redis 使用 FakeRedis。
"""

import pytest
from httpx import ASGITransport, AsyncClient

from app.config import get_settings
from app.main import app
from app.middleware.rate_limit import RateLimitMiddleware

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
async def test_mcp_path_exceeds_ip_limit_returns_429(fake_rate_limit_redis):
    """US-066 §8.2：同一 IP 高频调用 MCP 路径 → 429。

    请求不携带 Authorization（放行限流层后由 MCP 鉴权层返回 401），
    验证 IP 维度计数覆盖 /mcp-server/**。
    """
    ip_limit = get_settings().rate_limit_ip_per_minute
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        for i in range(ip_limit):
            response = await client.post("/mcp-server/mcp")
            # 限流放行 → 到达 MCP 鉴权层被拒（无 Bearer token）
            assert response.status_code == 401, f"request {i} should pass rate limit"

        limited = await client.post("/mcp-server/mcp")
        assert limited.status_code == 429
        assert limited.json()["code"] == "RATE_LIMITED"


@pytest.mark.asyncio
async def test_mcp_path_ip_dimension_only(fake_rate_limit_redis):
    """MCP 路径仅 IP 维度：携带 X-Internal-Token 不触发 token 维度限流。

    token 维度阈值（2）低于 IP 维度（3）；若 token 维度误作用于 MCP 路径，
    第 3 次请求会 429。此处第 3 次应仍为 401（仅 IP 维度计数）。
    """
    token_limit = get_settings().rate_limit_user_per_minute
    ip_limit = get_settings().rate_limit_ip_per_minute
    assert token_limit < ip_limit, "测试前提：token 维度阈值低于 IP 维度"

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        for i in range(token_limit + 1):
            response = await client.post(
                "/mcp-server/mcp",
                headers={"X-Internal-Token": "some-internal-token"},
            )
            assert response.status_code == 401, f"request {i} should not hit token-dimension limit"

    # token 维度 key 不应出现在计数存储中
    stored_keys = list(fake_rate_limit_redis._store.keys())
    assert all(":token:" not in key for key in stored_keys), stored_keys
