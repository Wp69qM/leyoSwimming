# US-010 教练提交入驻资料

> **状态**：[APPROVAL]（已确认）
> **优先级**：[MVP]
> **估时**：1 人天
> **作者**：PM　|　**最后更新**：2026-08-05
> **配套文档**：Figma：[§13](#13-figma-链接)　·　技术设计：[tech-design.md](./tech-design.md)　·　测试计划：[test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-010 |
| **标题** | 教练提交入驻资料 |
| **角色（Actor）** | 教练（主）、系统（辅）|
| **业务价值（Why）** | 教练提交资质资料，启动入驻审核流程，是教练端可用前提 |
| **优先级** | [MVP] |
| **估时** | 1 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

---

## 2. 触发条件

- **触发方**：教练
- **触发动作**：在教练端首次进入工作台，或点击「申请入驻 / 重新提交入驻资料 / 重新入驻」
- **触发时机**：coach.status = -1（未提交入驻资料）、coach.status = 2（已驳回）或 coach.status = 3（已离职）时主动进入

---

## 3. 前置条件

- [x] 教练已完成微信授权登录（依赖 US-051）或手机号验证码登录（依赖 US-054）
- [x] 教练已同意隐私协议与用户须知（依赖 US-009）
- [x] 系统已配置入驻所需资料字段与校验规则
- [x] 教练状态为 -1（未提交）、2（已驳回）或 3（已离职）时可进入入驻资料填写页；状态为 0 时进入等待审核页
- [x] 已离职/已驳回教练重新提交时，系统中存在其历史 coach 记录与 coach_application 记录

---

## 4. 业务流程

### 4.1 主路径

1. 教练进入教练端，系统根据 `coach.status = -1 / 2 / 3` 自动跳转「C-入驻资料填写页」。
2. 页面顶部按进入状态展示提示区：
   - `status = -1`：无顶部提示；如有 coach_application 草稿则自动带出草稿内容。
   - `status = 2`：顶部展示红色驳回原因条，文案来自该教练最新一条 `status = rejected` 的 `coach_application` 记录的 `rejection_reason`。
   - `status = 3`：顶部展示黄色/橙色重新入驻说明条，文案固定为「你的账号已离职，请重新提交入驻资料，审核通过后即可恢复接单。」（前端硬编码，方案 A）。
3. 页面展示**基础信息**分组，字段如下：
   - 姓名/昵称输入框（必填，1-32 字符）。
   - 手机号（只读）。
   - 性别（必填）：单选，选项为男 / 女。
   - 年龄（必填）：整数，18-80 岁。
   - 邮箱（必填）：有效邮箱格式，长度 ≤128 字符。
   - 微信二维码（必填）：图片 1 张，JPG/PNG，≤5MB，用于学员添加教练微信。
   - `status = -1` 且存在 coach_application 草稿时，自动回显草稿内容；`status = 2 / 3` 时自动回显最新 coach_application（rejected/draft）中的历史提交内容，若不存在则回显 coach 表当前有效内容。
4. 教练在**实名与资质**分组填写/上传：
   - 身份证号（18 位中国大陆身份证，必填）
   - 身份证正面照、反面照（各 1 张，JPG/PNG，≤5MB）
   - 教练资格证（至少 1 张，可多张）
   - 健康证（1 张）
   - 个人形象照（1 张，用于教练列表/详情展示）
5. 教练在**教学履历**分组填写：任教年限（0-60 整数，必填）、总学员数（0-99999 整数，必填）、总课时数（0-99999 整数，必填）、擅长泳姿（蛙泳/自由泳/仰泳/蝶泳，多选，可选）、个人简介（10-500 字符，必填）。
6. 教练在**服务设置**分组填写：参考单价（50-2000 元/节，DECIMAL(10,2)，保留两位小数，必填）。
7. 教练点击底部「保存草稿」或「提交审核」。
8. 系统执行校验：
   - 「保存草稿」：仅校验字段格式（如身份证号合法、图片合规），不校验必填完整性；将当前填写内容写入或更新 `coach_application` 快照，状态为 `draft`，`coach.status` 保持不变（仍为 `-1 / 2 / 3`）。
   - 「提交审核」：校验全部必填项、身份证合法性、图片大小/格式、参考单价范围；校验通过后创建或更新 `coach_application` 快照，状态为 `pending`，记录 `previous_coach_status`（提交前 coach.status：-1/2/3）与 `submitted_at`；随后 `coach.status` 变为 `0`、`coach.submitted_at = 当前时间`，并写入 `coach_audit_log`。
9. 提交成功后，页面跳转「C-入驻提交成功页」，展示文案「提交成功，等待审核」。
10. 成功页 2 秒后自动跳转「C-等待审核页」，教练可查看已提交资料摘要、审核状态与客服/帮助入口。

### 4.2 异常分支

- **分支 1：必填项缺失** → 前端阻止提交，缺失字段下方提示「此项为必填」，后端不接收请求。
- **分支 2：身份证号不合法** → 前端/后端校验拒绝，返回错误码 `INVALID_ID_CARD`，提示「请输入 18 位有效身份证号」。
- **分支 3：必填资质缺失** → 前端阻止提交，提示「请上传身份证正反面、教练资格证、健康证和个人形象照」。
- **分支 4：图片过大或格式错误** → 前端/后端校验拒绝，返回错误码 `IMAGE_TOO_LARGE` 或 `INVALID_IMAGE_FORMAT`，提示「请上传小于 5MB 的 JPG/PNG 图片」。
- **分支 5：参考单价超出范围** → 前端/后端校验拒绝，返回错误码 `INVALID_REFERENCE_PRICE`，提示「参考单价需在 50-2000 元之间」。
- **分支 6：重复提交** → 已存在 `coach.status = 0` 的待审核记录（即存在 pending 状态的 coach_application）时，返回错误码 `COACH_APPLICATION_PENDING`，提示「您已提交入驻申请，请勿重复提交」。
- **分支 7：已驳回教练重新提交** → `coach.status = 2` 时进入填写页，顶部展示红色驳回原因条（文案来自最新 `status = rejected` 的 coach_application 的 `rejection_reason`）；教练修改资料后点击「提交审核」，系统创建新的 coach_application 快照，状态为 `pending`，`coach.status` 重置为 `0`，并跳转成功页。
- **分支 8：已离职教练重新入驻** → `coach.status = 3` 时进入填写页，顶部展示重新入驻说明条，自动回显 coach 表历史内容或最新 rejected/draft coach_application 快照；教练修改资料后点击「提交审核」，系统创建新的 coach_application 快照，状态为 `pending`，`coach.status` 重置为 `0`，并跳转成功页。

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 首次注册需上传证书、任教年限、总学员数、总课时数、个人简介 | [§5.4.1](../../prd/prd.md) |
| 2 | 入驻资质需管理员审核 | [§5.4.1](../../prd/prd.md) |
| 3 | 教练可设置一节课的参考单价 | [§5.4.1](../../prd/prd.md) |
| 4 | 身份证号须为 18 位中国大陆身份证，后端 AES 加密存储，管理员审核详情页可完整展示身份证号 | 字段设计（C-入驻资料填写页）|
| 5 | 身份证正反面、教练资格证、健康证、个人形象照必须上传；单张图片 ≤5MB，格式 JPG/PNG | 字段设计（C-入驻资料填写页）|
| 6 | 参考单价范围 50-2000 元/节，保留两位小数 | 字段设计（C-入驻资料填写页）|
| 7 | 保存草稿仅更新 `coach_application` 快照，`status = draft`，`coach.status` 保持不变；提交审核后创建/更新 `coach_application` 快照为 `pending`，`coach.status = 0` 并写入 `submitted_at`，进入审核队列 | 本 US 设计决策 |
| 8 | 已离职教练（status=3）重新入驻时复用原 coach 记录，创建新的 coach_application 快照，不回滚/不隔离历史数据，提交后 coach.status 重置为 0 | 本 US 设计决策 |
| 10 | 驳回后 coach.status 恢复为提交前的 previous_coach_status；previous_coach_status=3 时恢复为 3，previous_coach_status=-1 时变为 2，previous_coach_status=2 时保持 2 | 本 US 设计决策 |
| 11 | 管理员审核教练入驻资质 | [§5.5.1](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

> L2（1 人天）：2 正常 + 3 异常 = 5 个场景；本 US 扩展覆盖字段级校验、驳回重新提交与等待审核页查看。

### 6.1 场景 1：正常提交入驻资料

```gherkin
Given 教练已完成登录且 coach.status = -1
And   姓名、手机号已带入
When  教练选择性别「男」
And   教练填写年龄 30 岁、邮箱 "coach@example.com"
And   教练上传微信二维码 1 张
And   教练填写身份证号 "110101199001011234"
And   教练上传身份证正面照、反面照、教练资格证 2 张、健康证、个人形象照
And   教练填写任教年限 5 年、总学员数 100、总课时数 500、擅长泳姿「蛙泳/自由泳」、个人简介 50 字
And   教练设置参考单价 300.00 元/节
And   教练点击「提交审核」
Then  系统创建 coach_application 记录，status = pending
And   coach_application.previous_coach_status = -1
And   coach.status = 0（待审核）
And   coach.submitted_at 不为 NULL
And   coach_certificate_application 表写入 6 条证书记录（含 cert_type）
And   coach_audit_log 写入 action = 'submit'、from_status = -1、to_status = 0 的日志
And   系统返回"提交成功，等待审核"
And   页面跳转 C-入驻提交成功页，2 秒后自动进入 C-等待审核页
```

### 6.2 场景 2：保存入驻资料草稿

```gherkin
Given 教练已完成登录且 coach.status = -1
When  教练填写姓名 "李教练"、身份证号 "110101199001011234"、任教年限 3 年
And   教练上传身份证正面照和教练资格证各 1 张
And   教练未填写健康证、个人形象照、个人简介和参考单价
And   教练点击「保存草稿」
Then  系统创建或更新 coach_application 记录，status = draft
And   coach.status 保持为 -1（未提交）
And   coach.submitted_at = NULL
And   已填字段被保存到 coach_application 快照，下次进入可继续编辑
And   不新增独立草稿状态
And   该记录不进入 US-011 审核队列
```

### 6.3 场景 3：必填项缺失

```gherkin
Given 教练进入入驻资料页且 coach.status = -1
When  教练未选择性别、未填写年龄、未填写邮箱、未上传微信二维码、未上传身份证正面照、未上传个人形象照、未填写任教年限、未填写个人简介、未设置参考单价
And   教练点击「提交审核」
Then  前端阻止提交
And   缺失字段下方提示"此项为必填"
And   后端未收到提交请求
And   coach.status 保持 -1 不变
```

### 6.4 场景 4：身份证号不合法

```gherkin
Given 教练已进入入驻资料页
When  教练填写身份证号 "123456789012345678"
And   教练其他必填项均已填写
And   教练点击「提交审核」
Then  前端/后端校验拒绝
And   返回错误码 INVALID_ID_CARD
And   前端提示"请输入 18 位有效身份证号"
And   coach.status 保持 -1 不变
```

### 6.5 场景 5：证书图片过大或格式错误

```gherkin
Given 教练已进入入驻资料页
When  教练选择一张 8MB 的 PNG 健康证图片
Then  前端/后端校验拒绝
And   返回错误码 IMAGE_TOO_LARGE
And   前端提示"请上传小于 5MB 的 JPG/PNG 图片"
When  教练选择一张 HEIC 格式的教练资格证图片
Then  前端/后端校验拒绝
And   返回错误码 INVALID_IMAGE_FORMAT
And   前端提示"请上传小于 5MB 的 JPG/PNG 图片"
```

### 6.6 场景 6：参考单价超出范围

```gherkin
Given 教练已进入入驻资料页
When  教练设置参考单价为 5000.00 元
And   教练其他必填项均已填写
And   教练点击「提交审核」
Then  前端/后端校验拒绝
And   返回错误码 INVALID_REFERENCE_PRICE
And   前端提示"参考单价需在 50-2000 元之间"
And   coach.status 保持 -1 不变
And   未创建或修改 coach_application 为 pending 状态
```

### 6.7 场景 7：重复提交入驻申请

```gherkin
Given 教练已存在 coach.status = 0 的待审核记录（存在 pending 状态的 coach_application）
When  教练再次点击「提交审核」
Then  返回错误码 COACH_APPLICATION_PENDING
And   前端提示"您已提交入驻申请，请勿重复提交"
And   coach 记录与 coach_application 记录未发生变更
```

### 6.8 场景 8：已驳回教练查看驳回原因后修改重新提交

```gherkin
Given 教练当前 coach.status = 2
And   存在一条 status = rejected 的 coach_application，rejection_reason = "身份证照片不清晰，请重新上传"
And   该 coach_application 快照中任教年限为 3 年、参考单价为 300.00 元
When  教练进入 C-入驻资料填写页
Then  页面顶部展示红色驳回原因条"身份证照片不清晰，请重新上传"
And   表单自动回显 coach_application 快照中的任教年限 3 年与参考单价 300.00 元
When  教练重新上传身份证正面照与反面照，并将任教年限从 3 年改为 5 年
And   教练点击「提交审核」
Then  系统创建新的 coach_application 快照，status = pending，previous_coach_status = 2
And   coach.status 重置为 0（待审核）
And   coach.submitted_at 更新为当前时间
And   系统返回"提交成功，等待审核"
And   页面跳转 C-入驻提交成功页
```

### 6.9 场景 9：已离职教练重新入驻并回显历史资料

```gherkin
Given 教练当前 coach.status = 3（已离职）
And   coach 表中姓名为"李教练"、任教年限为 5 年、参考单价为 300.00 元
When  教练从 US-051 登录分流或「我的」页面入口进入 C-入驻资料填写页
Then  页面顶部展示重新入驻说明条"你的账号已离职，请重新提交入驻资料，审核通过后即可恢复接单。"
And   表单自动回显姓名"李教练"、任教年限 5 年、参考单价 300.00 元等历史内容
And   教练可编辑所有字段
When  教练确认资料并点击「提交审核」
Then  系统创建新的 coach_application 快照，status = pending，previous_coach_status = 3
And   coach.status 从 3 重置为 0（待审核）
And   coach.submitted_at 更新为当前时间
And   系统返回"提交成功，等待审核"
And   页面跳转 C-入驻提交成功页
```

### 6.10 场景 10：等待审核页查看已提交资料

```gherkin
Given 教练已成功提交入驻资料，coach.status = 0
And   存在 status = pending 的 coach_application
When  教练进入 C-等待审核页
Then  页面展示"审核中，请耐心等待"状态
And   展示已提交资料摘要卡片（个人形象照、姓名、手机号、参考单价、提交时间、审核中标签），数据来自 coach_application 快照
When  教练点击「查看完整入驻资料」
Then  弹出详情浮层，只读展示 coach_application 快照中的全部字段与证书图片列表
```

### 6.11 场景 11：入驻提交成功页展示

```gherkin
Given 教练点击「提交审核」成功
And   coach.status 由 -1/2/3 变为 0（待审核）
And   系统已创建 status = pending 的 coach_application
When  页面跳转 C-入驻提交成功页
Then  页面展示 120×120 成功插画
And   主标题为"提交成功"（首次提交/驳回后重新提交）或"重新入驻申请已提交"（已离职后重新入驻）
And   副标题为"提交成功，等待审核"
And   展示"2 秒后自动跳转等待审核页"提示
And   展示已提交资料摘要卡（个人形象照、姓名、手机号、参考单价、审核中标签）
And   展示"查看审核进度"主按钮
And   2 秒后自动跳转 C-等待审核页
When  教练点击"查看审核进度"按钮
Then  立即跳转 C-等待审核页
```

### 6.12 场景 12：已提交入驻资料的教练登录后直接跳转等待审核页

```gherkin
Given 教练已完成入驻资料提交，coach.status = 0
And   存在 status = pending 的 coach_application
When  教练通过 US-051/US-054 登录教练端
Then  系统返回 coach_status = 0
And   前端直接跳转 C-等待审核页
And   页面按场景 10 展示审核中状态与资料摘要
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | coach | 新增/修改 | 教练生效资料与生命周期状态：`coach_id` PK、`openid` UK、`union_id` UK、`phone` UK、`name`、`gender` TINYINT（1=男 / 2=女）、`age` INT（18-80）、`email` VARCHAR(128)、`wechat_qr_url`、`id_card_no`（AES 加密）、`teaching_years`、`total_students`、`total_hours`、`teaching_strokes`、`bio`、`reference_price`、`status` TINYINT（默认 -1，-1=未提交，0=待审核，1=已通过，2=已驳回，3=已离职，4=申请离职中）、`submitted_at`、`approved_at`、`created_at`、`updated_at` |
| 2 | coach_application | 新增 | 入驻申请快照：`application_id` PK、`coach_id` FK、`status` ENUM（draft/pending/approved/rejected）、`previous_coach_status` TINYINT（提交前 coach.status：-1/2/3）、与 coach 资料字段同构的快照字段（`name`/`gender`/`age`/`email`/`wechat_qr_url`/`id_card_no`/`teaching_years`/`total_students`/`total_hours`/`teaching_strokes`/`bio`/`reference_price`）、`submitted_at`、`approved_at`、`approved_by`、`rejection_reason`、`created_at`、`updated_at` |
| 3 | coach_certificate_application | 新增 | 申请快照关联证书：`cert_id` PK、`application_id` FK、`cert_type` ENUM（ID_CARD_FRONT, ID_CARD_BACK, COACH_CERT, HEALTH_CERT, PORTRAIT, OTHER）、`image_url`、`sort_order`、`created_at` |
| 4 | coach_audit_log | 新增 | 记录状态变更事件：`log_id`、`coach_id` FK、`application_id` FK、`admin_id`、`action`（submit/approve/reject/draft_save）、`from_status`、`to_status`、`reason`、`created_at` |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | /api/coach/application/submit | POST | 新增/修改 | 提交入驻资料；请求体含 coach 资料同构字段（`name`/`gender`/`age`/`email`/`wechat_qr_url`/`id_card_no`/`teaching_years`/`total_students`/`total_hours`/`teaching_strokes`/`bio`/`reference_price`）及 `certificates: [{cert_type, image_url}]`；创建 coach_application 快照为 pending，coach.status 变为 0 |
| 2 | /api/coach/application/save-draft | POST | 新增/修改 | 保存草稿；请求体同提交接口；创建或更新 coach_application 快照为 draft，coach.status 保持不变 |
| 3 | /api/coach/application/detail | POST | 新增 | 返回 coach_application 快照完整资料（含证书列表）；额外返回派生字段 `entry_type`（draft/first/rejected/reapply，由 coach.status 与 coach_application.status 推导）与 `prompt_message`（驳回原因或重新入驻说明，来自 `rejection_reason` 或固定文案），用于填写页与等待审核页 |
| 4 | /api/common/file/upload | POST | 新增 | 通用图片上传（JPG/PNG，≤5MB）|

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | coach.status | -1（未提交）→ 0（待审核） | 首次提交审核 | 创建 coach_application pending 快照，previous_coach_status = -1 |
| 2 | coach.status | 2（已驳回）→ 0（待审核） | 已驳回教练修改后重新提交 | 创建新的 coach_application pending 快照，previous_coach_status = 2 |
| 3 | coach.status | 3（已离职）→ 0（待审核） | 已离职教练重新入驻提交 | 创建新的 coach_application pending 快照，previous_coach_status = 3 |
| 4 | coach_application.status | draft → pending | 教练将草稿转为正式提交 | 复用同一条 application 记录或新建（实现可二选一，需保证幂等） |
| 5 | coach.status | 0（待审核）→ previous_coach_status | 管理员驳回申请 | previous_coach_status = 3 时恢复为 3；previous_coach_status = -1 时变为 2；previous_coach_status = 2 时保持 2 |
| 6 | coach.status | 0（待审核）→ 1（已通过） | 管理员通过申请 | 将 coach_application 快照字段覆盖 coach 表生效资料 |

---

## 8. 边界场景

### 8.1 边界场景 1：已离职教练重新入驻

- **触发条件**：coach.status = 3（已离职）的教练通过 US-051 登录分流或「我的」页面「重新入驻」入口进入
- **预期行为**：允许进入同一 C-入驻资料填写页，顶部展示重新入驻说明条，自动回显 coach 表历史内容或最新 rejected/draft coach_application 快照；提交后创建新的 coach_application pending 快照，coach.status 从 3 重置为 0
- **用户可见反馈**：顶部提示"你的账号已离职，请重新提交入驻资料，审核通过后即可恢复接单。"

### 8.2 边界场景 2：网络中断导致图片上传失败

- **触发条件**：提交过程中网络中断或上传服务异常
- **预期行为**：前端保留已填字段，提示上传失败，不创建不完整记录
- **用户可见反馈**："网络异常，请检查网络后重试"

### 8.3 边界场景 3：并发重复提交

- **触发条件**：教练快速点击两次「提交审核」
- **预期行为**：幂等处理，只产生一条待审核记录
- **用户可见反馈**：正常提示提交成功，不弹出重复错误

### 8.4 边界场景 4：审核中教练再次进入填写页

- **触发条件**：coach.status = 0 且存在 pending 状态的 coach_application 时，教练通过历史入口进入
- **预期行为**：系统自动重定向至 C-等待审核页，不允许修改资料
- **用户可见反馈**：展示"审核中，请耐心等待"

### 8.5 边界场景 5：已离职教练重新入驻后又被驳回

- **触发条件**：coach.status = 3 → 重新入驻提交创建 coach_application（previous_coach_status=3）→ coach.status = 0 → 管理员审核驳回 → coach.status 恢复为 3
- **预期行为**：coach.status 恢复为 3，coach_application 标记为 rejected；教练再次进入 C-入驻资料填写页，顶部展示新的驳回原因条，历史内容继续回显 coach_application 快照，可再次编辑提交
- **用户可见反馈**：顶部红色驳回原因条 + 表单回显上次提交内容；coach 表生效资料保持离职前状态不变

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-009（用户/教练隐私协议与用户须知授权）
- [x] US-051（教练微信授权登录并进入教练端）

### 9.2 后续 US（依赖本故事）

- [ ] US-011（管理员审核教练入驻资质）
- [ ] US-012（教练管理个人主页与参考单价）
- [ ] US-013（教练管理实时状态）
- [ ] US-014（教练管理可约时段）
- [ ] US-040（教练重新入驻）

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 不依赖其他 US 的实现细节，仅依赖登录态
- [x] **N**egotiable（可协商）- 必填项、分组方式、图片限制可协商
- [x] **V**aluable（有价值）- 教练入驻流程起点，平台教练资源可信基础
- [x] **E**stimable（可估算）- 1 人天明确
- [x] **S**mall（足够小）- 单一入驻资料提交，含 3 个关联页面
- [x] **T**estable（可测试）- 10 个 GWT 场景可验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符（除配套文档占位）
- [x] 业务规则引用明确

### 11.2 业务规则

- [x] 引用 §5.4.1 / §5.5.1 及字段设计规则
- [x] 与教练状态机一致（status -1=未提交，0=待审核）
- [x] 与数据模型一致（AES 加密 id_card_no、cert_type 枚举）

### 11.3 验收标准

- [x] 场景数量 ≥ 5，覆盖正常/异常/边界（当前 10 个场景）
- [x] 业务级 Gherkin，不绑死实现
- [x] 每个 Then 含具体状态码 / DB 字段值 / 文案

### 11.4 配套文档

- [x] Figma 链接/状态明确
- [x] 技术设计文档链接/状态明确
- [x] 测试计划链接/状态明确

---

## 12. 备注

- **图片限制**：单张 ≤ 5MB，格式 JPG/PNG；超出时返回 `IMAGE_TOO_LARGE` / `INVALID_IMAGE_FORMAT`。
- **参考单价范围**：50-2000 元/节，保留两位小数；超出范围返回 `INVALID_REFERENCE_PRICE`。
- **身份证存储**：`id_card_no` 后端 AES 加密，后台审核详情页脱敏展示；前端仅做 18 位格式校验。
- **幂等键**：`coach_apply:{user_id}:{timestamp}`，防止快速重复点击产生多条记录。
- **草稿状态**：MVP 不新增独立草稿状态；保存草稿将数据写入 `coach_application` 快照，`status = draft`，`coach.status` 保持不变；提交审核后创建/更新 `coach_application` 快照为 `pending`，`coach.status = 0`。US-011 审核列表必须过滤 `coach_application.status = pending` 的记录，避免草稿进入审核队列。
- **快照机制**：每次正式提交均生成 `coach_application` 快照，pending 期间 coach 表生效资料保持不变；审核通过后将快照字段覆盖写入 coach 表；审核驳回后 coach.status 恢复为 `previous_coach_status`（3 保持 3，-1 变为 2，2 保持 2），教练可继续编辑并创建新的 application 快照。
- **驳回后重新提交**：status = 2 的教练可修改资料后重新提交，创建新的 coach_application pending 快照，`coach.status` 重置为 0；驳回原因保留在 coach_application.rejection_reason 与 coach_audit_log。
- **已离职重新入驻**：status = 3 的教练进入同一 C-入驻资料填写页，顶部展示前端硬编码的重新入驻说明条，自动回显 coach 表历史内容或最新 rejected/draft coach_application 快照；提交后创建新的 coach_application pending 快照（previous_coach_status=3），`coach.status` 从 3 重置为 0。历史数据不回滚、不隔离。
- **性能要求**：提交接口 P99 < 500ms（含图片处理），状态查询 P99 < 100ms。

---

## 13. Figma 链接

> Figma **设计系统规范**（token / 组件 / 状态徽标 / 4 态模板 / 文案）见 [docs/figma/README.md](../figma/README.md)。
> 页面规格详见：
> - [C-入驻资料填写页](../../figma/page-spec/C-coach-onboarding-page.md)
> - [C-入驻提交成功页](../../figma/page-spec/C-coach-onboarding-success-page.md)
> - [C-等待审核页](../../figma/page-spec/C-coach-pending-page.md)

| # | 内容 | 链接 | 状态 |
|---|------|------|------|
| 1 | C-入驻资料填写页 page-spec | [C-coach-onboarding-page.md](../../figma/page-spec/C-coach-onboarding-page.md) | ✅ |
| 2 | C-入驻提交成功页 page-spec | [C-coach-onboarding-success-page.md](../../figma/page-spec/C-coach-onboarding-success-page.md) | ✅ |
| 3 | C-等待审核页 page-spec | [C-coach-pending-page.md](../../figma/page-spec/C-coach-pending-page.md) | ✅ |

### 13.1 状态截图清单

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **C-入驻资料填写页** | 🔲 | 🔲 | 🔲 | 🔲 | 含表单校验错误；含驳回原因条 / 重新入驻说明条状态 |
| **C-入驻提交成功页** | 🔲 | 🔲 | 🔲 | 🔲 | 结果页主态即成功态 |
| **C-等待审核页** | 🔲 | 🔲 | 🔲 | 🔲 | 含资料详情浮层 |

---

## 14. 页面级设计决策

### 14.1 入驻资料表单分组

- **背景**：入驻字段多且涉及基础信息、实名资质、教学履历、服务设置四类，需要清晰的信息架构。
- **结论**：按「基础信息 / 实名与资质 / 教学履历 / 服务设置」四个卡片分组展示，每组内部纵向排列，组间 12px 间距。
- **影响范围**：C-入驻资料填写页 UI 结构、校验提示定位、页面滚动锚点。

### 14.2 证书上传类型区分

- **背景**：教练需上传多种证书，不同证书数量要求不同（身份证正反面各 1 张、教练资格证至少 1 张可多张、健康证 1 张、形象照 1 张）。
- **结论**：每个上传区通过 `cert_type` 标记类型，UI 上显示对应标签与数量提示；后端校验每种类型最小数量。
- **影响范围**：C-入驻资料填写页上传组件、coach_certificate 表设计、API 请求体结构。

### 14.3 驳回原因展示

- **背景**：已驳回教练需要明确知道修改方向。
- **结论**：status = 2 时，页面顶部固定展示红色驳回原因条，文案来自该教练最新一条 `status = rejected` 的 `coach_application` 记录的 `rejection_reason`；表单自动回显上次提交内容，可编辑，提交按钮文案改为「重新提交」。 coach 表不冗余存储 rejection_reason。
- **影响范围**：C-入驻资料填写页状态提示区、按钮文案、表单默认值。

### 14.4 已离职教练重新入驻与首次/驳回提交共用同一页面

- **背景**：status = 3 的教练需要重新入驻，为避免维护多个资料填写页，希望与首次提交、驳回重新提交共用同一页面。
- **选项**：A. 独立重新入驻资料页；B. 与 US-010 的入驻资料填写页合并
- **结论**：选 B，status = 3 时直接进入 C-入驻资料填写页，顶部展示前端硬编码的重新入驻说明条，表单自动回显 coach 表历史内容，提交后 status 从 3 重置为 0。
- **影响范围**：US-040 流程、US-051 登录分流、C-入驻资料填写页 UI、后端 `POST /api/coach/application/detail` 响应。

### 14.5 参考单价输入方式

- **背景**：教练需设置一节课的参考单价。
- **结论**：数字输入框，单位「元/节」，最小 50、最大 2000，允许两位小数，输入框下方展示范围提示。
- **影响范围**：C-入驻资料填写页表单。

### 14.5 入驻资料是否分步填写

- **背景**：入驻字段较多，考虑分步或单页。
- **选项**：A. 单页长表单；B. 分步骤向导。
- **结论**：选择 A，MVP 先做单页长表单，降低复杂度；通过分组卡片与锚点提升可读性。
- **影响范围**：C-入驻资料填写页 UI。

### 14.6 页面级交互说明

| 交互 | 触发 | 反馈 | 备注 |
|------|------|------|------|
| 上传证书 | 点击上传区 | 本地预览 + 进度 + 成功后替换占位 | 超出限制提示错误 |
| 保存草稿 | 点击按钮 | Toast "草稿已保存" | 不进入审核队列 |
| 提交审核 | 点击按钮 | 加载态 + 成功后跳转成功页 | 需防重复点击 |
| 查看完整入驻资料 | 等待审核页点击 | 底部浮层/弹窗只读展示全部字段 | 浮层内可点击证书放大 |

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-08-05 | PM | 需按字段设计补全身份证、形象照、健康证等字段与 3 个新页面 | 已更新 user-story / tech-design / test-plan / page-spec |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v2.1 | 2026-08-05 | PM | 已离职教练（status=3）重新入驻与 US-010 入驻资料填写页合并；新增 entry_type/prompt_message、重新入驻说明条、历史数据回显；GWT 与边界场景扩展至 10 个 |
| v2.0 | 2026-08-05 | PM | 按字段设计补全 coach/coach_certificate 字段、增加 C-等待审核页、更新 GWT 场景与错误码、同步 page-spec |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a>　·　填写示例见 <a href="../../spec/user-story/EXAMPLE.md">EXAMPLE.md</a></sub>
</p>
