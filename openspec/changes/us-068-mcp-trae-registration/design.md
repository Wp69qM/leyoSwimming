## Context

本 US 是 MCP MVP 链路的验收闭环：注册、连接、全工具真实调用、异常路径验证、文档沉淀。

## Goals / Non-Goals

**Goals:**

- Trae 项目级 MCP 配置注册成功，4 个工具全部可见可调用
- 异常路径（错误 token / 服务未启动 / backend 降级）行为符合预期
- 验证记录归档；`.env.example` 与部署文档更新

**Non-Goals:**

- 不做远程服务器部署、Nginx 反代、HTTPS、token 轮换审计（后续阶段）
- 不改动 MCP server 代码（如验证发现缺陷，回归 US-066/067 修复）

## Data Model

无。

## Configuration Design

### Trae MCP 注册（项目级）

```json
{
  "mcpServers": {
    "leyo-ai-service": {
      "url": "http://localhost:8000/mcp-server/mcp",
      "headers": {
        "Authorization": "Bearer <MCP_API_TOKEN>"
      }
    }
  }
}
```

- 配置位置以 Trae 当前版本实际入口为准（核心要素：URL + Bearer header）
- token 不入仓库：配置模板使用占位符，本地替换

### 验证记录格式

| 工具 | 调用参数 | 结果摘要 | 状态 |
|------|---------|---------|------|
| query_knowledge | "野泳的注意事项" | 返回 N 条带来源片段 | ✅ |

## Key Design Decisions

| 决策 | 结论 | 理由 |
|------|------|------|
| 配置级别 | 项目级（随仓库协作） | 团队共享配置；token 本地替换 |
| 验证方式 | Trae 手动 + SDK 脚本双轨 | 脚本可重复执行，手动覆盖真实交互 |
| token 管理 | 本地明文可接受 | 远程部署阶段再引入更安全方案 |

## Risks

- Trae 版本配置格式差异：以实际 IDE 格式为准，两要素不变（URL + Bearer）
- `.trae/mcp.json` 入仓库的泄露面：占位符 + 本地替换约定，或 ignore 处理
