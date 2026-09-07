"""MCP 协议测试共享工具。

供 test_server.py / test_recommendation_tools.py 复用：
JSON-RPC 请求构造、响应解析、session manager 生命周期管理。
"""

import json
from contextlib import asynccontextmanager

from httpx import ASGITransport, AsyncClient

from app.main import app

MCP_TOKEN = "test-mcp-token-for-ai-service-45678"
PROTOCOL_VERSION = "2025-06-18"

MCP_HEADERS = {
    "Authorization": f"Bearer {MCP_TOKEN}",
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream",
}


@asynccontextmanager
async def mcp_test_client():
    """进入 MCP session manager 上下文并提供已鉴权的 HTTP 客户端。

    ASGITransport 不触发 FastAPI lifespan，需手动运行 session manager。
    mcp SDK 的 StreamableHTTPSessionManager.run() 限制每实例只能调用一次
    （生产环境 lifespan 全程仅进入一次，不受影响）；测试中每个用例需要
    独立进入/退出，故进入前与退出后均重置 _has_started 标志——进入前重置
    用于免疫同进程中其他测试（如 test_auth.py 的 lifespan 用例）真实启动
    session manager 后的遗留状态。stateless 模式下 run() 退出时 SDK 已
    清理全部内部状态（task group / server instances），重置该标志即可
    安全复用。
    """
    from app.mcp.server import mcp as mcp_server

    sm = mcp_server.session_manager
    try:
        sm._has_started = False  # noqa: SLF001 - 测试专用重置，见 docstring
        async with sm.run():
            transport = ASGITransport(app=app)
            async with AsyncClient(transport=transport, base_url="http://test", headers=MCP_HEADERS) as client:
                yield client
    finally:
        sm._has_started = False  # noqa: SLF001 - 测试专用重置，见 docstring


def parse_jsonrpc_response(response) -> dict:
    """解析 JSON-RPC 响应，兼容 application/json 与 text/event-stream 两种格式。"""
    content_type = response.headers.get("content-type", "")
    if content_type.startswith("text/event-stream"):
        for line in response.text.splitlines():
            if line.startswith("data:"):
                return json.loads(line[len("data:"):].strip())
        raise ValueError(f"SSE 响应中无 data 行: {response.text!r}")
    return response.json()


def jsonrpc_request(request_id: int, method: str, params: dict | None = None) -> dict:
    payload: dict = {"jsonrpc": "2.0", "id": request_id, "method": method}
    if params is not None:
        payload["params"] = params
    return payload


def make_initialize_params() -> dict:
    return {
        "protocolVersion": PROTOCOL_VERSION,
        "capabilities": {},
        "clientInfo": {"name": "pytest-client", "version": "0.1.0"},
    }


async def list_tools(client) -> list[dict]:
    """握手并返回 tools/list 清单。"""
    response = await client.post(
        "/mcp-server/mcp", json=jsonrpc_request(1, "initialize", make_initialize_params())
    )
    assert response.status_code == 200
    await client.post("/mcp-server/mcp", json={"jsonrpc": "2.0", "method": "notifications/initialized"})
    response = await client.post("/mcp-server/mcp", json=jsonrpc_request(2, "tools/list"))
    assert response.status_code == 200
    return parse_jsonrpc_response(response)["result"]["tools"]


async def call_tool(client, request_id: int, name: str, arguments: dict) -> dict:
    """调用 tools/call 并返回 result 部分。"""
    response = await client.post(
        "/mcp-server/mcp",
        json=jsonrpc_request(request_id, "tools/call", {"name": name, "arguments": arguments}),
    )
    assert response.status_code == 200, f"HTTP {response.status_code}: {response.text}"
    response_json = parse_jsonrpc_response(response)
    assert "error" not in response_json, f"JSON-RPC error: {response_json['error']}"
    return response_json["result"]


def tool_result_payload(result: dict) -> dict:
    """tools/call 的 content[0].text 反序列化为 Python 对象。

    工具统一返回单 JSON 块：正常 {"list": [...]}，故障 {"error": "..."}。
    """
    assert result["content"], "工具返回 content 为空"
    assert result.get("isError") is not True, f"工具执行错误: {result}"
    return json.loads(result["content"][0]["text"])
