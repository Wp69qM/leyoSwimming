> **OpenSpec Spec | 映射自 `docs/stories/US-068-系统-MCP服务注册Trae并完成端到端验证/user-story.md` §6**

## Capability

ai-service MCP 服务在 Trae 完成注册并通过端到端验证

## ADDED Requirements

### Requirement: REQ-001 Trae 注册并全工具验证通过

系统 MUST 可在 Trae 项目级 MCP 配置中注册 `leyo-ai-service`（URL + Bearer token），连接后 4 个工具全部可见且真实调用各至少 1 次成功，验证记录归档。

#### Scenario: Trae 注册并全工具验证通过

- **GIVEN** ai-service 与 backend 本地运行中
- **AND** Trae MCP 配置指向 localhost:8000/mcp-server/mcp 且携带有效 token
- **WHEN** 开发者在 Trae 中重载 MCP 配置
- **THEN** 连接状态正常
- **AND** tools 列表显示 4 个工具且 description 完整
- **AND** 4 个工具真实调用各至少 1 次全部成功

### Requirement: REQ-002 错误 token 连接被拒

系统 MUST 在 Trae 携带错误 token 时拒绝连接（401），失败原因可定位。

#### Scenario: 错误 token 连接被拒

- **GIVEN** Trae MCP 配置携带错误 token
- **WHEN** 重载 MCP 配置
- **THEN** 连接失败（401）
- **AND** 失败原因可从 Trae 提示或 ai-service 日志定位

### Requirement: REQ-003 服务未启动提示明确

系统 MUST 在 ai-service 未启动时给出连接拒绝的明确提示（非模糊协议错误）。

#### Scenario: 服务未启动时连接失败提示明确

- **GIVEN** ai-service 未启动
- **WHEN** Trae 尝试连接 MCP server
- **THEN** 连接失败
- **AND** 错误提示为连接拒绝（非模糊的协议错误）

### Requirement: REQ-004 恢复性与文档同步

系统 MUST 支持 token 轮换与服务重启后的恢复（更新配置/重连即恢复），且 `MCP_API_TOKEN` 说明已同步到 `.env.example` 与部署文档。

#### Scenario: token 轮换恢复

- **GIVEN** MCP_API_TOKEN 已更新
- **WHEN** Trae 旧配置调用失败后更新为新 token
- **THEN** 连接恢复可用

#### Scenario: 服务重启恢复

- **GIVEN** ai-service 重启完成
- **WHEN** Trae 重新发起连接
- **THEN** 工具恢复可用，无残留会话问题
