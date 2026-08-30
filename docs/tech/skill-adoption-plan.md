# Skill 引入决策依据

> 本文件记录从 `D:\AI Agent\ECC-main\skills` 评估后，确定优先引入到 leyoSwimming 项目的 skill 列表，作为后续 skill 扩展的决策依据。

## 评估背景

- 评估来源：`D:\AI Agent\ECC-main\skills`
- 项目技术栈：Spring Boot（后端）+ Taro React（用户端/教练端小程序）+ Vue 3（web-admin 管理后台）+ FastAPI/LangChain（ai-service）+ MySQL + Redis
- 评估原则：与当前栈匹配、能填补现有规范空白、优先工程质量与可维护性

## 已引入的优先 Skill（Top 6）

| 序号 | Skill 名称 | 适用场景 | 引入原因 |
|---|---|---|---|
| 1 | **fastapi-patterns** | ai-service（FastAPI） | 当前 AI 服务缺乏统一规范，可借鉴 lifespan、Pydantic v2 schemas、依赖注入、事务化 service layer、测试模式 |
| 2 | **python-patterns** | ai-service（Python） | 补齐 Pythonic 写法、类型注解、PEP 8、异常处理等基础规范 |
| 3 | **python-testing** | ai-service（测试） | 建立 pytest、mock、fixture、async 测试标准，提升 AI 服务测试覆盖率 |
| 4 | **vue-patterns** | web-admin（Vue 3） | web-admin 使用 Vue 3.4 + Vite + Pinia + Vue Router，与 skill 覆盖范围完全匹配 |
| 5 | **mysql-patterns** | 后端数据库 | 规范 schema 设计、索引策略、EXPLAIN 分析、连接池配置，避免慢查询 |
| 6 | **redis-patterns** | 后端缓存/会话/限流 | 当前已用 Redis 做验证码、限流、会话，可统一缓存策略、分布式锁、限流模式 |

## 存放位置

以上 skill 已复制到项目本地：

```
.trae/skills/
├── fastapi-patterns/
├── python-patterns/
├── python-testing/
├── vue-patterns/
├── mysql-patterns/
└── redis-patterns/
```

## 使用说明

- 开发/审查对应模块代码时，优先调用相关 skill。
- 现有项目规则（如 Calicat 视觉规范、Taro React 规范、API 约定）优先级不变；skill 作为补充参考。
- 后续若引入新 skill，应先更新本文件并说明引入理由。

## 变更日志

| 日期 | 变更 | 作者 |
|---|---|---|
| 2026-08-25 | 初始创建，引入 Top 6 skill | AI |
