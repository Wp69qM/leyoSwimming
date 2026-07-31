# Spec Delta: student-browse-formal-packages

> 本 spec 为 US-019 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 教练正价套餐浏览

系统 MUST 通过 `GET /api/coaches/{id}/packages` 向学员/游客返回指定教练的正价套餐选项。系统 MUST 仅返回 `coach.status = 1`（已通过且可约）的教练套餐；教练状态 ≠ 1 时 SHALL 返回 HTTP 404 与错误码 `COACH_NOT_FOUND`。系统 MUST 返回状态为 active 的标准套餐列表（`package_template.status = active`）与教练参考单价；当教练未设置参考单价时 SHALL 将 `custom_package_enabled` 置为 `false`。

#### Scenario: 正常浏览正价套餐

```gherkin
Given 教练 A 状态为已通过且可约（status=1）
And   管理员已配置 4 个状态为 active 的标准套餐模板（1/6/8/10 节）
And   教练 A 已设置参考单价 = 200 元/节
When  用户请求 GET /api/coaches/{A}/packages
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
When  用户请求 GET /api/coaches/{B}/packages
Then  系统返回 HTTP 200
And   standard_packages 列表正常返回
And   custom_package_enabled = false
And   前端据此将自定义课时入口置灰
```

#### Scenario: 教练状态不可见

```gherkin
Given 教练 C 状态为待审核（status=0）
When  用户请求 GET /api/coaches/{C}/packages
Then  系统返回 HTTP 404
And   错误码为 COACH_NOT_FOUND
And   响应不含任何套餐数据
```

#### Scenario: 标准套餐配置为空

```gherkin
Given 教练 D 状态为已通过且可约（status=1）
And   教练 D 已设置参考单价
And   管理员尚未配置任何状态为 active 的标准套餐模板
When  用户请求 GET /api/coaches/{D}/packages
Then  系统返回 HTTP 200
And   standard_packages 为空数组
And   custom_package_enabled = true
And   前端展示空状态"暂无标准套餐，可选择自定义课时"
```
