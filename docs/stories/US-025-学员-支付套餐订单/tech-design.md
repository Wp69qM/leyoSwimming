# Tech Design: US-025 学员支付套餐订单

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `order` | 读写 | 更新状态为已支付，回填 paid_at |
| `payment` | 写 | 支付流水，记录渠道、金额、状态 |
| `package` | 写 | 支付成功后创建 active 课时包 |
| `user` | 读 | 校验登录态 |
| `agreement_sign` | 读 | 校验协议已签署 |

#### order

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `amount` | 订单金额（分）|
| `status` | 待支付 / 已支付 / 已取消 / 已退款 |
| `created_at` | 创建时间 |
| `paid_at` | 支付时间 |
| `expire_at` | 支付截止时间（24h）|

#### payment

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `order_id` | FK |
| `idempotency_key` | 幂等键（UK）|
| `channel` | 0=微信 / 1=支付宝 |
| `channel_trade_no` | 第三方流水号（UK）|
| `amount` | 支付金额（分）|
| `status` | 0=待支付 / 1=成功 / 2=失败 / 3=已退款 |
| `paid_at` | 支付时间 |

#### package

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `order_id` | FK |
| `package_type` | 1=正式一对一 |
| `total_hours` | 总课时 |
| `reserved_count` | 预占中，初始 0 |
| `consumed_count` | 已消耗，初始 0 |
| `available_count` | 可用，初始 = total_hours |
| `paid_amount` | 实付金额（分）|
| `status` | active |
| `expire_at` | 过期时间 |

### 1.2 索引

```sql
CREATE INDEX idx_order_user_status ON order(user_id, status);
CREATE INDEX idx_payment_order_idempotency ON payment(order_id, idempotency_key);
CREATE UNIQUE INDEX idx_payment_channel_trade ON payment(channel, channel_trade_no);
CREATE INDEX idx_package_order ON package(order_id);
```

## 2. API 设计

### 2.1 POST /api/orders/{order_id}/pay

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "channel": 0,
    "return_url": "https://miniapp.example.com/pay/result"
  }
  ```
- **Response 200**: `{ payment_id, prepay_params }`
- **Response 400**: `{ code: ORDER_EXPIRED | ORDER_NOT_PAYABLE | INVALID_CHANNEL }`
- **Response 404**: `{ code: ORDER_NOT_FOUND }`

### 2.2 POST /api/payments/callback/wechat

- **鉴权**：微信支付签名验证
- **Request**: 微信支付回调 XML/JSON
- **Response 200**: `{ code: "SUCCESS" }`（失败也返回 SUCCESS 避免重试，记录异常）

### 2.3 POST /api/payments/callback/alipay

- **鉴权**：支付宝签名验证
- **Request**: 支付宝回调参数
- **Response 200**: `"success"`

### 2.4 GET /api/orders/{order_id}

- **鉴权**：必须登录且为订单所有者
- **Response 200**: `{ order_id, status, amount, paid_at, package_id }`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `order` | 待支付 → 已支付 | 支付回调成功且幂等通过 |
| `payment` | 待支付 → 成功 | 回调验证通过 |
| `package` | 无 → active | order 已支付事务内创建 |
| `user` | 注册用户 → 学员 | 异步身份重算 |

## 4. 缓存

- 支付回调处理使用 Redis 分布式锁：`payment:lock:{channel_trade_no}`，TTL 30s，防止并发回调
- 订单状态缓存：`order:status:{order_id}`，TTL 60s，支付页轮询时读取
- 缓存失效：order/payment 状态变更时立即删除

## 5. 性能

| 指标 | 目标 |
|------|------|
| 预支付参数获取 P99 | < 200ms |
| 支付回调处理 P99 | < 500ms |
| 订单状态查询 P99 | < 100ms |
| 并发支付回调 | 支持 1000 QPS |

## 6. 安全

- 支付回调必须验证渠道签名（微信/支付宝）
- 订单归属校验：用户只能支付自己的订单
- 金额校验：回调金额必须与 order.amount 一致
- 防重放：channel_trade_no + 幂等键去重
- 支付页 HTTPS，敏感字段加密传输

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录/注册资料 |
| US-020 | 依赖 | 创建待支付订单 |
| US-021 | 被依赖 | 我的套餐展示 package |
| US-026 | 被依赖 | 订单列表展示支付状态 |
| US-027 | 被依赖 | 退款基于已支付订单 |
| US-029 | 被依赖 | 预约使用 active package |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 微信支付成功 | `test_pay_wechat_success` |
| 支付宝支付成功 | `test_pay_alipay_success` |
| 用户取消支付 | `test_pay_user_cancel` |
| 订单超时 | `test_pay_order_expired` |
| 重复回调 | `test_pay_duplicate_callback` |
| 并发支付 | `test_pay_concurrent_request` |
