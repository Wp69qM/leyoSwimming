# 第五批次开发设计文档 - AI 助理 MCP 能力暴露

> **批次范围**：US-066 ~ US-068
> **批次目标**：将 ai-service 已建成的知识库检索与业务推荐能力通过 MCP（Model Context Protocol）协议对外暴露，在 Trae 等 MCP 客户端完成注册与端到端验证，形成 AI 能力平台化入口与 dogfooding 闭环。
> **状态**：需求设计已 APPROVAL（业务评审通过，见 [review-mcp-exposure-us066-068-v1.md](../guides/review-mcp-exposure-us066-068-v1.md)），进入开发阶段
> **创建日期**：2026-09-07
> **重要特征**：纯后端能力暴露，**无前端页面交付物、无数据表变更、无 backend 代码改动**

---

## 1. 涉及用户故事

| US | 标题 | 状态 | 估时 | 优先级 |
|----|------|------|------|--------|
| US-066 | 系统-通过MCP暴露知识库检索工具 | [APPROVAL] | 1.5 人天 | MVP |
| US-067 | 系统-通过MCP暴露业务推荐查询工具 | [APPROVAL] | 1 人天 | MVP |
| US-068 | 系统-MCP服务注册Trae并完成端到端验证 | [APPROVAL] | 0.5 人天 | MVP |

---

## 2. 开发顺序与依赖关系

```
US-062/064（已完成：知识库向量数据 + KnowledgeService）
US-058（已完成：JavaInternalClient 查询链路）
    │
    ▼
US-066 MCP 基础设施（FastMCP 挂载 + 独立鉴权 + 限流 + query_knowledge）
    │
    ▼
US-067 推荐工具 ×3（normalizers 提取 + query_coaches/query_packages/get_hot_recommendations）
    │
    ▼
US-068 Trae 注册 + 端到端验证（MVP 链路终点）
```

### 2.1 推荐的开发顺序

**严格串行**，三者为链式依赖，不可并行：

1. **US-066 先行**：建立 MCP 基础设施（`app/mcp/` 包、鉴权中间件、限流打通、FastMCP 挂载）+ 首个工具 `query_knowledge`。基础设施不就绪，US-067 无处注册工具。
2. **US-067 随后**：提取 `normalizers.py` 共享模块（先做，保证 LangChain 链路行为不变的回归），再注册 3 个推荐工具。
3. **US-068 收尾**：4 工具全部可用后，注册 Trae 配置并完成端到端验证，回填验证记录。

### 2.2 开发分组建议

| 分组 | 负责内容 | 可并行 |
|------|----------|--------|
| ai-service 组 | `app/mcp/` 包、normalizers 提取、config、中间件改造、挂载 | 单人串行（总量 3 人天） |
| 测试组 | 单元/集成测试与开发同步（TDD RED → GREEN，对应 OpenSpec tasks.md） | 与 ai-service 组结对 |

> 本批次无 backend、web-admin、miniapp 改动；backend 仅作为推荐工具的运行时数据源（本地启动 :8080 即可）。

---

## 3. 数据表变更

**无。**

- MySQL：不新增、不修改任何表。
- Chroma：`knowledge_base` collection 只读复用（US-062/064 既有数据），无写路径。
- MCP 工具对 Chroma 的访问全部经由 `KnowledgeService.query()`，无直接存储操作。

---

## 4. API 清单

### 4.1 MCP Endpoint（新增）

| Endpoint | 协议 | 鉴权 | 归属 US | 说明 |
|----------|------|------|---------|------|
| `/mcp-server/mcp` | MCP Streamable HTTP（JSON-RPC：initialize / tools/list / tools/call） | `Authorization: Bearer <MCP_API_TOKEN>`（独立于 X-Internal-Token） | US-066 | FastMCP `streamable_http_app()` 挂载 |

> **api-convention 豁免依据**：MCP endpoint 为 JSON-RPC 协议端点，不受 REST 三段式 URL 约束（协议自带 endpoint 语义，天然全 POST；GET 仅用于协议内 SSE 流）。豁免已在 US-066 §7.2/§12 显式记录。

### 4.2 MCP 工具清单（4 个，全部只读）

| 工具名 | 参数 | 返回 | 归属 US |
|--------|------|------|---------|
| `query_knowledge` | `query: str`、`top_k: int = 3`（截断至 10） | `[{content, source, source_type, category, score}]` | US-066 |
| `query_coaches` | `stroke / gender / max_price / max_age / class_size / limit=5`（limit 截断至 20），中文自动归一化 | 教练列表（游客端公开口径） | US-067 |
| `query_packages` | `stroke / package_mode / limit`（同上归一化与截断） | 套餐列表 | US-067 |
| `get_hot_recommendations` | `limit=5`（截断至 20） | 热门推荐列表 | US-067 |

**安全红线——禁止暴露清单**（实现为逐个显式 `@mcp.tool()` 注册，无批量导入路径）：

- `get_user_profile` / `get_user_packages`：user_hash 身份闭包，参数化暴露 = 任意用户数据越权查询面（US-067 §5-2）
- `ingest` / `chat` / `web_search`：管理/对话/联网能力不外放（US-066 §5-1）

### 4.3 复用的既有接口（无新增 REST）

| 接口 | 消费方 | 说明 |
|------|--------|------|
| backend `/api/internal/ai/*`（US-058 链路） | `JavaInternalClient` | 推荐工具数据源，X-Internal-Token 鉴权不变 |
| `/api/ai-assistant/chat` | 小程序既有链路 | MCP 挂载后行为必须零回归（US-066 §6.5） |

### 4.4 中间件改造

| 中间件 | 变更 | 归属 US |
|--------|------|---------|
| `InternalAuthMiddleware` | 放行 `/mcp-server/**` 前缀（MCP 层自行鉴权，避免双重校验冲突） | US-066 |
| `RateLimitMiddleware` | 限流路径前缀扩展：`/api/ai-assistant/` + `/mcp-server/`（MCP 路径仅 IP 维度，token 维度不适用）。> 注：规划时误判为"无代码改动"，实现时确认 dispatch 仅拦截 `/api/ai-assistant/`，已修正 | US-066 |

---

## 5. 关键实现点

### 5.1 ai-service 文件结构

```
ai-service/
├── app/
│   ├── mcp/                        # 新增包（US-066）
│   │   ├── __init__.py
│   │   ├── auth.py                 # MCP token 校验 ASGI 中间件（Bearer header，401 拒绝）
│   │   └── server.py               # FastMCP 实例 + 4 个工具逐个显式注册
│   ├── tools/
│   │   ├── normalizers.py          # 新增（US-067）：normalize_stroke/gender/package_mode 模块级共享
│   │   └── recommendation_tools.py # 修改：改为引用 normalizers.py，行为不变
│   ├── config.py                   # 修改：新增 mcp_api_token、mcp_top_k_max（默认 10）
│   ├── main.py                     # 修改：app.mount("/mcp-server", mcp_app)；lifespan 校验 token
│   ├── middleware/auth.py          # 修改：InternalAuthMiddleware 放行 /mcp-server/**
│   └── middleware/rate_limit.py    # 修改：限流路径前缀扩展覆盖 /mcp-server/（仅 IP 维度）
├── tests/mcp/                      # 新增测试目录
│   ├── test_auth.py
│   ├── test_server.py              # FastMCP 挂载 + query_knowledge 工具
│   ├── test_rate_limit.py          # MCP 路径限流覆盖
│   └── test_recommendation_tools.py
├── scripts/
│   └── verify_mcp.py               # 新增（US-068）：SDK client 自动化验证脚本（可选辅助）
├── requirements.txt                # 修改：新增 mcp 官方 SDK 依赖（锁定 minor 版本）
└── .env.example                    # 修改：MCP_API_TOKEN 生成说明
```

### 5.2 核心逻辑（US-066）

1. **挂载**：`mcp = FastMCP("leyo-ai-service", stateless_http=True)`；`main.py` 中 `app.mount("/mcp-server", mcp.streamable_http_app())`，lifespan 内管理 `session_manager`（实现时以 MCP SDK 官方文档为准）。
2. **鉴权**：`auth.py` 校验 `Authorization: Bearer <MCP_API_TOKEN>`，不匹配返回 401；与 `INTERNAL_API_TOKEN` 完全独立、可单独轮换。
3. **token 强度**：lifespan 启动校验 `MCP_API_TOKEN` 长度 < 32 时 `RuntimeError`（与 `INTERNAL_API_TOKEN` 同等强度）。
4. **知识工具**：直调 `KnowledgeService.query()`（复用 knowledge_tools 单例），`top_k = min(top_k, mcp_top_k_max)` 无条件截断，阈值复用 `KNOWLEDGE_SIMILARITY_THRESHOLD`；空结果返回 `[]`，由调用方 Agent 自行兜底。

### 5.3 核心逻辑（US-067）

1. **normalizers 提取先行**：从 `recommendation_tools.py` 闭包原样迁移三个归一化函数，LangChain 与 MCP 两条协议层共用同一实现；先跑既有 LangChain 回归测试确认行为不变。
2. **推荐工具**：直调 `JavaInternalClient`（不包装 LangChain @tool 闭包）；中文参数归一化，未知值降级为 None 按无过滤查询（静默降级）；`limit = min(limit, 20)`。
3. **故障隔离**：工具内 try/catch，backend 不可达/超时返回结构化 `{"error": "业务数据服务暂不可用，请稍后再试"}`，不抛异常、不崩溃、不影响其他工具。

### 5.4 核心逻辑（US-068）

1. **Trae 注册**：项目级 `.trae/mcp.json`（以 Trae 当前版本实际配置入口格式为准），URL + Bearer header 两要素；token 不提交仓库（占位符 + 本地替换）。
2. **验证双轨**：SDK 脚本（initialize → tools/list → 4 工具各调用一次）可重复执行；Trae 手动验证覆盖真实交互。
3. **记录归档**：验证结果表回填 US-068 §15；`deploy/.env` 模板与部署文档补充 `MCP_API_TOKEN` 与注册步骤。

---

## 6. 测试策略

> 开发遵循 TDD（RED → GREEN → COMMIT），测试用例与 OpenSpec tasks.md 的 REQ 标注一一对应。

### 6.1 单元测试

| 测试对象 | 归属 | 覆盖场景 |
|----------|------|---------|
| `normalize_stroke/gender/package_mode` | US-067 | 中文归一化正确；未知值（"狗刨"）降级 None |
| top_k 截断 `min(top_k, 10)` | US-066 | top_k=100 → 实际检索 ≤ 10（REQ-005） |
| limit 截断 `min(limit, 20)` | US-067 | limit=1000 → 实际返回 ≤ 20（REQ-005） |
| `query_knowledge` 工具 | US-066 | Mock KnowledgeService，断言返回结构与 source 字段 |
| 推荐工具错误隔离 | US-067 | Mock JavaInternalClient 抛异常 → 返回结构化 error |

### 6.2 集成测试（MCP SDK client 或 HTTP 直调）

| 场景 | 归属 | 对应 Gherkin |
|------|------|--------------|
| initialize + tools/list 握手成功，清单含 4 工具 | US-066/067 | US-066 §6.1 |
| 无/错 token → 401，不执行工具 | US-066 | US-066 §6.3 |
| 知识检索返回带来源片段；无匹配返回空数组 | US-066 | US-066 §6.2/§6.4 |
| tools/list **不含** get_user_profile / get_user_packages | US-067 | US-067 §6.4（安全红线断言） |
| query_coaches(stroke="蛙泳", gender="女") 归一化过滤 | US-067 | US-067 §6.1 |
| backend 停止 → 推荐工具结构化错误，知识工具正常 | US-067 | US-067 §6.3 |
| 高频调用 → 429 限流 | US-066 | US-066 §8.2 |
| MCP 挂载后既有 REST 链路（ingest / chat）行为不变 | US-066 | US-066 §6.5（零回归） |

### 6.3 E2E 验证（US-068，人工 + 脚本双轨）

| 场景 | 说明 |
|------|------|
| Trae 注册连接成功 | 重载配置后连接状态正常，tools 列表 4 工具 |
| 4 工具真实调用 | query_knowledge("野泳的注意事项") / query_coaches(stroke="蛙泳") / query_packages / get_hot_recommendations 各 ≥1 次成功 |
| token 错误 | 连接失败（401），原因可从 Trae 提示或 ai-service 日志定位 |
| 服务未启动 | 连接拒绝（非模糊协议错误） |
| 服务重启恢复 | ai-service 重启后 Trae 重连无残留会话问题 |

---

## 7. 环境变量与配置

| 配置项 | 说明 | 示例/默认 |
|--------|------|-----------|
| `MCP_API_TOKEN` | **新增**。MCP endpoint 独立鉴权 token，≥32 位强随机，与 `INTERNAL_API_TOKEN` 分离 | `openssl rand -hex 32` 生成 |
| `MCP_TOP_K_MAX` | **新增**。query_knowledge 检索条数上限 | `10` |
| `KNOWLEDGE_SIMILARITY_THRESHOLD` | 复用（当前值 **0.2**；第四批次文档中 0.7 为历史值，已调优） | `0.2` |
| `INTERNAL_API_TOKEN` | 不变。既有内部接口鉴权，InternalAuthMiddleware 放行 `/mcp-server/**` 后仍保护其余路径 | — |
| 限流配置 | 复用 `RateLimitMiddleware` 既有配置（IP 维度） | — |

**部署同步**（US-068 交付物）：`deploy/.env` 模板与部署文档增加 `MCP_API_TOKEN` 说明、生成方式与 Trae 注册步骤；`.trae/mcp.json` 中 token 使用占位符，本地替换，不入仓库。

---

## 8. 风险与回滚方案

| 风险 | 影响 | 缓解/回滚措施 |
|------|------|---------------|
| MCP SDK 版本迭代快 | 中 | requirements.txt 锁定 minor 版本；Trae 不支持 Streamable HTTP 时降级 SSE（仅改挂载方式） |
| Trae MCP 配置入口随版本变化 | 低 | tech-design §5 已声明以当前 IDE 实际格式为准，核心是 URL + Bearer header 两要素 |
| token 泄露面（.trae/mcp.json 明文） | 低（本地阶段） | 占位符 + 本地替换约定；远程部署阶段（Nginx 反代 + HTTPS + 轮换审计）为后续独立需求 |
| 外部 Agent 耗尽 Embedding 配额 | 中 | RateLimitMiddleware IP 维度限流 + top_k ≤ 10 + limit ≤ 20 三重防护；极端场景可加 token 维度限流 |
| backend 依赖（推荐工具数据源） | 低 | 工具层 try/catch 故障隔离，MCP 服务启动不依赖 backend；本地验证需先启动 backend |
| MCP 挂载影响既有链路 | 高（概率低） | US-066 §6.5 零回归场景为必测项；异常时注释掉 `app.mount` 一行即完全回退 |

### 回滚方案

- **代码**：MCP 能力全部收敛在 `app/mcp/` 包与 `main.py` 一行 mount，回滚 = 移除 mount（或整体 revert），既有 REST 链路零影响。
- **配置**：清空/变更 `MCP_API_TOKEN` 即刻使所有外部客户端失效（天然熔断开关）。
- **数据**：无数据变更，无数据回滚需求。

---

## 9. 验收标准

- [x] US-066：MCP 握手与 tools/list 正常；query_knowledge 返回带来源片段（content/source/source_type/category/score）；无 token 401；无匹配空数组；top_k 截断 10；限流 429；既有 REST 链路零回归
- [x] US-067：query_coaches/query_packages/get_hot_recommendations 可用且中文归一化正确；tools/list 不含 get_user_profile / get_user_packages；backend 不可达时结构化错误且不影响其他工具；limit 截断 20；未知参数静默降级
- [x] US-068：Trae 注册连接成功；4 工具真实调用各 ≥1 次成功；token 错误/服务未启动提示明确；验证记录回填 US-068 §15；deploy/.env 与部署文档同步更新
- [x] 全批次：OpenSpec validate 3/3 通过；测试覆盖率 ≥ 80%（实测 87%，134 passed）

---

## 10. 相关文档

### 10.1 需求与设计

- 需求文档：[docs/prd/mcp-server-requirements.md](../prd/mcp-server-requirements.md)
- 批次梳理：[docs/figma/第五批次页面梳理.md](../figma/第五批次页面梳理.md)
- 业务评审报告：[docs/guides/review-mcp-exposure-us066-068-v1.md](../guides/review-mcp-exposure-us066-068-v1.md)

### 10.2 用户故事三件套

- US-066：[user-story.md](../stories/US-066-系统-通过MCP暴露知识库检索工具/user-story.md) · [tech-design.md](../stories/US-066-系统-通过MCP暴露知识库检索工具/tech-design.md) · [test-plan.md](../stories/US-066-系统-通过MCP暴露知识库检索工具/test-plan.md)
- US-067：[user-story.md](../stories/US-067-系统-通过MCP暴露业务推荐查询工具/user-story.md) · [tech-design.md](../stories/US-067-系统-通过MCP暴露业务推荐查询工具/tech-design.md) · [test-plan.md](../stories/US-067-系统-通过MCP暴露业务推荐查询工具/test-plan.md)
- US-068：[user-story.md](../stories/US-068-系统-MCP服务注册Trae并完成端到端验证/user-story.md) · [tech-design.md](../stories/US-068-系统-MCP服务注册Trae并完成端到端验证/tech-design.md) · [test-plan.md](../stories/US-068-系统-MCP服务注册Trae并完成端到端验证/test-plan.md)

### 10.3 OpenSpec Changes

- `openspec/changes/us-066-mcp-knowledge-tool/`
- `openspec/changes/us-067-mcp-recommendation-tools/`
- `openspec/changes/us-068-mcp-trae-registration/`

### 10.4 规范依据

- API 约定：[docs/tech/api-convention.md](./api-convention.md)（MCP 协议端点豁免依据见 §4.1）
- 部署运维：deploy 目录部署文档（US-068 交付时同步更新 MCP_API_TOKEN 章节）
