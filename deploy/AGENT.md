# deploy AI 协作规范

> **适用范围**：`deploy/` 目录下所有代码。  
> **上级规范**：[AGENTS.md](../AGENTS.md)（根目录硬约束优先）。  
> **技术方案**：[docs/tech/frontend-backend-setup-guide.md](../docs/tech/frontend-backend-setup-guide.md)。  
> **多 Agent 规范**：[docs/tech/multi-agent-dev-guide.md](../docs/tech/multi-agent-dev-guide.md)。

---

## 1. 技术栈

- 容器化：Docker / Docker Compose
- 反向代理：Nginx
- CI/CD：GitHub Actions（可选）
- 编排：K8s（后续阶段）

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

- 环境变量模板使用 `.env.example`，禁止提交真实密钥。
- Docker Compose 必须包含本地开发所需全部服务（MySQL、Redis、Backend、Nginx）。
- Nginx 配置需统一处理 CORS、静态资源缓存、反向代理。
- 生产配置与开发配置分离。

---

## 4. 文件组织

```
deploy/
├── docker-compose.yml          # 本地开发环境
├── docker-compose.prod.yml     # 生产环境（后续）
├── nginx.conf                  # Nginx 反向代理配置
├── .env.example                # 环境变量模板
└── AGENT.md
```

---

## 5. 安全红线

- 禁止提交 `.env`、密钥、证书到仓库。
- 数据库密码必须强随机，生产环境通过 secrets 管理。
- 容器镜像使用非 root 用户运行。
