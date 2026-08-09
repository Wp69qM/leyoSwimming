# US-057 管理员管理管理员账号

> **状态**：[APPROVAL]（已确认）
> **优先级**：[MVP]
> **估时**：0.5 人天
> **作者**：PM　|　**最后更新**：2026-08-08
> **配套文档**：Figma：[A-admin-management-page.md](../../figma/page-spec/A-admin-management-page.md) / [A-admin-create-modal.md](../../figma/page-spec/A-admin-create-modal.md) / [A-admin-edit-modal.md](../../figma/page-spec/A-admin-edit-modal.md) / [A-admin-view-modal.md](../../figma/page-spec/A-admin-view-modal.md) / [A-admin-reset-password-modal.md](../../figma/page-spec/A-admin-reset-password-modal.md)　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-057 |
| **标题** | 管理员管理管理员账号 |
| **角色（Actor）** | 超级管理员 `super_admin`（主）、普通管理员 `admin`（辅，只读）、系统（辅） |
| **业务价值（Why）** | 让 `super_admin` 在后台集中维护管理员账号，支持新建、查看、编辑、禁用/启用、删除及重置密码，保障后台权限可控与账号安全 |
| **优先级** | [MVP] |
| **估时** | 0.5 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方） |

---

## 2. 触发条件

- **触发方**：已登录管理员
- **触发动作**：在 Web 管理后台「系统设置 → 管理员账号」进行查询、新建、查看、编辑、禁用/启用、删除或重置密码操作
- **触发时机**：管理员主动操作

---

## 3. 前置条件

- [x] 管理员已登录 Web 管理后台（依赖 US-053）
- [x] 当前管理员角色为 `super_admin` 时，方可执行写操作（新建、编辑他人、禁用/启用、删除、重置密码）
- [x] 当前管理员角色为 `admin` 时，仅可查看管理员列表和查看自身资料详情
- [x] 目标管理员账号存在于 `admin_user` 表中
- [x] 任何管理员均不可删除自己的账号
- [x] 任何管理员均不可禁用/删除唯一在线的 `super_admin` 账号，避免系统无超级管理员

---

## 4. 业务流程

### 4.1 主路径

1. 管理员进入「系统设置 → 管理员账号」
2. 系统展示管理员账号列表，默认按创建时间降序，支持按角色、状态、关键词筛选
3. 管理员搜索/筛选目标账号
4. 管理员执行以下任一操作：
   - **查看详情**：点击「查看」打开详情弹窗，展示管理员基础信息（姓名、登录账号、角色、状态、最近登录时间、创建时间）及操作日志
   - **新建账号**：`super_admin` 点击「新建管理员」，填写姓名、登录账号、初始密码、角色（`admin` / `super_admin`），提交后系统写入 `admin_user` 表，`status=0`，记录审计日志
   - **编辑资料**：`super_admin` 点击「编辑」，可修改管理员姓名、角色；保存后更新 `admin_user` 并记录审计日志
   - **禁用/启用**：`super_admin` 点击「禁用」或「启用」，二次确认后更新 `admin_user.status`（0=启用 / 1=禁用），禁用后该账号无法登录（依赖 US-053 的 `ADMIN_DISABLED` 校验），记录审计日志
   - **删除账号**：`super_admin` 点击「删除」，二次确认后从 `admin_user` 表逻辑删除（或物理删除，按 tech-design 约定），被删除账号的所有 `admin_session` 立即失效，记录审计日志
   - **重置密码**：`super_admin` 点击「重置密码」，系统生成随机强密码并展示一次（或发送给指定渠道），更新 `admin_user.password_hash`，记录审计日志
5. 系统记录操作审计日志
6. 返回操作成功提示

### 4.2 异常分支

- **分支 1**：当前管理员无权限 → 返回错误码 `ADMIN_PERMISSION_DENIED`
- **分支 2**：查看/编辑/禁用/启用/删除/重置密码时目标账号不存在 → 返回错误码 `ADMIN_NOT_FOUND`（列表查询为空时正常返回空数组，不触发本错误）
- **分支 3**：新建账号时登录账号已存在 → 返回错误码 `ADMIN_USERNAME_ALREADY_EXISTS`
- **分支 4**：`admin` 角色尝试执行写操作 → 返回错误码 `ADMIN_PERMISSION_DENIED`
- **分支 5**：尝试删除当前登录账号自身 → 返回错误码 `ADMIN_CANNOT_DELETE_SELF`
- **分支 6**：尝试禁用/删除最后一个启用的 `super_admin` → 返回错误码 `ADMIN_LAST_SUPER_ADMIN_PROTECTED`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 后台管理系统需通过独立的管理员账号密码登录 | [§10.1](../../prd/prd.md) |
| 2 | 管理员账号由系统预置或超级管理员创建，不支持自助注册 | [§10.1](../../prd/prd.md) |
| 3 | 敏感操作需记录审计日志 | [§11.1](../../prd/prd.md) |
| 4 | `super_admin` 拥有最高权限，`admin` 为普通后台操作员 | [§10.1](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

> 本 US 为 L2（0.5 人天），包含 5 个正常场景 + 3 个异常场景 = 8 个场景。

### 6.1 场景 1：super_admin 查看管理员列表

```gherkin
Given 管理员 M 已登录且角色为 super_admin
And   系统中存在 2 位 super_admin 和 3 位 admin
When  管理员 M 进入「系统设置 → 管理员账号」
Then  系统返回管理员账号列表
And   列表每行展示管理员 ID、姓名、登录账号、角色、状态、最近登录时间、创建时间
And   默认按创建时间降序排列
And   返回 HTTP 200
```

### 6.2 场景 2：super_admin 新建管理员账号

```gherkin
Given 管理员 M 已登录且角色为 super_admin
And   登录账号 "new_admin" 未被占用
When  管理员 M 点击「新建管理员」
And   填写姓名 "新管理员"、登录账号 "new_admin"、初始密码 "Admin@1234"、角色 "admin"
And   点击「保存」
Then  admin_user 表新增 1 条记录
And   新记录 name="新管理员"、username="new_admin"、role="admin"、status=0
And   password_hash 为加密后的密文
And   audit_log 新增 1 条 action='CREATE' 记录
And   返回 HTTP 200 与提示"管理员账号创建成功"
```

### 6.3 场景 3：super_admin 编辑管理员角色

```gherkin
Given 管理员 M 已登录且角色为 super_admin
And   管理员 A 存在，user_id=1001，角色为 admin
When  管理员 M 在列表点击管理员 A 的「编辑」
And   将角色从 "admin" 修改为 "super_admin"
And   点击「保存」
Then  admin_user 表中管理员 A 的 role 更新为 "super_admin"
And   admin_user.updated_at 更新
And   audit_log 新增 1 条 action='UPDATE' 记录
And   返回 HTTP 200 与提示"管理员资料已更新"
```

### 6.4 场景 4：super_admin 禁用管理员账号

```gherkin
Given 管理员 M 已登录且角色为 super_admin
And   管理员 A 存在，user_id=1001，status=0
And   系统中启用的 super_admin 数量 >= 2
When  管理员 M 点击管理员 A 的「禁用」并确认
Then  admin_user 表中管理员 A 的 status 更新为 1
And   该账号所有 admin_session 记录被置为失效（或删除）
And   audit_log 新增 1 条 action='DISABLE' 记录
And   返回 HTTP 200 与提示"账号已禁用"
```

### 6.5 场景 5：super_admin 删除管理员账号

```gherkin
Given 管理员 M 已登录且角色为 super_admin
And   管理员 A 存在，user_id=1001
And   管理员 A 不是当前登录账号
When  管理员 M 点击管理员 A 的「删除」并确认
Then  admin_user 表中管理员 A 的记录被删除（或标记为已删除）
And   该账号所有 admin_session 记录被删除/失效
And   audit_log 新增 1 条 action='DELETE' 记录
And   返回 HTTP 200 与提示"账号已删除"
```

### 6.6 场景 6：admin 尝试新建管理员账号（无权限）

```gherkin
Given 管理员 M 已登录且角色为 admin
When  管理员 M 调用新建管理员接口
Then  系统返回错误码 ADMIN_PERMISSION_DENIED
And   admin_user 表无新增记录
And   返回 HTTP 403
```

### 6.7 场景 7：super_admin 尝试删除自己的账号

```gherkin
Given 管理员 M 已登录且角色为 super_admin，user_id=1001
When  管理员 M 尝试删除 user_id=1001 的管理员账号
Then  系统返回错误码 ADMIN_CANNOT_DELETE_SELF
And   admin_user 表中管理员 M 的记录未被删除
And   返回 HTTP 400
```

### 6.8 场景 8：super_admin 尝试禁用最后一个 super_admin

```gherkin
Given 管理员 M 已登录且角色为 super_admin
And   系统中 status=0 的 super_admin 仅管理员 M 1 人
When  管理员 M 尝试禁用自己的账号
Then  系统返回错误码 ADMIN_LAST_SUPER_ADMIN_PROTECTED
And   admin_user 表中管理员 M 的 status 保持为 0
And   返回 HTTP 400
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 数据表 | 操作 | 说明 |
|---|--------|------|------|
| 1 | `admin_user` | 查询/新增/更新/删除 | 管理员账号主表，与 US-053 共用 |
| 2 | `admin_session` | 查询/更新/删除 | 禁用/删除账号时使会话失效 |
| 3 | `admin_audit_log` | 新增 | 记录所有写操作 |

### 7.2 API 设计

| # | URL | 方法 | 类型 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/admin/list` | POST | 新增 | 分页查询管理员账号列表 |
| 2 | `/api/admin/admin/detail` | POST | 新增 | 查看管理员账号详情 |
| 3 | `/api/admin/admin/add` | POST | 新增 | 新建管理员账号 |
| 4 | `/api/admin/admin/update` | POST | 新增 | 编辑管理员资料 |
| 5 | `/api/admin/admin/toggle-status` | POST | 新增 | 禁用/启用管理员账号 |
| 6 | `/api/admin/admin/delete` | POST | 新增 | 删除管理员账号 |
| 7 | `/api/admin/admin/reset-password` | POST | 新增 | 重置管理员密码 |

### 7.3 状态机影响

| 状态 | 触发 US | 转换 |
|------|---------|------|
| 管理员账号状态 | US-057 | 启用 ↔ 禁用（status 0 ↔ 1） |
| 管理员账号存在性 | US-057 | 存在 → 已删除（delete） |

---

## 8. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-053 | 被本 US 依赖 | 管理员账号密码登录提供认证与 `admin_user` 表 |
| US-041 ~ US-049 | 反向依赖 | 这些管理后台 US 的操作者账号由本 US 维护 |

---

## 9. 异常与边界

| 场景 | 处理 |
|------|------|
| 无权限操作 | 返回 403 `ADMIN_PERMISSION_DENIED` |
| 目标管理员不存在 | 返回 404 `ADMIN_NOT_FOUND` |
| 登录账号重复 | 返回 400 `ADMIN_USERNAME_ALREADY_EXISTS` |
| 删除自身 | 返回 400 `ADMIN_CANNOT_DELETE_SELF` |
| 禁用自身 | 返回 400 `ADMIN_CANNOT_DISABLE_SELF` |
| 禁用/删除最后一个 super_admin | 返回 400 `ADMIN_LAST_SUPER_ADMIN_PROTECTED` |
| 重置密码 | 生成随机强密码，展示一次后不再明文可回显 |

---

## 10. 验收检查清单（INVEST 与 DoR）

- [x] **I**ndependent（独立）- 在 US-053 完成后可独立交付
- [x] **N**egotiable（可协商）- 删除策略（逻辑/物理）可在技术设计中确认
- [x] **V**aluable（有价值）- 保障后台账号安全与权限可控
- [x] **E**stimable（可估算）- 0.5 人天，界面与接口范围明确
- [x] **S**mall（小）- 仅管理员账号 CRUD，不包含 RBAC 权限点配置
- [x] **T**estable（可测试）- 8 个 GWT 场景可客观验证
- [x] 边界清晰：仅维护 `admin_user` 账号本身，不管理角色权限点
- [x] 前置依赖明确：US-053 管理员登录认证
- [x] 异常场景覆盖：权限、重复、自删除、最后超级管理员保护
- [x] 场景数量符合 L2（5 正常 + 3 异常 = 8 个）

---

## 11. 相关文档索引

- [PRD §10.1](../../prd/prd.md) — 管理员后台登录与账号管理
- [PRD §11.1](../../prd/prd.md) — 审计日志
- [US-053 管理员账号密码登录](../US-053-管理员-账号密码登录/user-story.md) — 管理员认证入口
- [A-admin-management-page.md](../../figma/page-spec/A-admin-management-page.md) — 管理员账号管理列表页
- [A-admin-create-modal.md](../../figma/page-spec/A-admin-create-modal.md) — 新建管理员弹窗
- [A-admin-edit-modal.md](../../figma/page-spec/A-admin-edit-modal.md) — 编辑管理员弹窗
- [A-admin-view-modal.md](../../figma/page-spec/A-admin-view-modal.md) — 管理员账号查看弹窗
- [A-admin-reset-password-modal.md](../../figma/page-spec/A-admin-reset-password-modal.md) — 重置密码结果弹窗

---

## 12. 备注

- **安全要求**：
  - 密码必须加密存储（bcrypt/scrypt 等），禁止明文存储
  - 重置密码生成的随机密码应满足复杂度要求
  - 删除/禁用账号应立即失效其所有会话
- **权限说明**：
  - 本 US 仅维护 `admin_user` 账号记录
  - 细粒度 RBAC 权限点的分配由其他机制负责（如需独立 US 可后续补充）
- **审计要求**：所有写操作必须写入 `admin_audit_log`，包含操作人、目标账号、操作类型、变更前后快照、操作时间

---

## 13. Figma 链接

> 提供 Figma file URL 与关键 frame 引用。Figma **设计系统规范**（token / 组件 / 状态徽标 / 4 态模板 / 文案）见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 | 状态 |
|---|------|------|------|
| 1 | 管理员账号管理列表页 | [A-admin-management-page.md](../../figma/page-spec/A-admin-management-page.md) | ✅ |
| 2 | 新建管理员弹窗 | [A-admin-create-modal.md](../../figma/page-spec/A-admin-create-modal.md) | ✅ |
| 3 | 编辑管理员弹窗 | [A-admin-edit-modal.md](../../figma/page-spec/A-admin-edit-modal.md) | ✅ |
| 4 | 管理员账号查看弹窗 | [A-admin-view-modal.md](../../figma/page-spec/A-admin-view-modal.md) | ✅ |
| 5 | 重置密码结果弹窗 | [A-admin-reset-password-modal.md](../../figma/page-spec/A-admin-reset-password-modal.md) | ✅ |

### 13.1 状态截图清单

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **管理员账号管理列表页** | 🔲 | 🔲 | 🔲 | 🔲 | 含筛选、搜索、新建按钮、操作列 |
| **新建管理员弹窗** | — | 🔲 | 🔲 | 🔲 | 含姓名、登录账号、初始密码、角色 |
| **编辑管理员弹窗** | — | 🔲 | 🔲 | 🔲 | 含姓名、角色；不可修改登录账号 |
| **禁用/启用二次确认弹窗** | — | — | — | — | 使用全局确认弹窗组件 |
| **删除二次确认弹窗** | — | — | — | — | 使用全局确认弹窗组件 |
| **重置密码结果弹窗** | — | — | — | 🔲 | 展示一次性随机密码 |

---

## 14. 页面级设计决策

### 14.1 列表页操作列权限控制

- **背景**：不同角色看到操作列的入口不同
- **选项**：A. 前端统一渲染所有按钮再按权限灰显；B. 后端返回操作列表，前端只渲染有权限的按钮
- **结论**：选择 B，后端返回 `allowed_actions` 数组，前端只渲染有权限的入口，避免无意义按钮干扰
- **影响范围**：管理员账号管理列表页、所有后台管理列表页

### 14.2 删除策略

- **背景**：删除管理员账号后，历史审计日志仍需保留操作人信息
- **选项**：A. 物理删除，审计日志只记录 admin_id；B. 逻辑删除，保留账号快照
- **结论**：按 tech-design 约定执行；默认推荐逻辑删除或软删除，保留姓名等快照用于审计展示
- **影响范围**：`admin_user` 表结构、删除接口实现

---

## 15. 变更日志

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| v1.0 | 2026-08-08 | PM | 初版：定义管理员管理管理员账号的 CRUD 流程、权限边界、8 个 Gherkin 场景及 Figma page-spec 映射 |
