# Spec Delta: coach-manage-profile-price

> 本 spec 为 US-012 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 教练获取与更新个人主页

系统 MUST 提供 `POST /api/coach/profile/detail` 接口，供已登录且 `coach.status = 1` 的教练查询个人主页信息。系统 MUST 提供 `POST /api/coach/profile/update` 接口，供已登录且 `coach.status = 1` 的教练更新个人主页字段，包括 `name`、`gender`、`age`、`email`、`wechat_qr_url`、`portrait_url`、`teaching_years`、`teaching_strokes`、`bio` 与证书图片列表。系统 MUST 校验个人简介不含敏感词，证书图片大小不超过 5MB 且格式为 JPG/PNG。系统 MUST 在更新成功后立即失效教练详情缓存与教练列表缓存，并写入 `coach_update_log` 记录变更前后值。系统 MUST 拒绝 `coach.status ≠ 1` 的教练调用，返回 `COACH_STATUS_NOT_ALLOWED`。

#### Scenario: 正常更新个人主页

```gherkin
Given 教练已通过审核且 coach.status = 1
When  教练修改个人简介为"专注儿童游泳教学 10 年"
And   教练上传新的证书照片（≤ 5MB，JPG/PNG）
And   教练点击「保存」
Then  系统返回 HTTP 200
And   coach.bio 更新为"专注儿童游泳教学 10 年"
And   coach_certificate 新增一条记录
And   coach_update_log 新增一条 field_name='bio' 的变更记录
And   学员端教练详情页展示更新后的简介与证书
```

#### Scenario: 个人简介含敏感词

```gherkin
Given 教练已进入个人主页编辑页
When  教练输入包含敏感词的个人简介
And   教练点击「保存」
Then  系统返回 HTTP 400 + 错误码 SENSITIVE_CONTENT
And   前端提示"简介包含敏感内容，请修改"
And   coach.bio 保持不变
And   未产生 coach_update_log 记录
```

#### Scenario: 证书图片过大

```gherkin
Given 教练已进入个人主页编辑页
When  教练上传一张 8MB 的证书图片
And   教练点击「保存」
Then  系统返回 HTTP 400 + 错误码 IMAGE_TOO_LARGE
And   前端提示"请上传小于 5MB 的图片"
And   coach_certificate 未新增记录
```

#### Scenario: 账号状态异常禁止修改

```gherkin
Given 教练编辑资料期间管理员将其 coach.status 修改为 2（驳回/封禁）
When  教练点击「保存」
Then  系统返回 HTTP 403 + 错误码 COACH_STATUS_NOT_ALLOWED
And   前端提示"您的账号状态异常，无法修改资料"
And   coach.bio 保持不变
```

### Requirement: REQ-002 教练更新参考单价

系统 MUST 提供 `POST /api/coach/reference-price/update` 接口，供已登录且 `coach.status = 1` 的教练更新一节课的参考单价。系统 MUST 校验参考单价在 50-2000 元之间（含边界）。系统 MUST 限制每位教练每天最多修改 3 次参考单价，超过次数返回 `PRICE_CHANGE_LIMIT`。系统 MUST 在更新成功后更新 `coach.reference_price`、`price_changed_at` 与 `price_change_count_today`，立即失效教练详情缓存与教练列表缓存。系统 MUST 保证参考单价变更不影响已购套餐价格，仅影响新购套餐的价格基准。

#### Scenario: 正常更新参考单价

```gherkin
Given 教练当前参考单价为 300 元且 coach.status = 1
And   教练今日改价次数为 0
When  教练将参考单价修改为 350 元
And   教练点击「保存」
Then  系统返回 HTTP 200
And   coach.reference_price = 350.00
And   coach.price_change_count_today = 1
And   学员端该教练套餐价格基准同步更新为 350 元
And   已购套餐价格保持不变
```

#### Scenario: 参考单价超出范围

```gherkin
Given 教练已进入参考单价设置页
When  教练输入参考单价 5000 元
And   教练点击「保存」
Then  系统返回 HTTP 400 + 错误码 INVALID_REFERENCE_PRICE
And   前端提示"参考单价需在 50-2000 元之间"
And   coach.reference_price 保持不变
```

#### Scenario: 参考单价修改次数超限

```gherkin
Given 教练今日已成功修改参考单价 3 次
When  教练再次尝试修改参考单价为 400 元
Then  系统返回 HTTP 429 + 错误码 PRICE_CHANGE_LIMIT
And   前端提示"今日参考单价修改次数已达上限"
And   coach.reference_price 保持不变
And   coach.price_change_count_today = 3
```

#### Scenario: 更新后学员端可见

```gherkin
Given 教练已更新个人简介为"专注儿童游泳教学 10 年"且参考单价为 350 元
When  学员打开教练详情页
Then  页面展示更新后的简介"专注儿童游泳教学 10 年"
And   页面展示更新后的参考单价 350 元
And   页面展示"刚刚更新"标识
```
