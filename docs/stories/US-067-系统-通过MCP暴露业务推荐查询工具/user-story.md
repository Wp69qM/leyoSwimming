# US-067 系统-通过MCP暴露业务推荐查询工具

> **状态**：[APPROVAL]
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：AI　|　**最后更新**：2026-09-07
> **配套文档**：需求：[mcp-server-requirements.md](../../prd/mcp-server-requirements.md) · 技术设计：[tech-design.md](./tech-design.md) · 测试计划：[test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-067 |
| **标题** | 系统-通过MCP暴露业务推荐查询工具 |
| **角色（Actor）** | 系统（消费方为外部 MCP 客户端 Agent，如 Trae） |
| **业务价值（Why）** | 编码 Agent 可直接查询平台教练/套餐业务数据辅助开发与答疑，无需人工查库；与 US-066 共同构成完整的 MCP 能力面 |
| **优先级** | [MVP] |
| **估时** | 1 人天（L2） |

---

## 2. 触发条件

- **触发方**：外部 MCP 客户端（Trae / Claude Desktop 等）
- **触发动作**：调用 `tools/call`（`query_coaches` / `query_packages` / `get_hot_recommendations`）
- **触发时机**：Agent 需要查询平台教练、套餐、热门推荐业务数据时

---

## 3. 前置条件

- [ ] MCP 基础设施已就绪（US-066：FastMCP 挂载 + token 鉴权 + 限流）
- [ ] `JavaInternalClient` 业务查询链路可用（US-058）
- [ ] backend 运行中（:8080，推荐工具的数据源）

---

## 4. 业务流程

### 4.1 主路径

1. MCP 客户端通过 US-066 的鉴权与握手
2. 客户端调用 `tools/list`，获得 3 个推荐工具（建立在 query_knowledge 之上，共 4 个）
3. 客户端调用 `tools/call`，如 `query_coaches(stroke="蛙泳", gender="女")`
4. 服务端归一化中文参数（`normalize_stroke` / `normalize_gender` / package_mode 归一化）
5. 经 `JavaInternalClient` 调用 backend 内部接口
6. 返回结构化教练/套餐/热门推荐列表

### 4.2 异常分支

- **分支 1**：backend 不可达/超时 → 工具返回结构化错误信息（不抛异常、不影响其他工具）
- **分支 2**：参数无法归一化（如未知泳姿）→ 归一化为 None，按无过滤条件查询（静默降级，过滤条件语义由工具 description 说明）
- **分支 3**：backend 返回空数据 → 返回空数组

---

## 5. 业务规则引用

> prd.md 无 AI 助理章节，遵循 US-062~065 先例引用需求文档。

| # | 规则 | 来源 |
|---|------|------|
| 1 | 暴露 `query_coaches` / `query_packages` / `get_hot_recommendations` 三个只读工具 | 需求文档 §2.1 |
| 2 | `get_user_profile` / `get_user_packages` **禁止暴露**（user_hash 身份闭包，参数化暴露 = 任意用户数据越权查询面） | 需求文档 §2.2 |
| 3 | 中文参数归一化复用现有逻辑（泳姿/性别/套餐模式） | 需求文档 §4.2-2 |
| 4 | backend 不可达时返回结构化错误，故障隔离 | 需求文档 §5 |
| 5 | Tool description 面向调用方 LLM 优化，说明数据来自游泳培训平台 | 需求文档 §2.1 |

---

## 6. 验收标准（业务级 Gherkin）

> L2 级：2 正常 + 3 异常 = 5 个场景

### 6.1 场景 1：教练查询含中文归一化（正常）

```gherkin
Given backend 运行中且存在蛙泳女教练数据
When  客户端调用 tools/call query_coaches(stroke="蛙泳", gender="女")
Then  返回 HTTP 200
And   服务端将 stroke 归一化为 breaststroke、gender 归一化为 female
And   返回的教练列表仅含符合条件的数据
```

### 6.2 场景 2：热门推荐与套餐查询可用（正常）

```gherkin
Given backend 运行中
When  客户端调用 get_hot_recommendations(limit=5)
And   客户端调用 query_packages(stroke="自由泳", package_mode="体验")
Then  两个工具均返回 HTTP 200
And   package_mode 归一化为 experience
```

### 6.3 场景 3：backend 不可达（异常）

```gherkin
Given backend 已停止
When  客户端调用 query_coaches(stroke="蛙泳")
Then  返回结构化错误信息（含可读的错误说明）
And   MCP 服务不崩溃
And   query_knowledge 工具不受影响
```

### 6.4 场景 4：用户身份类工具不暴露（异常）

```gherkin
Given MCP server 已注册全部工具
When  客户端调用 tools/list
Then  工具清单不含 get_user_profile 与 get_user_packages
```

### 6.5 场景 5：未知参数归一化降级（异常）

```gherkin
Given 客户端传入未知泳姿 stroke="狗刨"
When  调用 query_coaches(stroke="狗刨")
Then  stroke 归一化为 None
And   按无泳姿过滤条件返回结果
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

无新增/修改表。经 `JavaInternalClient` 读取 backend 业务数据（复用 US-058 链路）。

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | MCP tools（3 个） | tools/call | 新增 | `query_coaches` / `query_packages` / `get_hot_recommendations` |

### 7.3 状态机影响

无。

---

## 8. 边界场景

### 8.1 边界场景 1：limit 极值

- **触发条件**：客户端传 `limit=1000`
- **预期行为**：截断至合理上限（如 20），防止大结果集
- **用户可见反馈**：返回截断后的列表

### 8.2 边界场景 2：backend 响应超时

- **触发条件**：backend 响应超过 `JavaInternalClient` 超时阈值
- **预期行为**：返回结构化超时错误，MCP 连接保持
- **用户可见反馈**：调用方 Agent 收到超时说明

### 8.3 边界场景 3：并发调用多个推荐工具

- **触发条件**：Agent 并发调用 query_coaches 与 query_packages
- **预期行为**：工具无共享可变状态，各自独立返回
- **用户可见反馈**：两个结果均正确返回

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- US-058 用户-AI助理推荐教练与套餐（JavaInternalClient 查询链路）
- US-066 系统-通过MCP暴露知识库检索工具（MCP 基础设施）

### 9.2 后续 US（依赖本故事）

- US-068 系统-MCP服务注册Trae并完成端到端验证

---

## 10. INVEST 自检

- [x] **I**ndependent - 在 US-066 基础设施上可独立交付
- [x] **N**egotiable - 聚焦"暴露哪些查询能力"
- [x] **V**aluable - 编码 Agent 直接查业务数据
- [x] **E**stimable - 1 人天
- [x] **S**mall - 三个同构工具 + 归一化重构
- [x] **T**estable - 验收标准全部可客观验证

---

## 11. 完整性检查

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（§13 无 UI 属合理 N/A）
- [x] 业务规则引用明确（需求文档章节）
- [x] 场景数量符合 L2（5 个）
- [x] 越权风险工具的排除有明确规则依据（§5-2）

---

## 12. 备注

- 归一化函数（`normalize_stroke` / `normalize_gender` / package_mode 归一化）需从 `recommendation_tools.py` 闭包中提取为模块级共享，LangChain 与 MCP 两条协议层复用同一份
- backend 可达性是推荐工具的运行时依赖，但 MCP 服务启动不依赖 backend（故障隔离）

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
| 2026-09-07 | AI | 业务评审通过（7.9/10，见 review-mcp-exposure-us066-068-v1.md）；P1-2 已修复（分支 2 改为静默降级）、P2-2 已修复（limit 锁值 20） | 状态推进 [APPROVAL] |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-09-06 | AI | 初版：3 个推荐工具 MCP 暴露 + 归一化共享重构 |
| v1.1 | 2026-09-07 | AI | 评审修复 P1-2/P2-2：§4.2 分支 2 改为静默降级语义、§8.1 limit 锁值 20；业务评审通过，状态推进 [APPROVAL] |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
