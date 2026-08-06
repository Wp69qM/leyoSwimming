# shared AI 协作规范

> **适用范围**：`shared/` 目录下所有代码。  
> **上级规范**：[AGENTS.md](../AGENTS.md)（根目录硬约束优先）。  
> **技术方案**：[docs/tech/frontend-backend-setup-guide.md](../docs/tech/frontend-backend-setup-guide.md)。  
> **多 Agent 规范**：[docs/tech/multi-agent-dev-guide.md](../docs/tech/multi-agent-dev-guide.md)。

---

## 1. 技术栈

- 语言：TypeScript
- 构建：tsc / tsup
- 包管理：pnpm workspace
- 类型生成：openapi-typescript（从 OpenAPI 生成 TS 类型）

---

## 2. 必须调用的 Skill

| 用户意图 | Skill |
|----------|-------|
| 写用户故事 | `openspec-new-change` |
| 实现 / 开发 / 编码 | `test-driven-development` |
| 修 bug / 测试失败 | `systematic-debugging` |
| 代码审查 | `requesting-code-review` |
| 新功能 | `brainstorming` → `openspec-new-change` |
| 复杂计划 / 跨端改动 | `writing-plans` |

---

## 3. 代码规范

- **只放纯逻辑，不放框架相关代码。**
- 禁止引入 UI 框架（React / Vue / Taro）依赖。
- 禁止引入后端框架（Spring）依赖。
- 禁止直接引用 `miniapp-user/`、`miniapp-coach/`、`web-admin/`、`backend/` 下的代码。
- 变更后必须同步到前端类型与后端 DTO。
- 常量、枚举必须与中/英文文案解耦，仅存放业务键值。

---

## 4. 文件组织

```
shared/
├── src/
│   ├── types/                 # TypeScript 类型（用户、教练、套餐、订单）
│   ├── constants/             # 错误码、枚举、配置键
│   ├── utils/                 # 纯函数工具（日期、金额、验证）
│   └── index.ts               # 统一导出
├── openapi/
│   ├── leyo-swimming-v1.yaml  # 主 OpenAPI 契约
│   └── CHANGELOG.md           # API 变更日志
├── tsconfig.json
├── package.json
└── AGENT.md
```

---

## 5. API 契约管理

- `shared/openapi/leyo-swimming-v1.yaml` 是前后端唯一 API 真相源。
- 后端 Agent 负责维护该文件，确保与代码一致。
- 前端 Agent 只消费已冻结版本的 OpenAPI。
- 变更 API 时必须升级版本号，并在 CHANGELOG.md 中记录：变更时间、变更人、变更内容、影响范围。
