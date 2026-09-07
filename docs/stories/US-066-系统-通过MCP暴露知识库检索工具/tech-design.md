# Tech Design: US-066 系统-通过MCP暴露知识库检索工具

## 1. 总体架构

```
外部 MCP 客户端（Trae / Claude Desktop）
    │  Authorization: Bearer <MCP_API_TOKEN>
    ▼
ai-service FastAPI (:8000)
    ├── CORSMiddleware
    ├── InternalAuthMiddleware ──放行── /mcp-server/**
    ├── RateLimitMiddleware（覆盖 MCP 路径）
    │
    ├── /api/ai-assistant/*     既有 REST 链路（不动）
    └── /mcp-server/mcp         FastMCP（streamable_http_app mount）
            │
            ├── MCP Token 鉴权（ASGI 中间件，独立于 INTERNAL_API_TOKEN）
            │
            └── query_knowledge 工具
                    │
                    ▼
                KnowledgeService.query() ──→ Chroma knowledge_base
```

## 2. 新增/修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `ai-service/requirements.txt` | 修改 | 新增 `mcp` 官方 SDK 依赖 |
| `ai-service/app/config.py` | 修改 | 新增 `mcp_api_token`、`mcp_top_k_max`（默认 10） |
| `ai-service/app/mcp/__init__.py` | 新增 | MCP 包入口 |
| `ai-service/app/mcp/auth.py` | 新增 | MCP token 校验 ASGI 中间件（Bearer header） |
| `ai-service/app/mcp/server.py` | 新增 | FastMCP 实例 + `query_knowledge` 工具注册 |
| `ai-service/app/main.py` | 修改 | `app.mount("/mcp-server", mcp_app)`；lifespan 校验 token 长度 |
| `ai-service/app/middleware/auth.py` | 修改 | `InternalAuthMiddleware` 放行 `/mcp-server/**` |
| `ai-service/app/middleware/rate_limit.py` | 修改 | 限流路径前缀扩展覆盖 `/mcp-server/**`（仅 IP 维度） |
| `ai-service/.env.example` | 修改 | 增加 `MCP_API_TOKEN` 说明 |

## 3. 关键实现

### 3.1 FastMCP 挂载（server.py）

```python
from mcp.server.fastmcp import FastMCP
from mcp.server.transport_security import TransportSecuritySettings

# 显式禁用 DNS 重绑定保护：FastMCP 在 host 为默认 127.0.0.1 时会自动启用
# （allowed_hosts 仅 localhost），生产环境真实域名 Host 会被 421 拒绝；
# MCP 端点已有独立 Bearer 鉴权 + IP 限流，该保护不适用。
mcp = FastMCP(
    "leyo-ai-service",
    stateless_http=True,
    transport_security=TransportSecuritySettings(enable_dns_rebinding_protection=False),
)

@mcp.tool()
async def query_knowledge(query: str, top_k: int = 3) -> dict:
    """从游泳培训平台知识库检索相关知识片段。"""
    service = _get_knowledge_service()  # 复用 knowledge_tools 的单例
    top_k = min(top_k, settings.mcp_top_k_max)
    raw = await service.query(query=query, top_k=top_k,
                              threshold=settings.knowledge_similarity_threshold)
    # {"list": [...]} 包装：FastMCP 会把 list 返回值展开为多个 content 块、
    # 空 list 产生空 content，均不利于 MCP 客户端解析
    return {"list": [{"content": r["content"], "source": r["title"],
             "source_type": "knowledge_base", "category": r["category"],
             "score": r["score"]} for r in raw]}
```

挂载方式：`mcp_app = mcp.streamable_http_app()`，在 `main.py` 中 `app.mount("/mcp-server", mcp_app)`。生命周期：主 app lifespan 内 `await mcp.session_manager.run()` / `close()`（或采用 SDK 提供的挂载时自动管理方式，实现时以官方文档为准）。

### 3.2 MCP 独立鉴权（auth.py）

- 校验 `Authorization: Bearer <MCP_API_TOKEN>`，不匹配返回 401
- 与 `INTERNAL_API_TOKEN` 完全独立，可单独轮换
- `InternalAuthMiddleware` 增加 `/mcp-server` 前缀放行（避免双重校验冲突）

### 3.3 配置（config.py）

```python
mcp_api_token: str = Field(default="")
mcp_top_k_max: int = Field(default=10)
```

lifespan 启动校验：`MCP_API_TOKEN` 长度 < 32 时 `RuntimeError`（与 `INTERNAL_API_TOKEN` 同等强度）。

## 4. 设计决策

| 决策 | 选项 | 结论 |
|------|------|------|
| 部署形态 | 独立进程 / 同进程 mount | **同进程 mount**：单端口、复用日志配置与限流，运维零增量 |
| 共享层 | 包装 LangChain @tool / 直调 Service | **直调 KnowledgeService**：LangChain tool 是闭包构建，与 MCP 注册模型不兼容 |
| 传输协议 | stdio / SSE（deprecated）/ Streamable HTTP | **Streamable HTTP**：远程可用且为当前规范推荐 |
| 阈值 | MCP 独立阈值 / 复用现有配置 | **复用 `KNOWLEDGE_SIMILARITY_THRESHOLD`**：两条协议链路口径一致 |
| 工具返回结构 | 裸 list / `{"list": [...]}` 包装 | **`{"list": [...]}` 包装**：FastMCP 会把 list 返回值展开为多个 content 块、空 list 产生空 content，单 JSON 块对客户端最友好，且与项目 `data.list` 约定对齐 |
| DNS 重绑定保护 | SDK 默认（localhost 白名单）/ 显式禁用 | **显式禁用**：默认配置下生产域名 Host 会被 421 拒绝；MCP 端点已有 Bearer 鉴权 + IP 限流，浏览器侧 DNS rebinding 攻击无法携带有效 token |

## 5. 风险与对策

- **SDK 版本兼容**：MCP SDK 迭代快，锁定 minor 版本；若 Trae 不支持 Streamable HTTP 则降级 SSE（仅改挂载方式）
- **限流粒度**：当前 `RateLimitMiddleware` 以 IP 计，MCP 客户端单 IP 高频调用的极端场景可后续加 token 维度限流
