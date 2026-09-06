from typing import Any
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.tools import knowledge_tools


@pytest.fixture
def mock_knowledge_service():
    return AsyncMock()


@pytest.fixture
def reset_tools():
    """确保测试之间不共享 knowledge_service 单例状态。"""
    original = getattr(knowledge_tools, "_knowledge_service", None)
    yield
    knowledge_tools._knowledge_service = original


@pytest.fixture
def tavily_api_key(monkeypatch):
    """为 web_search 工具提供 Tavily API Key，避免被空配置拦截。"""
    from app.config import Settings

    fake_settings = Settings(tavily_api_key="fake-tavily-key")
    monkeypatch.setattr(knowledge_tools, "get_settings", lambda: fake_settings)


@pytest.mark.asyncio
async def test_query_knowledge_returns_formatted_results(
    mock_knowledge_service, monkeypatch, reset_tools
):
    monkeypatch.setattr(knowledge_tools, "_knowledge_service", mock_knowledge_service)
    mock_knowledge_service.query.return_value = [
        {
            "document_id": "doc-1",
            "title": "野泳安全指南",
            "category": "safety",
            "content": "不要单独野泳。",
            "score": 0.85,
        }
    ]

    tools = knowledge_tools.build_tools()
    query_tool = next(t for t in tools if t.name == "query_knowledge")
    results = await query_tool.ainvoke({"query": "野泳安全吗"})

    assert len(results) == 1
    assert results[0]["content"] == "不要单独野泳。"
    assert results[0]["source"] == "野泳安全指南"
    assert results[0]["source_type"] == "knowledge_base"
    assert results[0]["category"] == "safety"
    assert results[0]["score"] == 0.85


@pytest.mark.asyncio
async def test_query_knowledge_uses_default_top_k(
    mock_knowledge_service, monkeypatch, reset_tools
):
    from app.config import Settings

    fake_settings = Settings(knowledge_similarity_threshold=0.7)
    monkeypatch.setattr(knowledge_tools, "_knowledge_service", mock_knowledge_service)
    monkeypatch.setattr(knowledge_tools, "get_settings", lambda: fake_settings)
    mock_knowledge_service.query.return_value = []

    tools = knowledge_tools.build_tools()
    query_tool = next(t for t in tools if t.name == "query_knowledge")
    await query_tool.ainvoke({"query": "游泳"})

    mock_knowledge_service.query.assert_called_once_with(
        query="游泳", top_k=3, threshold=0.7
    )


@pytest.mark.asyncio
async def test_query_knowledge_accepts_custom_top_k(
    mock_knowledge_service, monkeypatch, reset_tools
):
    from app.config import Settings

    fake_settings = Settings(knowledge_similarity_threshold=0.2)
    monkeypatch.setattr(knowledge_tools, "_knowledge_service", mock_knowledge_service)
    monkeypatch.setattr(knowledge_tools, "get_settings", lambda: fake_settings)
    mock_knowledge_service.query.return_value = []

    tools = knowledge_tools.build_tools()
    query_tool = next(t for t in tools if t.name == "query_knowledge")
    await query_tool.ainvoke({"query": "游泳", "top_k": 5})

    mock_knowledge_service.query.assert_called_once_with(
        query="游泳", top_k=5, threshold=0.2
    )


@pytest.mark.asyncio
async def test_web_search_returns_tavily_results(monkeypatch, tavily_api_key):
    fake_client = MagicMock()
    fake_response = {
        "results": [
            {
                "title": "游泳安全常识",
                "content": "游泳前要充分热身。",
                "url": "https://example.com/swim-safety",
            }
        ]
    }
    fake_client.search.return_value = fake_response

    with patch("app.tools.knowledge_tools.TavilyClient", return_value=fake_client):
        tools = knowledge_tools.build_tools()
        web_tool = next(t for t in tools if t.name == "web_search")
        results = await web_tool.ainvoke({"query": "游泳安全"})

    assert len(results) == 1
    assert results[0]["title"] == "游泳安全常识"
    assert results[0]["content"] == "游泳前要充分热身。"
    assert results[0]["url"] == "https://example.com/swim-safety"
    assert results[0]["source_type"] == "web_search"


@pytest.mark.asyncio
async def test_web_search_gracefully_handles_api_error(monkeypatch, tavily_api_key):
    fake_client = MagicMock()
    fake_client.search.side_effect = RuntimeError("Tavily API error")

    with patch("app.tools.knowledge_tools.TavilyClient", return_value=fake_client):
        tools = knowledge_tools.build_tools()
        web_tool = next(t for t in tools if t.name == "web_search")
        results = await web_tool.ainvoke({"query": "游泳安全"})

    assert results == []


@pytest.mark.asyncio
async def test_web_search_returns_empty_when_no_results(monkeypatch, tavily_api_key):
    fake_client = MagicMock()
    fake_client.search.return_value = {"results": []}

    with patch("app.tools.knowledge_tools.TavilyClient", return_value=fake_client):
        tools = knowledge_tools.build_tools()
        web_tool = next(t for t in tools if t.name == "web_search")
        results = await web_tool.ainvoke({"query": "不存在的关键词"})

    assert results == []


@pytest.mark.asyncio
async def test_web_search_limits_results(monkeypatch, tavily_api_key):
    fake_client = MagicMock()
    fake_client.search.return_value = {
        "results": [
            {"title": f"结果{i}", "content": f"内容{i}", "url": f"https://example.com/{i}"}
            for i in range(5)
        ]
    }

    with patch("app.tools.knowledge_tools.TavilyClient", return_value=fake_client):
        tools = knowledge_tools.build_tools()
        web_tool = next(t for t in tools if t.name == "web_search")
        results = await web_tool.ainvoke({"query": "游泳", "max_results": 2})

    assert len(results) == 2
    fake_client.search.assert_called_once()
    call_args = fake_client.search.call_args.kwargs
    assert call_args["max_results"] == 2


@pytest.mark.asyncio
async def test_web_search_skips_non_dict_results(monkeypatch, tavily_api_key):
    fake_client = MagicMock()
    fake_client.search.return_value = {
        "results": [
            {"title": "有效结果", "content": "内容", "url": "https://example.com/valid"},
            "invalid-item",
        ]
    }

    with patch("app.tools.knowledge_tools.TavilyClient", return_value=fake_client):
        tools = knowledge_tools.build_tools()
        web_tool = next(t for t in tools if t.name == "web_search")
        results = await web_tool.ainvoke({"query": "游泳"})

    assert len(results) == 1
    assert results[0]["title"] == "有效结果"


@pytest.mark.asyncio
async def test_web_search_returns_empty_without_api_key(monkeypatch, reset_tools):
    from app.config import Settings

    fake_settings = Settings(tavily_api_key="")
    monkeypatch.setattr(knowledge_tools, "get_settings", lambda: fake_settings)

    tools = knowledge_tools.build_tools()
    web_tool = next(t for t in tools if t.name == "web_search")
    results = await web_tool.ainvoke({"query": "游泳"})

    assert results == []


@pytest.mark.asyncio
async def test_get_knowledge_service_initializes_singleton(monkeypatch, reset_tools):
    from app.config import Settings

    fake_settings = Settings(tavily_api_key="fake-key")
    monkeypatch.setattr(knowledge_tools, "get_settings", lambda: fake_settings)
    monkeypatch.setattr(knowledge_tools, "_knowledge_service", None)

    service = knowledge_tools._get_knowledge_service()
    assert service is not None
    assert knowledge_tools._knowledge_service is service

    # 再次调用应返回同一实例
    assert knowledge_tools._get_knowledge_service() is service
