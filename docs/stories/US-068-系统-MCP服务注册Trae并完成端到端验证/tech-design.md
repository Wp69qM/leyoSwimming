# Tech Design: US-068 系统-MCP服务注册Trae并完成端到端验证

## 1. 总体架构

```
Trae IDE（MCP 客户端）
    │  .trae/mcp.json → url: http://localhost:8000/mcp-server/mcp
    │                   headers: Authorization: Bearer <MCP_API_TOKEN>
    ▼
ai-service（US-066/067 已部署的 MCP endpoint）
    ├── query_knowledge ──→ Chroma
    └── 推荐工具 ×3 ──→ backend
```

## 2. 新增/修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `.trae/mcp.json`（或 Trae 当前版本的 MCP 配置入口） | 新增 | 注册 `leyo-ai-service` server（URL + Bearer header） |
| `ai-service/.env.example` | 修改 | 增加 `MCP_API_TOKEN` 生成说明 |
| `deploy/.env` 模板与部署文档 | 修改 | 增加 `MCP_API_TOKEN` 与 Trae 注册步骤 |
| `docs/stories/US-068-*/user-story.md` §15 | 回填 | 验证记录归档 |

## 3. 关键实现

### 3.1 Trae MCP 配置（示例结构，以 Trae 当前配置格式为准）

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

### 3.2 验证脚本（可选辅助）

复用/扩展 `ai-service/scripts/` 下的验证脚本：用 MCP SDK client 完成 initialize → tools/list → 4 工具各调用一次，输出结果摘要，作为 Trae 之外的自动化验证手段。

### 3.3 验证记录格式

| 工具 | 调用参数 | 结果摘要 | 状态 |
|------|---------|---------|------|
| query_knowledge | "野泳的注意事项" | 返回 N 条带来源片段 | ✅ |
| query_coaches | stroke="蛙泳" | 返回 N 名教练 | ✅ |
| query_packages | stroke="自由泳" | 返回 N 个套餐 | ✅ |
| get_hot_recommendations | limit=5 | 返回 N 条热门 | ✅ |

## 4. 设计决策

| 决策 | 选项 | 结论 |
|------|------|------|
| 配置位置 | 项目级 `.trae/mcp.json` / 用户级全局 | **项目级**：随仓库协作，token 走本地环境约定 |
| token 管理 | 明文入配置 / 环境变量引用 | 本地开发明文可接受（需求文档 §5）；远程部署阶段再引入更安全方案 |
| 验证方式 | 仅手动 Trae / 加 SDK 脚本 | **双轨**：SDK 脚本可重复执行，Trae 手动验证覆盖真实交互 |

## 5. 风险与对策

- **Trae 配置格式差异**：不同 Trae 版本 MCP 配置入口可能不同，以当前 IDE 实际格式为准，核心是 URL + Bearer header 两要素
- **token 泄露面**：`.trae/mcp.json` 若入仓库需评估；本地阶段约定 token 不提交（加入 ignore 或使用占位符 + 本地替换）
