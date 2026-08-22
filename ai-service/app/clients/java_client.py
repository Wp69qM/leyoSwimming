import json
import uuid
from typing import Any

import httpx

from app.config import Settings, get_settings
from app.utils.logger import get_logger

logger = get_logger(__name__)

MOCK_COACHES = [
    {
        "coach_hash": "c_3e5b12",
        "name": "王教练",
        "avatar_url": "https://api.dicebear.com/7.x/avataaars/svg?seed=wang&gender=female",
        "gender": "female",
        "rating": 4.9,
        "reference_price": 200,
        "teaching_years": 8,
        "teaching_strokes": ["freestyle", "breaststroke"],
    },
    {
        "coach_hash": "c_7a8f22",
        "name": "李教练",
        "avatar_url": "https://api.dicebear.com/7.x/avataaars/svg?seed=li&gender=male",
        "gender": "male",
        "rating": 4.8,
        "reference_price": 180,
        "teaching_years": 5,
        "teaching_strokes": ["freestyle", "backstroke"],
    },
    {
        "coach_hash": "c_9c1d44",
        "name": "张教练",
        "avatar_url": "https://api.dicebear.com/7.x/avataaars/svg?seed=zhang&gender=female",
        "gender": "female",
        "rating": 4.7,
        "reference_price": 220,
        "teaching_years": 6,
        "teaching_strokes": ["butterfly", "freestyle"],
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
        "strokes": ["freestyle"],
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
        "strokes": ["freestyle"],
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
        "strokes": ["breaststroke"],
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
                return response.json()
            except json.JSONDecodeError as exc:
                logger.error("java_internal_invalid_json", path=path, body=response.text[:200], error=str(exc))
                raise httpx.HTTPError(f"Java 后端返回非 JSON 响应: {exc}") from exc
        except httpx.HTTPError as exc:
            logger.error("java_internal_request_failed", path=path, error=str(exc))
            raise

    async def query_coaches(
        self,
        stroke: str | None = None,
        gender: str | None = None,
        min_price: int | None = None,
        max_price: int | None = None,
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
            ][:limit]

        result = await self._post(
            "/coaches/query",
            {
                "stroke": stroke,
                "gender": gender,
                "minPrice": min_price,
                "maxPrice": max_price,
                "classSize": class_size,
                "limit": limit,
            },
        )
        return result.get("data", [])

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
        return result.get("data", [])

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
        return result.get("data")

    async def get_user_packages(
        self, user_hash: str, statuses: list[str] | None = None
    ) -> list[dict[str, Any]]:
        if self.settings.enable_mock_data:
            return []
        result = await self._post(
            "/user/packages", {"userHash": user_hash, "statuses": statuses or ["active"]}
        )
        return result.get("data", [])

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
        return result.get("data", [])

    async def close(self) -> None:
        await self.client.aclose()


def generate_message_id() -> str:
    return f"msg_{uuid.uuid4().hex[:16]}"


def generate_session_id() -> str:
    return f"sess_{uuid.uuid4().hex[:16]}"
