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


def test_extract_recommendations_custom_package(chat_service):
    tool_outputs = [
        [
            {
                "package_mode": "custom",
                "package_hash": "cp_1",
                "coach_id": 1,
                "coach_name": "王教练",
                "name": "王教练",
                "reason": "可定制课时",
                "hours": 12,
                "price_per_hour": 200,
                "total_price": 2400,
                "class_size": "一对一",
                "strokes": ["freestyle"],
            }
        ]
    ]
    recommendations = chat_service._extract_recommendations(tool_outputs)
    assert len(recommendations) == 1
    assert recommendations[0].type == "custom_package"
    assert recommendations[0].coach_id == 1
    assert recommendations[0].total_price == 2400


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


def test_extract_hours_and_class_size(chat_service):
    assert chat_service._extract_hours("我想买7节课") == 7
    assert chat_service._extract_hours("10节自由泳") == 10
    assert chat_service._extract_hours("多少钱") is None
    assert chat_service._extract_class_size("一对二") == "一对二"
    assert chat_service._extract_class_size("1对1") == "一对一"
    assert chat_service._extract_class_size("自由泳") is None


def test_build_recommendations_exact_package_match(chat_service):
    tool_outputs = [
        [
            {
                "package_hash": "p_10ab01",
                "package_mode": "standard",
                "name": "成人自由泳 10 节私教",
                "hours": 10,
                "price": 1800,
                "class_size": "一对一",
                "strokes": ["freestyle"],
            }
        ]
    ]
    recs = chat_service._build_recommendations(
        tool_outputs, focus="package", stroke="自由泳", hours=10, class_size="一对一", max_price=None
    )
    assert len(recs) == 1
    assert recs[0].type == "package"
    assert recs[0].hours == 10
    assert recs[0].class_size == "一对一"


def test_build_recommendations_generates_custom_package_when_no_exact_match(chat_service):
    tool_outputs = [
        [
            {
                "package_hash": "p_10ab01",
                "package_mode": "standard",
                "name": "成人自由泳 10 节私教",
                "hours": 10,
                "price": 1800,
                "class_size": "一对一",
                "strokes": ["freestyle"],
            }
        ],
        [
            {
                "coach_hash": "c_7a8f22",
                "name": "李教练",
                "gender": "male",
                "reference_price": 180,
                "teaching_strokes": ["freestyle", "backstroke"],
            }
        ],
    ]
    recs = chat_service._build_recommendations(
        tool_outputs, focus="package", stroke="自由泳", hours=7, class_size="一对二", max_price=None
    )
    assert any(r.type == "custom_package" for r in recs)
    custom = next(r for r in recs if r.type == "custom_package")
    assert custom.hours == 7
    assert custom.class_size == "一对二"
    assert custom.total_price == 180 * 7


def test_build_recommendations_coach_focus(chat_service):
    tool_outputs = [
        [
            {"coach_hash": "c_1", "name": "王教练", "gender": "female"},
            {"coach_hash": "c_2", "name": "李教练", "gender": "male"},
        ]
    ]
    recs = chat_service._build_recommendations(
        tool_outputs, focus="coach", stroke=None, hours=None, class_size=None, max_price=None
    )
    assert len(recs) == 2
    assert all(r.type == "coach" for r in recs)
