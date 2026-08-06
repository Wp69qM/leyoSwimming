> 本 spec 为 US-010 新增能力，全部使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 提交与保存入驻资料

系统 MUST 提供 `POST /api/coach/application` 接口，供教练提交入驻资料。系统 MUST 校验必填基础信息（头像、姓名、手机号、性别、年龄、邮箱、微信二维码），其中性别选项为男/女/其他，年龄为 18-80 整数，邮箱为有效格式且长度 ≤128 字符；校验必填实名与履历字段（身份证号、任教年限、总学员数、总课时数、个人简介、参考单价）与必填资质（身份证正面照、身份证反面照、至少一张教练资格证、健康证、个人形象照）。系统 MUST 校验身份证号为 18 位中国大陆身份证号。系统 MUST 校验参考单价在 50-2000 元/节范围内。系统 MUST 校验上传图片为 JPG/PNG 且单张 ≤5MB。系统 MUST 在提交成功后创建 `coach_application` 快照，`status = pending`，记录 `previous_coach_status`（提交前 coach.status：-1/2/3）与 `submitted_at`；同时 `coach.status` 变为 `0`（待审核），`coach.submitted_at` 写入当前时间，并记录 `coach_audit_log`。系统 MUST 提供 `PUT /api/coach/application/draft` 接口保存草稿，保存后创建或更新 `coach_application` 快照，`status = draft`，`coach.status` 保持不变（仍为 -1/2/3），`submitted_at` 为 NULL。系统 MUST 防止同一教练重复正式提交（已存在 `coach_application.status = pending` 记录时返回 `COACH_APPLICATION_PENDING`）。已驳回（`coach.status=2`）或已离职（`coach.status=3`）教练重新提交时，系统 MUST 创建新的 `coach_application` pending 快照，`coach.status` 重置为 `0`；`status=3` 时不隔离、不回滚历史数据。历史驳回原因保留在 `coach_application.rejection_reason` 与 `coach_audit_log`，不在 `coach` 表冗余存储。头像回填规则与 US-051/US-054 一致：微信授权登录默认回填微信头像且可编辑，手机号登录需手动上传。

#### Scenario: 正常提交入驻资料

```gherkin
Given 教练已完成登录，coach.status = -1（未提交入驻资料）
And   头像、姓名、手机号已带入
When  教练选择性别 "男"
And   教练填写年龄 30 岁、邮箱 "coach@example.com"
And   教练上传微信二维码 1 张
And   教练填写身份证号 "110101199001011234"
And   教练上传身份证正面照、反面照、教练资格证 2 张、健康证、个人形象照
And   教练填写任教年限 5 年、总学员数 100、总课时数 500
And   教练选择擅长泳姿 "蛙泳,自由泳"
And   教练填写个人简介 "专业游泳教练，擅长少儿与成人教学"
And   教练设置参考单价 300.00 元/节
And   教练点击「提交审核」
Then  系统创建 coach_application 记录，status = pending
And   coach_application.previous_coach_status = -1
And   coach.status = 0（待审核）
And   coach.submitted_at 不为空
And   coach_certificate_application 写入 6 条证书记录
And   coach_audit_log 新增 1 条 action='submit' 记录
And   系统返回"提交成功，等待审核"
And   前端跳转"入驻提交成功页"
```

#### Scenario: 保存入驻资料草稿

```gherkin
Given 教练正在填写入驻资料，coach.status = -1
When  教练仅填写姓名 "张教练" 与任教年限 5 年
And   教练未填写健康证、个人形象照、个人简介和参考单价
And   教练点击「保存草稿」
Then  系统创建或更新 coach_application 记录，status = draft
And   coach.status 保持为 -1（未提交）
And   coach.submitted_at 为 NULL
And   已填字段被保存到 coach_application 快照，下次进入可继续编辑
And   不新增独立草稿状态
And   US-011 审核列表不出现该草稿
```

#### Scenario: 必填项缺失

```gherkin
Given 教练进入入驻资料页
When  教练未上传头像、未选择性别、未填写年龄、未填写邮箱、未上传微信二维码、未填写身份证号、未上传身份证正面照、未填写任教年限即点击「提交审核」
Then  前端阻止提交
And   缺失字段下方提示"此项为必填"
And   后端未收到提交请求
And   coach.status 保持 -1 不变
```

#### Scenario: 身份证号不合法

```gherkin
Given 教练进入入驻资料页
When  教练填写身份证号 "123456789012345678"
And   教练点击「提交审核」
Then  前端/后端校验拒绝
And   返回 HTTP 400 + 错误码 INVALID_ID_CARD
And   前端提示"请输入 18 位有效身份证号"
```

#### Scenario: 必填资质缺失

```gherkin
Given 教练进入入驻资料页
When  教练未上传健康证和个人形象照即点击「提交审核」
Then  前端/后端校验拒绝
And   返回 HTTP 400 + 错误码 MISSING_REQUIRED_FIELDS
And   前端提示"请上传完整的实名与资质照片"
```

#### Scenario: 重复提交入驻申请

```gherkin
Given 教练已存在 coach_application.status = pending 的入驻申请
When  教练再次点击「提交审核」
Then  返回 HTTP 400 + 错误码 COACH_APPLICATION_PENDING
And   前端提示"您已提交入驻申请，请勿重复提交"
And   coach 记录与 coach_application 记录未发生变更
```

#### Scenario: 已驳回教练重新提交入驻资料

```gherkin
Given 教练当前 coach.status = 2
And   存在一条 status = rejected 的 coach_application，rejection_reason = "证书不清晰"
And   该 coach_application 快照中任教年限为 3 年、参考单价为 300.00 元
When  教练进入 C-入驻资料填写页
Then  页面顶部展示红色驳回原因条"证书不清晰"
And   表单自动回显 coach_application 快照中的任教年限 3 年与参考单价 300.00 元
When  教练重新上传身份证正面照与反面照，并将任教年限从 3 年改为 5 年
And   教练点击「提交审核」
Then  系统创建新的 coach_application 快照，status = pending，previous_coach_status = 2
And   coach.status 重置为 0（待审核）
And   coach.submitted_at 更新为当前时间
And   系统返回"提交成功，等待审核"
And   页面跳转 C-入驻提交成功页
```

#### Scenario: 已离职教练重新入驻提交资料

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

#### Scenario: 证书图片过大

```gherkin
Given 教练正在上传健康证图片
When  教练选择一张 8MB 的 PNG 图片
Then  前端/后端校验拒绝
And   返回 HTTP 400 + 错误码 IMAGE_TOO_LARGE
And   前端提示"请上传小于 5MB 的 JPG/PNG 图片"
```

#### Scenario: 证书图片格式错误

```gherkin
Given 教练正在上传身份证正面照
When  教练选择一张 HEIC 图片
Then  前端/后端校验拒绝
And   返回 HTTP 400 + 错误码 INVALID_IMAGE_FORMAT
And   前端提示"仅支持 JPG/PNG 格式"
```

### Requirement: REQ-002 参考单价范围校验

系统 MUST 校验教练提交的参考单价在 50-2000 元/节之间。系统 MUST 对超出范围的参考单价返回 `INVALID_REFERENCE_PRICE`。

#### Scenario: 参考单价超出范围

```gherkin
Given 教练进入入驻资料页
When  教练设置参考单价为 5000 元
And   教练点击「提交审核」
Then  返回 HTTP 400 + 错误码 INVALID_REFERENCE_PRICE
And   前端提示"参考单价需在 50-2000 元之间"
And   未创建或修改 coach_application 为 pending 状态
```

### Requirement: REQ-003 等待审核页查看已提交资料

系统 MUST 提供 `GET /api/coach/application` 接口，供已登录教练查询当前最新 `coach_application` 快照资料。系统 MUST 返回完整字段（含证书列表），身份证号脱敏展示。系统 MUST 在响应中返回 `entry_type`（枚举：`draft`/`first`/`rejected`/`reapply`）与 `prompt_message`（coach.status=2 时返回最新 rejected `coach_application` 的 `rejection_reason`；coach.status=3 时返回重新入驻说明文案；其他状态返回 `null`）。系统 MUST 在 coach 记录不存在时返回 404 `NO_APPLICATION`。

#### Scenario: 等待审核页查看已提交资料

```gherkin
Given 教练已成功提交入驻资料，coach.status = 0
And   存在 status = pending 的 coach_application
When  教练进入 C-等待审核页
Then  页面展示"审核中，请耐心等待"状态
And   展示已提交资料摘要卡片（头像、姓名、手机号、参考单价、提交时间、审核中标签），数据来自 coach_application 快照
When  教练点击「查看完整入驻资料」
Then  弹出详情浮层，只读展示 coach_application 快照中的全部字段与证书图片列表
```

#### Scenario: 草稿状态查询资料

```gherkin
Given 教练已登录且 coach.status = -1（未提交）
And   存在 status = draft 的 coach_application
When  教练端调用 GET /api/coach/application
Then  返回 coach_application 已保存的草稿字段
And   返回 entry_type = "draft"
And   返回 prompt_message = null
And   返回 HTTP 200
```

#### Scenario: 已离职教练进入填写页查询资料

```gherkin
Given 教练已存在 coach.status = 3（已离职）
And   不存在 pending 或 rejected 的 coach_application
When  教练端调用 GET /api/coach/application
Then  返回 coach 表历史生效资料
And   返回 certificates 列表（来自 coach_certificate 生效证书）
And   返回 entry_type = "reapply"
And   返回 prompt_message = "你的账号已离职，请重新提交入驻资料，审核通过后即可恢复接单。"
And   返回 HTTP 200
```

#### Scenario: 无入驻资料记录

```gherkin
Given 教练已登录但 coach 表无记录
When  教练端调用 GET /api/coach/application
Then  返回 HTTP 404 + 错误码 NO_APPLICATION
```

### Requirement: REQ-004 通用图片上传

系统 MUST 提供 `POST /api/upload/image` 接口，供教练端上传头像、证书、微信二维码等图片。系统 MUST 校验文件大小 ≤5MB，格式为 JPG/PNG。系统 MUST 对不合规图片返回 `IMAGE_TOO_LARGE` 或 `INVALID_IMAGE_FORMAT`。

#### Scenario: 上传合规图片

```gherkin
Given 教练已登录
When  教练上传一张 2MB 的 JPG 图片
Then  系统返回 HTTP 200 与图片 URL
And   图片可被后续提交接口引用
```

#### Scenario: 上传超大图片

```gherkin
Given 教练已登录
When  教练上传一张 8MB 的 PNG 图片
Then  系统返回 HTTP 400 + 错误码 IMAGE_TOO_LARGE
And   前端提示"请上传小于 5MB 的 JPG/PNG 图片"
```
