# tools AI 协作规范

> **适用范围**：`tools/` 目录下所有代码。  
> **上级规范**：[AGENTS.md](../AGENTS.md)（根目录硬约束优先）。  
> **技术方案**：[docs/tech/frontend-backend-setup-guide.md](../docs/tech/frontend-backend-setup-guide.md)。  
> **多 Agent 规范**：[docs/tech/multi-agent-dev-guide.md](../docs/tech/multi-agent-dev-guide.md)。

---

## 1. 技术栈

- 脚本语言：Node.js / TypeScript（优先）或 Python
- 运行方式：CLI 命令

---

## 2. 必须调用的 Skill

| 用户意图 | Skill |
|----------|-------|
| 写用户故事 | `openspec-new-change` |
| 实现 / 开发 / 编码 | `test-driven-development` |
| 修 bug / 测试失败 | `systematic-debugging` |
| 代码审查 | `requesting-code-review` |
| 新功能 | `brainstorming` → `openspec-new-change` |
| 复杂计划 | `writing-plans` |

---

## 3. 代码规范

- 脚本语言统一，优先 Node.js + TypeScript。
- 输入参数必须校验。
- 工具脚本必须有 `--help` 说明。
- 禁止直接操作生产数据库。
- 敏感操作（如数据迁移）必须先备份、再执行、最后校验。

---

## 4. 文件组织

```
tools/
├── scripts/
│   ├── generate-api-types.ts   # 从 OpenAPI 生成 shared 类型
│   ├── seed-dev-data.ts        # 开发环境初始数据
│   └── sync-figma-tokens.ts    # 同步 Figma 设计 token
├── package.json
├── tsconfig.json
└── AGENT.md
```

---

## 5. 安全红线

- 禁止脚本中硬编码生产数据库连接串。
- 涉及数据库的脚本必须支持 `--dry-run` 模式。
- 禁止直接修改生产环境数据。
