# AI-Service MCP Server 需求文档

> **文档性质**：ai-service 能力 MCP 化的需求规格说明书
> **版本**：v1.0
> **创建日期**：2026-09-06
> **适用范围**：MVP 阶段（本地开发环境）

---

## 1. 背景与目标

### 1.1 背景

`ai-service` 已具备两类经过生产验证的 AI 能力：

- **知识库检索**（`query_knowledge`）：Chroma 向量检索 + DashScope Embedding，含来源标注
- **业务推荐**（`query_coaches` / `query_packages` / `get_hot_recommendations`）：经 `JavaInternalClient` 调用 backend 查询教练/套餐业务数据

但这些能力目前只能在 ai-service 自身的 chat 链路（LangChain function calling）内使用。MCP（Model Context Protocol）是 AI 工具互操作的事实标准，Trae、Claude Desktop、Cursor 等主流 Agent 客户端均原生支持。将 ai-service 封装为 MCP server 后，任何 MCP 客户端都能直接复用这套能力。

直接收益：开发 leyoSwimming 项目时，编码 Agent 可通过 MCP 直接查询游泳知识库与业务数据（如"课时预占规则是什么"命中知识库、"查一下蛙泳女教练"命中推荐链路），形成 dogfooding 闭环；同时为后续多 Agent 场景（如运营 Agent、客服 Agent）提供统一的 AI 能力入口。

### 1.2 目标

- ai-service 挂载 MCP endpoint（Streamable HTTP 传输），与现有 REST API 同进程同端口共存
- 暴露 **4 个只读 Tool**：`query_knowledge`、`query_coaches`、`query_packages`、`get_hot_recommendations`
- MCP 端点使用**独立 Token 鉴权**（与 `INTERNAL_API_TOKEN` 分离，可独立轮换）
- 在 Trae 中完成本地注册与端到端验证（真实调用 4 个 tool 各至少 1 次成功）

---

## 2. 需求范围

### 2.1 In Scope（MVP）

- 安装 `mcp` 官方 Python SDK，使用 `FastMCP` 挂载到现有 FastAPI app（`app.mount()`，同进程）
- 封装 4 个只读 Tool，复用现有 Service 层（`KnowledgeService` / `JavaInternalClient`），不重复实现业务逻辑
- Tool description 面向调用方 LLM 优化：写明用途、参数语义（中文泳姿 → 归一化说明）、返回结构
- 新增 `MCP_API_TOKEN` 环境变量（≥32 位强随机），MCP 路径独立校验；`InternalAuthMiddleware` 放行 MCP 路径（`/mcp-server/**`），MCP 层自行鉴权
- MCP 路径纳入现有 `RateLimitMiddleware` 限流覆盖
- Trae 本地注册（`.trae/mcp.json` 或等价配置），带 token，验证连接与工具调用
- 更新部署文档：`deploy/.env` 增加 `MCP_API_TOKEN` 说明

### 2.2 Out of Scope（MVP 不做）

- `web_search`（Tavily）暴露——主流 MCP 客户端自带联网能力，重复暴露无价值
- `get_user_profile` / `get_user_packages` 暴露——这两个工具依赖终端用户身份（`user_hash` 闭包注入），MCP 场景无用户上下文；若以参数形式暴露，任何持有 MCP token 的 Agent 都可查询**任意用户**的画像与订单，构成越权面，**安全上不可接受**
- `ingest` 等写操作暴露——MCP 客户端不受信，写操作暴露需更完善的审计与授权体系
- `chat` 完整对话链路暴露——依赖会话管理（Redis），与 MCP 工具调用模型不匹配
- 远程服务器部署 + Nginx 反向代理 + 公网 HTTPS（后续阶段，届时需 token 轮换与访问审计）
- MCP OAuth 2.1 完整授权流程（本地/内网场景静态 token 足够）
- backend 业务 API 的 MCP 化（仅暴露 ai-service 已有能力）
- 多租户 / 多 token 权限分级

---

## 3. 用户故事（草稿级）

### US-A：系统-MCP 暴露知识库检索工具
> 作为系统，我需要通过 MCP 协议暴露知识库检索能力，以便任何 MCP 客户端 Agent 都能查询游泳知识库。

**验收标准**
- FastMCP 挂载到现有 FastAPI app，`/mcp-server/**` 路径可完成 MCP 握手（initialize → tools/list → tools/call）
- `query_knowledge(query, top_k?)` 返回与内部 LangChain tool 一致的结构（content/source/source_type/category/score）
- 无有效 MCP token 的请求被拒绝（HTTP 401），现有 REST 链路行为不受影响
- 相似度阈值复用 `KNOWLEDGE_SIMILARITY_THRESHOLD` 配置

### US-B：系统-MCP 暴露业务推荐工具
> 作为系统，我需要通过 MCP 协议暴露教练/套餐推荐查询能力，以便编码 Agent 能直接查询业务数据辅助开发。

**验收标准**
- `query_coaches(stroke?, gender?, max_price?, max_age?, class_size?, limit?)`、`query_packages(stroke?, package_mode?, max_price?, hours?, limit?)`、`get_hot_recommendations(stroke?, limit?)` 三个工具可用
- 中文参数归一化复用现有逻辑（`normalize_stroke` / `normalize_gender` / package_mode 归一化）
- backend 不可达时工具返回结构化错误信息，不崩溃、不影响知识库工具
- Tool description 明确说明这是查询游泳培训平台的教练/套餐数据

### US-C：开发者-Trae 注册并验证 MCP 工具
> 作为开发者，我希望在 Trae 中注册 ai-service 的 MCP server 并验证工具可用，以便日常开发中直接使用这些 AI 能力。

**验收标准**
- Trae MCP 配置指向本地 endpoint 并携带 token，连接状态正常
- 4 个 tool 均出现在工具列表，description 完整
- 真实调用 `query_knowledge` 与 `query_coaches` 各一次，返回有效结果
- 验证记录（含截图或调用日志）归档到需求文档附录或验收记录

---

## 4. 架构设计

### 4.1 整体架构

```
┌─────────────────┐
│  Trae (MCP Client) │──── token ────┐
└─────────────────┘                │
                                   ▼
              ┌──────────────────────────────────────┐
              │  ai-service (FastAPI, :8000)         │
              │  ┌────────────────────────────────┐  │
              │  │ CORSMiddleware                 │  │
              │  │ InternalAuthMiddleware ──放行──┼──┼── /mcp-server/** 单独鉴权
              │  │ RateLimitMiddleware（覆盖 MCP）│  │
              │  └────────────────────────────────┘  │
              │  /api/ai-assistant/*  既有 REST 链路   │
              │  /api/internal/ai/*   内部 ingest 链路 │
              │  /mcp-server/mcp     FastMCP(挂载)    │
              │      ├─ query_knowledge ── KnowledgeService ─ Chroma
              │      ├─ query_coaches ────┐            │
              │      ├─ query_packages ───┼─ JavaInternalClient ─ backend(:8080)
              │      └─ get_hot_reco ─────┘            │
              └──────────────────────────────────────┘
```

### 4.2 关键设计决策

1. **同进程挂载，不独立部署**：`FastMCP.streamable_http_app()` 产出 ASGI 子应用，`app.mount("/mcp-server", ...)` 到现有 app。单进程单端口，复用日志/配置/生命周期管理，运维零增量
2. **共享 Service 层而非 Tool 层**：现有 LangChain `@tool` 是闭包构建（`build_tools(client, user_hash)`），与 MCP 的注册模型不兼容。MCP tool 直接调用 `KnowledgeService` / `JavaInternalClient`，归一化函数（`normalize_stroke` 等）提取为模块级共享——两个协议层是薄壳，业务逻辑只有一份
3. **鉴权分层**：`InternalAuthMiddleware` 按路径放行 `/mcp-server/**`；FastMCP 侧（挂载前包一层 ASGI middleware 或在 mount app 上加校验）用 `MCP_API_TOKEN` 独立校验 `Authorization: Bearer` header。token 独立于 `INTERNAL_API_TOKEN`，因为 Trae 配置文件是新的泄露面
4. **传输协议**：Streamable HTTP（MCP 2025-03-26 规范），单 endpoint 支持请求/通知/流式响应；不使用已标记 deprecated 的 HTTP+SSE 传输和 stdio（远程不可用）
5. **限流复用**：MCP 路径不过 `InternalAuthMiddleware` 但仍过 `RateLimitMiddleware`（IP 维度），防止外部 Agent 高频调用耗尽 Embedding 配额

### 4.3 核心数据流（query_coaches 为例）

```
Trae Agent → POST /mcp-server/mcp (JSON-RPC tools/call, Bearer MCP_API_TOKEN)
  → FastMCP 校验 token → 解析参数（stroke="蛙泳"）
  → normalize_stroke("蛙泳") → "breaststroke"
  → JavaInternalClient.query_coaches(stroke="breaststroke", ...)
  → backend /api/internal/ai/coach/list（backend 可达性依赖）
  → 结构化 JSON 返回 → Trae Agent 拿到教练列表
```

---

## 5. 非功能需求与风险

| 类别 | 要求/风险 | 应对 |
|------|-----------|------|
| 安全 | token 在 `.trae/mcp.json` 中明文存储 | 本地开发可接受；远程部署阶段引入 HTTPS + 定期轮换 + 访问日志审计 |
| 安全 | 越权面收窄 | 仅暴露无用户身份依赖的只读工具；用户画像/订单工具明确排除（§2.2） |
| 兼容性 | MCP SDK 与 Trae 客户端的协议版本匹配 | 选 Streamable HTTP（双端主流支持）；若 Trae 版本不支持则回退 SSE 传输（FastMCP 同样支持，改动仅限挂载方式） |
| 性能 | 每次 `query_knowledge` 调用消耗一次 DashScope Embedding 配额 | 依赖 RateLimitMiddleware 限流；`top_k` 参数限制上限（≤10） |
| 可用性 | 推荐工具依赖 backend 可达（本地需先启动 backend :8080） | 工具内部 try/catch 返回结构化错误；MCP 服务启动不依赖 backend，故障隔离 |
| 可运维性 | lifespan 冲突：FastMCP 子应用与主 app 的 session manager 生命周期 | 挂载时合并 lifespan（官方 SDK 提供 `streamable_http_app()` + lifespan 注入方式），细节在 tech-design 明确 |

---

## 附录：变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-09-06 | 初版：MCP server MVP 需求（4 只读工具 / 独立 token / 本地 Trae 验证） |
