# 技术设计规范

> **本目录用途**：定义 `docs/stories/US-XXX-.../tech-design.md` 的编写规范。
> **文档状态**：✅ 已生效
> **最后更新**：2026-07-30

---

## 0. 文档角色

`tech-design.md` 承担 SDD 工作流中 `plan.md` / `design.md` 的角色：

- **业务层**（`user-story.md`）回答：用户看到什么？
- **设计层**（本文档）回答：用什么数据模型 / API / 状态机 / 缓存实现？
- **执行层**（`test-plan.md`）回答：开发按什么顺序 TDD 实现？

> **三者的关系**：
> - `user-story.md` 的 GWT 决定 **验收标准**
> - `tech-design.md` 决定 **实现方案**
> - `test-plan.md` 决定 **执行步骤**

---

## 1. 必填章节（标准结构）

每个 `tech-design.md` 必须按以下顺序包含：

| # | 章节 | 必填 | 内容 |
|---|------|------|------|
| 0 | 文档定位 | ✓ | 说明本文档是设计层，不替代 test-plan.md |
| 1 | 数据模型影响 | ✓ | 涉及表 / 字段 / 索引 / 迁移 SQL |
| 2 | API 设计 | ✓ | RESTful 路径 / 方法 / 请求 / 响应 / 错误码 |
| 3 | 状态机影响 | ✓ | 受影响的实体状态 + 转换条件 |
| 4 | 缓存策略 | ✓ | 缓存键 / 失效时机 / 一致性保证 / 降级策略 |
| 5 | 性能指标 | ✓ | P50 / P99 / QPS / 慢查询阈值 |
| 6 | 安全 / 鉴权 | ✓ | 权限校验 / 数据脱敏 / 防刷 / ID 遍历 |
| 7 | 跨 US 依赖 | ✓ | 被哪些 US 调用 / 调用哪些 US |
| 8 | 异常与边界 | ✓ | 所有异常场景的技术处理 |
| 9 | 实现顺序（与 test-plan 的映射） | ✓ | 每个章节对应 test-plan 的哪些 Task |
| 10 | 上下游引用 | ✓ | user-story / test-plan / 全局规范链接 |
| 11 | 变更日志 | ✓ | 版本 / 日期 / 作者 / 变更 |

---

## 2. 每章详细要求

### §1 数据模型影响

- 列出所有新增 / 修改 / 读取的表
- 给出关键索引及理由
- 如果涉及迁移，给出 SQL

```markdown
### 1.2 索引

```sql
CREATE INDEX idx_coach_status_rating ON coach(status, rating DESC);
```
```

### §2 API 设计

每个 API 必须包含：
- 路径 + 方法
- 鉴权要求
- 幂等性
- Query / Body / Path 参数
- 成功响应（JSON 示例）
- 错误响应（状态码 + 错误码 + message）
- 业务规则

### §3 状态机影响

- 用 ASCII 或 Mermaid 画出状态流转
- 标注触发条件
- 说明本 US 是"只读"还是"会修改状态"

### §4 缓存策略

- 缓存层（Redis / 前端 storage）
- Key 命名规范
- TTL
- 失效机制（主动失效 / 定时失效）
- 缓存穿透 / 击穿 / 雪崩处理
- 降级策略

### §5 性能指标

- 每个接口给出 P50 / P99 目标
- DB 查询耗时目标
- 并发目标
- 验证方式（test-plan 哪个 Task / 哪个压测）

### §6 安全 / 鉴权

- 是否需要登录
- 是否需要特定角色权限
- 限流策略
- 敏感数据是否脱敏
- 防止 ID 遍历、越权访问

### §7 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-010 | 被依赖 | 产生 coach 数据 |
| US-017 | 依赖本 US | 从详情页跳转 |

### §8 异常与边界

| 场景 | 处理 |
|------|------|
| 无数据 | 返回空数组 + 200 |
| ID 不存在 | 404 + 统一错误码 |
| Redis 宕机 | 降级查 DB |

### §9 实现顺序（与 test-plan 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 | Task 1, Task 2 |
| §2.1 GET /coaches | Task 3 |
| §2.2 GET /coaches/:id | Task 4 |
| §4 缓存策略 | Task 5 |

---

## 3. 与 test-plan.md 的分工边界

| 内容 | 归属 | 原因 |
|------|------|------|
| API 路径 + 字段定义 | tech-design.md | 设计层 |
| API 的具体测试代码 | test-plan.md | 执行层 |
| 数据模型 / 索引 | tech-design.md | 设计层 |
| Repository 的测试代码 | test-plan.md | 执行层 |
| 缓存策略 | tech-design.md | 设计层 |
| 缓存相关的 TDD 步骤 | test-plan.md | 执行层 |
| 性能指标目标 | tech-design.md | 设计层 |
| 性能验证 task | test-plan.md | 执行层 |

---

## 4. 禁止项

| # | 禁止行为 | 原因 |
|---|---------|------|
| 1 | 在 tech-design.md 写逐 task 的执行步骤 | 那是 test-plan.md 的职责 |
| 2 | 在 tech-design.md 写 Figma 相关内容 | 已合并到 user-story.md §13-15 |
| 3 | 在 tech-design.md 写业务级 GWT | 已在 user-story.md §6 |
| 4 | 使用 "TBD" / "TODO" / "待补充" | 设计层必须完整可评审 |
| 5 | 与 test-plan.md 的实现代码重复 | 设计层只给方案，不给执行命令 |

---

## 5. 与其他规范的关系

- **上游**：[docs/spec/user-story/SPECIFICATION.md](../user-story/SPECIFICATION.md) 约束 US 文档结构
- **下游产物**：`docs/stories/US-XXX-.../tech-design.md`（每 US 一份）
- **配套**：[docs/spec/test-plan/README.md](../test-plan/README.md) 约束 TDD 任务清单

---

## 6. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v0.1 | 2026-07-30 | Dev | 初版占位 |
| v1.0 | 2026-07-30 | Dev | 完整定义：11 个必填章节 + 与 test-plan 的分工边界 + 禁止项 |
