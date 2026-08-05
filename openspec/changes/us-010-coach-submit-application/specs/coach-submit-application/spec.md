# Spec Delta: coach-submit-application

> 本 spec 为 US-010 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 提交入驻资料

系统 MUST 提供 `POST /api/coach/application` 接口，供教练提交入驻资料。系统 MUST 校验必填字段（姓名、身份证号、任教年限、总学员数、总课时数、个人简介、参考单价）与必填资质（身份证正面照、身份证反面照、至少一张教练资格证、健康证、个人形象照）。系统 MUST 校验身份证号为 18 位中国大陆身份证号。系统 MUST 校验参考单价在 50-2000 元/节范围内。系统 MUST 校验上传图片为 JPG/PNG 且单张 ≤5MB。系统 MUST 在提交成功后创建或更新 `coach` 记录，`status=0`（待审核），`submitted_at` 写入当前时间，并记录 `coach_audit_log`。系统 MUST 防止同一教练重复正式提交（已有 `status=0` 且 `submitted_at IS NOT NULL` 记录时返回 `COACH_APPLICATION_PENDING`）。已驳回（`status=2`）教练重新提交时，系统 MUST 复用原记录，清空 `rejection_reason`，更新 `submitted_at`。

#### Scenario: 正常提交入驻资料

```gherkin
Given 教练已完成 US-051 登录，coach.status = -1（未提交入驻资料）
When  教练填写姓名 "张教练"
And   教练填写身份证号 "110101199001011234"
And   教练上传身份证正面照、反面照、教练资格证 2 张、健康证、个人形象照
And   教练填写任教年限 5 年、总学员数 100、总课时数 500
And   教练选择擅长泳姿 "蛙泳,自由泳"
And   教练填写个人简介 "专业游泳教练，擅长少儿与成人教学"
And   教练设置参考单价 300.00 元/节
And   教练点击「提交审核」
Then  系统更新 coach 记录
And   coach.status = 0（待审核）
And   coach.submitted_at 不为空
And   coach_audit_log 新增 1 条 action='submit' 记录
And   系统返回"提交成功，等待审核"
And   前端跳转"入驻提交成功页"
```

#### Scenario: 保存入驻资料草稿

```gherkin
Given 教练正在填写入驻资料，coach.status = -1
When  教练仅填写姓名 "张教练" 与任教年限 5 年
And   教练点击「保存草稿」
Then  系统更新 coach 记录
And   coach.status = 0（待审核）
And   coach.submitted_at 为 NULL
And   已填字段被保存，下次进入可继续编辑
And   不新增独立草稿状态
And   US-011 审核列表不出现该草稿
```

#### Scenario: 必填项缺失

```gherkin
Given 教练进入入驻资料页
When  教练未填写身份证号、未上传身份证正面照、未填写任教年限即点击「提交审核」
Then  前端阻止提交
And   缺失字段下方提示"此项为必填"
And   后端未收到提交请求
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
Given 教练已存在 status = 0 且 submitted_at 不为空的入驻申请
When  教练再次点击「提交审核」
Then  返回 HTTP 400 + 错误码 COACH_APPLICATION_PENDING
And   前端提示"您已提交入驻申请，请勿重复提交"
```

#### Scenario: 已驳回教练重新提交入驻资料

```gherkin
Given 教练已存在 status = 2 的驳回记录，rejection_reason = "证书不清晰"
And   教练已在等待审核页/重新提交页查看驳回原因
When  教练修改教练资格证与个人简介后点击「提交审核」
Then  系统更新原 coach 记录
And   coach.status 重置为 0（待审核）
And   coach.rejection_reason 清空
And   coach.submitted_at 更新为当前时间
And   系统返回"提交成功，等待审核"
```

#### Scenario: 证书图片过大

```gherkin
Given 教练正在上传健康证图片
When  教练选择一张 8MB 的 PNG 图片
Then  前端/后端校验拒绝
And   前端提示"请上传小于 5MB 的 JPG/PNG 图片"
```

#### Scenario: 证书图片格式错误

```gherkin
Given 教练正在上传身份证正面照
When  教练选择一张 GIF 图片
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
```

### Requirement: REQ-003 等待审核页查看已提交资料

系统 MUST 提供 `GET /api/coach/application` 接口，供已登录教练查询当前入驻资料。系统 MUST 返回完整字段（含证书列表），并对手机号、身份证号进行脱敏展示。系统 MUST 在 coach 记录不存在时返回 404 `NO_APPLICATION`。

#### Scenario: 等待审核页查看已提交资料

```gherkin
Given 教练已提交入驻资料，coach.status = 0，submitted_at 不为空
And   教练在等待审核页点击「查看完整入驻资料」
When  教练端调用 GET /api/coach/application
Then  返回 coach 完整资料
And   返回 certificates 列表（含 cert_type 与 image_url）
And   coach.phone 脱敏展示
And   coach.id_card_no 脱敏展示
And   返回 HTTP 200
```

#### Scenario: 无入驻资料记录

```gherkin
Given 教练已登录但 coach.status = -1（未提交入驻资料）
When  教练端调用 GET /api/coach/application
Then  返回 HTTP 404 + 错误码 NO_APPLICATION
```
