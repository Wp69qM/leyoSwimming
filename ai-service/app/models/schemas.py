from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    session_id: str = Field(..., min_length=1, max_length=64)
    message: str = Field(..., min_length=1, max_length=500)
    user_hash: str | None = Field(default=None, max_length=64)


class SessionListRequest(BaseModel):
    user_hash: str | None = Field(default=None, max_length=64)
    page: int = Field(default=1, ge=1)
    size: int = Field(default=20, ge=1, le=100)


class SessionDetailRequest(BaseModel):
    session_id: str = Field(..., min_length=1, max_length=64)


class RecommendationItem(BaseModel):
    type: Literal["coach", "package", "custom_package"]
    id: int | None = None
    name: str
    reason: str
    avatar_url: str | None = None
    rating: float | None = None
    teaching_years: int | None = None
    reference_price: int | None = None
    price: int | None = None
    hours: int | None = None
    price_per_hour: int | None = None
    total_price: int | None = None
    class_size: str | None = None
    validity_days: int | None = None
    strokes: list[str] | None = None
    coach_id: int | None = None
    coach_name: str | None = None


class ChatReply(BaseModel):
    text: str
    recommendations: list[RecommendationItem] = Field(default_factory=list)
    suggested_questions: list[str] = Field(default_factory=list)


class ChatResponse(BaseModel):
    session_id: str
    message_id: str
    reply: ChatReply


class SessionCreateResponse(BaseModel):
    session_id: str
    welcome_message: str
    suggested_questions: list[str] = Field(default_factory=list)


class HistorySessionItem(BaseModel):
    session_id: str
    title: str
    last_message_at: datetime
    message_count: int


class SessionListResponse(BaseModel):
    items: list[HistorySessionItem]
    total: int
    page: int
    size: int


class SessionDetailResponse(BaseModel):
    session_id: str
    messages: list[dict[str, Any]]


class HealthResponse(BaseModel):
    status: str
    version: str = "1.0.0"


class ErrorResponse(BaseModel):
    code: str
    message: str
