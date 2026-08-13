# Spec Delta: student-buy-formal-package

> 本 spec 为 US-020 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-001 正价套餐订单创建

系统 MUST 允许已登录用户提交正价套餐订单。标准套餐流程为：套餐详情页 → 选择/确认教练 → 确认订单页 → 支付页（US-025）。自定义套餐流程为：套餐详情页 → 选择/确认教练 → 自定义配置页 → 支付页（US-025）。系统 MUST 校验用户已同意最新版协议、教练状态为已通过、用户未持有其他教练的 active 套餐；如用户为未成年人，MUST 校验用户信息中已维护监护人手机号；自定义套餐 MUST 校验课时数、有效期、泳姿在允许范围内。校验通过后 MUST 创建 status=待支付的订单、记录协议签署。

#### Scenario: 从教练详情页购买标准套餐

```gherkin
Given 注册用户已登录且同意最新版协议
And   教练 A 状态为已通过（status=1）
And   用户名下无其他教练的 active 套餐
And   系统已配置 10 节标准套餐
When  用户在教练 A 详情页点击该 10 节标准套餐卡片
Then  进入套餐详情页
And   页面底部展示当前教练 A 横向自适应卡片（头像、姓名、评分、擅长泳姿、教龄、总学员数）
When  用户点击「立即购买」
Then  进入确认订单页
And   确认订单页展示教练 A 卡片（含评分）、套餐信息、价格信息、协议勾选区
When  用户勾选协议并点击「确认订单」
Then  系统创建 order.status = 待支付，course_type = 1
And   系统记录 agreement_sign 版本号与签署时间
And   接口返回 HTTP 201 与订单 ID
And   前端跳转支付页（US-025）
```

#### Scenario: 从全局列表购买标准套餐

```gherkin
Given 注册用户已登录且同意最新版协议
And   系统已配置 10 节标准套餐
And   教练 A、教练 B 均适配该套餐且状态为已通过
And   用户名下无其他教练的 active 套餐
When  用户在全局套餐列表页点击该 10 节标准套餐卡片
Then  进入套餐详情页
And   页面底部展示横向滚动的适配教练竖向卡片：教练 A、教练 B
And   「立即购买」按钮不可用（未选择教练）
When  用户选择教练 A
Then  教练 A 卡片高亮，「立即购买」按钮可用
When  用户点击「立即购买」
Then  进入确认订单页
And   确认订单页顶部展示教练 A 横向自适应卡片（含评分）
When  用户勾选协议并点击「确认订单」
Then  系统创建 order.status = 待支付，course_type = 1
And   订单中 coach_id = 教练 A
And   接口返回 HTTP 201 与订单 ID
And   前端跳转支付页（US-025）
```

#### Scenario: 从教练详情页购买自定义课时

```gherkin
Given 注册用户已登录且同意最新版协议
And   教练 A 状态为已通过（status=1）
And   教练 A 已设置参考单价 300 元/课时
And   系统已开启自定义课时
And   用户名下无其他教练的 active 套餐
When  用户在教练 A 详情页点击自定义课时卡片
Then  进入自定义套餐详情页
And   页面展示参考单价、默认课时选项、有效期选项、可学习泳姿、购买须知
And   页面底部展示当前教练 A 横向自适应卡片（头像、姓名、评分、擅长泳姿、教龄、总学员数、参考单价 300 元/课时）
When  用户点击「立即购买」
Then  进入自定义套餐配置页
And   配置页展示教练 A 横向自适应卡片（含参考单价）
And   课时数量默认选中 8，支持切换 4 / 12 / 20 或自定义输入
And   有效期默认选中 60 天，支持切换 30 / 90 / 180 天
And   学习泳姿支持多选
And   价格明细实时展示 8 × 300 = 2400 元
When  用户将课时数量切换为 12
Then  价格明细实时更新为 12 × 300 = 3600 元
When  用户选择学习泳姿为「自由泳、蛙泳」
And   用户勾选协议并点击「提交订单」
Then  系统校验课时数 12、有效期 60 天、泳姿选项在允许范围内
And   系统按参考单价计算订单金额 3600 元
And   系统创建 order.status = 待支付，course_type = 1，total_hours = 12，valid_days = 60
And   接口返回 HTTP 201 与订单 ID
And   前端跳转支付页（US-025）
```

#### Scenario: 从全局列表购买自定义课时

```gherkin
Given 注册用户已登录且同意最新版协议
And   系统已开启自定义课时
And   教练 A、教练 B 均支持自定义课时且状态为已通过
And   教练 A 参考单价 300 元/课时
And   教练 B 参考单价 280 元/课时
And   用户名下无其他教练的 active 套餐
When  用户在全局套餐列表页点击自定义课时卡片
Then  进入自定义套餐详情页
And   页面底部展示横向滚动的适配教练竖向卡片：教练 A、教练 B
And   每个教练卡片展示头像、姓名、评分、擅长泳姿、教龄、总学员数、参考单价
And   「立即购买」按钮不可用（未选择教练）
When  用户选择教练 A
Then  教练 A 卡片高亮，「立即购买」按钮可用
When  用户点击「立即购买」
Then  进入自定义套餐配置页
And   配置页展示教练 A 横向自适应卡片（含参考单价 300 元/课时）
When  用户选择课时数量 20、有效期 90 天、学习泳姿「蝶泳」
And   用户勾选协议并点击「提交订单」
Then  系统按参考单价计算订单金额 20 × 300 = 6000 元
And   系统创建 order.status = 待支付，course_type = 1，coach_id = 教练 A，total_hours = 20，valid_days = 90
And   接口返回 HTTP 201 与订单 ID
And   前端跳转支付页（US-025）
```

#### Scenario: 自定义套餐配置超出允许范围

```gherkin
Given 注册用户已登录且同意最新版协议
And   教练 A 状态为已通过（status=1）
And   系统已开启自定义课时
And   用户名下无其他教练的 active 套餐
When  用户在自定义套餐配置页选择课时数量 100
And   用户点击「提交订单」
Then  系统返回 HTTP 400，错误码 INVALID_CUSTOM_PACKAGE_CONFIG
And   不创建订单
```

#### Scenario: 未同意协议

```gherkin
Given 注册用户已登录
And   用户未勾选《用户须知》
When  用户提交正价套餐订单
Then  系统返回 HTTP 400，错误码 AGREEMENT_REQUIRED
And   不创建订单
```

#### Scenario: 已持有其他教练 active 套餐

```gherkin
Given 用户已持有教练 B 的 active 套餐
When  用户尝试购买教练 A 的正价套餐
Then  系统返回 HTTP 400，错误码 COACH_CONFLICT
And   文案提示"您已持有其他教练的有效套餐，需先更换教练"
And   不创建订单
```

#### Scenario: 未成年人用户信息中未维护监护人手机号

```gherkin
Given 注册用户已登录且同意最新版协议
And   用户年龄为 16 岁
And   教练 A 状态为已通过（status=1）
And   用户名下无其他教练的 active 套餐
And   用户信息中未维护监护人手机号
When  用户提交正价套餐订单
Then  系统返回 HTTP 400，错误码 GUARDIAN_PHONE_REQUIRED
And   文案提示"请前往个人资料完善监护人手机号"
And   不创建订单
When  用户在 US-005 完善监护人手机号 13800138000 后重新提交
Then  系统创建 order.status = 待支付
And   接口返回 HTTP 201
```

#### Scenario: 教练状态不可用

```gherkin
Given 教练 C 状态为待审核（status=0）
When  用户购买教练 C 的正价套餐
Then  系统返回 HTTP 400，错误码 COACH_UNAVAILABLE
And   不创建订单
```
