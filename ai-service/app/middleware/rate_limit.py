import hashlib
import time

import redis.asyncio as redis
from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

from app.config import Settings, get_settings
from app.models.schemas import ErrorResponse
from app.utils.logger import get_logger

logger = get_logger(__name__)

WINDOW_SECONDS = 60

_shared_redis: redis.Redis | None = None


class RateLimitBackendError(Exception):
    """Redis 限流后端异常，触发 fail-closed。"""


async def get_shared_redis(settings: Settings) -> redis.Redis:
    """获取全局共享的 Redis 连接，用于 lifespan 统一生命周期管理。"""
    global _shared_redis
    if _shared_redis is None:
        _shared_redis = redis.from_url(
            settings.redis_url,
            decode_responses=True,
            protocol=2,
        )
    return _shared_redis


async def close_shared_redis() -> None:
    """关闭全局共享 Redis 连接。"""
    global _shared_redis
    if _shared_redis is not None:
        await _shared_redis.close()
        _shared_redis = None


class RateLimitMiddleware(BaseHTTPMiddleware):
    """基于 Redis 的固定窗口限流中间件。

    对 /api/ai-assistant/* 路径按请求 IP 与内部 Token 分别统计每分钟请求数。
    超出配置阈值时返回 429 Too Many Requests；Redis 异常时返回 503，fail-closed。
    """

    def __init__(self, app, settings: Settings | None = None) -> None:
        super().__init__(app)
        self.settings = settings or get_settings()

    async def _get_redis(self) -> redis.Redis:
        """始终使用 lifespan 统一管理的共享 Redis 连接；首次请求时懒加载。"""
        return await get_shared_redis(self.settings)

    def _client_ip(self, request: Request) -> str:
        host = request.client.host if request.client else "unknown"
        forwarded = request.headers.get("X-Forwarded-For")
        if forwarded and self.settings.trusted_proxy_count > 0:
            addrs = [addr.strip() for addr in forwarded.split(",") if addr.strip()]
            idx = len(addrs) - self.settings.trusted_proxy_count - 1
            if idx >= 0:
                return addrs[idx]
        return host

    def _token_hash(self, token: str) -> str:
        return hashlib.sha256(token.encode("utf-8")).hexdigest()

    async def _is_limited(self, key: str, limit: int) -> bool:
        try:
            r = await self._get_redis()
            pipe = r.pipeline()
            pipe.incr(key)
            pipe.expire(key, WINDOW_SECONDS)
            results = await pipe.execute()
            count = results[0]
            return count > limit
        except Exception as exc:
            logger.error("rate_limit_redis_failed", key=key, error=str(exc))
            raise RateLimitBackendError("限流后端异常") from exc

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        path = request.url.path.lower()
        if not path.startswith("/api/ai-assistant/"):
            return await call_next(request)

        try:
            now = int(time.time())
            window = now // WINDOW_SECONDS
            client_ip = self._client_ip(request)
            token = request.headers.get("X-Internal-Token", "")

            ip_key = f"ai:ratelimit:ip:{client_ip}:{window}"
            if await self._is_limited(ip_key, self.settings.rate_limit_ip_per_minute):
                logger.warning("rate_limit_exceeded", dimension="ip", key=client_ip, path=path)
                return JSONResponse(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    content=ErrorResponse(
                        code="RATE_LIMITED",
                        message="请求过于频繁，请稍后再试",
                    ).model_dump(),
                )

            if token:
                token_key = f"ai:ratelimit:token:{self._token_hash(token)}:{window}"
                if await self._is_limited(token_key, self.settings.rate_limit_user_per_minute):
                    logger.warning("rate_limit_exceeded", dimension="token", path=path)
                    return JSONResponse(
                        status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                        content=ErrorResponse(
                            code="RATE_LIMITED",
                            message="请求过于频繁，请稍后再试",
                        ).model_dump(),
                    )
        except RateLimitBackendError:
            return JSONResponse(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                content=ErrorResponse(
                    code="RATE_LIMIT_ERROR",
                    message="服务暂时无法处理请求，请稍后再试",
                ).model_dump(),
            )

        return await call_next(request)
