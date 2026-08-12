# Spec Delta: student-view-orders

> 本 spec 为 US-026 新增能力，使用 ADDED delta 标记。

## ADDED Requirements

### Requirement: REQ-026-1 订单列表查询

系统 MUST 提供当前登录学员的订单列表查询接口，按创建时间倒序排列，支持分页与状态筛选。系统 MUST 仅返回当前用户的订单。

#### Scenario: 正常查看订单列表

```gherkin
Given 学员已登录且名下有 3 笔订单
And   订单 1 的 package_mode = "standard"；订单 2 的 package_mode = "experience"；订单 3 的 package_mode = "standard"
When  学员请求「我的订单」列表
Then  系统返回 HTTP 200
And   items 按 created_at 倒序包含 3 笔订单
And   每笔订单包含 order_id、amount、status
And   每笔订单包含 package_mode 字段，"standard" 映射为"正价"，"experience" 映射为"体验课"
```

#### Scenario: 无订单空状态

```gherkin
Given 学员已登录但名下无任何订单
When  学员请求「我的订单」列表
Then  系统返回 HTTP 200
And   items = []
And   前端展示"暂无订单"
```

#### Scenario: 越权访问他人订单

```gherkin
Given 学员 A 已登录
And   存在学员 B 的订单
When  学员 A 请求学员 B 的订单详情
Then  系统返回 HTTP 403，错误码 ORDER_ACCESS_DENIED
```

---

### Requirement: REQ-026-2 订单详情查询

系统 MUST 提供订单详情查询接口，展示订单金额、套餐信息、教练信息、支付状态、支付时间与退款入口。系统 MUST 校验订单归属。

#### Scenario: 查看已支付订单详情

```gherkin
Given 学员已登录且存在 order.status = 已支付的订单
And   该订单快照字段：package_name = "蛙泳基础 10 节", package_mode = "standard", coach_name = "王教练", teaching_type = "1v1", total_hours = 10, duration_minutes = 60, valid_days = 90, original_price = 2000, paid_amount = 1800, refund_enabled = true, refund_ratio = 0.8, refund_valid_days = 30
And   对应 package_template 后续已被管理员修改为其他内容
When  学员请求该订单详情
Then  系统返回 HTTP 200
And   详情包含订单号、金额、支付时间
And   套餐信息区包含购买时快照：package_name、package_mode（显示"正价"）、coach_name、teaching_type、total_hours、duration_minutes、valid_days、original_price、paid_amount、退款规则
And   套餐信息取值不受 package_template 后续变更影响
And   展示「申请退款」入口
```
