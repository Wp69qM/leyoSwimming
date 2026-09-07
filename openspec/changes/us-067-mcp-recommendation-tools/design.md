## Context

本 US 在 US-066 建立的 MCP 基础设施上追加 3 个业务推荐查询工具，并完成归一化逻辑的跨协议层共享重构。

## Goals / Non-Goals

**Goals:**

- 注册 `query_coaches` / `query_packages` / `get_hot_recommendations` 三个只读 MCP 工具
- 中文参数归一化（泳姿/性别/套餐模式）与 LangChain 链路共用一份实现
- backend 故障时结构化错误返回，工具间故障隔离
- `tools/list` 不出现 `get_user_profile` / `get_user_packages`

**Non-Goals:**

- 不改动 US-066 的鉴权/挂载/限流
- 不暴露用户个体数据（画像/已购套餐）
- 不做 Trae 注册验证（US-068）

## Data Model

无表变更。经 `JavaInternalClient` 调用 backend `/api/internal/ai/*` 既有内部接口。

## API Design

### MCP Tools（3 个，均只读）

| 工具 | 参数 | 说明 |
|------|------|------|
| `query_coaches` | stroke?, gender?, max_price?, max_age?, class_size?, limit=5 | 教练查询，中文自动归一化 |
| `query_packages` | stroke?, package_mode?, max_price?, hours?, limit=5 | 套餐查询 |
| `get_hot_recommendations` | stroke?, limit=5 | 热门教练/套餐 |

- **limit 上限**：20（截断）
- **错误返回**：`[{"error": "业务数据服务暂不可用，请稍后再试"}]`（结构化，不抛异常）
- **数据口径**：与游客端公开浏览数据一致，不含用户个体数据

## Key Design Decisions

| 决策 | 结论 | 理由 |
|------|------|------|
| 工具复用方式 | 直调 JavaInternalClient | @tool 闭包签名与 MCP schema 不兼容 |
| 归一化 | 提取 normalizers.py 共享 | 单一事实来源，防两协议层行为漂移 |
| backend 故障 | 结构化错误 | MCP 连接稳定，工具间隔离 |
| 身份类工具 | 白名单显式注册，不暴露 | 防任意用户数据越权查询 |

## Risks

- 业务数据外泄面：仅公开口径数据（教练列表/套餐），与游客浏览一致，无个体数据
- backend 是运行时依赖：本地验证需先启动 backend；启动顺序无要求（故障隔离）
