> **OpenSpec Spec | 映射自 `docs/stories/US-067-系统-通过MCP暴露业务推荐查询工具/user-story.md` §6**

## Capability

通过 MCP 协议向外部 Agent 客户端暴露教练/套餐/热门推荐只读查询工具

## ADDED Requirements

### Requirement: REQ-001 教练查询含中文归一化

系统 MUST 提供 `query_coaches` MCP 工具，将中文参数（如 stroke="蛙泳"、gender="女"）归一化后经 `JavaInternalClient` 查询并返回符合条件的教练列表。

#### Scenario: 教练查询含中文归一化

- **GIVEN** backend 运行中且存在蛙泳女教练数据
- **WHEN** 客户端调用 tools/call query_coaches(stroke="蛙泳", gender="女")
- **THEN** 返回 HTTP 200
- **AND** 服务端将 stroke 归一化为 breaststroke、gender 归一化为 female
- **AND** 返回的教练列表仅含符合条件的数据

### Requirement: REQ-002 套餐与热门推荐查询可用

系统 MUST 提供 `query_packages` 与 `get_hot_recommendations` MCP 工具，套餐模式中文参数（如"体验"）归一化为内部枚举（experience）。

#### Scenario: 套餐与热门推荐查询可用

- **GIVEN** backend 运行中
- **WHEN** 客户端调用 get_hot_recommendations(limit=5)
- **AND** 客户端调用 query_packages(stroke="自由泳", package_mode="体验")
- **THEN** 两个工具均返回 HTTP 200
- **AND** package_mode 归一化为 experience

### Requirement: REQ-003 backend 故障隔离

系统 MUST 在 backend 不可达或超时时返回结构化错误信息（不抛异常），MCP 服务不崩溃，且 `query_knowledge` 工具不受影响。

#### Scenario: backend 不可达

- **GIVEN** backend 已停止
- **WHEN** 客户端调用 query_coaches(stroke="蛙泳")
- **THEN** 返回结构化错误信息（含可读的错误说明）
- **AND** MCP 服务不崩溃
- **AND** query_knowledge 工具不受影响

### Requirement: REQ-004 用户身份类工具禁止暴露

系统 MUST NOT 将 `get_user_profile` 与 `get_user_packages` 注册到 MCP 工具清单（user_hash 身份闭包，参数化暴露构成任意用户数据越权查询面）。

#### Scenario: 用户身份类工具不暴露

- **GIVEN** MCP server 已注册全部工具
- **WHEN** 客户端调用 tools/list
- **THEN** 工具清单不含 get_user_profile 与 get_user_packages

### Requirement: REQ-005 参数降级与结果集上限

系统 MUST 将未知参数归一化为 None 按无过滤查询（如 stroke="狗刨"），并将 `limit` 截断至 20。

#### Scenario: 未知参数归一化降级

- **GIVEN** 客户端传入未知泳姿 stroke="狗刨"
- **WHEN** 调用 query_coaches(stroke="狗刨")
- **THEN** stroke 归一化为 None
- **AND** 按无泳姿过滤条件返回结果

#### Scenario: limit 超限截断

- **GIVEN** 客户端传 limit=1000
- **WHEN** 调用任一推荐工具
- **THEN** 实际返回条数 ≤ 20
