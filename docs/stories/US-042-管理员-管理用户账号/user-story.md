# US-042 管理员管理用户账号

> **状态**：[APPROVAL]（已确认）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-08-08
> **配套文档**：Figma：[A-user-management-page.md](../../figma/page-spec/A-user-management-page.md) / [A-user-view-modal.md](../../figma/page-spec/A-user-view-modal.md) / [A-user-edit-modal.md](../../figma/page-spec/A-user-edit-modal.md) / [A-user-create-modal.md](../../figma/page-spec/A-user-create-modal.md)　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-042 |
| **标题** | 管理员管理用户账号 |
| **角色（Actor）** | 管理员（主）、系统（辅） |
| **业务价值（Why）** | 让管理员可查看、筛选用户列表，查看用户在小程序端完善的个人资料（头像、姓名、手机号、性别、年龄、游泳档案、监护人信息等），编辑除手机号外的资料字段，必要时封禁/解封账号，以及手动新建用户 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方） |

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：在管理后台「用户管理 → 用户账号」进行查询、编辑、新建或封禁/解封操作
- **触发时机**：管理员主动操作

---

## 3. 前置条件

- [x] 管理员已登录且 RBAC 角色为 `super_admin` 或 `admin`
- [x] 管理员具备当前操作对应的细粒度权限（`USER:READ` / `USER:WRITE` / `USER:BAN`，详见 §4.1 与 tech-design.md §8）
- [x] 目标用户账号存在于 `user` 表（C 端用户/学员）中
- [x] 本 US 仅操作 `user` 表，不涉及 `admin_user` 表；管理员账号的增删改查由 US-057「管理员管理管理员账号」负责

---

## 4. 业务流程

### 4.1 主路径

1. 管理员进入「用户管理 → 用户账号」
2. 系统展示用户列表，支持按身份、注册时间筛选
3. 管理员搜索/筛选目标用户
4. 管理员点击用户进入查看弹窗或编辑弹窗
5. 管理员执行以下任一操作：
   - 查看用户：按 RBAC 可见范围返回数据，查看弹窗展示用户档案（基础信息、游泳档案、监护人信息）
   - 编辑用户资料：管理员可在编辑弹窗修改用户在小程序端填写的资料字段（头像、姓名、性别、年龄、游泳基础、泳姿、游泳年限、个人描述、监护人信息等），并记录审计日志
   - 新建用户：管理员在用户列表点击「新建用户」，填写与用户侧完善个人资料一致的字段（头像、姓名、手机号、性别、年龄、游泳基础、泳姿、游泳年限、个人描述、监护人信息等），来源标记为「管理员添加」，并记录审计日志
   - 封禁/解封账号：管理员填写原因，系统更新 `user.status` 并记录审计日志
6. 系统记录操作审计日志
7. 返回操作成功提示

### 4.2 异常分支

- **分支 1**：管理员无权限 → 返回错误 `ADMIN_PERMISSION_DENIED`
- **分支 2**：目标用户不存在 → 返回错误 `USER_NOT_FOUND`
- **分支 3**：新建/编辑用户时未成年人缺少监护人信息 → 返回错误 `INVALID_GUARDIAN_INFO`
- **分支 4**：新建用户时手机号已被占用 → 返回错误 `PHONE_ALREADY_EXISTS`

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 管理员可查看所有游客、注册用户（含学员），按身份、注册时间筛选与统计 | [§5.5.1](../../prd/prd.md) |
| 2 | 管理员可编辑用户资料 | [§5.5.1](../../prd/prd.md) |
| 3 | 管理员可手动新建用户，来源标记为「管理员添加」 | [§5.5.1](../../prd/prd.md) |
| 4 | 敏感操作需记录审计日志 | [§11.1](../../prd/prd.md) |
| 5 | 新建用户时手机号唯一性校验；管理员不可修改用户手机号 | 本 US 约定 |
| 6 | 封禁/解封账号需记录原因并写入审计日志 | [§11.1](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

> 本 US 为 L2（1 人天），包含 6 个正常场景 + 5 个异常场景 = 11 个场景。

### 6.1 场景 1：管理员查看用户详情及档案

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   用户 U 已完善个人资料，user_id=1001
When  管理员 M 在用户列表点击用户 U 的「查看」按钮
Then  系统返回用户 U 的档案数据
And   页面展示用户 U 的头像、姓名、手机号、性别、年龄、注册来源、注册时间、最后登录时间
And   页面展示用户 U 的游泳档案：有无游泳基础、会什么泳姿、游泳年限、个人描述
And   若用户 U 年龄 < 18 周岁，页面展示监护人信息：监护人姓名、监护人手机号
And   返回 HTTP 200
```

### 6.2 场景 2：管理员编辑用户资料

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   用户 U 已完善个人资料，user_id=1001
When  管理员 M 在用户查看弹窗点击「编辑资料」，打开用户编辑弹窗
And   将用户 U 的姓名从 "张 swimmer" 修改为 "张教练"
And   将游泳年限从 3 年修改为 5 年
And   点击「保存」
Then  user.name 更新为 "张教练"
And   user.swim_years 更新为 5
And   user.updated_at 更新
And   audit_log 新增 1 条 action='ADMIN_UPDATE_PROFILE' 记录
And   返回 HTTP 200 与提示"用户资料已更新"
```

### 6.3 场景 3：管理员手动新建用户

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   手机号"13800138000"未被占用
When  管理员 M 在用户列表点击「新建用户」
And   上传头像、填写姓名 "张 swimmer"、手机号 "13800138000"、年龄 25、性别 男
And   选择有游泳基础，填写泳姿 "蛙泳/自由泳"、游泳年限 3 年、个人描述 "想提高自由泳"
And   点击「保存」
Then  系统创建 user 记录
And   user.phone="13800138000"、user.name="张 swimmer"、user.age=25、user.gender=1
And   user.source="ADMIN_CREATED"（管理员添加）
And   user.identity=1（注册用户）、user.status=0（正常）
And   user.profile_completed=true
And   audit_log 新增 1 条 action='ADMIN_CREATE_USER' 记录
And   返回 HTTP 201 与提示"用户已创建"
```

### 6.4 场景 4：管理员手动新建未成年人用户

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   手机号"13900139000"未被占用
When  管理员 M 在用户列表点击「新建用户」
And   上传头像、填写姓名 "李小小"、手机号 "13900139000"、年龄 12、性别 女
And   填写监护人姓名 "李大伟"、监护人手机号 "13800138000"
And   选择无游泳基础
And   点击「保存」
Then  系统创建 user 记录
And   user.phone="13900139000"、user.name="李小小"、user.age=12、user.gender=2
And   user.guardian_name="李大伟"、user.guardian_phone="13800138000"
And   user.source="ADMIN_CREATED"（管理员添加）
And   user.identity=1（注册用户）、user.status=0（正常）
And   user.profile_completed=true
And   audit_log 新增 1 条 action='ADMIN_CREATE_USER' 记录
And   返回 HTTP 201 与提示"用户已创建"
```

### 6.5 场景 5：管理员封禁用户账号

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   用户 U 当前状态为"正常"，user_id=1001
When  管理员 M 对用户 U 执行"封禁账号"并填写原因"涉嫌违规"
Then  user.status 更新为 2（封禁）
And   user.updated_at 更新
And   audit_log 新增 1 条 action='ADMIN_BAN_USER' 记录，remark="涉嫌违规"
And   返回 HTTP 200 与提示"账号已封禁"
```

### 6.6 场景 6：管理员解封用户账号

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   用户 U 当前状态为"封禁"，user_id=1001
When  管理员 M 对用户 U 执行"解封账号"并填写原因"申诉通过"
Then  user.status 更新为 0（正常）
And   user.updated_at 更新
And   audit_log 新增 1 条 action='ADMIN_UNBAN_USER' 记录，remark="申诉通过"
And   返回 HTTP 200 与提示"账号已解封"
```

### 6.7 场景 7：无权限管理员操作失败

```gherkin
Given 管理员 M2 已登录但无用户管理权限
When  管理员 M2 调用用户列表或修改用户资料接口
Then  返回错误码 ADMIN_PERMISSION_DENIED
And   HTTP 状态码 403
And   不修改任何用户数据
```

### 6.8 场景 8：目标用户不存在

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   用户 ID 999999 不存在
When  管理员 M 查询或修改该用户
Then  返回错误码 USER_NOT_FOUND
And   HTTP 状态码 404
```

### 6.9 场景 9：新建用户时手机号已被占用

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   手机号"13800138000"已被用户 U2 占用
When  管理员 M 新建用户并填写手机号"13800138000"
Then  返回错误码 PHONE_ALREADY_EXISTS
And   HTTP 状态码 409
And   不创建新用户
```

### 6.10 场景 10：新建未成年人用户时缺少监护人信息

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   手机号"13900139000"未被占用
When  管理员 M 新建用户并填写手机号"13900139000"、姓名"李小小"、年龄 12、性别 女
And   未填写监护人姓名和监护人手机号
And   点击「保存」
Then  返回错误码 INVALID_GUARDIAN_INFO
And   HTTP 状态码 400
And   不创建新用户
```

### 6.11 场景 11：编辑未成年人用户时缺少监护人信息

```gherkin
Given 管理员 M 已登录且具有用户管理权限
And   用户 U 年龄 12 岁，已填写监护人信息，user_id=1001
When  管理员 M 编辑用户 U 的资料
And   将监护人姓名和监护人手机号清空
And   点击「保存」
Then  返回错误码 INVALID_GUARDIAN_INFO
And   HTTP 状态码 400
And   不修改用户数据
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `user` | 修改/新增 | 用户档案字段（avatar_url、name、phone、age、gender、has_swim_basis、swim_strokes、swim_years、personal_desc、guardian_name、guardian_phone、profile_completed）、source、`status`（含 0=正常/1=注销/2=封禁）等；新建用户时 source='ADMIN_CREATED' |
| 2 | `audit_log` | 新增 | 记录敏感操作 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/user/list` | POST | 新增 | 查询用户列表；请求体 `{ page, pageSize, keyword, identity, status, profileCompleted, startDate, endDate }` |
| 2 | `/api/admin/user/add` | POST | 新增 | 手动新建用户 |
| 3 | `/api/admin/user/detail` | POST | 新增 | 查看用户详情；请求体 `{ userId }` |
| 4 | `/api/admin/user/update` | POST | 新增 | 编辑用户资料；请求体 `{ userId, profile, version }` |
| 5 | `/api/admin/user/ban` | POST | 新增 | 封禁账号；请求体 `{ userId, reason }` |
| 6 | `/api/admin/user/unban` | POST | 新增 | 解封账号；请求体 `{ userId, reason }` |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | — | — | — | 本 US 不涉及业务状态机转换 |

---

## 8. 边界场景

### 8.1 边界场景 1：并发编辑同一用户资料

- **触发条件**：两个管理员同时编辑同一用户资料
- **预期行为**：使用乐观锁，后到请求返回 `USER_CONCURRENTLY_UPDATED`，先到请求成功
- **用户可见反馈**：第二次操作提示「用户信息已被他人更新，请刷新后重试」

### 8.2 边界场景 2：批量导出用户列表

- **触发条件**：管理员需要导出筛选后的用户列表
- **预期行为**：系统支持按当前筛选条件导出 CSV（最多 10000 条），异步生成下载链接
- **用户可见反馈**：提示"导出任务已创建，完成后可下载"

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）/ US-006（用户手机号验证码登录）—— 产生 user 记录
- [x] US-005（用户完善个人资料）—— 完善 user 档案信息（头像、姓名、年龄、性别、监护人信息等）
- [x] US-007（用户账号注销）—— 产生 user.status = 1（软删除）状态变更，管理员需在后台查看和管理
- [x] US-053（管理员账号密码登录）—— 后台管理系统登录认证入口

### 9.2 后续 US（依赖本故事）

- [ ] US-047（管理员配置场馆、公告等）—— 同属于管理后台用户管理模块

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 可独立交付，不依赖其他后台 US
- [x] **N**egotiable（可协商）- 字段可在设计中细化
- [x] **V**aluable（有价值）- 支撑管理后台基础用户运营
- [x] **E**stimable（可估算）- 1 人天，范围明确
- [x] **S**mall（足够小）- 一个 Sprint 内可完成
- [x] **T**estable（可测试）- 11 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除 Figma 链接状态待设计填写）
- [x] 错误码明确（ADMIN_PERMISSION_DENIED / USER_NOT_FOUND / PHONE_ALREADY_EXISTS / USER_CONCURRENTLY_UPDATED / INVALID_GUARDIAN_INFO）

### 11.2 业务规则

- [x] 引用 §5.5.1 / §11.1
- [x] 与 RBAC 权限规则一致
- [x] 与审计日志规则一致

### 11.3 验收标准

- [x] 6 正常 + 5 异常 = 11 个 GWT 场景
- [x] 每个 Then 含具体状态码 / DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] Figma 链接/状态已列出
- [x] 技术设计文档已创建
- [x] 测试计划已创建

---

## 12. 备注

- **幂等键**：修改类接口使用 `admin:user:{user_id}:version:{user.updated_at}` 乐观锁
- **事务边界**：用户资料更新/新建与审计日志写入在同一事务
- **性能要求**：用户列表 P99 < 200ms，详情/修改/新建接口 P99 < 200ms
- **安全**：敏感操作（资料修改、新建用户、封禁）记录审计日志
- **来源标记**：管理员手动创建的用户 `source='ADMIN_CREATED'`，与用户侧注册来源区分

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 | 状态 |
|---|------|------|------|
| 1 | 用户账号列表页 | [A-user-management-page.md](../../figma/page-spec/A-user-management-page.md) | ✅ |
| 2 | 用户查看弹窗 | [A-user-view-modal.md](../../figma/page-spec/A-user-view-modal.md) | ✅ |
| 3 | 用户编辑弹窗 | [A-user-edit-modal.md](../../figma/page-spec/A-user-edit-modal.md) | ✅ |
| 4 | 新建用户弹窗 | [A-user-create-modal.md](../../figma/page-spec/A-user-create-modal.md) | ✅ |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **用户账号列表** | 🔲 | 🔲 | 🔲 | 🔲 | 空状态为无匹配用户；表格列详见 A-user-management-page.md |
| **用户查看弹窗** | — | 🔲 | 🔲 | 🔲 | 错误状态含用户不存在提示；档案字段详见 A-user-view-modal.md |
| **用户编辑弹窗** | — | 🔲 | 🔲 | 🔲 | 错误状态含字段校验提示；手机号只读，表单字段详见 A-user-edit-modal.md |
| **新建用户弹窗** | `A-新建用户弹窗-empty` | `A-新建用户弹窗-loading` | `A-新建用户弹窗-error` | `A-新建用户弹窗-success` | 空状态为全部空表单；手机号必填，表单字段详见 A-user-create-modal.md |

---

## 14. 页面级设计决策

### 14.1 管理员不可修改用户手机号

- **背景**：手机号是用户侧小程序登录凭证，与用户账号强绑定
- **选项**：A. 支持管理员修改手机号（需短信验证或强制变更）；B. 不支持管理员修改手机号
- **结论**：选择 B，MVP 中管理员不可修改用户手机号，避免登录凭证冲突；仅可在新建用户时指定手机号
- **影响范围**：用户编辑弹窗中手机号只读；移除原修改手机号 API

### 14.2 用户角色权限不在本 US 配置

- **背景**：后台系统的角色权限（super_admin / admin 等）用于管理后台访问控制，与用户侧小程序账号无关；管理员账号存储在 `admin_user` 表
- **选项**：A. 在用户管理页配置用户角色；B. 用户管理页仅管理普通用户/学员账号（`user` 表），管理员角色权限由单独的后台管理员管理功能配置
- **结论**：选择 B，本 US 不实现用户角色权限配置；用户列表/编辑/新建仅面向 C 端用户；管理员账号管理由 US-057「管理员管理管理员账号」负责
- **影响范围**：移除原「配置角色权限」操作及对应 API

### 14.3 新建用户弹窗复用编辑弹窗结构

- **背景**：管理员手动创建用户时，需要填写与用户侧完善资料一致的档案字段
- **选项**：A. 单独设计新建弹窗；B. 复用编辑弹窗表单结构，区别仅在于手机号可填、无初始数据
- **结论**：选择 B，降低设计/开发成本，字段规则与 US-005 对齐
- **影响范围**：新建用户弹窗字段与编辑弹窗一致，手机号必填且校验唯一性

### 14.4 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 筛选用户 | 选择身份/时间 | 列表实时刷新 | 支持组合筛选 |
| 编辑用户资料 | 保存 | 字段校验 → 成功/失败提示 | 手机号只读 |
| 新建用户 | 保存 | 字段校验 + 手机号唯一性校验 → 成功/失败提示 | 来源标记为 ADMIN_CREATED |
| 封禁/解封账号 | 点击按钮 | 二次确认弹窗 → 成功提示 | 需填写原因 |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-08-08 | PM | 管理员是否可修改用户手机号 | 明确 MVP 中不支持修改；编辑弹窗手机号只读 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.1 | 2026-07-31 | PM | §13 Figma 链接清理：预设占位 URL 改为 🔲 待设计填写，待设计师在 Figma Drafts 创建文件后回填真实链接 |
| v1.2 | 2026-08-05 | PM | §4.1 / §6.2 / §13 查看与编辑改为弹窗交互：新增 A-user-view-modal.md 与 A-user-edit-modal.md，操作入口改为列表页「查看」/「编辑」 |
| v1.3 | 2026-08-05 | PM | 移除重置密码功能：小程序用户无密码，无需管理员重置密码 |
| v1.4 | 2026-08-08 | PM | 明确管理员不可修改用户手机号；移除用户角色权限配置；新增管理员手动新建用户功能；同步更新 tech-design.md 与 test-plan.md |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
