import hmac

from fastapi import Request, Response, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

from app.config import Settings, get_settings
from app.models.schemas import ErrorResponse
from app.utils.logger import get_logger

logger = get_logger(__name__)

# MCP 协议路径由 MCP 层独立 Bearer 鉴权（app/mcp/auth.py），此处显式放行避免双重校验冲突。
MCP_SERVER_PREFIX = "/mcp-server/"


class InternalAuthMiddleware(BaseHTTPMiddleware):
    """校验调用方是否持有合法的内部接口 Token。

    仅对 /api/ai-assistant/* 路径生效；/mcp-server/** 由 MCP 层独立鉴权放行；
    /health、/docs、/openapi.json 等公开端点跳过。
    Token 通过请求头 X-Internal-Token 传递，需与 AI 服务配置的 INTERNAL_API_TOKEN 一致。
    """

    def __init__(self, app, settings: Settings | None = None) -> None:
        super().__init__(app)
        self.settings = settings or get_settings()

    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        path = request.url.path.lower()

        if path.startswith(MCP_SERVER_PREFIX):
            return await call_next(request)

        if not path.startswith("/api/ai-assistant/"):
            return await call_next(request)

        token = request.headers.get("X-Internal-Token", "")
        expected = self.settings.internal_api_token

        # 与 app/mcp/auth.py 一致：非 ASCII token 按 UTF-8 bytes 比较，
        # 规避 compare_digest str 重载遇非 ASCII 抛 TypeError 返回 500。
        if not expected or not hmac.compare_digest(token.encode("utf-8"), expected.encode("utf-8")):
            logger.warning("unauthorized_internal_request", path=path)
            return JSONResponse(
                status_code=status.HTTP_401_UNAUTHORIZED,
                content=ErrorResponse(
                    code="UNAUTHORIZED",
                    message="内部接口鉴权失败",
                ).model_dump(),
            )

        return await call_next(request)
