import pytest
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage

from app.services.chat_service import ChatService, deterministic_id


@pytest.fixture
def chat_service():
    return ChatService(settings=None)


def test_deterministic_id_is_stable():
    assert deterministic_id("coach:test") == deterministic_id("coach:test")
    assert deterministic_id("coach:test") != deterministic_id("coach:other")


def test_history_to_messages(chat_service):
    history = [
        {"role": "user", "content": "你好"},
        {"role": "assistant", "content": "你好！"},
        {
            "role": "assistant_tool_calls",
            "tool_calls": [{"id": "call_1", "name": "query_coaches", "args": {}}],
        },
        {"role": "tool", "tool_call_id": "call_1", "output": {"result": []}},
    ]
    messages = chat_service._history_to_messages(history)
    assert len(messages) == 4
    assert isinstance(messages[0], HumanMessage)
    assert isinstance(messages[1], AIMessage)
    assert isinstance(messages[2], AIMessage)
    assert isinstance(messages[3], ToolMessage)


def test_parse_suggested_questions_with_fallback(chat_service):
    text = "推荐王教练。"
    main, questions = chat_service._parse_suggested_questions(text)
    assert main == "推荐王教练。"
    assert len(questions) == 3


def test_parse_suggested_questions_extracts_questions(chat_service):
    text = "推荐王教练。追问：\n1. 能约什么时候？\n2. 多少钱？"
    main, questions = chat_service._parse_suggested_questions(text)
    assert main == "推荐王教练。"
    assert questions == ["能约什么时候？", "多少钱？"]


def test_extract_recommendations_coach_and_package(chat_service):
    tool_outputs = [
        [
            {
                "type": "coach",
                "coach_hash": "c_1",
                "name": "王教练",
                "reason": "8 年经验",
                "rating": 4.9,
            },
            {
                "type": "package",
                "package_hash": "p_1",
                "name": "自由泳 10 节",
                "reason": "性价比高",
                "price": 1800,
            },
        ]
    ]
    recommendations = chat_service._extract_recommendations(tool_outputs)
    assert len(recommendations) == 2
    assert recommendations[0].type == "coach"
    assert recommendations[1].type == "package"


@pytest.mark.asyncio
async def test_call_tools_with_mock_tools(chat_service):
    async def fake_tool(args):
        return {"ok": True}

    class FakeTool:
        name = "fake_tool"

        async def ainvoke(self, args):
            return await fake_tool(args)

    tools = [FakeTool()]
    call = type("ToolCall", (), {"name": "fake_tool", "args": {}, "id": "call_1"})()
    tool_messages, records, outputs = await chat_service._call_tools(tools, [call])

    assert len(tool_messages) == 1
    assert len(records) == 1
    assert outputs == [{"ok": True}]


@pytest.mark.asyncio
async def test_call_tools_skips_unknown_tools(chat_service):
    tools = []
    call = type("ToolCall", (), {"name": "missing", "args": {}, "id": "call_1"})()
    tool_messages, records, outputs = await chat_service._call_tools(tools, [call])
    assert tool_messages == records == outputs == []
