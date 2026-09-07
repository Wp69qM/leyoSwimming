"""MCP endpoint 独立鉴权中间件（US-066）。

校验 Authorization: Bearer <MCP_API_TOKEN>，不匹配返回 401。
与 INTERNAL_API_TOKEN 完全独立，可单独轮换。

采用纯 ASGI 中间件实现（而非 BaseHTTPMiddleware），
避免对 MCP Streamable HTTP 的 SSE 流式响应产生缓冲副作用。
"""

import hmac

from starlette.responses import JSONResponse

from app.config import Settings, get_settings
from app.models.schemas import ErrorResponse
from app.utils.logger import get_logger

logger = get_logger(__name__)

BEARER_PREFIX = "bearer "


class MCPAuthMiddleware:
    """MCP token 校验 ASGI 中间件。

    挂载方式：app.mount("/mcp-server", MCPAuthMiddleware(mcp.streamable_http_app()))
    对所有 http 类型请求（含 GET SSE 流）统一校验 Bearer token。
    """

    def __init__(self, app, settings: Settings | None = None) -> None:
        self.app = app
        self.settings = settings or get_settings()

    def _extract_bearer_token(self, headers: list[tuple[bytes, bytes]]) -> str:
        for key, value in headers:
            if key == b"authorization":
                auth_header = value.decode("latin-1")
                if auth_header.lower().startswith(BEARER_PREFIX):
                    return auth_header[len(BEARER_PREFIX):].strip()
                return ""
        return ""

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        token = self._extract_bearer_token(scope.get("headers") or [])
        expected = self.settings.mcp_api_token

        if not expected or not hmac.compare_digest(token, expected):
            logger.warning("unauthorized_mcp_request", path=scope.get("path", ""))
            response = JSONResponse(
                status_code=401,
                content=ErrorResponse(
                    code="UNAUTHORIZED",
                    message="MCP 接口鉴权失败",
                ).model_dump(),
            )
            await response(scope, receive, send)
            return

        await self.app(scope, receive, send)
