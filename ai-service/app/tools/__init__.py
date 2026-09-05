from typing import Any

from app.clients.java_client import JavaInternalClient
from app.tools.knowledge_tools import build_tools as build_knowledge_tools
from app.tools.recommendation_tools import build_tools as build_recommendation_tools


def build_tools(client: JavaInternalClient, user_hash: str | None = None) -> list[Any]:
    """组合推荐工具与知识库/联网搜索工具。"""
    return build_recommendation_tools(client, user_hash) + build_knowledge_tools()


__all__ = ["build_tools"]
