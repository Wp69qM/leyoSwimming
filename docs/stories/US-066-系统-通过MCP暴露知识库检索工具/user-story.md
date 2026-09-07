# US-066 系统-通过MCP暴露知识库检索工具

> **状态**：[APPROVAL]
> **优先级**：[MVP]
> **估时**：1.5 人天
> **作者**：AI　|　**最后更新**：2026-09-07
> **配套文档**：需求：[mcp-server-requirements.md](../../prd/mcp-server-requirements.md) · 技术设计：[tech-design.md](./tech-design.md) · 测试计划：[test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-066 |
| **标题** | 系统-通过MCP暴露知识库检索工具 |
| **角色（Actor）** | 系统（消费方为外部 MCP 客户端 Agent，如 Trae） |
| **业务价值（Why）** | 将已建成的游泳知识库检索能力通过 MCP 标准协议开放，任何 MCP 客户端的 Agent 可直接复用，形成 AI 能力平台化入口 |
| **优先级** | [MVP] |
| **估时** | 1.5 人天（L2） |

---

## 2. 触发条件

- **触发方**：外部 MCP 客户端（Trae / Claude Desktop 等）
- **触发动作**：向 `ai-service` 的 MCP endpoint 发起 `initialize` / `tools/list` / `tools/call`（JSON-RPC）
- **触发时机**：Agent 编码/回答过程中需要检索游泳知识时

---

## 3. 前置条件

- [ ] 知识库已存在启用文档且完成向量化（US-062）
- [ ] `KnowledgeService` 检索能力可用（US-064）
- [ ] `ai-service` 正常运行，已配置 `MCP_API_TOKEN`
- [ ] MCP 客户端持有有效 token

---

## 4. 业务流程

### 4.1 主路径

1. MCP 客户端向 `/mcp-server/mcp` 发起 `initialize` 握手（携带 `Authorization: Bearer <MCP_API_TOKEN>`）
2. MCP 鉴权层校验 token 通过
3. 客户端调用 `tools/list`，获得 `query_knowledge` 工具清单（含 description 与参数 schema）
4. 客户端调用 `tools/call`（`query_knowledge(query, top_k?)`）
5. 服务端复用 `KnowledgeService.query()` 检索 Chroma，应用 `KNOWLEDGE_SIMILARITY_THRESHOLD` 阈值
6. 返回结构化片段（content / source / source_type / category / score）

### 4.2 异常分支

- **分支 1**：token 缺失或无效 → MCP 鉴权层拒绝（HTTP 401），不进入工具执行
- **分支 2**：知识库无匹配（低于阈值）→ 返回空数组，由调用方 Agent 自行决定兜底策略
- **分支 3**：请求频率超限 → 复用 `RateLimitMiddleware` 限流（HTTP 429）

---

## 5. 业务规则引用

> prd.md 无 AI 助理章节，遵循 US-062~065 先例引用需求文档。

| # | 规则 | 来源 |
|---|------|------|
| 1 | 仅暴露 4 个只读工具，`ingest`/`chat`/`web_search` 不暴露 | 需求文档 §2.1 / §2.2 |
| 2 | MCP 端点独立 Token 鉴权，与 `INTERNAL_API_TOKEN` 分离 | 需求文档 §4.2-3 |
| 3 | `InternalAuthMiddleware` 放行 `/mcp-server/**`，MCP 层自行鉴权 | 需求文档 §4.2-3 |
| 4 | MCP 路径纳入现有 `RateLimitMiddleware` 限流覆盖 | 需求文档 §4.2-5 |
| 5 | `top_k` 上限 10，防止 Embedding 配额滥用 | 需求文档 §5 |
| 6 | 相似度阈值复用 `KNOWLEDGE_SIMILARITY_THRESHOLD` | 需求文档 §3 US-A |

---

## 6. 验收标准（业务级 Gherkin）

> L2 级：2 正常 + 3 异常 = 5 个场景

### 6.1 场景 1：MCP 握手与工具发现（正常）

```gherkin
Given ai-service 运行中且已配置 MCP_API_TOKEN
When  客户端携带有效 token 发起 initialize 与 tools/list
Then  握手成功且返回工具清单
And   清单包含 query_knowledge
And   工具 description 说明用途与返回结构
```

### 6.2 场景 2：知识检索返回带来源的片段（正常）

```gherkin
Given 知识库存在启用的"野泳安全指南"文档
When  客户端调用 tools/call query_knowledge(query="野泳的注意事项")
Then  返回 HTTP 200
And   片段数组非空且每条包含 content/source/source_type/category/score
And   source 为来源文档标题
```

### 6.3 场景 3：无 token 请求被拒（异常）

```gherkin
Given 客户端未携带 Authorization header
When  发起 initialize 或 tools/call
Then  返回 HTTP 401
And   不执行任何工具逻辑
```

### 6.4 场景 4：知识库无匹配（异常）

```gherkin
Given 知识库中无与 query 相关的启用文档
When  客户端调用 query_knowledge(query="<无关问题>")
Then  返回 HTTP 200
And   片段数组为空
```

### 6.5 场景 5：既有 REST 链路不受影响（异常）

```gherkin
Given MCP endpoint 已挂载
When  backend 按 INTERNAL_API_TOKEN 调用 /api/internal/ai/knowledge/ingest
And   小程序调用 /api/ai-assistant/chat
Then  两条既有链路行为与挂载前完全一致
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

无新增/修改表。读取 Chroma `knowledge_base` collection（复用 US-062/064）。

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/mcp-server/mcp` | POST/GET | 新增 | MCP Streamable HTTP endpoint（JSON-RPC 协议，非 REST；天然满足全 POST 约定，GET 仅用于协议内 SSE 流） |
| 2 | `InternalAuthMiddleware` | — | 修改 | 放行 `/mcp-server/**` 路径 |

### 7.3 状态机影响

无。

---

## 8. 边界场景

### 8.1 边界场景 1：top_k 超限

- **触发条件**：客户端传 `top_k=100`
- **预期行为**：参数校验拒绝或截断为 10
- **用户可见反馈**：返回参数错误或截断后的结果集

### 8.2 边界场景 2：限流触发

- **触发条件**：同一 IP 高频调用 MCP 工具
- **预期行为**：`RateLimitMiddleware` 返回 429
- **用户可见反馈**：调用方 Agent 收到限流响应

### 8.3 边界场景 3：token 长度不足

- **触发条件**：`MCP_API_TOKEN` 未配置或长度 < 32
- **预期行为**：服务启动时校验失败（与 `INTERNAL_API_TOKEN` 同等强度要求）
- **用户可见反馈**：启动日志明确报错

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- US-062 管理员-上传知识库文档（向量库数据）
- US-064 用户-从知识库获取游泳知识（KnowledgeService）

### 9.2 后续 US（依赖本故事）

- US-068 系统-MCP服务注册Trae并完成端到端验证

---

## 10. INVEST 自检

- [x] **I**ndependent - 可独立交付（MCP 基础设施 + 单工具）
- [x] **N**egotiable - 聚焦"暴露能力"而非具体实现
- [x] **V**aluable - AI 能力平台化，编码 Agent 直接复用
- [x] **E**stimable - 1.5 人天
- [x] **S**mall - 单一职责（基础设施 + 知识工具）
- [x] **T**estable - 验收标准全部可客观验证

---

## 11. 完整性检查

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（§13 无 UI 属合理 N/A）
- [x] 业务规则引用明确（需求文档章节）
- [x] 场景数量符合 L2（5 个）
- [x] 配套文档链接有效

---

## 12. 备注

- 本 US 承载 MCP 基础设施（FastMCP 挂载、鉴权、限流打通），US-067 在此之上追加推荐工具
- MCP 协议端点不受 api-convention REST 三段式 URL 约束（协议自带 endpoint 语义）
- LangChain 既有 `@tool` 链路不动，两协议层共享 Service 层

---

## 13. Figma 链接

> 本 US 无 UI 变更，无页面交付物。

| 内容 | 链接 | 状态 |
|------|------|------|
| 无 UI（纯后端能力暴露） | N/A | N/A |

---

## 14. 页面级设计决策

无页面，不适用。

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-09-06 | AI | DRAFT 初版，待业务评审 | — |
| 2026-09-06 | AI | 第五批次业务评审开始，进入 [REVIEW] | 三件套一致性检查进行中 |
| 2026-09-07 | AI | 业务评审通过（8.3/10，见 review-mcp-exposure-us066-068-v1.md）；P1-1 已修复：§8.1 top_k 口径统一为无条件截断 | 状态推进 [APPROVAL] |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-09-06 | AI | 初版：MCP 基础设施 + query_knowledge 工具暴露 |
| v1.1 | 2026-09-07 | AI | 评审修复 P1-1：§8.1 top_k 口径统一为无条件截断；业务评审通过，状态推进 [APPROVAL] |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
