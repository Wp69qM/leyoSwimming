import hashlib
import json
import re
import time
import uuid
from typing import Any

import redis.asyncio as redis
import structlog.contextvars
from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_openai import ChatOpenAI

from app.clients.java_client import JavaInternalClient, generate_message_id
from app.config import Settings, get_settings
from app.models.schemas import ChatReply, ChatRequest, ChatResponse, RecommendationItem
from app.services.knowledge_service import KnowledgeService
from app.tools import build_tools
from app.tools.recommendation_tools import normalize_stroke
from app.utils.logger import get_logger

logger = get_logger(__name__)

DEFAULT_SUGGESTED_QUESTIONS = [
    "这个教练能约什么时候？",
    "有没有更便宜的体验课？",
    "一对二课程多少钱？",
    "儿童可以学自由泳吗？",
]

RECOMMENDATION_KEYWORDS = [
    "推荐",
    "教练",
    "老师",
    "私教",
    "课程",
    "套餐",
    "体验课",
    "学游泳",
    "课",
    "节",
    "价格",
    "多少钱",
    "价位",
    "价",
    "钱",
]

KNOWLEDGE_KEYWORDS = {
    "安全",
    "危险",
    "事故",
    "抽筋",
    "呛水",
    "溺水",
    "淹",
    "急救",
    "心肺复苏",
    "CPR",
    "AED",
    "热身",
    "拉伸",
    "放松",
    "恢复",
    "换气",
    "呼吸",
    "憋气",
    "姿势",
    "动作",
    "技巧",
    "要领",
    "训练",
    "练习",
    "计划",
    "健康",
    "卫生",
    "水质",
    "装备",
    "泳镜",
    "泳帽",
    "泳衣",
    "耳塞",
    "鼻夹",
    "耳朵",
    "眼睛",
    "皮肤",
    "氯",
    "防晒",
    "饮食",
    "营养",
    "空腹",
    "饭后",
    "生病",
    "感冒",
    "伤口",
    "生理期",
    "孕妇",
    "儿童",
    "老人",
    "初学者",
    "新手",
    "自学",
}

SYSTEM_PROMPT = """你是 leyo，一位专业、友好的游泳学习助手。你的任务是根据用户的需求，推荐合适的游泳教练和课程套餐，并回答游泳安全、技巧、急救、健康等知识类问题。

重要规则：
1. 当用户提到任何与教练、课程、套餐、泳姿相关的需求时，你必须先调用工具获取真实数据，不能仅凭猜测回复。
2. 仔细判断用户意图：
   - 用户明确提到"教练""老师""私教"时，只调用 query_coaches 推荐教练。
   - 用户明确提到"套餐""课程""体验课""多少钱""课时"时，只调用 query_packages 推荐套餐。
   - 用户提到预算（如"1000元左右"）时，必须将 max_price 传给对应工具，只返回不超过预算的结果。
   - 用户只提到泳姿（如"推荐自由泳"）但没有明确教练或套餐时，可以同时调用 query_coaches 和 query_packages。
   - 用户只说"推荐"或"热门"时，调用 get_hot_recommendations。
3. 当用户询问游泳安全、技巧、急救、健康等知识类问题时，优先调用 query_knowledge 从知识库检索答案；如果知识库返回的结果为空，则调用 web_search 进行联网搜索兜底。
4. 只有用户明确在闲聊、打招呼、或询问与游泳无关的问题时，才可以不调用工具直接回复。
5. 推荐时必须调用工具获取真实数据，不能编造；回答知识类问题时必须基于 query_knowledge 或 web_search 返回的结果，并引用来源。
6. 已登录用户可以调用 get_user_profile 和 get_user_packages 做个性化推荐；游客用户只能调用 query_coaches、query_packages、get_hot_recommendations。
7. 如果用户询问自己的套餐、订单、个人资料等，但你是游客模式（没有 user_hash），请友好地引导用户登录。
8. 当标准/体验套餐无法满足用户需求（如课时数、班级规模不匹配）时，必须推荐自定义套餐（custom_package）；自定义套餐必须给出 coach_id、hours、class_size、price_per_hour、total_price，且 total_price = price_per_hour * hours。
9. 回复要简洁、口语化、友好，突出推荐理由，每条推荐理由控制在 30 字以内。
10. 不要每次推荐都列出相同的教练或套餐，优先根据用户的具体需求（预算、泳姿、课时、班级规模、教练描述、套餐描述）筛选最匹配的项。
11. 你收到的工具结果中已经包含教练的 description（个人介绍/擅长方向）和套餐的 description（课程介绍/适合人群），请重点参考这些描述来判断是否匹配用户需求，并在推荐理由中体现关键信息。
12. 你生成的文字回复中提到的教练或套餐，必须和最终返回的 recommendations 列表完全一致：不能提到列表里没有的项，也不能漏掉列表里要展示的关键项。介绍顺序必须严格按照 recommendations 列表从上到下，不要重新排序。
13. 如果推荐项是 custom_package（自定义套餐），请在理由中引用教练 description 里的核心优势，让用户感受到推荐的针对性。
14. 回答知识类问题时，必须根据工具结果中的 `source_type` 字段判断答案来源：
    - 当 `source_type` 为 `knowledge_base` 时，回复开头必须写"以下内容来自知识库《{{source}}》"，其中 `{{source}}` 替换为工具结果中的 `source` 字段值；若有多条结果，开头写"以下内容来自知识库"，并在末尾按"1. 《{{source}}》"的格式列出所有 `source` 字段值。
    - 当 `source_type` 为 `web_search` 时，回复开头必须写"以下内容来自网络，仅供参考"，并在正文中或末尾标注每条结果的标题和链接（格式："{{title}}：{{url}}"）。
    - 若同时存在知识库和网络搜索结果，先说明知识库来源，再说明网络来源。
15. 最后可以给出 2-3 个用户可能想继续问的问题，用「追问：」开头并换行列出。
16. 如果对话中已经包含 query_knowledge 或 web_search 的工具调用结果（由系统或工具消息提供），请直接基于这些结果回答，不要再调用 query_knowledge 或 web_search 工具。

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
        self._knowledge_service: KnowledgeService | None = None

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

    @property
    def knowledge_service(self) -> KnowledgeService:
        if self._knowledge_service is None:
            self._knowledge_service = KnowledgeService(settings=self.settings)
        return self._knowledge_service

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
        # 跳过没有对应 assistant_tool_calls 的孤立 tool 消息（历史数据可能损坏）
        after_tool_calls = False
        for msg in history[-self.settings.session_max_messages :]:
            role = msg.get("role")
            if role == "user":
                messages.append(HumanMessage(content=msg.get("content", "")))
                after_tool_calls = False
            elif role == "assistant":
                messages.append(AIMessage(content=msg.get("content", "")))
                after_tool_calls = False
            elif role == "assistant_tool_calls":
                messages.append(AIMessage(content="", tool_calls=msg.get("tool_calls", [])))
                after_tool_calls = True
            elif role == "tool":
                if after_tool_calls:
                    messages.append(
                        ToolMessage(
                            content=json.dumps(msg.get("output"), ensure_ascii=False),
                            tool_call_id=msg.get("tool_call_id", ""),
                        )
                    )
                else:
                    logger.warning("skip_orphan_tool_message", tool_call_id=msg.get("tool_call_id"))
        return messages

    def _parse_suggested_questions(self, text: str) -> tuple[str, list[str]]:
        if "追问：" not in text:
            return text, DEFAULT_SUGGESTED_QUESTIONS[:3]
        parts = text.split("追问：", 1)
        main_text = parts[0].strip()
        questions_text = parts[1].strip()
        questions = [q.strip("-0123456789. \n") for q in questions_text.split("\n") if q.strip()]
        return main_text, questions[:3] or DEFAULT_SUGGESTED_QUESTIONS[:3]

    def _build_default_reason(self, item: RecommendationItem) -> str:
        parts: list[str] = []
        if item.type == "coach":
            if item.teaching_years:
                parts.append(f"{item.teaching_years} 年教学经验")
            if item.rating:
                parts.append(f"评分 {item.rating}")
            if item.reference_price:
                parts.append(f"参考价 ¥{item.reference_price}/课时")
            if item.strokes:
                parts.append(f"擅长 {', '.join(item.strokes[:3])}")
            if not parts:
                return "推荐该教练"
        elif item.type == "package":
            if item.price is not None and item.hours:
                avg = item.price // item.hours if item.hours else None
                if avg:
                    parts.append(f"{item.hours} 课时 ¥{item.price}，均价 ¥{avg}/课时")
                else:
                    parts.append(f"{item.hours} 课时 ¥{item.price}")
            elif item.price is not None:
                parts.append(f"总价 ¥{item.price}")
            elif item.hours:
                parts.append(f"共 {item.hours} 课时")
            if item.validity_days:
                parts.append(f"有效期 {item.validity_days} 天")
            if item.class_size:
                parts.append(f"{item.class_size}")
            if item.strokes:
                parts.append(f"适合 {', '.join(item.strokes[:3])}")
            if not parts:
                return "推荐该套餐"
        elif item.type == "custom_package":
            if item.description:
                return item.description
            if item.coach_name:
                parts.append(f"{item.coach_name} 定制")
            if item.hours:
                parts.append(f"{item.hours} 课时")
            if item.total_price is not None:
                parts.append(f"预估 ¥{item.total_price}")
            elif item.price_per_hour and item.hours:
                parts.append(f"预估 ¥{item.price_per_hour * item.hours}")
            if item.class_size:
                parts.append(f"{item.class_size}")
            if not parts:
                return "推荐该定制方案"
        return "，".join(parts)

    def _extract_recommendations(self, tool_outputs: list[Any]) -> list[RecommendationItem]:
        recommendations: list[RecommendationItem] = []
        for output in tool_outputs:
            if not isinstance(output, list):
                continue
            for item in output:
                if not isinstance(item, dict):
                    continue
                item_type = item.get("type") or item.get("package_mode")
                if item_type == "custom" or item_type == "custom_package" or item.get("package_mode") == "custom":
                    package_id = item.get("package_id")
                    coach_id = item.get("coach_id")
                    source_id = package_id or item.get("package_hash") or coach_id or item.get("name", "")
                    hours = item.get("hours")
                    price_per_hour = item.get("price_per_hour") or item.get("reference_price")
                    total_price = item.get("total_price")
                    if total_price is None and hours is not None and price_per_hour is not None:
                        total_price = hours * price_per_hour
                    recommendations.append(
                        RecommendationItem(
                            type="custom_package",
                            id=package_id if package_id is not None else deterministic_id(f"custom_package:{source_id}"),
                            name=item.get("name", ""),
                            reason=item.get("reason", ""),
                            avatar_url=item.get("avatar_url"),
                            hours=hours,
                            price_per_hour=price_per_hour,
                            total_price=total_price,
                            class_size=item.get("class_size") or item.get("teaching_type"),
                            validity_days=item.get("validity_days"),
                            strokes=item.get("strokes") or item.get("teaching_strokes"),
                            coach_id=coach_id,
                            coach_name=item.get("coach_name") or item.get("name", ""),
                            description=item.get("description"),
                        )
                    )
                elif item_type == "coach" or "coach_hash" in item or "coach_id" in item:
                    coach_id = item.get("coach_id")
                    source_id = item.get("coach_hash") or coach_id or item.get("name", "")
                    recommendations.append(
                        RecommendationItem(
                            type="coach",
                            id=coach_id if coach_id is not None else deterministic_id(f"coach:{source_id}"),
                            coach_id=coach_id,
                            name=item.get("name", ""),
                            reason=item.get("reason", ""),
                            avatar_url=item.get("avatar_url"),
                            rating=item.get("rating"),
                            teaching_years=item.get("teaching_years"),
                            reference_price=item.get("reference_price"),
                            strokes=item.get("teaching_strokes") or item.get("strokes"),
                            description=item.get("description"),
                        )
                    )
                elif item_type == "package" or "package_hash" in item or "package_id" in item:
                    package_id = item.get("package_id")
                    source_id = item.get("package_hash") or package_id or item.get("name", "")
                    recommendations.append(
                        RecommendationItem(
                            type="package",
                            id=package_id if package_id is not None else deterministic_id(f"package:{source_id}"),
                            name=item.get("name", ""),
                            reason=item.get("reason", ""),
                            price=item.get("price"),
                            hours=item.get("hours"),
                            price_per_hour=item.get("price_per_hour"),
                            class_size=item.get("class_size") or item.get("teaching_type"),
                            validity_days=item.get("validity_days"),
                            strokes=item.get("strokes"),
                            description=item.get("description"),
                        )
                    )
        for rec in recommendations:
            if not rec.reason:
                rec.reason = self._build_default_reason(rec)
        if not recommendations:
            return recommendations

        # 对模糊推荐做类型均衡：避免只返回单一类型
        coach_items = [r for r in recommendations if r.type == "coach"]
        package_items = [r for r in recommendations if r.type in {"package", "custom_package"}]

        if coach_items and package_items:
            mixed: list[RecommendationItem] = []
            coach_iter = iter(coach_items)
            package_iter = iter(package_items)
            while len(mixed) < 5:
                added = False
                if len(mixed) % 2 == 0:
                    for item in coach_iter:
                        mixed.append(item)
                        added = True
                        break
                else:
                    for item in package_iter:
                        mixed.append(item)
                        added = True
                        break
                if not added:
                    break
            remaining = list(coach_iter) + list(package_iter)
            mixed.extend(remaining[: 5 - len(mixed)])
            return mixed

        return recommendations[:5]

    def _collect_items_from_outputs(self, tool_outputs: list[Any]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        """从工具输出中分离教练和套餐原始数据。"""
        coaches: list[dict[str, Any]] = []
        packages: list[dict[str, Any]] = []
        for output in tool_outputs:
            if not isinstance(output, list):
                continue
            for item in output:
                if not isinstance(item, dict):
                    continue
                item_type = item.get("type") or item.get("package_mode")
                if item_type == "coach" or "coach_hash" in item or "coach_id" in item:
                    coaches.append(item)
                elif item_type in {"package", "custom", "custom_package"} or "package_hash" in item or "package_id" in item:
                    packages.append(item)
        return coaches, packages

    def _coach_to_recommendation(self, item: dict[str, Any]) -> RecommendationItem:
        coach_id = item.get("coach_id")
        source_id = item.get("coach_hash") or coach_id or item.get("name", "")
        reason = item.get("reason", "")
        if item.get("_gender_fallback"):
            reason = f"（性别信息未录入）{reason}" if reason else "性别信息未录入"
        return RecommendationItem(
            type="coach",
            id=coach_id if coach_id is not None else deterministic_id(f"coach:{source_id}"),
            coach_id=coach_id,
            name=item.get("name", ""),
            reason=reason,
            avatar_url=item.get("avatar_url"),
            rating=item.get("rating"),
            teaching_years=item.get("teaching_years"),
            reference_price=item.get("reference_price"),
            strokes=item.get("teaching_strokes") or item.get("strokes"),
            description=item.get("description"),
        )

    def _package_to_recommendation(self, item: dict[str, Any]) -> RecommendationItem:
        package_id = item.get("package_id")
        source_id = item.get("package_hash") or package_id or item.get("name", "")
        return RecommendationItem(
            type="package",
            id=package_id if package_id is not None else deterministic_id(f"package:{source_id}"),
            name=item.get("name", ""),
            reason=item.get("reason", ""),
            price=item.get("price"),
            hours=item.get("hours"),
            price_per_hour=item.get("price_per_hour"),
            class_size=item.get("class_size") or item.get("teaching_type"),
            validity_days=item.get("validity_days"),
            strokes=item.get("strokes"),
            description=item.get("description"),
        )

    def _custom_package_to_recommendation(
        self,
        coach: dict[str, Any],
        hours: int,
        class_size: str,
        stroke: str | None,
    ) -> RecommendationItem:
        coach_id = coach.get("coach_id")
        price_per_hour = coach.get("reference_price") or 0
        total_price = price_per_hour * hours
        strokes = coach.get("teaching_strokes") or []
        if stroke and stroke not in strokes:
            strokes = [stroke, *strokes]
        return RecommendationItem(
            type="custom_package",
            id=deterministic_id(f"custom_package:{coach_id or coach.get('name', '')}:{hours}:{class_size}"),
            name=f"{coach.get('name', '教练')} 定制 {class_size} 课程",
            reason=coach.get("description") or coach.get("reason") or f"按你要求的 {hours} 节 {class_size} 定制，灵活匹配",
            avatar_url=coach.get("avatar_url"),
            hours=hours,
            price_per_hour=price_per_hour,
            total_price=total_price,
            class_size=class_size,
            strokes=strokes,
            coach_id=coach_id,
            coach_name=coach.get("name", ""),
            description=coach.get("description"),
        )

    def _has_exact_package_match(
        self,
        packages: list[dict[str, Any]],
        hours: int | None,
        class_size: str | None,
    ) -> bool:
        """判断标准套餐列表中是否存在同时匹配课时和班型的套餐。"""
        for pkg in packages:
            if not isinstance(pkg, dict):
                continue
            if self._matches_hours(pkg, hours) and self._matches_class_size(pkg, class_size):
                return True
        return False

    def _bootstrap_record(
        self,
        name: str,
        args: dict[str, Any],
        output: Any,
    ) -> dict[str, Any]:
        return {
            "id": f"bootstrap_{name}_{int(time.time() * 1000)}",
            "name": name,
            "args": args,
            "output": output,
        }

    def _matches_class_size(self, item: dict[str, Any], class_size: str | None) -> bool:
        """判断套餐/教练是否匹配指定班型。MVP 阶段教练不维护班型，默认匹配。"""
        if not class_size:
            return True
        item_class_size = item.get("class_size") or item.get("teaching_type") or ""
        if not item_class_size:
            return True
        return class_size in str(item_class_size)

    def _matches_hours(self, item: dict[str, Any], hours: int | None) -> bool:
        if hours is None:
            return True
        item_hours = item.get("hours")
        return item_hours == hours

    def _matches_stroke(self, item: dict[str, Any], stroke: str | None) -> bool:
        if not stroke:
            return True
        strokes = item.get("strokes") or item.get("teaching_strokes") or []
        if isinstance(strokes, str):
            strokes = [s.strip() for s in strokes.split(",") if s.strip()]
        normalized_strokes = {normalize_stroke(str(s)) or str(s).lower() for s in strokes}
        return stroke.lower() in normalized_strokes

    def _score_package_match(self, item: dict[str, Any], stroke: str | None, hours: int | None, class_size: str | None) -> int:
        """套餐匹配度评分，越高越优先。"""
        score = 0
        if self._matches_stroke(item, stroke):
            score += 10
        if self._matches_hours(item, hours):
            score += 8
        if self._matches_class_size(item, class_size):
            score += 6
        return score

    def _build_recommendations(
        self,
        tool_outputs: list[Any],
        focus: str,
        stroke: str | None,
        hours: int | None,
        class_size: str | None,
        max_price: int | None,
        all_strokes: list[str] | None = None,
        session_id: str | None = None,
    ) -> list[RecommendationItem]:
        """根据用户意图和过滤条件，从工具输出中构建最终推荐列表（最多 5 条）。"""
        normalized_stroke = normalize_stroke(stroke)
        coaches, packages = self._collect_items_from_outputs(tool_outputs)
        recommendations: list[RecommendationItem] = []

        if focus == "coach":
            # 如果用户提到多个泳姿，要求教练同时会所有提到的泳姿
            required_strokes = all_strokes or ([stroke] if stroke else [])
            for coach in coaches:
                if required_strokes and not all(
                    self._matches_stroke(coach, normalize_stroke(s)) for s in required_strokes
                ):
                    continue
                recommendations.append(self._coach_to_recommendation(coach))
                if len(recommendations) >= 5:
                    break
            return recommendations

        if focus == "package":
            # 过滤并排序套餐：优先完全匹配泳姿、课时、班型
            filtered_packages = [p for p in packages if self._matches_stroke(p, normalized_stroke)]
            filtered_packages.sort(key=lambda p: self._score_package_match(p, normalized_stroke, hours, class_size), reverse=True)

            # 如果存在完全匹配课时和班型的标准套餐，直接返回
            exact_matches = [p for p in filtered_packages if self._matches_hours(p, hours) and self._matches_class_size(p, class_size)]
            if exact_matches:
                for pkg in exact_matches[:5]:
                    recommendations.append(self._package_to_recommendation(pkg))
                return recommendations

            # 否则优先返回最匹配的标准套餐（最多 3 条），再用教练生成自定义套餐补足
            for pkg in filtered_packages[:3]:
                recommendations.append(self._package_to_recommendation(pkg))

            # 当用户明确指定了课时或班型但标准套餐不完全匹配时，生成 custom_package
            if (hours or class_size) and coaches:
                effective_class_size = class_size or "一对一"
                effective_hours = hours or 10
                for coach in coaches[: 5 - len(recommendations)]:
                    recommendations.append(
                        self._custom_package_to_recommendation(coach, effective_hours, effective_class_size, normalized_stroke)
                    )

            # 统一按总价升序排列，让文字介绍和卡片顺序一致
            recommendations.sort(key=lambda r: (r.total_price or r.price or 0))
            return recommendations[:5]

        # mixed / 默认：教练和套餐混排
        coach_recs = [self._coach_to_recommendation(c) for c in coaches[:3]]
        package_recs = [self._package_to_recommendation(p) for p in packages[:3] if self._matches_stroke(p, normalized_stroke)]
        mixed: list[RecommendationItem] = []
        coach_iter = iter(coach_recs)
        package_iter = iter(package_recs)
        while len(mixed) < 5:
            added = False
            if len(mixed) % 2 == 0:
                for item in coach_iter:
                    mixed.append(item)
                    added = True
                    break
            else:
                for item in package_iter:
                    mixed.append(item)
                    added = True
                    break
            if not added:
                break
        remaining = list(coach_iter) + list(package_iter)
        mixed.extend(remaining[: 5 - len(mixed)])
        return mixed

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

    def _is_recommendation_intent(self, message: str) -> bool:
        return any(keyword in message for keyword in RECOMMENDATION_KEYWORDS)

    def _is_knowledge_intent(self, message: str) -> bool:
        return any(keyword in message for keyword in KNOWLEDGE_KEYWORDS)

    def _detect_recommendation_focus(self, message: str) -> str:
        msg = message.lower()
        coach_keywords = {"教练", "老师", "私教"}
        package_keywords = {"套餐", "课程", "课", "体验课", "多少钱", "课时", "节", "价格", "钱"}
        has_coach = any(k in msg for k in coach_keywords)
        has_package = any(k in msg for k in package_keywords)
        if has_package and not has_coach:
            return "package"
        if has_coach and not has_package:
            return "coach"
        return "mixed"

    def _extract_price_limit(self, message: str) -> int | None:
        match = re.search(r"(\d{3,})\s*(?:元|块|块钱)?", message)
        if match:
            return int(match.group(1))
        return None

    def _extract_gender(self, message: str) -> str | None:
        msg = message.lower()
        if "女" in msg:
            return "female"
        if "男" in msg:
            return "male"
        return None

    def _extract_age_limit(self, message: str) -> int | None:
        match = re.search(r"(\d{1,3})\s*岁\s*以\s*下", message)
        if match:
            return int(match.group(1))
        return None

    def _extract_hours(self, message: str) -> int | None:
        """从用户消息中提取课时数，如 '7节'、'10节课'。"""
        match = re.search(r"(\d+)\s*节", message)
        if match:
            return int(match.group(1))
        return None

    def _extract_class_size(self, message: str) -> str | None:
        """从用户消息中提取班型，如一对一、一对二、一对三。"""
        msg = message.lower()
        if "一对二" in msg or "1对2" in msg or "一对 2" in msg:
            return "一对二"
        if "一对三" in msg or "1对3" in msg or "一对 3" in msg:
            return "一对三"
        if "一对一" in msg or "1对1" in msg or "一对 1" in msg:
            return "一对一"
        return None

    def _build_intent_context(self, history: list[dict[str, Any]]) -> str:
        """从完整会话历史中提取用户问题，用于累积需求识别。"""
        user_messages = [
            str(msg.get("content", ""))
            for msg in history
            if msg.get("role") == "user"
        ]
        return "\n".join(user_messages[-10:])

    async def _bootstrap_knowledge_data(
        self,
        query: str,
        tools: list[Any],
        session_id: str | None = None,
    ) -> tuple[dict[str, Any] | None, list[BaseMessage] | None]:
        """针对知识类请求，主动检索知识库并将结果注入上下文。

        若知识库未命中，则自动调用 web_search 进行联网搜索兜底。
        返回的记录和消息列表模拟一次完整的工具调用：
        - AIMessage 表示助手已决定调用 query_knowledge/web_search；
        - ToolMessage 表示工具返回的检索结果。
        这样后续 LLM 会直接基于工具结果生成回答，避免再次输出工具调用标记。
        """
        if session_id:
            structlog.contextvars.bind_contextvars(session_id=session_id)
        try:
            logger.info(
                "bootstrap_knowledge_query",
                session_id=session_id,
                query=query,
            )
            results = await self.knowledge_service.query(
                query=query,
                top_k=3,
                threshold=0.2,
            )

            tool_name = "query_knowledge"
            if not results:
                logger.info(
                    "bootstrap_knowledge_empty",
                    session_id=session_id,
                    query=query,
                )
                web_search_tool = next(
                    (t for t in tools if getattr(t, "name", None) == "web_search"),
                    None,
                )
                if web_search_tool is None:
                    logger.warning("web_search_tool_not_found", session_id=session_id)
                    return None, None
                try:
                    web_results = await web_search_tool.ainvoke({"query": query})
                except Exception as exc:
                    logger.error(
                        "bootstrap_web_search_failed",
                        session_id=session_id,
                        query=query,
                        error=str(exc),
                    )
                    return None, None
                if not web_results:
                    logger.info(
                        "bootstrap_web_search_empty",
                        session_id=session_id,
                        query=query,
                    )
                    return None, None
                results = web_results
                tool_name = "web_search"

            record = self._bootstrap_record(
                tool_name,
                {"query": query},
                [
                    {
                        "content": r.get("content", ""),
                        "source": r.get("title", ""),
                        "source_type": "knowledge_base" if tool_name == "query_knowledge" else "web_search",
                        "category": r.get("category", ""),
                        "score": r.get("score", 0),
                        "url": r.get("url", ""),
                    }
                    for r in results
                ],
            )
            source_hint = (
                "以下是与用户问题相关的网络搜索结果，每条结果包含 source_type、标题、链接和内容；"
                "请基于这些内容回答，并严格按系统提示要求标注来源。如果内容不足以完整回答，"
                "可以补充通用建议，但不要编造没有的信息。"
                if tool_name == "web_search"
                else "以下是与用户问题相关的知识库内容，每条结果包含 source_type、来源标题、分类、相似度和内容；"
                "请基于这些内容回答，并严格按系统提示要求标注来源。如果内容不足以完整回答，"
                "可以补充通用建议，但不要编造知识库中没有的信息。"
            )
            structured_results = [
                {
                    "content": r.get("content", ""),
                    "source": r.get("title", ""),
                    "source_type": "knowledge_base" if tool_name == "query_knowledge" else "web_search",
                    "category": r.get("category", ""),
                    "score": r.get("score", 0),
                    "url": r.get("url", ""),
                }
                for r in results
            ]
            tool_result_content = (
                f"{source_hint}\n\n"
                f"检索结果（JSON）：\n{json.dumps(structured_results, ensure_ascii=False, indent=2)}"
            )
            tool_call_message = AIMessage(
                content="",
                tool_calls=[
                    {
                        "id": record["id"],
                        "name": record["name"],
                        "args": record["args"],
                    }
                ],
            )
            tool_result_message = ToolMessage(
                content=tool_result_content,
                tool_call_id=record["id"],
            )
            logger.info(
                "bootstrap_knowledge_finished",
                session_id=session_id,
                query=query,
                tool_name=tool_name,
                result_count=len(results),
            )
            return record, [tool_call_message, tool_result_message]
        except Exception as exc:
            logger.error("bootstrap_knowledge_failed", session_id=session_id, query=query, error=str(exc))
            return None, None
        finally:
            if session_id:
                structlog.contextvars.unbind_contextvars("session_id")

    async def _bootstrap_recommendation_data(
        self,
        tools: list[Any],
        messages: list[BaseMessage],
        history: list[dict[str, Any]],
        user_hash: str | None,
        session_id: str | None = None,
    ) -> tuple[list[dict[str, Any]], list[Any], list[BaseMessage], list[RecommendationItem]]:
        """针对推荐类请求，主动调用工具获取真实数据，并生成最终 recommendations 列表。"""
        tool_map = {tool.name: tool for tool in tools}
        selected_tools: list[tuple[str, dict[str, Any]]] = []

        context_text = self._build_intent_context(history)
        last_message = str(getattr(messages[-1], "content", "")) if messages else ""

        stroke_keywords = {"自由泳", "蛙泳", "仰泳", "蝶泳"}
        strokes = [s for s in stroke_keywords if s in context_text]
        stroke = strokes[0] if strokes else None
        # 推荐类型只看当前消息，避免历史中的教练/套餐关键词互相干扰
        focus = self._detect_recommendation_focus(last_message)
        # 过滤条件（预算、性别、年龄、课时、班型）允许跨轮累积
        max_price = self._extract_price_limit(context_text)
        gender = self._extract_gender(context_text)
        max_age = self._extract_age_limit(context_text)
        hours = self._extract_hours(context_text)
        class_size = self._extract_class_size(context_text)
        logger.info(
            "bootstrap_intent_extracted",
            session_id=session_id,
            focus=focus,
            stroke=stroke,
            strokes=strokes,
            gender=gender,
            max_age=max_age,
            max_price=max_price,
            hours=hours,
            class_size=class_size,
        )

        if user_hash and "我" in context_text:
            selected_tools.append(("get_user_profile", {}))
            selected_tools.append(("get_user_packages", {}))

        async def invoke_tool(name: str, args: dict[str, Any]) -> Any:
            if session_id:
                structlog.contextvars.bind_contextvars(session_id=session_id)
            try:
                logger.info(
                    "bootstrap_invoking_tool",
                    session_id=session_id,
                    name=name,
                    args=args,
                )
                tool = tool_map.get(name)
                if tool is None:
                    return {"error": f"工具 {name} 未找到"}
                try:
                    output = await tool.ainvoke(args)
                    result_count = len(output) if isinstance(output, list) else None
                    logger.info(
                        "bootstrap_tool_finished",
                        session_id=session_id,
                        name=name,
                        result_count=result_count,
                    )
                    return output
                except Exception as exc:
                    logger.error(
                        "bootstrap_tool_failed",
                        session_id=session_id,
                        name=name,
                        error=str(exc),
                    )
                    return {"error": f"工具调用失败：{exc}"}
            finally:
                if session_id:
                    structlog.contextvars.unbind_contextvars("session_id")

        tool_call_records: list[dict[str, Any]] = []
        tool_outputs: list[Any] = []

        if user_hash and "我" in context_text:
            for name, args in [("get_user_profile", {}), ("get_user_packages", {})]:
                output = await invoke_tool(name, args)
                tool_outputs.append(output)
                tool_call_records.append(self._bootstrap_record(name, args, output))

        if focus == "coach":
            coach_args = {"stroke": stroke, "gender": gender, "max_price": max_price, "max_age": max_age, "limit": 5}
            output = await invoke_tool("query_coaches", coach_args)
            # 如果按性别严格过滤没有结果，回退到忽略性别，避免数据不完整导致漏推
            if (not output or len(output) == 0) and gender:
                logger.info("coach_gender_filter_empty_fallback", gender=gender, stroke=stroke)
                fallback_args = {**coach_args, "gender": None}
                output = await invoke_tool("query_coaches", fallback_args)
                for item in output if isinstance(output, list) else []:
                    item["_gender_fallback"] = True
            tool_outputs.append(output)
            tool_call_records.append(self._bootstrap_record("query_coaches", coach_args, output))
        elif focus == "package":
            package_output = await invoke_tool(
                "query_packages",
                {"stroke": stroke, "hours": hours, "max_price": max_price, "limit": 10},
            )
            tool_outputs.append(package_output)
            tool_call_records.append(
                self._bootstrap_record(
                    "query_packages",
                    {"stroke": stroke, "hours": hours, "max_price": max_price, "limit": 10},
                    package_output,
                )
            )
            # 当用户指定课时/班型且标准套餐没有精确匹配时，再查教练生成 custom_package
            if (hours or class_size) and not self._has_exact_package_match(
                package_output if isinstance(package_output, list) else [], hours, class_size
            ):
                coach_output = await invoke_tool(
                    "query_coaches",
                    {"stroke": stroke, "gender": gender, "max_age": max_age, "limit": 5},
                )
                tool_outputs.append(coach_output)
                tool_call_records.append(
                    self._bootstrap_record(
                        "query_coaches",
                        {"stroke": stroke, "gender": gender, "max_age": max_age, "limit": 5},
                        coach_output,
                    )
                )
        elif stroke:
            for name, args in [
                ("query_coaches", {"stroke": stroke, "gender": gender, "max_price": max_price, "max_age": max_age, "limit": 5}),
                ("query_packages", {"stroke": stroke, "max_price": max_price, "limit": 5}),
            ]:
                output = await invoke_tool(name, args)
                tool_outputs.append(output)
                tool_call_records.append(self._bootstrap_record(name, args, output))
        else:
            for name, args in [
                ("get_hot_recommendations", {"limit": 5}),
                ("query_coaches", {"gender": gender, "max_price": max_price, "max_age": max_age, "limit": 3}),
                ("query_packages", {"max_price": max_price, "limit": 3}),
            ]:
                output = await invoke_tool(name, args)
                tool_outputs.append(output)
                tool_call_records.append(self._bootstrap_record(name, args, output))

        recommendations = self._build_recommendations(
            tool_outputs=tool_outputs,
            focus=focus,
            stroke=stroke,
            hours=hours,
            class_size=class_size,
            max_price=max_price,
            all_strokes=strokes,
        )

        focus_desc = {
            "coach": "教练",
            "package": "套餐/课程",
            "mixed": "教练和套餐",
        }.get(focus, "教练和套餐")

        filters: list[str] = []
        if stroke:
            filters.append(f"泳姿：{stroke}")
        if gender:
            filters.append(f"性别：{'女' if gender == 'female' else '男'}")
        if max_age:
            filters.append(f"年龄：{max_age}岁以下")
        if max_price:
            filters.append(f"预算上限：{max_price}元")
        if hours:
            filters.append(f"课时：{hours}节")
        if class_size:
            filters.append(f"班型：{class_size}")
        filter_desc = "；".join(filters) if filters else "无额外筛选条件"

        logger.info(
            "build_recommendations_result",
            session_id=session_id,
            focus=focus,
            stroke=stroke,
            all_strokes=strokes,
            gender=gender,
            max_price=max_price,
            hours=hours,
            class_size=class_size,
            recommendation_count=len(recommendations),
            recommendation_types=[r.type for r in recommendations],
            recommendation_names=[r.name for r in recommendations],
        )

        rec_details = []
        for rec in recommendations:
            detail = {
                "type": rec.type,
                "name": rec.name,
                "reason": rec.reason,
            }
            if rec.rating is not None:
                detail["rating"] = rec.rating
            if rec.teaching_years is not None:
                detail["teaching_years"] = rec.teaching_years
            if rec.reference_price is not None:
                detail["reference_price"] = rec.reference_price
            if rec.price is not None:
                detail["price"] = rec.price
            if rec.hours is not None:
                detail["hours"] = rec.hours
            if rec.price_per_hour is not None:
                detail["price_per_hour"] = rec.price_per_hour
            if rec.total_price is not None:
                detail["total_price"] = rec.total_price
            if rec.class_size is not None:
                detail["class_size"] = rec.class_size
            if rec.validity_days is not None:
                detail["validity_days"] = rec.validity_days
            if rec.strokes:
                detail["strokes"] = rec.strokes
            if rec.coach_name:
                detail["coach_name"] = rec.coach_name
            if rec.description:
                detail["description"] = rec.description
            rec_details.append(detail)

        context = (
            f"用户请求重点：{focus_desc}。已根据筛选条件整理出最终要展示给用户的推荐列表（最多 5 条）。"
            f"请结合用户最新问题{'（' + last_message + '）' if last_message else ''}和上下文理解需求。"
            f"当前已识别的筛选条件：{filter_desc}。"
            "你最终生成的文字回复中，只能提到下面「最终推荐列表」中的教练或套餐，不能编造、不能补充列表之外的项。"
            "请直接基于这些推荐项，向用户做出口语化、简洁的推荐说明，不要再调用工具。"
            "优先根据用户具体需求（预算、泳姿、课时、班型、教练/套餐描述）筛选最匹配的项，并在推荐理由中体现关键信息。\n\n"
            "最终推荐列表（JSON）：\n"
            f"{json.dumps(rec_details, ensure_ascii=False, indent=2)}"
        )
        return tool_call_records, tool_outputs, [AIMessage(content=context)], recommendations

    async def _run_agent(
        self,
        tools: list[Any],
        messages: list[BaseMessage],
        session_id: str,
        user_hash: str | None,
    ) -> tuple[str, list[dict[str, Any]], list[Any]]:
        llm_with_tools = self.llm.bind_tools(tools)

        config = {
            "metadata": {
                "session_id": session_id,
                "user_hash": user_hash or "guest",
                "user_identity": "logged_in" if user_hash else "guest",
            },
            "tags": ["chat", "leyo"],
        }

        all_tool_call_records: list[dict[str, Any]] = []
        all_tool_outputs: list[Any] = []
        response = await llm_with_tools.ainvoke(messages, config=config)

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

            response = await llm_with_tools.ainvoke(messages, config=config)

        if not isinstance(response, AIMessage):
            raise RuntimeError("LLM response is not an AIMessage")

        return str(response.content), all_tool_call_records, all_tool_outputs

    async def _prepare_messages(
        self, session_id: str, user_hash: str | None, message: str
    ) -> tuple[list[Any], list[BaseMessage], list[dict[str, Any]]]:
        user_identity = "已登录用户" if user_hash else "游客"
        tools = build_tools(self.client, user_hash)
        history = await self._load_messages(session_id)
        messages: list[BaseMessage] = [self._build_system_message(user_identity)]
        messages.extend(self._history_to_messages(history))
        messages.append(HumanMessage(content=message))
        return tools, messages, history

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
        logger.info(
            "chat_user_message",
            session_id=request.session_id,
            message_id=message_id,
            message=request.message,
        )

        tools, messages, history = await self._prepare_messages(
            request.session_id, request.user_hash, request.message
        )

        tool_call_records: list[dict[str, Any]] = []
        tool_outputs: list[Any] = []
        bootstrap_records: list[dict[str, Any]] = []
        bootstrap_outputs: list[Any] = []
        bootstrap_messages: list[BaseMessage] = []
        bootstrap_recommendations: list[RecommendationItem] = []

        context_history = history + [{"role": "user", "content": request.message}]
        context_text = self._build_intent_context(context_history)
        is_recommendation = (
            self._is_recommendation_intent(request.message)
            or self._is_recommendation_intent(context_text)
        )
        is_knowledge = self._is_knowledge_intent(request.message)
        logger.info(
            "chat_intent_detected",
            session_id=request.session_id,
            message=request.message,
            is_recommendation=is_recommendation,
            is_knowledge=is_knowledge,
        )
        if is_recommendation:
            (
                bootstrap_records,
                bootstrap_outputs,
                bootstrap_messages,
                bootstrap_recommendations,
            ) = await self._bootstrap_recommendation_data(
                tools,
                messages,
                context_history,
                request.user_hash,
                session_id=request.session_id,
            )
            tool_call_records.extend(bootstrap_records)
            tool_outputs.extend(bootstrap_outputs)
            messages.extend(bootstrap_messages)

        if is_knowledge:
            knowledge_record, knowledge_messages = await self._bootstrap_knowledge_data(
                request.message,
                tools,
                session_id=request.session_id,
            )
            if knowledge_record and knowledge_messages:
                bootstrap_records.append(knowledge_record)
                bootstrap_messages.extend(knowledge_messages)
                tool_call_records.append(knowledge_record)
                tool_outputs.append(knowledge_record["output"])
                messages.extend(knowledge_messages)

        config = {
            "metadata": {
                "session_id": request.session_id,
                "user_hash": request.user_hash or "guest",
                "user_identity": "logged_in" if request.user_hash else "guest",
            },
            "tags": ["chat", "leyo"],
        }

        try:
            if bootstrap_records:
                # 已经预取到真实数据，直接让 LLM 基于数据生成回复，避免重复调用工具
                direct_response = await self.llm.ainvoke(messages, config=config)
                agent_text = str(getattr(direct_response, "content", ""))
                agent_records: list[dict[str, Any]] = []
                agent_outputs: list[Any] = []
            else:
                agent_text, agent_records, agent_outputs = await self._run_agent(
                    tools, messages, request.session_id, request.user_hash
                )
        except Exception as exc:
            logger.error("agent_execution_failed", error=str(exc))
            raise

        output_text = agent_text
        tool_call_records.extend(agent_records)
        tool_outputs.extend(agent_outputs)

        main_text, suggested_questions = self._parse_suggested_questions(output_text)
        if bootstrap_recommendations:
            # bootstrap 模式下已由服务端生成最终推荐列表，保证和 text 一致
            recommendations = bootstrap_recommendations[:5]
        else:
            recommendations = self._extract_recommendations(tool_outputs)

        for rec in recommendations:
            if not rec.reason:
                rec.reason = self._build_default_reason(rec)

        logger.info(
            "chat_ai_response",
            session_id=request.session_id,
            message_id=message_id,
            text=main_text,
            recommendation_count=len(recommendations),
            recommendation_names=[r.name for r in recommendations],
            suggested_questions=suggested_questions,
        )

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
