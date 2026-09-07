# US-062 管理员-上传知识库文档

> **状态**：[APPROVAL]
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：AI | **最后更新**：2026-09-03
> **配套文档**：需求：[rag-function-calling-requirements.md](../../tech/AI-assistant-requirements/rag-function-calling-requirements.md) · 设计：[2026-09-02-ai-assistant-rag-design.md](../../superpowers/spec/2026-09-02-ai-assistant-rag-design.md) · Figma：[A-knowledge-upload-modal.md](../../figma/page-spec/A-knowledge-upload-modal.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-062 |
| **标题** | 管理员-上传知识库文档 |
| **角色（Actor）** | 管理员 |
| **业务价值（Why）** | 为 AI 助理提供可信赖的游泳知识来源，使其能回答安全/急救/训练类问题 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |

---

## 2. 触发条件

- **触发方**：已登录管理员
- **触发动作**：在知识库管理页面点击"新增文档"，填写内容或上传文件后提交
- **触发时机**：主动操作

---

## 3. 前置条件

- [x] 管理员已登录（US-053）
- [x] 管理员拥有 ADMIN 角色
- [x] `ai-service` 已部署并可访问
- [x] `backend` 与 `ai-service` 内部接口可通信

---

## 4. 业务流程

### 4.1 主路径：手动输入上传

1. 管理员进入「知识库管理」页面
2. 点击「新增文档」按钮，弹出上传弹窗
3. 选择「手动输入」方式
4. 填写标题、选择分类（安全/技巧/急救）、选择内容类型（text/markdown）
5. 在文本域中输入文档内容
6. 点击「保存」
7. `backend` 校验标题唯一性、权限
8. `backend` 将元数据写入 `ai_knowledge_document` 表
9. `backend` 调用 `ai-service` `/api/internal/ai/knowledge/ingest`
10. `ai-service` 解析、分块、Embedding、写入 Chroma
11. 返回成功提示，弹窗关闭，列表刷新

### 4.2 主路径：文件上传

1-2. 同手动输入
3. 选择「文件上传」方式
4. 填写标题、选择分类
5. 点击上传区域，选择 `.txt` 或 `.md` 文件
6. 点击「保存」
7-11. 同手动输入，但 content 来自文件解析

### 4.3 异常分支

- **分支 1**：标题已存在 → 提示"该标题已存在"
- **分支 2**：文件格式不支持 → 提示"仅支持 .txt/.md 文件"
- **分支 3**：`ai-service` 索引失败 → 回滚 MySQL 写入，提示"索引失败，请重试"
- **分支 4**：管理员无权限 → 返回 403

---

## 5. 业务规则引用

| # | 规则 | 来源 |
|---|------|------|
| 1 | 仅 ADMIN 角色可上传知识库文档 | 本 US §3 |
| 2 | 文档标题全局唯一 | 设计规格 §4.1 |
| 3 | 支持内容类型：text / markdown | 设计规格 §2 |
| 4 | 上传失败的文档不进入向量库 | 需求文档 §3 |
| 5 | 文档内容需经 `ai-service` 分块、Embedding 后入向量库 | 设计规格 §3.1 |

---

## 6. 验收标准（业务级 Gherkin）

### 场景 1：手动输入成功上传

```gherkin
Given 管理员已登录且为 ADMIN 角色
When  管理员在知识库管理页点击"新增文档"
And   选择手动输入
And   填写标题为"野泳安全指南"
And   选择分类为"安全"
And   输入 Markdown 内容
And   点击保存
Then  系统返回上传成功
And   MySQL 中新增一条 ai_knowledge_document 记录
And   ai-service 向量库中存在该文档的分块向量
And   列表页展示新上传的文档
```

### 场景 2：文件上传成功

```gherkin
Given 管理员已登录
And   本地有一个 swimming_safety.md 文件
When  管理员选择文件上传
And   填写标题和分类
And   选择 swimming_safety.md 文件
And   点击保存
Then  系统解析文件内容并入库
And   返回上传成功
```

### 场景 3：标题重复

```gherkin
Given 已存在标题为"野泳安全指南"的文档
When  管理员再次上传标题为"野泳安全指南"的文档
Then  系统提示"该标题已存在"
And   不上传入库
```

### 场景 4：ai-service 索引失败回滚

```gherkin
Given 管理员上传有效文档
And   ai-service 当前不可用
When  管理员点击保存
Then  backend 不向 MySQL 写入记录（或写入后回滚）
And   提示"索引失败，请稍后重试"
And   向量库中不存在该文档
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `ai_knowledge_document` | 新增 | 知识库文档元数据表 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/knowledge/add` | POST | 新增 | 手动输入新增知识库文档 |
| 2 | `/api/admin/knowledge/upload` | POST | 新增 | 文件上传新增知识库文档 |
| 3 | `/api/internal/ai/knowledge/ingest` | POST | 新增 | 内部接口：ai-service 构建向量索引 |

### 7.3 状态机影响

无。

---

## 8. 边界场景

### 8.1 边界场景 1：标题重复
已存在相同标题文档时，接口返回业务错误，提示"该标题已存在"。

### 8.2 边界场景 2：文件格式不支持
上传非 `.txt` / `.md` 文件时，前端拦截或在接口返回格式错误。

### 8.3 边界场景 3：ai-service 索引失败
backend 写入 MySQL 后调用 ai-service 失败，需回滚 MySQL 记录。

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）
- US-053 管理员-账号密码登录

### 9.2 后续 US（依赖本故事）
- US-063 管理员-管理知识库文档
- US-064 用户-从知识库获取游泳知识

---

## 10. INVEST 自检

| 维度 | 评估 |
|------|------|
| Independent | 是，可独立开发部署 |
| Negotiable | 是，MVP 仅支持文本/Markdown |
| Valuable | 是，为 AI 助理提供知识来源 |
| Estimable | 是，估时 1 天 |
| Small | 是，范围明确 |
| Testable | 是，有明确验收标准 |

---

## 11. 完整性检查

- [x] 业务流程覆盖了手动输入与文件上传两条路径
- [x] 验收标准覆盖成功、失败、回滚场景
- [x] API 影响表列出所有新增接口
- [x] page-spec 链接完整

---

## 12. 备注

- 文件上传仅支持 `.txt` / `.md`，PDF/Word 后续批次扩展
- ai-service 索引失败时必须回滚 MySQL 写入

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| 内容 | 链接 | 状态 |
|------|------|------|
| 新增知识库文档弹窗 page-spec | [A-knowledge-upload-modal.md](../../figma/page-spec/A-knowledge-upload-modal.md) | ✅ |

---

## 14. 设计评审记录

| 版本 | 日期 | 评审人 | 结论 |
|------|------|--------|------|
| v1.0 | 2026-09-02 | AI | DRAFT，待评审 |
| v1.1 | 2026-09-02 | AI | REVIEW，三件套一致性检查通过 |
| v1.2 | 2026-09-03 | AI | APPROVAL，API 已按 api-convention 规范化，OpenSpec / page-spec / Calicat 已对齐 |
