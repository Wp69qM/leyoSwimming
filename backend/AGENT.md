# backend AI 协作规范

> **适用范围**：`backend/` 目录下所有代码。  
> **上级规范**：[AGENTS.md](../AGENTS.md)（根目录硬约束优先）。  
> **技术方案**：[docs/tech/frontend-backend-setup-guide.md](../docs/tech/frontend-backend-setup-guide.md)。  
> **多 Agent 规范**：[docs/tech/multi-agent-dev-guide.md](../docs/tech/multi-agent-dev-guide.md)。

---

## 1. 技术栈

- 框架：Spring Boot 3.2 + Spring Web
- 语言：Java 21（LTS）
- 构建：Maven
- ORM：MyBatis-Plus 3.5
- 数据库连接：HikariCP
- 缓存：Spring Data Redis + Redisson
- 安全：Spring Security + JWT
- 校验：Jakarta Validation
- 文档：SpringDoc OpenAPI（Knife4j 可选）
- 任务调度：Quartz / Spring Scheduler
- 测试：JUnit 5 + Mockito + Testcontainers（MySQL/Redis）
- 日志：SLF4J + Logback + MDC
- 数据库迁移：Flyway

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
| 多 US 并行开发隔离 | `using-git-worktrees` |

---

## 3. 代码规范

- 文件大小：200-400 行典型，800 行上限。
- 函数大小：<50 行。
- 禁止深嵌套（>4 层）。
- Controller 薄、Service 厚：Controller 只做参数解析、调用 Service、返回 DTO。
- 统一响应体：`ApiResponse<T>`，HTTP 状态码语义化。
- 错误码枚举：按模块划分，如 `AUTH_001`、`BOOKING_003`。
- 参数校验：`@Valid` + 分组校验。
- 幂等设计：关键写接口使用幂等键 + 数据库唯一索引。
- 分布式锁：Redis Redisson，用于预约锁定、库存扣减。
- 敏感信息：日志脱敏、接口返回脱敏。
- 禁止直接引用前端代码。

---

## 4. 测试要求

- 覆盖率 ≥ 80%，核心业务逻辑 ≥ 90%。
- 单元 / 集成 / E2E 三类都要有。
- TDD：RED → GREEN → REFACTOR → COMMIT。
- 状态机转换 100% 覆盖。
- 关键接口必须使用 Testcontainers 做集成测试。

---

## 5. 安全红线

- 不硬编码密钥。
- 所有用户输入校验。
- 防止 SQL 注入（MyBatis `#{}`）。
- 防止 XSS / CSRF。
- JWT secret 通过环境变量注入。
- 错误信息不泄露内部细节。
- 管理员接口必须校验 RBAC 权限。
- 所有写操作记录审计日志。

---

## 6. 文件组织

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/leyoswimming/
│   │   │   ├── LeyoSwimmingApplication.java
│   │   │   ├── config/              # 配置类（Security、Redis、MyBatis）
│   │   │   ├── controller/          # REST API（薄层）
│   │   │   ├── service/             # 业务逻辑
│   │   │   ├── repository/          # 数据访问接口（MyBatis-Plus Mapper）
│   │   │   ├── entity/              # 数据库实体
│   │   │   ├── dto/                 # Request / Response DTO
│   │   │   ├── vo/                  # 视图对象
│   │   │   ├── mapper/              # MapStruct 转换
│   │   │   ├── enums/               # 状态枚举
│   │   │   ├── exception/           # 全局异常 + 错误码
│   │   │   ├── security/            # JWT / 认证 / 鉴权
│   │   │   ├── scheduler/           # 定时任务
│   │   │   └── util/                # 工具类
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── mapper/              # XML（复杂 SQL）
│   │       └── db/migration/        # Flyway 迁移脚本
│   └── test/
│       ├── java/
│       └── resources/
├── Dockerfile
├── pom.xml
├── lombok.config
└── AGENT.md
```

---

## 7. API 文档产出要求

- 每完成一个 API，必须更新 `shared/openapi/leyo-swimming-v1.yaml`。
- 使用 SpringDoc 注解生成 OpenAPI 片段。
- 变更 API 时必须升级版本号，并在 `shared/openapi/CHANGELOG.md` 中记录。
- 未写入 OpenAPI 的接口视为不存在，前端有权拒绝联调。
