import hashlib
import json
import time
import uuid
from typing import Any

import redis.asyncio as redis
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_openai import ChatOpenAI

from app.clients.java_client import JavaInternalClient, generate_message_id
from app.config import Settings, get_settings
from app.models.schemas import ChatReply, ChatRequest, ChatResponse, RecommendationItem
from app.tools import build_tools
from app.utils.logger import get_logger

logger = get_logger(__name__)

DEFAULT_SUGGESTED_QUESTIONS = [
    "这个教练能约什么时候？",
    "有没有更便宜的体验课？",
    "一对二课程多少钱？",
    "儿童可以学自由泳吗？",
]

SYSTEM_PROMPT = """你是 leyo，一位专业、友好的游泳学习助手。你的任务是根据用户的需求，推荐合适的游泳教练和课程套餐。

注意：
1. 如果用户未提供关键信息（年龄、目标泳姿、预算、游泳基础），可以主动询问 1-2 个问题。
2. 推荐时必须调用 query_coaches 或 query_packages 工具获取真实数据，不能编造。
3. 已登录用户可以调用 get_user_profile 和 get_user_packages 做个性化推荐；游客用户只能调用 query_coaches、query_packages、get_hot_recommendations。
4. 如果用户询问自己的套餐、订单、个人资料等，但你是游客模式（没有 user_hash），请友好地引导用户登录。
5. 当标准/体验套餐无法满足用户需求（如课时数、班级规模不匹配）时，调用 query_packages 并设置 package_mode="custom" 推荐自定义套餐；自定义套餐必须给出 coach_id、hours、class_size、price_per_hour、total_price。
6. 回复要简洁、口语化、友好，突出推荐理由，每条推荐理由控制在 30 字以内。
7. 最后可以给出 2-3 个用户可能想继续问的问题，用「追问：」开头并换行列出。

当前时间：{current_time}
用户身份：{user_identity}
"""


def deterministic_id(source: str) -> int:
    return int(hashlib.sha256(source.encode("utf-8")).hexdigest(), 16) % (10**8)


class ChatService:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.client = JavaInternalClient(self.settings)
        self.redis: redis.Redis | None = None
        self._llm: ChatOpenAI | None = None

    @property
    def llm(self) -> ChatOpenAI:
        if self._llm is None:
            self._llm = ChatOpenAI(
                model=self.settings.llm_model,
                base_url=self.settings.llm_base_url,
                api_key=self.settings.llm_api_key,
                temperature=self.settings.llm_temperature,
                max_tokens=self.settings.llm_max_tokens,
                timeout=self.settings.llm_timeout_seconds,
            )
        return self._llm

    async def _get_redis(self) -> redis.Redis:
        if self.redis is None:
            self.redis = redis.from_url(
                self.settings.redis_url,
                decode_responses=True,
                protocol=2,
            )
        return self.redis

    async def close(self) -> None:
        await self.client.close()
        if self.redis is not None:
            await self.redis.close()

    def _session_key(self, session_id: str) -> str:
        return f"ai:session:{session_id}:messages"

    async def _load_messages(self, session_id: str) -> list[dict[str, Any]]:
        try:
            r = await self._get_redis()
            raw = await r.lrange(self._session_key(session_id), -self.settings.session_max_messages, -1)
            return [json.loads(item) for item in raw]
        except Exception as exc:
            logger.error("load_messages_failed", session_id=session_id, error=str(exc))
            return []

    async def _save_message(self, session_id: str, message: dict[str, Any]) -> None:
        try:
            r = await self._get_redis()
            key = self._session_key(session_id)
            await r.rpush(key, json.dumps(message, ensure_ascii=False))
            await r.ltrim(key, -self.settings.session_max_messages, -1)
            await r.expire(key, self.settings.session_message_ttl_seconds)
        except Exception as exc:
            logger.error("save_message_failed", session_id=session_id, message_role=message.get("role"), error=str(exc))

    def _build_system_message(self, user_identity: str) -> SystemMessage:
        return SystemMessage(
            content=SYSTEM_PROMPT.format(
                current_time=time.strftime("%Y-%m-%d %H:%M:%S"),
                user_identity=user_identity,
            )
        )

    def _history_to_messages(self, history: list[dict[str, Any]]) -> list[BaseMessage]:
        messages: list[BaseMessage] = []
        for msg in history[-6:]:
            role = msg.get("role")
            if role == "user":
                messages.append(HumanMessage(content=msg.get("content", "")))
            elif role == "assistant":
                messages.append(AIMessage(content=msg.get("content", "")))
            elif role == "assistant_tool_calls":
                messages.append(AIMessage(content="", tool_calls=msg.get("tool_calls", [])))
            elif role == "tool":
                messages.append(
                    ToolMessage(
                        content=json.dumps(msg.get("output"), ensure_ascii=False),
                        tool_call_id=msg.get("tool_call_id", ""),
                    )
                )
        return messages

    def _parse_suggested_questions(self, text: str) -> tuple[str, list[str]]:
        if "追问：" not in text:
            return text, DEFAULT_SUGGESTED_QUESTIONS[:3]
        parts = text.split("追问：", 1)
        main_text = parts[0].strip()
        questions_text = parts[1].strip()
        questions = [q.strip("-0123456789. \n") for q in questions_text.split("\n") if q.strip()]
        return main_text, questions[:3] or DEFAULT_SUGGESTED_QUESTIONS[:3]

    def _extract_recommendations(self, tool_outputs: list[Any]) -> list[RecommendationItem]:
        recommendations: list[RecommendationItem] = []
        for output in tool_outputs:
            if not isinstance(output, list):
                continue
            for item in output:
                if not isinstance(item, dict):
                    continue
                item_type = item.get("type") or item.get("package_mode")
                if item_type == "coach" or "coach_hash" in item:
                    source_id = item.get("coach_hash") or item.get("name", "")
                    recommendations.append(
                        RecommendationItem(
                            type="coach",
                            id=deterministic_id(f"coach:{source_id}"),
                            name=item.get("name", ""),
                            reason=item.get("reason", ""),
                            avatar_url=item.get("avatar_url"),
                            rating=item.get("rating"),
                            teaching_years=item.get("teaching_years"),
                            reference_price=item.get("reference_price"),
                            strokes=item.get("teaching_strokes") or item.get("strokes"),
                        )
                    )
                elif item_type == "custom" or item_type == "custom_package" or (item.get("package_mode") == "custom"):
                    source_id = item.get("package_hash") or item.get("coach_hash") or item.get("name", "")
                    hours = item.get("hours")
                    price_per_hour = item.get("price_per_hour") or item.get("reference_price")
                    total_price = item.get("total_price")
                    if total_price is None and hours is not None and price_per_hour is not None:
                        total_price = hours * price_per_hour
                    recommendations.append(
                        RecommendationItem(
                            type="custom_package",
                            id=deterministic_id(f"custom_package:{source_id}"),
                            name=item.get("name", ""),
                            reason=item.get("reason", ""),
                            avatar_url=item.get("avatar_url"),
                            hours=hours,
                            price_per_hour=price_per_hour,
                            total_price=total_price,
                            class_size=item.get("class_size") or item.get("teaching_type"),
                            validity_days=item.get("validity_days"),
                            strokes=item.get("strokes") or item.get("teaching_strokes"),
                            coach_id=item.get("coach_id"),
                            coach_name=item.get("coach_name") or item.get("name", ""),
                        )
                    )
                elif item_type == "package" or "package_hash" in item:
                    source_id = item.get("package_hash") or item.get("name", "")
                    recommendations.append(
                        RecommendationItem(
                            type="package",
                            id=deterministic_id(f"package:{source_id}"),
                            name=item.get("name", ""),
                            reason=item.get("reason", ""),
                            price=item.get("price"),
                            hours=item.get("hours"),
                            price_per_hour=item.get("price_per_hour"),
                            class_size=item.get("class_size") or item.get("teaching_type"),
                            validity_days=item.get("validity_days"),
                            strokes=item.get("strokes"),
                        )
                    )
        return recommendations[:5]

    async def _call_tools(
        self,
        tools: list[Any],
        tool_calls: list[Any],
    ) -> tuple[list[ToolMessage], list[dict[str, Any]], list[Any]]:
        tool_messages: list[ToolMessage] = []
        tool_call_records: list[dict[str, Any]] = []
        tool_outputs: list[Any] = []
        tool_map = {tool.name: tool for tool in tools}

        for call in tool_calls:
            name = getattr(call, "name", None)
            args = getattr(call, "args", {})
            call_id = getattr(call, "id", "")
            if not name or name not in tool_map:
                logger.warning("unknown_tool_call", name=name)
                continue

            tool = tool_map[name]
            try:
                output = await tool.ainvoke(args)
            except Exception as exc:
                logger.error("tool_execution_failed", name=name, args=args, error=str(exc))
                output = {"error": f"工具调用失败：{exc}"}

            tool_outputs.append(output)
            tool_call_records.append(
                {
                    "id": call_id or "",
                    "name": name,
                    "args": args,
                    "output": output,
                }
            )
            tool_messages.append(
                ToolMessage(
                    content=json.dumps(output, ensure_ascii=False),
                    tool_call_id=call_id or "",
                )
            )

        return tool_messages, tool_call_records, tool_outputs

    def _is_valid_tool_calls(self, tool_calls: Any) -> bool:
        if not isinstance(tool_calls, list) or not tool_calls:
            return False
        for call in tool_calls:
            name = getattr(call, "name", None)
            call_id = getattr(call, "id", None)
            if not name or not call_id:
                return False
        return True

    async def _run_agent(
        self,
        tools: list[Any],
        messages: list[BaseMessage],
    ) -> tuple[str, list[dict[str, Any]], list[Any]]:
        llm_with_tools = self.llm.bind_tools(tools)

        all_tool_call_records: list[dict[str, Any]] = []
        all_tool_outputs: list[Any] = []
        response = await llm_with_tools.ainvoke(messages)

        max_iterations = 3
        for _ in range(max_iterations):
            if not isinstance(response, AIMessage):
                break

            tool_calls = getattr(response, "tool_calls", None)
            if not self._is_valid_tool_calls(tool_calls):
                logger.warning(
                    "skip_invalid_tool_calls",
                    content=str(response.content)[:200],
                    tool_calls_type=type(tool_calls).__name__,
                )
                break

            messages.append(response)
            tool_messages, records, outputs = await self._call_tools(tools, tool_calls)
            all_tool_call_records.extend(records)
            all_tool_outputs.extend(outputs)
            messages.extend(tool_messages)

            response = await llm_with_tools.ainvoke(messages)

        if not isinstance(response, AIMessage):
            raise RuntimeError("LLM response is not an AIMessage")

        return str(response.content), all_tool_call_records, all_tool_outputs

    async def _prepare_messages(
        self, session_id: str, user_hash: str | None, message: str
    ) -> tuple[list[Any], list[BaseMessage]]:
        user_identity = "已登录用户" if user_hash else "游客"
        tools = build_tools(self.client, user_hash)
        history = await self._load_messages(session_id)
        messages: list[BaseMessage] = [self._build_system_message(user_identity)]
        messages.extend(self._history_to_messages(history))
        messages.append(HumanMessage(content=message))
        return tools, messages

    async def _persist_turn(
        self,
        session_id: str,
        user_message: str,
        tool_call_records: list[dict[str, Any]],
        assistant_text: str,
    ) -> None:
        await self._save_message(session_id, {"role": "user", "content": user_message})
        if tool_call_records:
            await self._save_message(
                session_id,
                {
                    "role": "assistant_tool_calls",
                    "tool_calls": [
                        {
                            "id": record["id"],
                            "name": record["name"],
                            "args": record["args"],
                        }
                        for record in tool_call_records
                    ],
                },
            )
            for record in tool_call_records:
                await self._save_message(
                    session_id,
                    {
                        "role": "tool",
                        "tool_call_id": record["id"],
                        "name": record["name"],
                        "output": record["output"],
                    },
                )
        await self._save_message(session_id, {"role": "assistant", "content": assistant_text})

    def _build_chat_response(
        self,
        session_id: str,
        message_id: str,
        main_text: str,
        recommendations: list[RecommendationItem],
        suggested_questions: list[str],
    ) -> ChatResponse:
        return ChatResponse(
            session_id=session_id,
            message_id=message_id,
            reply=ChatReply(
                text=main_text,
                recommendations=recommendations,
                suggested_questions=suggested_questions,
            ),
        )

    async def chat(self, request: ChatRequest) -> ChatResponse:
        start_time = time.time()
        message_id = generate_message_id()

        logger.info(
            "chat_start",
            session_id=request.session_id,
            message_id=message_id,
            user_hash=request.user_hash,
        )

        tools, messages = await self._prepare_messages(
            request.session_id, request.user_hash, request.message
        )

        try:
            output_text, tool_call_records, tool_outputs = await self._run_agent(tools, messages)
        except Exception as exc:
            logger.error("agent_execution_failed", error=str(exc))
            raise

        main_text, suggested_questions = self._parse_suggested_questions(output_text)
        recommendations = self._extract_recommendations(tool_outputs)

        await self._persist_turn(request.session_id, request.message, tool_call_records, main_text)

        latency_ms = int((time.time() - start_time) * 1000)
        logger.info(
            "chat_end",
            session_id=request.session_id,
            message_id=message_id,
            latency_ms=latency_ms,
            recommendation_count=len(recommendations),
        )

        return self._build_chat_response(
            request.session_id,
            message_id,
            main_text,
            recommendations,
            suggested_questions,
        )

    async def create_session(self) -> dict[str, Any]:
        session_id = f"sess_{uuid.uuid4().hex[:16]}"
        welcome = "你好！我是 leyo，你的游泳学习助手。可以帮你推荐教练、选套餐、查课程。你想了解什么？"
        await self._save_message(session_id, {"role": "assistant", "content": welcome})
        return {
            "session_id": session_id,
            "welcome_message": welcome,
            "suggested_questions": [
                "推荐一位自由泳教练",
                "帮我选个合适的套餐",
                "我想预约一节体验课",
            ],
        }

    async def list_sessions(self, user_hash: str, page: int, size: int) -> dict[str, Any]:
        # MVP 阶段通过 Redis 扫描实现；后续可对接 Java 持久化接口
        return {
            "items": [],
            "total": 0,
            "page": page,
            "size": size,
        }

    async def get_session_detail(self, session_id: str) -> dict[str, Any]:
        messages = await self._load_messages(session_id)
        return {"session_id": session_id, "messages": messages}
