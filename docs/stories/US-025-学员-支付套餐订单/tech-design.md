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
| `type` | purchase / refund，购买订单为 purchase |
| `user_id` | FK |
| `coach_id` | FK |
| `amount` | 订单金额（分）|
| `status` | 待支付 / 已支付 / 已取消 / 已退款 |
| `created_at` | 创建时间 |
| `paid_at` | 支付时间 |
| `expire_at` | 支付截止时间（24h）|
| `package_name` | 套餐名称（快照）|
| `package_mode` | 'experience'/'standard'（快照）|
| `coach_name` | 教练姓名（快照）|
| `teaching_type` | 教学类型（快照）|
| `stroke_ids` | 泳姿 ID 列表（快照，JSON）|
| `total_hours` | 总课时（快照）|
| `duration_minutes` | 每节课时长（快照）|
| `valid_days` | 有效期天数（快照）|
| `original_price` | 原价（分，快照）|
| `paid_amount` | 实付金额（分，快照）|
| `refund_enabled` | 是否可退款（快照）|
| `refund_ratio` | 退款比例（快照）|
| `refund_valid_days` | 退款有效期天数（快照）|

### payment

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
| `package_name` | 套餐名称（快照）|
| `package_mode` | 'experience'=体验 / 'standard'=正价（快照）|
| `coach_name` | 教练姓名（快照）|
| `teaching_type` | 教学类型（快照）|
| `stroke_ids` | 泳姿 ID 列表（快照，JSON）|
| `total_hours` | 总课时（快照）|
| `duration_minutes` | 每节课时长（快照）|
| `valid_days` | 有效期天数（快照）|
| `original_price` | 原价（分，快照）|
| `paid_amount` | 实付金额（分，快照）|
| `refund_enabled` | 是否可退款（快照）|
| `refund_ratio` | 退款比例（快照）|
| `refund_valid_days` | 退款有效期天数（快照）|
| `reserved_count` | 预占中，初始 0 |
| `consumed_count` | 已消耗，初始 0 |
| `available_count` | 可用，初始 = total_hours |
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

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 2.1 POST /api/order/pay

- **鉴权**：必须登录且为订单所有者
- **Request**:
  ```json
  {
    "orderId": 1,
    "channel": 0
  }
  ```
  - `channel`: 0=微信 Mock / 1=支付宝 Mock
- **Response 200**:
  ```json
  {
    "paymentId": "P-001",
    "channelTradeNo": "MOCK-WX-202608130001",
    "status": "success"
  }
  ```
- **Response 400**: `{ code: ORDER_EXPIRED | ORDER_NOT_PAYABLE | INVALID_CHANNEL }`
- **Response 404**: `{ code: ORDER_NOT_FOUND }`
- **实现说明**：
  - 校验订单归属、状态、有效期
  - 创建 payment 流水（status=待支付）
  - 调用 `MockPaymentProvider.pay(order, channel)` 生成 `channelTradeNo` 并即时返回支付成功
  - 由 MockProvider 异步调用 `POST /api/payment/mock-callback` 完成 order/package 更新；或直接在当前事务后触发回调

### 2.2 POST /api/payment/mock-callback

- **鉴权**：内部接口；开发/测试环境使用，生产环境禁用或替换为真实渠道回调
- **Request**:
  ```json
  {
    "channel": 0,
    "orderId": 1,
    "channelTradeNo": "MOCK-WX-202608130001",
    "amount": 180000,
    "success": true
  }
  ```
- **Response 200**: `{ code: "SUCCESS" }`
- **业务逻辑**：
  - 校验 payment 存在且金额一致
  - 幂等：同一 `channelTradeNo` 仅处理一次
  - 事务内更新 payment.status=成功、order.status=已支付、paidAt=now，并基于 order 快照创建 package.status=active
  - 异步触发用户身份重算
  - 失败时也返回 200，避免 Mock 渠道重试；异常记录日志并进入补偿队列

### 2.3 POST /api/order/detail

- **鉴权**：必须登录且为订单所有者
- **Request**:
  ```json
  {
    "orderId": 1
  }
  ```
- **Response 200**:
  ```json
  {
    "orderId": 1,
    "type": "purchase",
    "status": "paid",
    "amount": 180000,
    "paidAt": "2026-08-13T10:00:00Z",
    "packageId": 1
  }
  ```

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

- Mock 回调接口需配置内部 token / IP 白名单，生产环境应禁用或替换为真实渠道回调
- 订单归属校验：用户只能支付自己的订单
- 金额校验：回调金额必须与 order.amount 一致
- 防重放：channel_trade_no + 幂等键去重
- 支付页 HTTPS，敏感字段加密传输
- 真实支付渠道接入时，需补充对应签名验证与证书管理

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
