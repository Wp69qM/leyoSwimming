"""FastMCP 挂载与 query_knowledge 工具测试（US-066 Task 3）。

通过 httpx 直调 MCP Streamable HTTP（JSON-RPC）协议：
- initialize + tools/list 握手，清单含 query_knowledge（US-066 §6.1）
- query_knowledge 返回结构化片段 content/source/source_type/category/score（US-066 §6.2）
- 无匹配返回空 list（US-066 §6.4）
- top_k=100 无条件截断为 10（US-066 §8.1，REQ-005）
"""

import pytest

from app.config import get_settings
from tests.mcp.helpers import call_tool, list_tools, mcp_test_client, tool_result_payload


class FakeKnowledgeService:
    """Mock KnowledgeService，记录调用参数并返回预设结果。"""

    def __init__(self, results: list[dict]) -> None:
        self.results = results
        self.calls: list[dict] = []

    async def query(self, query: str, top_k: int, threshold: float) -> list[dict]:
        self.calls.append({"query": query, "top_k": top_k, "threshold": threshold})
        return self.results


@pytest.fixture
def fake_knowledge_service(monkeypatch):
    """替换 server.py 引用的 KnowledgeService 工厂，返回 mock 实例。"""
    fake = FakeKnowledgeService(
        results=[
            {
                "content": "野泳前必须结伴，并选择有救生员的正规场所。",
                "title": "野泳安全指南",
                "category": "safety",
                "score": 0.92,
            },
            {
                "content": "抽筋时应保持冷静，仰漂并呼叫救援。",
                "title": "急救手册",
                "category": "emergency",
                "score": 0.85,
            },
        ]
    )
    monkeypatch.setattr("app.mcp.server._get_knowledge_service", lambda *args, **kwargs: fake)
    return fake


@pytest.mark.asyncio
async def test_initialize_and_tools_list_contains_query_knowledge(fake_knowledge_service):
    """US-066 §6.1：握手成功且工具清单包含 query_knowledge。"""
    async with mcp_test_client() as client:
        tools = await list_tools(client)
        tool_names = [t["name"] for t in tools]
        assert "query_knowledge" in tool_names
        query_knowledge = next(t for t in tools if t["name"] == "query_knowledge")
        assert "游泳" in query_knowledge["description"]
        assert "query" in query_knowledge["inputSchema"]["properties"]


@pytest.mark.asyncio
async def test_query_knowledge_returns_structured_fragments(fake_knowledge_service):
    """US-066 §6.2：返回结构化片段（content/source/source_type/category/score）。"""
    async with mcp_test_client() as client:
        result = await call_tool(
            client, 1, "query_knowledge", {"query": "野泳的注意事项", "top_k": 2}
        )
        payload = tool_result_payload(result)
        fragments = payload["list"]
        assert len(fragments) == 2
        for fragment in fragments:
            assert set(fragment.keys()) == {"content", "source", "source_type", "category", "score"}
            assert fragment["source_type"] == "knowledge_base"
        assert fragments[0]["source"] == "野泳安全指南"


@pytest.mark.asyncio
async def test_query_knowledge_returns_empty_array_on_no_match(monkeypatch):
    """US-066 §6.4：知识库无匹配时返回空 list。"""
    empty_service = FakeKnowledgeService(results=[])
    monkeypatch.setattr("app.mcp.server._get_knowledge_service", lambda *a, **kw: empty_service)

    async with mcp_test_client() as client:
        result = await call_tool(client, 1, "query_knowledge", {"query": "无关问题"})
        payload = tool_result_payload(result)
        assert payload["list"] == []


@pytest.mark.asyncio
async def test_query_knowledge_top_k_truncated_to_max(fake_knowledge_service):
    """US-066 §8.1 / REQ-005：top_k=100 无条件截断为 MCP_TOP_K_MAX（默认 10）。"""
    async with mcp_test_client() as client:
        result = await call_tool(client, 1, "query_knowledge", {"query": "游泳安全", "top_k": 100})
        tool_result_payload(result)

    assert len(fake_knowledge_service.calls) == 1
    call = fake_knowledge_service.calls[0]
    assert call["top_k"] == 10
    assert call["threshold"] == get_settings().knowledge_similarity_threshold


@pytest.mark.asyncio
async def test_query_knowledge_reuses_threshold_from_settings(fake_knowledge_service):
    """阈值复用 KNOWLEDGE_SIMILARITY_THRESHOLD，两条协议链路口径一致。"""
    async with mcp_test_client() as client:
        await call_tool(client, 1, "query_knowledge", {"query": "抽筋怎么办"})
    call = fake_knowledge_service.calls[0]
    assert call["top_k"] == 3  # 默认 top_k
    assert call["threshold"] == get_settings().knowledge_similarity_threshold
