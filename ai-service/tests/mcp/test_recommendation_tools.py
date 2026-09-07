"""MCP 推荐工具测试（US-067 Task 2/3）。

覆盖：
- tools/list 含 3 个推荐工具，且**不含**用户身份类工具（§6.4 安全红线）
- 中文参数归一化（§6.1：stroke="蛙泳"→breaststroke、gender="女"→female）
- package_mode 归一化（§6.2："体验"→experience）
- 未知参数静默降级（§6.5：stroke="狗刨"→None 按无过滤查询）
- limit 截断至 20（§8.1）
- backend 故障返回结构化错误、不影响其他工具（§6.3）
- 统一 {"list": [...]} / {"error": "..."} 返回包装
"""

import pytest

from app.config import get_settings
from tests.mcp.helpers import call_tool, list_tools, mcp_test_client, tool_result_payload

MOCK_COACHES = [
    {"coach_id": 1, "name": "张教练", "gender": "female", "teaching_strokes": ["breaststroke"]},
    {"coach_id": 2, "name": "李教练", "gender": "male", "teaching_strokes": ["freestyle"]},
]
MOCK_PACKAGES = [
    {"package_id": 1, "title": "蛙泳体验课", "package_mode": "experience", "price": 99},
    {"package_id": 2, "title": "自由泳标准课", "package_mode": "standard", "price": 1999},
]
MOCK_HOT = [
    {"type": "coach", "coach_id": 1, "name": "张教练"},
    {"type": "package", "package_id": 2, "title": "自由泳标准课"},
]


class FakeJavaClient:
    """Mock JavaInternalClient，记录调用参数并返回预设结果。"""

    def __init__(self, *, fail: bool = False) -> None:
        self.fail = fail
        self.calls: list[tuple[str, dict]] = []

    async def query_coaches(self, stroke=None, gender=None, min_price=None,
                            max_price=None, max_age=None, class_size=None, limit=5):
        self.calls.append(("query_coaches", {
            "stroke": stroke, "gender": gender, "max_price": max_price,
            "max_age": max_age, "class_size": class_size, "limit": limit,
        }))
        if self.fail:
            raise ConnectionError("backend unreachable")
        return MOCK_COACHES

    async def query_packages(self, stroke=None, package_mode=None, min_price=None,
                             max_price=None, hours=None, limit=5):
        self.calls.append(("query_packages", {
            "stroke": stroke, "package_mode": package_mode,
            "max_price": max_price, "hours": hours, "limit": limit,
        }))
        if self.fail:
            raise ConnectionError("backend unreachable")
        return MOCK_PACKAGES

    async def get_hot_recommendations(self, stroke=None, limit=5):
        self.calls.append(("get_hot_recommendations", {"stroke": stroke, "limit": limit}))
        if self.fail:
            raise ConnectionError("backend unreachable")
        return MOCK_HOT


@pytest.fixture
def fake_java_client(monkeypatch):
    fake = FakeJavaClient()
    monkeypatch.setattr("app.mcp.server._get_java_client", lambda *args, **kwargs: fake)
    return fake


@pytest.mark.asyncio
async def test_tools_list_contains_three_recommendation_tools(fake_java_client):
    """US-067 §6.4 + §4.1：清单含 3 个推荐工具（共 4 个），无用户身份类工具。"""
    async with mcp_test_client() as client:
        tools = await list_tools(client)
    tool_names = {t["name"] for t in tools}
    assert {"query_coaches", "query_packages", "get_hot_recommendations"} <= tool_names
    # 安全红线（US-067 §6.4 / §5-2）：用户身份类工具禁止暴露
    assert "get_user_profile" not in tool_names
    assert "get_user_packages" not in tool_names


@pytest.mark.asyncio
async def test_query_coaches_normalizes_chinese_params(fake_java_client):
    """US-067 §6.1：中文参数归一化（蛙泳→breaststroke、女→female）。"""
    async with mcp_test_client() as client:
        result = await call_tool(
            client, 1, "query_coaches", {"stroke": "蛙泳", "gender": "女"}
        )
        payload = tool_result_payload(result)

    assert len(fake_java_client.calls) == 1
    method, kwargs = fake_java_client.calls[0]
    assert method == "query_coaches"
    assert kwargs["stroke"] == "breaststroke"
    assert kwargs["gender"] == "female"
    assert payload["list"] == MOCK_COACHES


@pytest.mark.asyncio
async def test_query_coaches_unknown_stroke_degrades_to_none(fake_java_client):
    """US-067 §6.5：未知泳姿归一化为 None，按无过滤条件查询（静默降级）。"""
    async with mcp_test_client() as client:
        result = await call_tool(client, 1, "query_coaches", {"stroke": "狗刨"})
        tool_result_payload(result)

    _, kwargs = fake_java_client.calls[0]
    assert kwargs["stroke"] is None


@pytest.mark.asyncio
async def test_query_coaches_limit_truncated_to_max(fake_java_client):
    """US-067 §8.1：limit=1000 截断至 MCP_LIMIT_MAX（默认 20）。"""
    async with mcp_test_client() as client:
        result = await call_tool(client, 1, "query_coaches", {"limit": 1000})
        tool_result_payload(result)

    _, kwargs = fake_java_client.calls[0]
    assert kwargs["limit"] == 20


@pytest.mark.asyncio
async def test_query_packages_normalizes_package_mode(fake_java_client):
    """US-067 §6.2：package_mode="体验" 归一化为 experience，stroke="自由泳"→freestyle。"""
    async with mcp_test_client() as client:
        result = await call_tool(
            client, 1, "query_packages", {"stroke": "自由泳", "package_mode": "体验"}
        )
        payload = tool_result_payload(result)

    method, kwargs = fake_java_client.calls[0]
    assert method == "query_packages"
    assert kwargs["stroke"] == "freestyle"
    assert kwargs["package_mode"] == "experience"
    assert payload["list"] == MOCK_PACKAGES


@pytest.mark.asyncio
async def test_get_hot_recommendations_normalizes_and_returns_list(fake_java_client):
    """US-067 §6.2：热门推荐可用，limit 正常传递。"""
    async with mcp_test_client() as client:
        result = await call_tool(
            client, 1, "get_hot_recommendations", {"stroke": "蛙泳", "limit": 5}
        )
        payload = tool_result_payload(result)

    method, kwargs = fake_java_client.calls[0]
    assert method == "get_hot_recommendations"
    assert kwargs["stroke"] == "breaststroke"
    assert kwargs["limit"] == 5
    assert payload["list"] == MOCK_HOT


@pytest.mark.asyncio
async def test_backend_failure_returns_structured_error(fake_java_client):
    """US-067 §6.3 / Task 3：backend 不可达 → 结构化错误，工具不抛异常。"""
    fake_java_client.fail = True
    async with mcp_test_client() as client:
        result = await call_tool(client, 1, "query_coaches", {"stroke": "蛙泳"})
        payload = tool_result_payload(result)

    assert "error" in payload
    assert payload["error"]
    # MCP 层面非协议错误（isError=False，连接保持）
    assert result.get("isError") is not True


@pytest.mark.asyncio
async def test_backend_failure_does_not_affect_knowledge_tool(fake_java_client, monkeypatch):
    """US-067 §6.3 / Task 3：backend 停止时推荐工具报错，query_knowledge 不受影响。"""
    from tests.mcp.test_server import FakeKnowledgeService

    knowledge_service = FakeKnowledgeService(
        results=[{"content": "抽筋时仰漂呼救。", "title": "急救手册", "category": "emergency", "score": 0.9}]
    )
    monkeypatch.setattr("app.mcp.server._get_knowledge_service", lambda *a, **kw: knowledge_service)

    async with mcp_test_client() as client:
        # 先触发 backend 故障
        await call_tool(client, 1, "query_packages", {"stroke": "蛙泳"})
        # 再调用知识工具：应正常返回
        result = await call_tool(client, 2, "query_knowledge", {"query": "抽筋怎么办"})
        payload = tool_result_payload(result)

    assert payload["list"][0]["source"] == "急救手册"


@pytest.mark.asyncio
async def test_all_recommendation_tools_share_limit_max(fake_java_client):
    """三个推荐工具的 limit 截断口径一致（MCP_LIMIT_MAX）。"""
    limit_max = get_settings().mcp_limit_max
    async with mcp_test_client() as client:
        await call_tool(client, 1, "query_packages", {"limit": 999})
        await call_tool(client, 2, "get_hot_recommendations", {"limit": 999})

    for method, kwargs in fake_java_client.calls:
        assert kwargs["limit"] == limit_max, f"{method} limit 未按 MCP_LIMIT_MAX 截断"


@pytest.mark.asyncio
async def test_concurrent_tool_calls_independent(fake_java_client):
    """US-067 §8.3：并发调用多个推荐工具，无共享可变状态，各自独立返回。"""
    import asyncio

    async with mcp_test_client() as client:
        results = await asyncio.gather(
            call_tool(client, 1, "query_coaches", {"stroke": "蛙泳"}),
            call_tool(client, 2, "query_packages", {"package_mode": "体验"}),
            call_tool(client, 3, "get_hot_recommendations", {}),
            call_tool(client, 4, "query_coaches", {"gender": "女"}),
        )

    payloads = [tool_result_payload(r) for r in results]
    assert payloads[0]["list"] == MOCK_COACHES
    assert payloads[1]["list"] == MOCK_PACKAGES
    assert payloads[2]["list"] == MOCK_HOT
    assert payloads[3]["list"] == MOCK_COACHES

    assert len(fake_java_client.calls) == 4
    methods = {method for method, _ in fake_java_client.calls}
    assert methods == {"query_coaches", "query_packages", "get_hot_recommendations"}
