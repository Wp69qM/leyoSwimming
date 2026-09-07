"""MCP 鉴权中间件与放行逻辑测试（US-066 Task 2）。

覆盖：
- 无 token / 错误 token → 401，不进入下游 app
- 有效 Bearer token → 放行
- InternalAuthMiddleware 对 /mcp-server/** 放行（不要求 X-Internal-Token）
- lifespan 启动校验 MCP_API_TOKEN 长度 < 32 时 RuntimeError
"""

from types import SimpleNamespace

import pytest
from httpx import ASGITransport, AsyncClient
from starlette.responses import PlainTextResponse

from app.middleware.auth import InternalAuthMiddleware

MCP_TOKEN = "test-mcp-token-for-ai-service-45678"  # 32+ 位
INTERNAL_TOKEN = "test-internal-token-for-ai-service-123"


def make_settings(mcp_token: str = MCP_TOKEN) -> SimpleNamespace:
    return SimpleNamespace(
        internal_api_token=INTERNAL_TOKEN,
        mcp_api_token=mcp_token,
    )


async def dummy_app(scope, receive, send):
    response = PlainTextResponse("ok")
    await response(scope, receive, send)


@pytest.mark.asyncio
async def test_mcp_request_without_token_rejected_401():
    """US-066 §6.3：无 Authorization header → 401，不执行任何工具逻辑。"""
    from app.mcp.auth import MCPAuthMiddleware

    app = MCPAuthMiddleware(dummy_app, settings=make_settings())
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/mcp-server/mcp")
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


@pytest.mark.asyncio
async def test_mcp_request_with_wrong_token_rejected_401():
    """US-066 §8.1：错误 token → 401。"""
    from app.mcp.auth import MCPAuthMiddleware

    app = MCPAuthMiddleware(dummy_app, settings=make_settings())
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/mcp-server/mcp",
            headers={"Authorization": "Bearer wrong-token"},
        )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_mcp_request_with_valid_token_passes():
    """有效 Bearer token → 放行进入下游 app。"""
    from app.mcp.auth import MCPAuthMiddleware

    app = MCPAuthMiddleware(dummy_app, settings=make_settings())
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/mcp-server/mcp",
            headers={"Authorization": f"Bearer {MCP_TOKEN}"},
        )
    assert response.status_code == 200
    assert response.text == "ok"


@pytest.mark.asyncio
async def test_mcp_request_with_internal_token_only_rejected():
    """INTERNAL_API_TOKEN 与 MCP_API_TOKEN 独立：内部 token 不能通过 MCP 鉴权。"""
    from app.mcp.auth import MCPAuthMiddleware

    app = MCPAuthMiddleware(dummy_app, settings=make_settings())
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post(
            "/mcp-server/mcp",
            headers={"Authorization": f"Bearer {INTERNAL_TOKEN}"},
        )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_internal_auth_middleware_bypasses_mcp_server_paths():
    """InternalAuthMiddleware 放行 /mcp-server/**：无 X-Internal-Token 也应到达下游。"""
    app = InternalAuthMiddleware(dummy_app, settings=make_settings())
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/mcp-server/mcp")
    assert response.status_code == 200
    assert response.text == "ok"


@pytest.mark.asyncio
async def test_internal_auth_middleware_still_guards_ai_assistant_paths():
    """放行 /mcp-server/** 后，/api/ai-assistant/* 仍要求 X-Internal-Token。"""
    app = InternalAuthMiddleware(dummy_app, settings=make_settings())
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/api/ai-assistant/chat")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_lifespan_rejects_short_mcp_token(monkeypatch):
    """MCP_API_TOKEN 长度 < 32 时启动报错（与 INTERNAL_API_TOKEN 同等强度）。"""
    import app.main as main_module

    fake_settings = SimpleNamespace(
        app_name="test",
        app_env="test",
        internal_api_token="x" * 32,
        mcp_api_token="short-token",
    )
    monkeypatch.setattr(main_module, "settings", fake_settings)
    with pytest.raises(RuntimeError, match="MCP_API_TOKEN"):
        async with main_module.lifespan(main_module.app):
            pass


@pytest.mark.asyncio
async def test_lifespan_accepts_valid_mcp_token(monkeypatch):
    """MCP_API_TOKEN 达到 32 位时启动校验通过。"""
    import app.main as main_module

    fake_settings = SimpleNamespace(
        app_name="test",
        app_env="test",
        internal_api_token="x" * 32,
        mcp_api_token=MCP_TOKEN,
    )
    monkeypatch.setattr(main_module, "settings", fake_settings)

    async def _noop_close():
        return None

    monkeypatch.setattr(main_module.chat_service, "close", _noop_close)
    monkeypatch.setattr(main_module, "close_shared_redis", _noop_close)
    async with main_module.lifespan(main_module.app):
        pass
