# Test Plan: US-066 系统-通过MCP暴露知识库检索工具

## 1. 测试范围

- MCP 握手（initialize）与工具发现（tools/list）
- `query_knowledge` 工具调用与返回结构
- MCP token 鉴权（有效/缺失/错误）
- `InternalAuthMiddleware` 放行与既有 REST 链路回归
- `top_k` 上限与限流

## 2. 测试用例

### 2.1 单元测试

| 用例 | 输入 | 预期 | 对应场景 |
|------|------|------|---------|
| `test_mcp_tools_list_contains_knowledge_tool` | 有效 token 调 tools/list | 清单含 query_knowledge 及完整 schema | 场景 1 |
| `test_mcp_query_knowledge_returns_structured_chunks` | query="野泳的注意事项" | 数组非空，含 content/source/source_type/category/score | 场景 2 |
| `test_mcp_query_knowledge_empty_result` | query 为无关问题 | 返回空数组 | 场景 4 |
| `test_mcp_top_k_clamped_to_max` | top_k=100 | 实际检索 ≤ 10 | 边界 1 |
| `test_mcp_token_missing_rejected` | 无 Authorization header | 401 | 场景 3 |
| `test_mcp_token_wrong_rejected` | 错误 token | 401 | 场景 3 |

### 2.2 集成测试

| 用例 | 步骤 | 预期 |
|------|------|------|
| MCP 全链路 | initialize → tools/list → tools/call query_knowledge | 三步全部成功，返回知识片段 |
| 既有 REST 回归 | 调 /api/ai-assistant/chat 与 /api/internal/ai/knowledge/ingest | 行为与挂载前一致（场景 5） |
| token 启动校验 | MCP_API_TOKEN 长度 < 32 启动服务 | 启动失败并报错（边界 3） |
| 限流覆盖 | 超 IP 限流阈值连续调用 | 返回 429（边界 2） |

### 2.3 E2E 测试

| 用例 | 步骤 | 预期 |
|------|------|------|
| MCP 客户端真实调用 | 用 MCP SDK client 连接并发起完整会话 | 握手、发现、调用全部成功 |

> 真实 Trae 注册验证由 US-068 承载，本 US 用 SDK 客户端脚本验证。

## 3. 验收检查清单

- [ ] initialize / tools/list / tools/call 三步链路通
- [ ] query_knowledge 返回结构与内部 LangChain tool 一致
- [ ] 无效/缺失 token 返回 401
- [ ] 既有 REST 与内部 ingest 链路无回归
- [ ] top_k > 10 被截断
- [ ] MCP_API_TOKEN < 32 位启动失败
- [ ] 单测覆盖 ≥ 80%
