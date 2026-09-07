# US-068 系统-MCP服务注册Trae并完成端到端验证

> **状态**：[APPROVAL]
> **优先级**：[MVP]
> **估时**：0.5 人天
> **作者**：AI　|　**最后更新**：2026-09-07
> **配套文档**：需求：[mcp-server-requirements.md](../../prd/mcp-server-requirements.md) · 技术设计：[tech-design.md](./tech-design.md) · 测试计划：[test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-068 |
| **标题** | 系统-MCP服务注册Trae并完成端到端验证 |
| **角色（Actor）** | 系统（操作者为开发者，验证对象为 Trae MCP 客户端） |
| **业务价值（Why）** | 完成从"能力暴露"到"真实消费"的闭环验证，开发者日常编码中可直接使用平台 AI 能力（dogfooding），并为后续远程部署提供基线配置 |
| **优先级** | [MVP] |
| **估时** | 0.5 人天（L1） |

---

## 2. 触发条件

- **触发方**：开发者
- **触发动作**：在 Trae 中注册 ai-service MCP server 配置并执行验证
- **触发时机**：US-066/067 开发完成后的验收阶段

---

## 3. 前置条件

- [ ] MCP server 4 个工具全部可用（US-066、US-067）
- [ ] `MCP_API_TOKEN` 已生成（≥32 位强随机）
- [ ] ai-service 本地运行（:8000）
- [ ] backend 本地运行（:8080，推荐工具数据源）
- [ ] 知识库存在启用文档（验证检索有数据）

---

## 4. 业务流程

### 4.1 主路径

1. 开发者在本项目 `.trae/mcp.json`（或 Trae MCP 配置入口）注册 server：URL 指向 `http://localhost:8000/mcp-server/mcp`，header 携带 `Authorization: Bearer <MCP_API_TOKEN>`
2. 重载 Trae MCP 配置，确认连接状态正常
3. 在 Trae 会话中触发工具调用验证：
   - `query_knowledge("野泳的注意事项")` → 返回带来源知识片段
   - `query_coaches(stroke="蛙泳")` → 返回教练列表
   - `query_packages` / `get_hot_recommendations` → 返回业务数据
4. 将验证记录（调用日志/截图）归档到本 US §15 评审记录
5. 更新部署文档：`deploy/.env` 增加 `MCP_API_TOKEN` 说明与 Trae 注册步骤

### 4.2 异常分支

- **分支 1**：连接失败（token 错误/服务未启动）→ 按 Trae 错误提示排查，验证 401/连接拒绝的行为符合预期
- **分支 2**：工具调用超时 → 检查 backend 依赖与限流配置

---

## 5. 业务规则引用

> prd.md 无 AI 助理章节，遵循 US-062~065 先例引用需求文档。

| # | 规则 | 来源 |
|---|------|------|
| 1 | 验证环境为本地开发（localhost），远程部署为后续阶段 | 需求文档 §1.2 / §2.2 |
| 2 | Trae 配置携带 token，明文存储仅本地可接受 | 需求文档 §5 |
| 3 | 4 个工具均需真实调用验证至少 1 次 | 需求文档 §3 US-C |
| 4 | 验证记录归档 | 需求文档 §3 US-C |
| 5 | 部署文档同步更新 `MCP_API_TOKEN` | 需求文档 §2.1 |

---

## 6. 验收标准（业务级 Gherkin）

> L1 级：1 正常 + 2 异常 = 3 个场景

### 6.1 场景 1：Trae 注册并全工具验证通过（正常）

```gherkin
Given ai-service 与 backend 本地运行中
And   .trae MCP 配置指向 localhost:8000/mcp-server/mcp 且携带有效 token
When  开发者在 Trae 中重载 MCP 配置
Then  连接状态正常
And   tools 列表显示 4 个工具且 description 完整
And   4 个工具真实调用各至少 1 次全部成功
```

### 6.2 场景 2：token 错误连接被拒（异常）

```gherkin
Given Trae MCP 配置携带错误 token
When  重载 MCP 配置
Then  连接失败（401）
And   失败原因可从 Trae 提示或 ai-service 日志定位
```

### 6.3 场景 3：服务未启动时连接失败提示明确（异常）

```gherkin
Given ai-service 未启动
When  Trae 尝试连接 MCP server
Then  连接失败
And   错误提示为连接拒绝（非模糊的协议错误）
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

无。

### 7.2 API 影响

无新增 API。仅消费 US-066/067 暴露的 MCP endpoint。

### 7.3 状态机影响

无。

---

## 8. 边界场景

### 8.1 边界场景 1：token 轮换

- **触发条件**：`MCP_API_TOKEN` 更新后 Trae 仍用旧 token
- **预期行为**：调用被拒（401），更新配置后恢复
- **用户可见反馈**：Trae 工具调用失败提示

### 8.2 边界场景 2：服务重启后重连

- **触发条件**：ai-service 重启（如 `--reload` 热重载）
- **预期行为**：Trae 重新发起连接即可恢复，无残留会话问题
- **用户可见反馈**：工具恢复可用

### 8.3 边界场景 3：backend 停止时推荐工具降级

- **触发条件**：仅 backend 停止、ai-service 运行
- **预期行为**：知识工具正常，推荐工具返回结构化错误（US-067 行为回归确认）
- **用户可见反馈**：Agent 收到"业务数据服务暂不可用"提示

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- US-066 系统-通过MCP暴露知识库检索工具
- US-067 系统-通过MCP暴露业务推荐查询工具

### 9.2 后续 US（依赖本故事）

- 无（MCP MVP 链路终点；远程部署为后续独立需求）

---

## 10. INVEST 自检

- [x] **I**ndependent - 配置与验证可独立交付
- [x] **N**egotiable - 聚焦"注册与验证闭环"
- [x] **V**aluable - dogfooding 闭环 + 远程部署基线
- [x] **E**stimable - 0.5 人天
- [x] **S**mall - 单一验证职责
- [x] **T**estable - 4 工具调用结果可客观验证

---

## 11. 完整性检查

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（§13 无 UI 属合理 N/A）
- [x] 业务规则引用明确（需求文档章节）
- [x] 场景数量符合 L1（3 个）
- [x] 配套文档链接有效

---

## 12. 备注

- 本 US 是 MCP MVP 链路的验收闭环，产出物为 Trae 配置 + 验证记录 + 部署文档更新
- 远程服务器部署（Nginx 反代 + 公网 HTTPS + token 轮换审计）为后续阶段独立需求，见需求文档 §2.2

---

## 13. Figma 链接

> 本 US 无 UI 变更，无页面交付物。

| 内容 | 链接 | 状态 |
|------|------|------|
| 无 UI（开发工具链验证） | N/A | N/A |

---

## 14. 页面级设计决策

无页面，不适用。

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-09-06 | AI | DRAFT 初版，待业务评审；验证记录待开发完成后回填 | — |
| 2026-09-06 | AI | 第五批次业务评审开始，进入 [REVIEW] | 三件套一致性检查进行中 |
| 2026-09-07 | AI | 业务评审通过（7.7/10，见 review-mcp-exposure-us066-068-v1.md）；P1-3 已修复（§1 表格分隔符） | 状态推进 [APPROVAL] |
| 2026-09-07 | AI | 开发完成，端到端验证记录回填（见下方 §15.1）；`.trae/mcp.json` 为 IDE 用户专有文件（模型不可写），以 `.trae/mcp.json.example` 模板入库，Trae 注册待开发者手动执行 | 状态推进 [COMPLETED]（Trae 手动注册除外） |
| 2026-09-07 | AI+开发者 | Trae 端手动验证通过（§15.1 补充记录）：`.trae/mcp.json` 注册成功，Agent 会话自然语言触发 query_knowledge / query_packages / query_coaches 真实调用（get_hot_recommendations 由自动化脚本覆盖），开发者确认 4/4 验证通过 | US-068 完全闭环，无遗留 |

### 15.1 端到端验证记录（2026-09-07，本地环境：ai-service + backend + MySQL/Redis + Chroma）

**自动化验证**：`ai-service/scripts/verify_mcp.py`（MCP SDK client，initialize → tools/list → 4 工具各真实调用一次，exit=0）

| 工具 | 调用参数 | 结果摘要 | 状态 |
|------|---------|---------|------|
| initialize | — | Streamable HTTP 握手成功 | ✅ |
| tools/list | — | 返回 4 个工具；禁止暴露清单（get_user_profile / get_user_packages / web_search / ingest / chat）均未出现 | ✅ |
| query_knowledge | query="游泳时抽筋怎么办" | 1 条带来源片段：《溺水急救处理流程与处理事项》score=0.259 | ✅ |
| query_coaches | stroke="蛙泳" | 5 条（胡教练、张起灵、李教练等），中文泳姿归一化生效 | ✅ |
| query_packages | stroke="自由泳"、package_mode="体验" | 2 条（体验课1节、零基础体验课2节），package_mode 归一化生效 | ✅ |
| get_hot_recommendations | limit=5 | 5 条 | ✅ |

**异常/边界场景**：

| 场景 | 验证结果 | 状态 |
|------|---------|------|
| §6.2 错误 token / 无 token | HTTP 401（"MCP 接口鉴权失败"），不执行工具 | ✅ |
| §6.3 服务未启动 | ConnectError: All connection attempts failed（明确的连接拒绝提示，非模糊协议错误） | ✅ |
| §8.3 backend 停止、ai-service 运行 | query_knowledge 正常返回；3 个推荐工具返回结构化错误"业务数据服务暂不可用，请稍后再试"，MCP 服务不崩溃（US-067 故障隔离回归确认） | ✅ |

**Trae 手动注册**：`.trae/mcp.json` 为 Trae IDE 用户专有配置文件（模型修改被 IDE 拒绝），已提供 `.trae/mcp.json.example` 模板与部署文档注册步骤（first-deployment-guide.md §6.5）；开发者复制模板、替换 token 后在 Trae 中重载即可完成 §6.1 的 Trae 侧验证。

**过程中的额外修复**：`config.py` 的 `knowledge_similarity_threshold` 默认值 0.7 → 0.2（历史值在未显式配置时阻断 text-embedding-v2 有效召回，与 .env.example 及第四批次调优结论对齐）。

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-09-06 | AI | 初版：Trae 注册配置与端到端验证方案 |
| v1.1 | 2026-09-07 | AI | 评审修复 P1-3：§1 基本信息表估时行列分隔符修正；业务评审通过，状态推进 [APPROVAL] |
| v1.2 | 2026-09-07 | AI | 开发完成：§15.1 回填端到端验证记录（SDK 脚本全通过 + 401/连接拒绝/故障隔离场景）；状态推进 [COMPLETED]（Trae 手动注册待开发者执行） |
| v1.3 | 2026-09-07 | AI | Trae 端手动验证通过：`.trae/mcp.json` 注册 + Agent 会话自然语言触发 4 工具真实调用（3 个手动 + 1 个自动化覆盖），开发者确认闭环；端口占用备注（8000 被 Trae 内部进程占用，本地实例改用 8001） |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
