## Why

US-066/067 完成了 MCP 能力暴露，但尚未完成"真实消费"闭环。在 Trae 中注册 ai-service 的 MCP server 并对 4 个工具做端到端验证，开发者日常编码即可直接使用平台 AI 能力（dogfooding），同时为后续远程部署提供基线配置与验证方法。

## What Changes

- 在项目级 Trae MCP 配置注册 `leyo-ai-service` server：URL 指向 `http://localhost:8000/mcp-server/mcp`，header 携带 `Authorization: Bearer <MCP_API_TOKEN>`
- 验证连接状态与工具清单（4 个工具，description 完整）
- 4 个工具真实调用各至少 1 次，验证记录归档到 US §15 评审记录
- 验证异常路径：错误 token（401）、服务未启动（连接拒绝）、backend 停止时推荐工具降级
- 更新 `ai-service/.env.example` 与部署文档：`MCP_API_TOKEN` 生成说明与 Trae 注册步骤

## Capabilities

### New Capabilities

- `mcp-trae-registration`: ai-service MCP 服务在 Trae 完成注册并通过端到端验证

### Modified Capabilities

- 无

## Impact

- **数据表**：无
- **API**：无新增（仅消费 US-066/067 的 MCP endpoint）
- **状态机**：无
- **前端**：无
- **依赖**：US-066、US-067（4 个工具全部可用）
- **影响**：MCP MVP 链路验收终点；远程部署（Nginx 反代 + HTTPS + token 轮换审计）为后续独立需求
