import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import get_settings
from app.middleware import InternalAuthMiddleware, RateLimitMiddleware
from app.middleware.rate_limit import close_shared_redis
from app.models.schemas import (
    ChatRequest,
    ChatResponse,
    ErrorResponse,
    HealthResponse,
    SessionCreateResponse,
    SessionDetailRequest,
    SessionDetailResponse,
    SessionListRequest,
    SessionListResponse,
)
from app.routers import knowledge_router
from app.services.chat_service import ChatService
from app.utils.logger import configure_logging, get_logger

configure_logging()
logger = get_logger(__name__)

settings = get_settings()
chat_service = ChatService(settings)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # uvicorn 启动时会覆盖 logging 配置，在 lifespan 中重新配置确保文件 handler 生效
    configure_logging()
    logger.info("ai_service_starting", app_name=settings.app_name, env=settings.app_env)
    if len(settings.internal_api_token) < 32:
        logger.error("internal_api_token_too_short")
        raise RuntimeError("INTERNAL_API_TOKEN 长度不能少于 32 位，请配置强随机字符串")
    yield
    logger.info("ai_service_stopping")
    await chat_service.close()
    await close_shared_redis()


app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(RateLimitMiddleware)
app.add_middleware(InternalAuthMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(knowledge_router.router, prefix="/api/ai-assistant")


@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    latency = time.time() - start
    logger.info(
        "http_request",
        method=request.method,
        path=request.url.path,
        status_code=response.status_code,
        latency_ms=round(latency * 1000, 2),
    )
    return response


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.warning("request_validation_error", path=request.url.path, error=str(exc))
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content=ErrorResponse(
            code="INVALID_REQUEST",
            message="请求参数校验失败",
        ).model_dump(),
    )


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error("unhandled_exception", path=request.url.path, error=str(exc))
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=ErrorResponse(
            code="INTERNAL_ERROR",
            message="leyo 暂时走神了，请稍后再试",
        ).model_dump(),
    )


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.post("/api/ai-assistant/chat", response_model=ChatResponse)
async def chat(body: ChatRequest) -> ChatResponse:
    try:
        return await chat_service.chat(body)
    except Exception as exc:
        logger.error("chat_endpoint_error", error=str(exc))
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content=ErrorResponse(
                code="INTERNAL_ERROR",
                message="leyo 暂时走神了，请稍后再试",
            ).model_dump(),
        )


@app.post("/api/ai-assistant/session/create", response_model=SessionCreateResponse)
async def create_session() -> SessionCreateResponse:
    result = await chat_service.create_session()
    return SessionCreateResponse(**result)


@app.post("/api/ai-assistant/session/list", response_model=SessionListResponse)
async def list_sessions(request: SessionListRequest) -> SessionListResponse:
    result = await chat_service.list_sessions(request.user_hash or "", request.page, request.size)
    return SessionListResponse(**result)


@app.post("/api/ai-assistant/session/detail", response_model=SessionDetailResponse)
async def session_detail(request: SessionDetailRequest) -> SessionDetailResponse:
    result = await chat_service.get_session_detail(request.session_id)
    return SessionDetailResponse(**result)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        log_level=settings.log_level.lower(),
        reload=settings.app_env == "development",
    )
