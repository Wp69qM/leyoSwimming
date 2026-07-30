# Tech Design: US-026 学员查看订单列表与详情

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `order` | 读 | 主表 |
| `package` | 读 | 展示套餐课时 |
| `coach` | 读 | 展示教练姓名 |
| `payment` | 读 | 展示支付时间 |
| `refund` | 读 | 展示退款信息 |

#### order

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `amount` | 订单金额（分）|
| `status` | 0-无 / 1-待支付 / 2-已支付 / 3-已取消 / 4-退款审批中 / 5-争议退款处理中 / 6-已退款 / 7-退款被拒 / 8-退款处理中（与 PRD §6.2.2 v11.1 统一） |
| `created_at` | 创建时间 |
| `paid_at` | 支付时间 |
| `refunded_at` | 退款完成时间（渠道成功回调时回填，US-028 写入） |
| `refund_status` | 退款子状态（无 / 处理中 / 成功 / 失败，用于细化 order.status=8 时的渠道状态） |

### 1.2 索引

```sql
CREATE INDEX idx_order_user_created ON order(user_id, created_at DESC);
```

## 2. API 设计

### 2.1 GET /api/orders

- **鉴权**：必须登录
- **Query**: `page`, `size`, `status`
- **Response 200**: `{ items: OrderListItem[], total, page, size }`

### 2.2 GET /api/orders/{order_id}

- **鉴权**：必须登录且为订单所有者
- **Response 200**: `{ order_id, status, amount, package, coach, payment, refund }`
- **Response 403**: `ORDER_ACCESS_DENIED`
- **Response 404**: `ORDER_NOT_FOUND`

## 3. 状态机

本 US 只读，不触发状态转换。

## 4. 缓存

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis 订单列表 | `orders:list:{user_id}:{page}:{size}:{status}` | 60s | order 状态变更时删除 |
| Redis 订单详情 | `order:detail:{user_id}:{order_id}` | 300s | order/payment/refund 变更时删除 |

> **安全约束**：订单详情缓存 Key 必须包含 `user_id`，防止跨用户越权读取缓存。读缓存前先校验 `order.user_id = current_user.id`。

## 5. 性能

| 指标 | 目标 |
|------|------|
| 列表查询 P99 | < 200ms |
| 详情查询 P99 | < 100ms |
| 分页最大 size | 50 |

## 6. 安全

- 严格订单归属校验（DB 查询 + 缓存 Key 双重防护）
- 禁止跨用户查询
- 只返回当前用户订单
- 缓存 Key 含 user_id，防止越权缓存命中

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 订单数据 |
| US-025 | 依赖 | 支付状态与 package |
| US-027 | 被依赖 | 退款入口 |
| US-046 | 被依赖 | 管理端订单查看 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 列表查询 | `test_order_list_pagination` |
| 空状态 | `test_order_list_empty` |
| 详情查询 | `test_order_detail_success` |
| 越权访问 | `test_order_detail_forbidden` |
