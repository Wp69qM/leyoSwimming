import pytest
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
from unittest.mock import AsyncMock, Mock

from app.models.schemas import ChatRequest
from app.services.chat_service import ChatService, deterministic_id


@pytest.fixture
def chat_service():
    return ChatService(settings=None)


def test_deterministic_id_is_stable():
    assert deterministic_id("coach:test") == deterministic_id("coach:test")
    assert deterministic_id("coach:test") != deterministic_id("coach:other")


def test_build_system_message_no_keyerror(chat_service):
    msg = chat_service._build_system_message("游客")
    content = msg.content
    assert "{current_time}" not in content
    assert "{user_identity}" not in content
    assert "游客" in content
    assert "{{source}}" not in content
    assert "{source}" in content
    assert "{title}" in content
    assert "{url}" in content


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


def test_is_knowledge_intent_detects_knowledge_questions(chat_service):
    assert chat_service._is_knowledge_intent("游泳时抽筋怎么办")
    assert chat_service._is_knowledge_intent("自由泳换气技巧")
    assert chat_service._is_knowledge_intent("溺水急救方法")
    assert chat_service._is_knowledge_intent("游泳前需要热身吗")


def test_is_knowledge_intent_ignores_pure_recommendations(chat_service):
    assert not chat_service._is_knowledge_intent("推荐一位教练")
    assert not chat_service._is_knowledge_intent("自由泳套餐多少钱")


def test_stroke_names_are_not_recommendation_intent(chat_service):
    assert not chat_service._is_recommendation_intent("自由泳")
    assert not chat_service._is_recommendation_intent("蛙泳技巧")
    assert chat_service._is_recommendation_intent("推荐自由泳教练")


@pytest.mark.asyncio
async def test_chat_includes_knowledge_context_for_equipment_question(chat_service):
    """当用户询问装备类知识问题时，服务端应主动检索知识库并将结果注入上下文。"""
    fake_results = [
        {
            "content": "新手游泳装备优先级：泳衣/泳裤、泳镜、泳帽。",
            "title": "游泳装备选购指南与品牌推荐",
            "category": "other",
            "score": 0.58,
        }
    ]
    chat_service._knowledge_service = AsyncMock()
    chat_service._knowledge_service.query = AsyncMock(return_value=fake_results)

    captured_messages = []

    async def mock_ainvoke(messages, config=None):
        captured_messages.extend(messages)
        return AIMessage(content="根据知识库内容，新手必备泳衣、泳镜、泳帽。")

    chat_service._llm = Mock()
    chat_service._llm.ainvoke = mock_ainvoke
    chat_service._llm.bind_tools.return_value = AsyncMock()
    chat_service._llm.bind_tools.return_value.ainvoke = mock_ainvoke

    request = ChatRequest(session_id="sess_test_knowledge", message="新手购入游泳装备有推荐的吗")
    response = await chat_service.chat(request)

    assert any("游泳装备选购指南与品牌推荐" in str(msg.content) for msg in captured_messages)
    assert "泳衣" in response.reply.text


@pytest.mark.asyncio
async def test_chat_triggers_both_recommendation_and_knowledge_for_overlapping_intent(
    chat_service,
):
    """当消息同时命中推荐和知识意图时，应同时执行推荐引导和知识引导。"""
    fake_results = [
        {
            "content": "泳衣选购要点",
            "title": "游泳装备选购指南与品牌推荐",
            "category": "other",
            "score": 0.55,
        }
    ]
    chat_service._knowledge_service = AsyncMock()
    chat_service._knowledge_service.query = AsyncMock(return_value=fake_results)

    captured_messages = []

    async def mock_ainvoke(messages, config=None):
        captured_messages.extend(messages)
        return AIMessage(content="已为您找到推荐和知识内容。")

    chat_service._llm = Mock()
    chat_service._llm.ainvoke = mock_ainvoke
    chat_service._llm.bind_tools.return_value = AsyncMock()
    chat_service._llm.bind_tools.return_value.ainvoke = mock_ainvoke

    request = ChatRequest(session_id="sess_test_overlap", message="推荐泳衣")
    response = await chat_service.chat(request)

    # 应同时包含推荐工具结果和知识库结果
    assert any(
        "游泳装备选购指南与品牌推荐" in str(msg.content) for msg in captured_messages
    )
    assert response.reply.text == "已为您找到推荐和知识内容。"


@pytest.mark.asyncio
async def test_chat_knowledge_empty_results_falls_back_to_agent(chat_service):
    """当知识库返回空结果时，应回退到普通 agent 流程，不崩溃。"""
    chat_service._knowledge_service = AsyncMock()
    chat_service._knowledge_service.query = AsyncMock(return_value=[])

    chat_service._llm = Mock()
    chat_service._llm.ainvoke = AsyncMock(
        return_value=AIMessage(content="我暂时没有找到相关内容，换个问法试试。")
    )
    chat_service._llm.bind_tools.return_value = AsyncMock()
    chat_service._llm.bind_tools.return_value.ainvoke = AsyncMock(
        return_value=AIMessage(content="我暂时没有找到相关内容，换个问法试试。")
    )

    request = ChatRequest(session_id="sess_test_empty", message="泳衣泳裤的选购注意事项")
    response = await chat_service.chat(request)

    assert "暂时没有找到" in response.reply.text


@pytest.mark.asyncio
async def test_chat_knowledge_service_failure_falls_back_to_agent(chat_service):
    """当知识库服务异常时，应捕获异常并回退到普通 agent 流程。"""
    chat_service._knowledge_service = AsyncMock()
    chat_service._knowledge_service.query = AsyncMock(side_effect=RuntimeError("vector store down"))

    chat_service._llm = Mock()
    chat_service._llm.ainvoke = AsyncMock(
        return_value=AIMessage(content="知识库暂时不可用，请稍后再试。")
    )
    chat_service._llm.bind_tools.return_value = AsyncMock()
    chat_service._llm.bind_tools.return_value.ainvoke = AsyncMock(
        return_value=AIMessage(content="知识库暂时不可用，请稍后再试。")
    )

    request = ChatRequest(session_id="sess_test_fail", message="泳衣泳裤的选购注意事项")
    response = await chat_service.chat(request)

    assert "知识库暂时不可用" in response.reply.text


@pytest.mark.asyncio
async def test_bootstrap_knowledge_data_uses_web_search_when_knowledge_empty(chat_service):
    """当知识库为空时，_bootstrap_knowledge_data 应调用 web_search 并将结果注入上下文。"""
    web_search_results = [
        {
            "title": "Speedo 新手泳镜推荐",
            "content": "Speedo Mariner Supreme 适合新手，防雾效果好。",
            "url": "https://example.com/speedo",
            "source_type": "web_search",
        }
    ]

    mock_web_search = AsyncMock()
    mock_web_search.name = "web_search"
    mock_web_search.ainvoke = AsyncMock(return_value=web_search_results)

    chat_service._knowledge_service = AsyncMock()
    chat_service._knowledge_service.query = AsyncMock(return_value=[])

    record, messages = await chat_service._bootstrap_knowledge_data(
        query="速比涛新手型号",
        tools=[mock_web_search],
        session_id="sess_test_web_search",
    )

    assert record is not None
    assert record["name"] == "web_search"
    assert record["args"] == {"query": "速比涛新手型号"}
    assert len(record["output"]) == 1
    assert record["output"][0]["source_type"] == "web_search"
    assert any(isinstance(msg, AIMessage) for msg in messages)
    assert any(isinstance(msg, ToolMessage) for msg in messages)
    tool_message = next(msg for msg in messages if isinstance(msg, ToolMessage))
    assert "source_type" in tool_message.content
    assert "Speedo 新手泳镜推荐" in tool_message.content
