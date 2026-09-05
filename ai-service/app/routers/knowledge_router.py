from typing import Any

from fastapi import APIRouter, status
from pydantic import BaseModel, ConfigDict, Field

from app.config import get_settings
from app.services.knowledge_service import KnowledgeService
from app.stores.vector_store import ChromaVectorStore
from app.utils.logger import get_logger

logger = get_logger(__name__)

router = APIRouter(prefix="/knowledge", tags=["knowledge"])

settings = get_settings()
knowledge_service = KnowledgeService(
    vector_store=ChromaVectorStore(settings),
    settings=settings,
)


class IngestRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    document_id: str = Field(..., min_length=1, max_length=64, alias="documentId")
    title: str = Field(..., min_length=1, max_length=200)
    category: str = Field(..., min_length=1, max_length=50)
    content: str = Field(..., min_length=1)


class DeleteRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    document_id: str = Field(..., min_length=1, max_length=64, alias="documentId")


class RebuildRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    document_id: str = Field(..., min_length=1, max_length=64, alias="documentId")
    title: str = Field(..., min_length=1, max_length=200)
    category: str = Field(..., min_length=1, max_length=50)
    content: str = Field(..., min_length=1)


class KnowledgeResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    document_id: str = Field(..., alias="documentId")
    status: str


@router.post("/ingest", response_model=KnowledgeResponse, status_code=status.HTTP_200_OK)
async def ingest(request: IngestRequest) -> dict[str, Any]:
    logger.info(
        "knowledge_router_ingest",
        document_id=request.document_id,
        title=request.title,
        category=request.category,
    )
    return await knowledge_service.ingest(
        document_id=request.document_id,
        title=request.title,
        category=request.category,
        content=request.content,
    )


@router.post("/delete", response_model=KnowledgeResponse, status_code=status.HTTP_200_OK)
async def delete(request: DeleteRequest) -> dict[str, Any]:
    logger.info("knowledge_router_delete", document_id=request.document_id)
    return await knowledge_service.delete(document_id=request.document_id)


@router.post("/rebuild", response_model=KnowledgeResponse, status_code=status.HTTP_200_OK)
async def rebuild(request: RebuildRequest) -> dict[str, Any]:
    logger.info(
        "knowledge_router_rebuild",
        document_id=request.document_id,
        title=request.title,
        category=request.category,
    )
    return await knowledge_service.rebuild(
        document_id=request.document_id,
        title=request.title,
        category=request.category,
        content=request.content,
    )
