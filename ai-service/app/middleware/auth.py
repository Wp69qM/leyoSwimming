import hmac

from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

from app.config import Settings, get_settings
from app.models.schemas import ErrorResponse
from app.utils.logger import get_logger

logger = get_logger(__name__)


class InternalAuthMiddleware(BaseHTTPMiddleware):
    """校验调用方是否持有合法的内部接口 Token。

    仅对 /api/ai-assistant/* 路径生效；/health、/docs、/openapi.json 等公开端点跳过。
    Token 通过请求头 X-Internal-Token 传递，需与 AI 服务配置的 INTERNAL_API_TOKEN 一致。
    """

    def __init__(self, app, settings: Settings | None = None) -> None:
        super().__init__(app)
        self.settings = settings or get_settings()

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        path = request.url.path.lower()

        if not path.startswith("/api/ai-assistant/"):
            return await call_next(request)

        token = request.headers.get("X-Internal-Token", "")
        expected = self.settings.internal_api_token

        if not expected or not hmac.compare_digest(token, expected):
            logger.warning("unauthorized_internal_request", path=path)
            return JSONResponse(
                status_code=status.HTTP_401_UNAUTHORIZED,
                content=ErrorResponse(
                    code="UNAUTHORIZED",
                    message="内部接口鉴权失败",
                ).model_dump(),
            )

        return await call_next(request)
