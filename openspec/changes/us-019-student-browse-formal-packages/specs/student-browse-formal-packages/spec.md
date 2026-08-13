# Spec Delta: student-browse-formal-packages

> 本 spec 为 US-019 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 正价套餐浏览

系统 MUST 提供两个入口供学员/游客浏览正价套餐选项：

- **入口 A（全局套餐列表）**：`POST /api/packages/list` 返回所有已上架套餐，按 `package_mode` 区分体验课、标准正价课、自定义套餐。
- **入口 B（教练详情页）**：`POST /api/coach/packages/list` 返回指定教练支持的套餐选项，仅当 `coach.status = 1`（已通过且可约）时返回数据，否则返回 HTTP 404 与错误码 `COACH_NOT_FOUND`。

系统 MUST 通过 `POST /api/packages/detail` 返回单个套餐模板详情，并根据是否传入 `coach_id` 返回适配教练列表（全局入口）或当前教练信息（教练详情页入口）。模板未上架时返回 HTTP 404 与错误码 `PACKAGE_NOT_FOUND`。

#### Scenario: 全局套餐列表

```gherkin
Given 管理员已配置多个套餐模板：
  | 套餐名称 | package_mode | status |
  | 体验课 | experience | active |
  | 标准 6 节 | standard | active |
  | 自定义 | custom | active |
When  用户请求 POST /api/packages/list
Then  系统返回 HTTP 200
And   响应包含所有 status='active' 的套餐
And   套餐按 package_mode 分组/标注为：体验课、标准正价课、自定义套餐
```

#### Scenario: 教练详情页套餐列表

```gherkin
Given 教练 A 状态为已通过且可约（status=1）
And   管理员已配置 4 个状态为 active 的标准套餐模板（1/6/8/10 节），均适配教练 A
And   教练 A 已设置参考单价 = 200 元/节
When  用户请求 POST /api/coach/packages/list，JSON body 传入 coach_id = 教练 A
Then  系统返回 HTTP 200
And   响应包含 4 个标准套餐，每个套餐含 hours / price / validity_days
And   custom_package_enabled = true
And   custom_hours_min = 1，custom_hours_max = 50
And   reference_price = 20000（分）
```

#### Scenario: 教练未设置参考单价

```gherkin
Given 教练 B 状态为已通过且可约（status=1）
And   管理员已配置状态为 active 的标准套餐模板
And   教练 B 未设置参考单价（reference_price 为空）
When  用户请求 POST /api/coach/packages/list，JSON body 传入 coach_id = 教练 B
Then  系统返回 HTTP 200
And   standard_packages 列表正常返回
And   custom_package_enabled = false
And   前端据此将自定义课时入口置灰
```

#### Scenario: 教练状态不可见

```gherkin
Given 教练 C 状态为待审核（status=0）
When  用户请求 POST /api/coach/packages/list，JSON body 传入 coach_id = 教练 C
Then  系统返回 HTTP 404
And   错误码为 COACH_NOT_FOUND
And   响应不含任何套餐数据
```

#### Scenario: 全局套餐详情页适配教练列表

```gherkin
Given 系统已配置 10 节标准套餐，status='active'
And   教练 A、教练 B 均适配该套餐且状态为已通过
When  用户请求 POST /api/packages/detail，JSON body 传入 package_id = 该套餐
Then  系统返回 HTTP 200
And   响应包含套餐详情
And   响应包含适配教练列表：教练 A、教练 B
And   每个教练卡片含头像、姓名、评分、擅长泳姿、教龄、总学员数
```

#### Scenario: 教练详情页套餐详情

```gherkin
Given 系统已配置 10 节标准套餐，status='active'
And   教练 A 适配该套餐且状态为已通过
When  用户请求 POST /api/packages/detail，JSON body 传入 package_id = 该套餐，coach_id = 教练 A
Then  系统返回 HTTP 200
And   响应包含套餐详情
And   响应包含当前教练 A 卡片信息
```
