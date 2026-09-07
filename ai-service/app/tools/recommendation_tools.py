from typing import Any

from langchain_core.tools import tool

from app.clients.java_client import JavaInternalClient
from app.tools.normalizers import normalize_gender, normalize_package_mode, normalize_stroke
from app.utils.logger import get_logger

logger = get_logger(__name__)


def build_tools(client: JavaInternalClient, user_hash: str | None = None) -> list[Any]:
    @tool
    async def query_coaches(
        stroke: str | None = None,
        gender: str | None = None,
        max_price: int | None = None,
        max_age: int | None = None,
        class_size: str | None = None,
        limit: int = 5,
    ) -> list[dict[str, Any]]:
        """根据目标泳姿、教练性别、价格上限、年龄上限、班级规模召回候选教练。

        Args:
            stroke: 目标泳姿，如自由泳、蛙泳、仰泳、蝶泳
            gender: 教练性别，如女、男
            max_price: 每课时价格上限（元）
            max_age: 教练年龄上限（岁）
            class_size: 班级规模，如一对一、一对二、一对三
            limit: 最多返回几条，默认 5
        """
        logger.info(
            "tool_query_coaches",
            stroke=stroke,
            gender=gender,
            max_price=max_price,
            max_age=max_age,
            class_size=class_size,
        )
        return await client.query_coaches(
            stroke=normalize_stroke(stroke),
            gender=normalize_gender(gender),
            max_price=max_price,
            max_age=max_age,
            class_size=class_size,
            limit=limit,
        )

    @tool
    async def query_packages(
        stroke: str | None = None,
        package_mode: str | None = None,
        max_price: int | None = None,
        hours: int | None = None,
        limit: int = 5,
    ) -> list[dict[str, Any]]:
        """根据目标泳姿、套餐模式、价格上限、课时数召回候选套餐。

        Args:
            stroke: 目标泳姿，如自由泳、蛙泳
            package_mode: 套餐模式，如 standard（标准套餐）、experience（体验课）、custom（自定义套餐）
            max_price: 套餐总价上限（元）
            hours: 课时数
            limit: 最多返回几条，默认 5
        """
        logger.info(
            "tool_query_packages",
            stroke=stroke,
            package_mode=package_mode,
            max_price=max_price,
            hours=hours,
        )
        return await client.query_packages(
            stroke=normalize_stroke(stroke),
            package_mode=normalize_package_mode(package_mode),
            max_price=max_price,
            hours=hours,
            limit=limit,
        )

    @tool
    async def get_user_profile() -> dict[str, Any] | None:
        """获取已登录用户的画像信息，用于个性化推荐。游客场景不要调用。"""
        if not user_hash:
            return {"error": "游客用户无法获取画像，请先引导登录"}
        logger.info("tool_get_user_profile", user_hash=user_hash)
        return await client.get_user_profile(user_hash)

    @tool
    async def get_user_packages() -> list[dict[str, Any]]:
        """获取用户已购套餐，用于避免重复推荐。游客场景不要调用。"""
        if not user_hash:
            return []
        logger.info("tool_get_user_packages", user_hash=user_hash)
        return await client.get_user_packages(user_hash, statuses=["active", "exhausted"])

    @tool
    async def get_hot_recommendations(stroke: str | None = None, limit: int = 5) -> list[dict[str, Any]]:
        """获取热门教练或套餐，用于冷启动或游客默认推荐。"""
        logger.info("tool_get_hot_recommendations", stroke=stroke)
        return await client.get_hot_recommendations(stroke=normalize_stroke(stroke), limit=limit)

    return [
        query_coaches,
        query_packages,
        get_user_profile,
        get_user_packages,
        get_hot_recommendations,
    ]
