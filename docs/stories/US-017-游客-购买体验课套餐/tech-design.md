# Tech Design: US-017 游客购买体验课套餐

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `order` | 写 | 体验课订单 |
| `package` | 写 | 体验套餐 |
| `payment` | 写 | 支付流水 |
| `user` | 改 | identity |
| `coach` | 读 | 校验教练状态 |

#### order

| 字段 | 说明 |
|------|------|
| `order_id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `package_mode` | 'experience'=体验 |
| `status` | 待支付/已支付/已取消/... |
| `amount` | 实付金额 |
| `created_at` | |
| `expire_at` | 24h 支付超时 |

#### package

| 字段 | 说明 |
|------|------|
| `package_id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `package_mode` | 'experience'=体验 |
| `total_hours` | 1 |
| `available_count` | 1 |
| `status` | active |
| `expire_at` | 30 天后 |

### 1.2 索引

```sql
CREATE INDEX idx_package_user_type_status ON package(user_id, package_type, status);
```

## 2. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递。

### 2.1 POST /api/order/trial

- **鉴权**：需登录
- **Request**: `{ coachId }`
- **Response 201**: 订单信息（不含 package，套餐在支付回调成功时创建）
- **Response 400**: `AGREEMENT_REQUIRED` / `TRIAL_PACKAGE_EXISTS` / `COACH_UNAVAILABLE` / `TRIAL_PACKAGE_INACTIVE`

### 2.2 POST /api/order/pay

- **鉴权**：需登录
- **Request**: `{ orderId, channel }`
- **Response 200**: 调起支付参数

### 2.3 POST /api/payment/mock-callback

- **鉴权**：Mock 支付平台签名
- **Request**: `{ orderId, channelTradeNo, amount, success }`
- **Response 200**: 成功

### 2.4 POST /api/agreement/status

- **鉴权**：需登录
- **Request**: `{}`
- **Response 200**: 用户各协议签署状态

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `order` | 无 → 待支付 → 已支付 / 已取消 | 提交订单 / 支付回调成功、超时取消 |
| `package` | 无 → active | 支付回调成功（US-025），非订单创建时预创建 |
| `user` | 注册用户 → 学员 | 支付回调成功（与套餐创建同一事务） |

## 4. 缓存

（本 US 以写为主，缓存少）

## 5. 性能

| 指标 | 目标 |
|------|------|
| 下单接口 P99 | < 300ms |
| 支付回调 P99 | < 200ms |

## 6. 安全

- 支付回调验签
- 同一用户体验套餐唯一性校验
- 教练状态校验

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004 | 依赖 | 登录 |
| US-011 | 依赖 | 教练审核 |
| US-018 | 被依赖 | 体验课预约 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常购买 | `test_trial_purchase_success` |
| 未登录 | `test_trial_purchase_requires_login` |
| 已有体验套餐 | `test_trial_purchase_duplicate_rejected` |
| 教练不可约 | `test_trial_purchase_coach_unavailable` |
| 支付超时 | `test_trial_order_auto_cancel` |

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | 开发 | 初版 |
| v1.1 | 2026-07-31 | 开发 | 同步 user-story v1.1 P0 修复：§2.1 Response 201 由"订单+套餐信息"改为"订单信息（不含 package，套餐在支付回调成功时创建）"；§3 状态机 package 转换补充触发条件"支付回调成功（US-025），非订单创建时预创建"，user 转换补充"与套餐创建同一事务" |
| v1.2 | 2026-08-15 | AI | 三件套一致性修复：§1.1 order/package 表字段由 course_type/package_type 统一为 package_mode='experience'；§2.1 /api/order/trial Response 400 错误码补充 AGREEMENT_REQUIRED、TRIAL_PACKAGE_INACTIVE |

---