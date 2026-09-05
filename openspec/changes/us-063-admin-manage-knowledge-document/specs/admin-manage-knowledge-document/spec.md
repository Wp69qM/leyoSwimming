> **OpenSpec Spec | 映射自 `docs/stories/US-063-管理员-管理知识库文档/user-story.md` §6**

## Capability

管理员查看、启用/禁用、删除知识库文档

## ADDED Requirements

### Requirement: REQ-001 知识库文档列表与筛选

系统 MUST 提供管理后台知识库文档分页列表能力。列表 MUST 支持按分类、状态筛选，MUST 支持按关键词模糊搜索标题，MUST 仅允许 ADMIN 访问。

#### Scenario: 管理员查看知识库列表
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 知识库中已存在 3 条文档
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/list`
- **THEN** 系统返回文档列表
- **AND** 每行展示文档 ID、标题、分类、状态、上传时间
- **AND** 默认展示第 1 页，每页 20 条

#### Scenario: 管理员按分类和状态筛选列表
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 知识库中存在分类为 `safety`、状态为 `0` 的文档
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/list` 传入 `{ "category": "safety", "status": 0 }`
- **THEN** 系统仅返回分类为 `safety` 且状态为启用的文档

#### Scenario: 非 ADMIN 访问列表被拒绝
- **GIVEN** 用户 U 未登录或角色不是 ADMIN
- **WHEN** 用户 U 调用 `POST /api/admin/knowledge/list`
- **THEN** 系统返回 HTTP 403，错误码 `ADMIN_PERMISSION_DENIED`

### Requirement: REQ-002 启用/禁用文档并同步向量库

系统 MUST 允许 ADMIN 启用或禁用知识库文档。禁用 MUST 删除向量库中对应 chunk；启用 MUST 重新为文档建立向量索引。

#### Scenario: 管理员禁用启用中的文档
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 文档 D 存在，`documentId=1001`，`status=0`
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/toggle` 传入 `{ "documentId": 1001, "status": 1 }`
- **THEN** `ai_knowledge_document` 中 D 的 `status` 更新为 1
- **AND** `ai-service` 向量库中不再包含 D 的 chunk
- **AND** 用户提问相关问题时检索不到 D

#### Scenario: 管理员启用已禁用的文档
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 文档 D 存在，`documentId=1001`，`status=1`
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/toggle` 传入 `{ "documentId": 1001, "status": 0 }`
- **THEN** `ai_knowledge_document` 中 D 的 `status` 更新为 0
- **AND** `ai-service` 重新为 D 建立向量索引
- **AND** 用户提问相关问题时可以检索到 D

### Requirement: REQ-003 删除文档并清理向量库

系统 MUST 允许 ADMIN 删除知识库文档。删除 MUST 同时清理 MySQL 元数据和向量库 chunk。

#### Scenario: 管理员删除文档成功
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 文档 D 存在，`documentId=1001`
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/delete` 传入 `{ "documentId": 1001 }`
- **THEN** `ai_knowledge_document` 中删除 D 的记录
- **AND** `ai-service` 向量库中删除 D 对应 chunk
- **AND** 列表中不再展示 D

#### Scenario: 删除时 ai-service 不可用仍清理元数据
- **GIVEN** 管理员 M 已登录且角色为 ADMIN
- **AND** 文档 D 存在，`documentId=1001`
- **AND** `ai-service` 当前不可用
- **WHEN** 管理员 M 调用 `POST /api/admin/knowledge/delete` 传入 `{ "documentId": 1001 }`
- **THEN** `ai_knowledge_document` 中删除 D 的记录
- **AND** 系统记录错误日志并触发后台告警
