# Spec Delta: coach-submit-application

> 本 spec 为 US-010 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 提交入驻资料

系统 MUST 提供 `POST /api/coach/application` 接口，供教练提交入驻资料。系统 MUST 校验必填字段（姓名、任教年限、总学员数、总课时数、个人简介、参考单价、至少一张证书）。系统 MUST 校验参考单价在 50-2000 元/节范围内。系统 MUST 在提交成功后创建 `coach` 记录，`status=0`（待审核），并记录 `coach_audit_log`。系统 MUST 防止同一用户重复提交（已有待审核或已通过记录时返回 `COACH_APPLICATION_PENDING`）。

#### Scenario: 正常提交入驻资料

```gherkin
Given 教练已完成登录且未提交过入驻申请
When  教练填写任教年限 5 年、总学员数 100、总课时数 500、个人简介
And   教练上传证书照片并设置参考单价 300 元
And   教练点击「提交审核」
Then  coach 记录创建成功
And   coach.status = 0（待审核）
And   系统返回"提交成功，等待审核"
```

#### Scenario: 保存入驻资料草稿

```gherkin
Given 教练正在填写入驻资料
When  教练填写部分资料后点击「保存草稿」
Then  coach 记录创建成功
And   coach.status = 0（待审核）
And   coach.submitted_at 为 NULL
And   已填字段被保存，下次进入可继续编辑
And   不新增独立草稿状态
```

#### Scenario: 必填项缺失

```gherkin
Given 教练进入入驻资料页
When  教练未上传证书且未填写任教年限即点击「提交审核」
Then  前端阻止提交
And   缺失字段下方提示"此项为必填"
And   后端未收到提交请求
```

#### Scenario: 重复提交入驻申请

```gherkin
Given 教练已存在 status = 0 的入驻申请
When  教练再次点击「提交审核」
Then  返回 HTTP 400 + 错误码 COACH_APPLICATION_PENDING
And   前端提示"您已提交入驻申请，请勿重复提交"
```

#### Scenario: 已驳回教练重新提交入驻资料

```gherkin
Given 教练已存在 status = 2 的驳回记录
And   驳回原因已查看
When  教练修改证书照片与任教年限后点击「提交审核」
Then  系统更新 coach 记录
And   coach.status 重置为 0（待审核）
And   coach.rejection_reason 清空
And   coach.submitted_at 更新为当前时间
And   系统返回"提交成功，等待审核"
```

#### Scenario: 证书图片过大

```gherkin
Given 教练正在上传证书照片
When  教练选择一张 8MB 的 PNG 图片
Then  前端/后端校验拒绝
And   前端提示"请上传小于 5MB 的 JPG/PNG 图片"
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
