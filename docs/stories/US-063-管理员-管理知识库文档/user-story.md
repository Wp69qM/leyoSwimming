# US-063 管理员-管理知识库文档

> **状态**：[APPROVAL]
> **优先级**：[MVP]
> **估时**：0.8 人天
> **作者**：AI | **最后更新**：2026-09-03
> **配套文档**：需求：[rag-function-calling-requirements.md](../../../tech/AI-assistant-requirements/rag-function-calling-requirements.md) · 设计：[2026-09-02-ai-assistant-rag-design.md](../../../superpowers/spec/2026-09-02-ai-assistant-rag-design.md) · Figma：[A-knowledge-management-page.md](../../../figma/page-spec/A-knowledge-management-page.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-063 |
| **标题** | 管理员-管理知识库文档 |
| **角色（Actor）** | 管理员 |
| **业务价值（Why）** | 让管理员能控制哪些知识文档可被 AI 助理引用，支持启停和清理 |
| **优先级** | [MVP] |
| **估时** | 0.8 人天 |

---

## 2. 触发条件

- **触发方**：已登录管理员
- **触发动作**：进入知识库管理页面，执行列表查询、启用/禁用、删除操作
- **触发时机**：主动操作

---

## 3. 前置条件

- [x] 管理员已登录（US-053）
- [x] 管理员拥有 ADMIN 角色
- [x] 已存在知识库文档（US-062）

---

## 4. 业务流程

### 4.1 主路径：查看列表

1. 管理员进入「知识库管理」页面
2. 页面默认展示所有启用/禁用状态的文档列表
3. 支持按分类、状态、关键词筛选
4. 支持分页，默认 20 条/页

### 4.2 主路径：启用/禁用文档

1. 管理员在列表中找到目标文档
2. 点击「禁用」按钮（当前为启用状态）
3. `backend` 更新 `ai_knowledge_document.status = 1`
4. `backend` 调用 `ai-service` `/api/internal/ai/knowledge/delete` 删除对应向量
5. 列表状态更新为「禁用」
6. 再次点击「启用」则反向操作，重新调用 ingest 重建索引

### 4.3 主路径：删除文档

1. 管理员点击「删除」按钮
2. 弹出二次确认弹窗
3. 确认后 `backend` 删除 MySQL 记录
4. `backend` 调用 `ai-service` 删除向量
5. 列表刷新

### 4.4 异常分支

- **分支 1**：删除时 `ai-service` 不可用 → MySQL 记录删除，向量删除失败记录日志，后台告警
- **分支 2**：非 ADMIN 访问 → 返回 403

---

## 5. 业务规则引用

| # | 规则 | 来源 |
|---|------|------|
| 1 | 仅 ADMIN 可访问知识库管理功能 | 本 US §3 |
| 2 | 禁用文档后向量库中对应 chunk 需被移除 | 设计规格 §5.2 |
| 3 | 删除文档需同时清理 MySQL 元数据和向量库 | 需求文档 §3 |
| 4 | 列表支持分页和筛选 | 需求文档 §3 |

---

## 6. 验收标准（业务级 Gherkin）

### 场景 1：查看知识库列表

```gherkin
Given 管理员已登录
And   知识库中已有 3 条文档
When  管理员进入知识库管理页
Then  列表展示 3 条文档的标题、分类、状态、上传时间
And   默认展示第 1 页，每页 20 条
```

### 场景 2：禁用文档

```gherkin
Given 管理员已登录
And   存在启用状态的文档"野泳安全指南"
When  管理员点击"禁用"
Then  文档状态变为"禁用"
And   ai-service 向量库中不再包含该文档 chunk
And   用户提问相关问题时检索不到该文档
```

### 场景 3：启用已禁用文档

```gherkin
Given 文档"野泳安全指南"处于禁用状态
When  管理员点击"启用"
Then  文档状态变为"启用"
And   ai-service 重新为该文档建立向量索引
And   用户提问相关问题时可以检索到该文档
```

### 场景 4：删除文档

```gherkin
Given 管理员已登录
And   存在文档"野泳安全指南"
When  管理员点击"删除"并确认
Then  MySQL 中删除该记录
And   ai-service 向量库中删除对应 chunk
And   列表中不再展示该文档
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `ai_knowledge_document` | 读/写 | 查询列表、更新状态、删除记录 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/knowledge/list` | POST | 新增 | 文档列表分页查询 |
| 2 | `/api/admin/knowledge/toggle` | POST | 新增 | 启用/禁用文档 |
| 3 | `/api/admin/knowledge/delete` | POST | 新增 | 删除文档 |
| 4 | `/api/internal/ai/knowledge/delete` | POST | 新增 | 内部接口：删除向量索引 |
| 5 | `/api/internal/ai/knowledge/rebuild` | POST | 新增 | 内部接口：重建向量索引 |

### 7.3 状态机影响

| 状态 | 说明 |
|------|------|
| 启用（0） | 文档可被用户检索 |
| 禁用（1） | 文档不可被用户检索，向量库中无对应 chunk |

---

## 8. 边界场景

### 8.1 边界场景 1：删除时 ai-service 不可用
MySQL 记录删除成功，但向量删除失败，记录日志并触发后台告警。

### 8.2 边界场景 2：非 ADMIN 访问
返回 403，无操作权限。

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）
- US-053 管理员-账号密码登录
- US-062 管理员-上传知识库文档

### 9.2 后续 US（依赖本故事）
- US-064 用户-从知识库获取游泳知识

---

## 10. INVEST 自检

| 维度 | 评估 |
|------|------|
| Independent | 否，依赖 US-062 的实体与接口 |
| Negotiable | 是，MVP 仅支持启用/禁用/删除 |
| Valuable | 是，管理员可控制知识库内容 |
| Estimable | 是，估时 0.8 天 |
| Small | 是，范围明确 |
| Testable | 是，有明确验收标准 |

---

## 11. 完整性检查

- [x] 业务流程覆盖列表、启用/禁用、删除
- [x] 验收标准覆盖状态同步到向量库
- [x] API 影响表列出所有新增接口
- [x] page-spec 链接完整

---

## 12. 备注

- 禁用/启用操作需同步更新向量库，保证用户检索结果一致
- 删除时若 ai-service 不可用，需记录日志并告警

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| 内容 | 链接 | 状态 |
|------|------|------|
| 知识库管理页 page-spec | [A-knowledge-management-page.md](../../figma/page-spec/A-knowledge-management-page.md) | ✅ |

---

## 14. 设计评审记录

| 版本 | 日期 | 评审人 | 结论 |
|------|------|--------|------|
| v1.0 | 2026-09-02 | AI | DRAFT，待评审 |
| v1.1 | 2026-09-02 | AI | REVIEW，三件套一致性检查通过 |
| v1.2 | 2026-09-03 | AI | APPROVAL，API 已按 api-convention 规范化，OpenSpec / page-spec / Calicat 已对齐 |
