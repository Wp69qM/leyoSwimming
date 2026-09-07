"""MCP Server：FastMCP 实例与工具注册（US-066 / US-067）。

安全约束：
- 仅逐个显式注册只读工具，无批量导入路径。
- 禁止暴露：get_user_profile / get_user_packages（user_hash 身份闭包，
  参数化暴露 = 任意用户数据越权查询面）、ingest / chat / web_search
  （管理/对话/联网能力不外放）。
"""

from typing import Any

from mcp.server.fastmcp import FastMCP
from mcp.server.transport_security import TransportSecuritySettings

from app.clients.java_client import JavaInternalClient
from app.config import Settings, get_settings
from app.tools.knowledge_tools import _get_knowledge_service
from app.tools.normalizers import normalize_gender, normalize_package_mode, normalize_stroke
from app.utils.logger import get_logger

logger = get_logger(__name__)

# FastMCP 在 host 为默认 127.0.0.1 时会自动启用 DNS 重绑定保护（allowed_hosts 仅
# localhost），生产环境真实域名会被 421 拒绝。本服务 MCP 端点已有独立 Bearer token
# 鉴权（app/mcp/auth.py）+ IP 限流（RateLimitMiddleware），浏览器侧 DNS rebinding
# 攻击无法携带有效 token，故显式禁用该保护。
mcp = FastMCP(
    "leyo-ai-service",
    stateless_http=True,
    transport_security=TransportSecuritySettings(enable_dns_rebinding_protection=False),
)


@mcp.tool()
async def query_knowledge(query: str, top_k: int = 3) -> dict[str, Any]:
    """从游泳培训平台知识库检索相关知识片段。

    当需要游泳安全、急救、训练技巧、健康等知识时调用。
    返回 {"list": [...]}，每条知识含原文内容（content）、来源标题（source）、
    来源类型（source_type）、分类（category）和相似度分数（score）；
    无匹配时 list 为空数组，由调用方 Agent 自行决定兜底策略。

    Args:
        query: 用户问题或检索关键词
        top_k: 最多返回几条知识片段，默认 3（上限 10，超出无条件截断）
    """
    settings = get_settings()
    service = _get_knowledge_service(settings)
    effective_top_k = min(top_k, settings.mcp_top_k_max)
    logger.info("mcp_query_knowledge", query=query, top_k=effective_top_k)
    raw_results = await service.query(
        query=query,
        top_k=effective_top_k,
        threshold=settings.knowledge_similarity_threshold,
    )
    return {
        "list": [
            {
                "content": r["content"],
                "source": r["title"],
                "source_type": "knowledge_base",
                "category": r["category"],
                "score": r["score"],
            }
            for r in raw_results
        ]
    }


_java_client: JavaInternalClient | None = None


def _get_java_client(settings: Settings | None = None) -> JavaInternalClient:
    """惰性单例：MCP 服务启动不依赖 backend，首次工具调用时才建立连接。"""
    global _java_client
    if _java_client is None:
        _java_client = JavaInternalClient(settings or get_settings())
    return _java_client


async def close_mcp_java_client() -> None:
    """应用停机时释放 MCP 模块持有的 HTTP 连接（main.py lifespan 调用）。"""
    global _java_client
    if _java_client is not None:
        await _java_client.close()
        _java_client = None


def _backend_error(tool_name: str, exc: Exception) -> dict[str, Any]:
    """backend 故障统一结构化错误（US-067 §6.3）：不抛异常，工具间故障隔离。"""
    logger.error("mcp_tool_backend_failed", tool=tool_name, error=str(exc))
    return {"error": "业务数据服务暂不可用，请稍后再试"}


@mcp.tool()
async def query_coaches(
    stroke: str | None = None,
    gender: str | None = None,
    max_price: int | None = None,
    max_age: int | None = None,
    class_size: str | None = None,
    limit: int = 5,
) -> dict[str, Any]:
    """查询游泳培训平台的教练列表（只读）。

    数据来自 leyoSwimming 游泳培训平台真实业务数据，用于向学员推荐教练。
    参数支持中文（如 stroke="蛙泳"、gender="女"）；无法识别的取值会被忽略，
    按无该过滤条件查询。

    Args:
        stroke: 目标泳姿（自由泳/蛙泳/仰泳/蝶泳）
        gender: 教练性别（男/女）
        max_price: 每课时价格上限（元）
        max_age: 教练年龄上限（岁）
        class_size: 班级规模（一对一/一对二/一对三）
        limit: 最多返回几条，默认 5（上限 20，超出无条件截断）
    """
    settings = get_settings()
    effective_limit = min(limit, settings.mcp_limit_max)
    logger.info("mcp_query_coaches", stroke=stroke, gender=gender, limit=effective_limit)
    try:
        client = _get_java_client(settings)
        coaches = await client.query_coaches(
            stroke=normalize_stroke(stroke),
            gender=normalize_gender(gender),
            max_price=max_price,
            max_age=max_age,
            class_size=class_size,
            limit=effective_limit,
        )
        return {"list": coaches}
    except Exception as exc:
        return _backend_error("query_coaches", exc)


@mcp.tool()
async def query_packages(
    stroke: str | None = None,
    package_mode: str | None = None,
    max_price: int | None = None,
    hours: int | None = None,
    limit: int = 5,
) -> dict[str, Any]:
    """查询游泳培训平台的课程套餐列表（只读）。

    数据来自 leyoSwimming 游泳培训平台真实业务数据，用于向学员推荐课程套餐。
    参数支持中文（如 stroke="自由泳"、package_mode="体验"）；无法识别的取值
    会被忽略，按无该过滤条件查询。

    Args:
        stroke: 目标泳姿（自由泳/蛙泳/仰泳/蝶泳）
        package_mode: 套餐模式（standard 标准 / experience 体验课 / custom 自定义）
        max_price: 套餐总价上限（元）
        hours: 课时数
        limit: 最多返回几条，默认 5（上限 20，超出无条件截断）
    """
    settings = get_settings()
    effective_limit = min(limit, settings.mcp_limit_max)
    logger.info("mcp_query_packages", stroke=stroke, package_mode=package_mode, limit=effective_limit)
    try:
        client = _get_java_client(settings)
        packages = await client.query_packages(
            stroke=normalize_stroke(stroke),
            package_mode=normalize_package_mode(package_mode),
            max_price=max_price,
            hours=hours,
            limit=effective_limit,
        )
        return {"list": packages}
    except Exception as exc:
        return _backend_error("query_packages", exc)


@mcp.tool()
async def get_hot_recommendations(
    stroke: str | None = None,
    limit: int = 5,
) -> dict[str, Any]:
    """获取游泳培训平台的热门教练与课程套餐（只读）。

    数据来自 leyoSwimming 游泳培训平台真实业务数据。
    适用于冷启动、游客默认推荐或无明确偏好的场景。

    Args:
        stroke: 目标泳姿（自由泳/蛙泳/仰泳/蝶泳），可选
        limit: 最多返回几条，默认 5（上限 20，超出无条件截断）
    """
    settings = get_settings()
    effective_limit = min(limit, settings.mcp_limit_max)
    logger.info("mcp_get_hot_recommendations", stroke=stroke, limit=effective_limit)
    try:
        client = _get_java_client(settings)
        hot_items = await client.get_hot_recommendations(
            stroke=normalize_stroke(stroke),
            limit=effective_limit,
        )
        return {"list": hot_items}
    except Exception as exc:
        return _backend_error("get_hot_recommendations", exc)
