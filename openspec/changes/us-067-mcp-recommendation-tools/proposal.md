## Why

MCP 基础设施（US-066）已就绪，但仅暴露知识检索工具。编码 Agent 查询平台业务数据（教练/套餐/热门推荐）仍需人工查库。暴露三个只读推荐查询工具后，外部 Agent 可直接获取真实业务数据辅助开发与答疑，补全 MCP 能力面。

## What Changes

- `ai-service` MCP server 注册 3 个只读推荐工具：`query_coaches` / `query_packages` / `get_hot_recommendations`，直调 `JavaInternalClient`（复用 US-058 链路）
- 归一化函数（`normalize_stroke` / `normalize_gender` / package_mode 归一化）从 `recommendation_tools.py` 闭包提取为 `app/tools/normalizers.py` 模块级共享，LangChain 与 MCP 两条协议层复用同一份实现
- backend 不可达/超时时工具返回结构化错误（不抛异常、不影响其他工具），MCP 服务启动不依赖 backend
- `limit` 截断至 20；Tool description 面向调用方 LLM 优化
- **安全约束**：`get_user_profile` / `get_user_packages` 禁止注册到 MCP（user_hash 身份闭包，参数化暴露构成任意用户数据越权查询面）

## Capabilities

### New Capabilities

- `mcp-recommendation-tools`: 通过 MCP 协议向外部 Agent 客户端暴露教练/套餐/热门推荐只读查询工具

### Modified Capabilities

- 无

## Impact

- **数据表**：无新增/修改；经 `JavaInternalClient` 读取 backend 业务数据
- **API**：MCP tools 新增 3 个（tools/call）；无 REST 变更
- **状态机**：无
- **前端**：无
- **依赖**：US-058（JavaInternalClient 推荐查询链路）、US-066（MCP 基础设施）
- **影响**：为 US-068（Trae 端到端验证）提供全部 4 个待验证工具；`recommendation_tools.py` 重构为引用共享 normalizers（行为不变）
