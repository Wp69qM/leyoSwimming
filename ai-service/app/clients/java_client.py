import json
import uuid
from typing import Any

import httpx

from app.config import Settings, get_settings
from app.utils.logger import get_logger

logger = get_logger(__name__)


def _snake_case_key(key: str) -> str:
    result = [key[0].lower()]
    for char in key[1:]:
        if char.isupper():
            result.append("_")
            result.append(char.lower())
        else:
            result.append(char)
    return "".join(result)


def _to_snake_case(value: Any) -> Any:
    if isinstance(value, dict):
        return {_snake_case_key(k): _to_snake_case(v) for k, v in value.items()}
    if isinstance(value, list):
        return [_to_snake_case(v) for v in value]
    return value


def _unwrap_list_response(result: dict[str, Any]) -> list[Any]:
    data = result.get("data", {})
    if isinstance(data, dict):
        return data.get("data", [])
    logger.warning("unexpected_list_response_data", data_type=type(data).__name__)
    return []


MOCK_COACHES = [
    {
        "coach_hash": "c_3e5b12",
        "name": "王教练",
        "avatar_url": "https://api.dicebear.com/7.x/avataaars/svg?seed=wang&gender=female",
        "gender": "female",
        "age": 32,
        "rating": 4.9,
        "reference_price": 200,
        "teaching_years": 8,
        "teaching_strokes": ["freestyle", "breaststroke"],
        "description": "国家二级运动员，8 年成人游泳教学经验，擅长自由泳入门与动作纠正，耐心细致，适合零基础学员。",
        "reason": "8 年教龄，自由泳+蛙泳双擅长，口碑极佳",
    },
    {
        "coach_hash": "c_7a8f22",
        "name": "李教练",
        "avatar_url": "https://api.dicebear.com/7.x/avataaars/svg?seed=li&gender=male",
        "gender": "male",
        "age": 26,
        "rating": 4.8,
        "reference_price": 180,
        "teaching_years": 5,
        "teaching_strokes": ["freestyle", "backstroke"],
        "description": "体育教育专业毕业，5 年青少年及成人游泳私教经验，自由泳、仰泳教学风趣易懂，性价比高。",
        "reason": "性价比高，入门友好，自由泳+仰泳均可教",
    },
    {
        "coach_hash": "c_9c1d44",
        "name": "张教练",
        "avatar_url": "https://api.dicebear.com/7.x/avataaars/svg?seed=zhang&gender=female",
        "gender": "female",
        "age": 28,
        "rating": 4.7,
        "reference_price": 220,
        "teaching_years": 6,
        "teaching_strokes": ["butterfly", "freestyle"],
        "description": "省队退役选手，专注蝶泳与自由泳进阶训练，对动作细节要求高，适合有一定基础想提升的学员。",
        "reason": "蝶泳+自由泳专长，适合想进阶的学员",
    },
]

MOCK_PACKAGES = [
    {
        "package_hash": "p_10ab01",
        "name": "成人自由泳 10 节私教",
        "package_mode": "standard",
        "hours": 10,
        "price": 1800,
        "price_per_hour": 180,
        "validity_days": 90,
        "teaching_type": "一对一",
        "class_size": "一对一",
        "strokes": ["freestyle"],
        "description": "系统学习自由泳，从换气、打腿到完整配合，适合零基础或想规范动作的成人学员。",
        "reason": "系统学习自由泳，单节价格划算",
    },
    {
        "package_hash": "p_20cd02",
        "name": "自由泳体验课 2 节",
        "package_mode": "experience",
        "hours": 2,
        "price": 299,
        "price_per_hour": 150,
        "validity_days": 30,
        "teaching_type": "一对一",
        "class_size": "一对一",
        "strokes": ["freestyle"],
        "description": "低价体验自由泳，感受教练教学风格，适合还在犹豫的学员。",
        "reason": "低价体验，适合零基础试水",
    },
    {
        "package_hash": "p_30ef03",
        "name": "儿童蛙泳 20 节私教",
        "package_mode": "standard",
        "hours": 20,
        "price": 3600,
        "price_per_hour": 180,
        "validity_days": 180,
        "teaching_type": "一对一",
        "class_size": "一对一",
        "strokes": ["breaststroke"],
        "description": "针对儿童设计的蛙泳长期课程，循序渐进，巩固效果好，培养水感与安全意识。",
        "reason": "儿童蛙泳专项，周期长、巩固效果好",
    },
    {
        "package_hash": "p_40gh04",
        "name": "自定义自由泳一对一",
        "package_mode": "custom",
        "hours": 12,
        "price": 2400,
        "price_per_hour": 200,
        "total_price": 2400,
        "validity_days": 120,
        "class_size": "一对一",
        "strokes": ["freestyle"],
        "coach_id": 1,
        "coach_name": "王教练",
        "avatar_url": "https://api.dicebear.com/7.x/avataaars/svg?seed=wang&gender=female",
        "description": "课时灵活，可跟随指定教练长期训练，适合时间不固定或有特定目标的学员。",
        "reason": "课时灵活，可跟随指定教练长期训练",
    },
]


class JavaInternalClient:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.client = httpx.AsyncClient(
            base_url=self.settings.java_internal_base_url,
            headers={"X-Internal-Token": self.settings.internal_api_token},
            timeout=10.0,
        )

    async def _post(self, path: str, payload: dict[str, Any]) -> dict[str, Any]:
        if self.settings.enable_mock_data:
            logger.info("mock_mode_enabled", path=path)
            return {"data": []}
        try:
            response = await self.client.post(path, json=payload)
            response.raise_for_status()
            try:
                result = response.json()
            except json.JSONDecodeError as exc:
                logger.error("java_internal_invalid_json", path=path, body=response.text[:200], error=str(exc))
                raise httpx.HTTPError(f"Java 后端返回非 JSON 响应: {exc}") from exc
            code = result.get("code")
            if code is not None and code != 0:
                message = result.get("message", "未知错误")
                logger.error("java_internal_business_error", path=path, code=code, message=message)
                raise httpx.HTTPError(f"Java 后端业务错误 [{code}]: {message}")
            return result
        except httpx.HTTPError as exc:
            logger.error("java_internal_request_failed", path=path, error=str(exc))
            raise

    async def query_coaches(
        self,
        stroke: str | None = None,
        gender: str | None = None,
        min_price: int | None = None,
        max_price: int | None = None,
        max_age: int | None = None,
        class_size: str | None = None,
        limit: int = 5,
    ) -> list[dict[str, Any]]:
        if self.settings.enable_mock_data:
            return [
                coach
                for coach in MOCK_COACHES
                if (not stroke or stroke.lower() in [s.lower() for s in coach["teaching_strokes"]])
                and (not gender or coach["gender"] == gender)
                and (min_price is None or coach["reference_price"] >= min_price)
                and (max_price is None or coach["reference_price"] <= max_price)
                and (max_age is None or coach.get("age", 999) <= max_age)
            ][:limit]

        result = await self._post(
            "/coaches/query",
            {
                "stroke": stroke,
                "gender": gender,
                "minPrice": min_price,
                "maxPrice": max_price,
                "maxAge": max_age,
                "classSize": class_size,
                "limit": limit,
            },
        )
        return _to_snake_case(_unwrap_list_response(result))

    async def query_packages(
        self,
        stroke: str | None = None,
        package_mode: str | None = None,
        min_price: int | None = None,
        max_price: int | None = None,
        hours: int | None = None,
        limit: int = 5,
    ) -> list[dict[str, Any]]:
        if self.settings.enable_mock_data:
            return [
                package
                for package in MOCK_PACKAGES
                if (not stroke or stroke.lower() in [s.lower() for s in package["strokes"]])
                and (not package_mode or package["package_mode"] == package_mode)
                and (max_price is None or package["price"] <= max_price)
                and (hours is None or package["hours"] == hours)
            ][:limit]

        result = await self._post(
            "/packages/query",
            {
                "stroke": stroke,
                "packageMode": package_mode,
                "minPrice": min_price,
                "maxPrice": max_price,
                "hours": hours,
                "limit": limit,
            },
        )
        return _to_snake_case(_unwrap_list_response(result))

    async def get_user_profile(self, user_hash: str) -> dict[str, Any] | None:
        if self.settings.enable_mock_data:
            return {
                "user_hash": user_hash,
                "age": 30,
                "target_stroke": "freestyle",
                "swimming_level": "beginner",
                "budget": 3000,
            }
        result = await self._post("/user/profile", {"userHash": user_hash})
        return _to_snake_case(result.get("data"))

    async def get_user_packages(
        self, user_hash: str, statuses: list[str] | None = None
    ) -> list[dict[str, Any]]:
        if self.settings.enable_mock_data:
            return []
        result = await self._post(
            "/user/packages", {"userHash": user_hash, "statuses": statuses or ["active"]}
        )
        return _to_snake_case(_unwrap_list_response(result))

    async def get_hot_recommendations(
        self, stroke: str | None = None, limit: int = 5
    ) -> list[dict[str, Any]]:
        if self.settings.enable_mock_data:
            coaches = MOCK_COACHES[:limit]
            packages = MOCK_PACKAGES[:limit]
            return [{"type": "coach", **c} for c in coaches] + [
                {"type": "package", **p} for p in packages
            ]
        result = await self._post("/recommendations/hot", {"stroke": stroke, "limit": limit})
        return _to_snake_case(_unwrap_list_response(result))

    async def close(self) -> None:
        await self.client.aclose()


def generate_message_id() -> str:
    return f"msg_{uuid.uuid4().hex[:16]}"


def generate_session_id() -> str:
    return f"sess_{uuid.uuid4().hex[:16]}"
