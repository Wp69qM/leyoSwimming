# Tech Design: US-020 学员购买正价套餐

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `order` | 写 | 待支付正价套餐订单 |
| `agreement_sign` | 写 | 协议签署记录 |
| `user` | 读 | 登录态、身份 |
| `package` | 读 | 校验同用户 active 套餐教练一致性 |
| `coach` | 读 | 教练状态、参考单价 |
| `standard_package` | 读 | 标准套餐配置 |

#### order

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `course_type` | 1=正价套餐 |
| `standard_package_id` | 标准套餐 ID（自定义时为 null） |
| `custom_hours` | 自定义课时数 |
| `amount` | 订单金额（分） |
| `status` | 待支付 / 已支付 / 已取消 / 已退款 |
| `expire_at` | 订单支付截止时间（24h） |

#### agreement_sign

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `agreement_type` | user_notice / health / disclaimer |
| `version` | 签署版本号 |
| `signed_at` | 签署时间 |

### 1.2 索引

```sql
CREATE INDEX idx_order_user_coach_status ON order(user_id, coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_agreement_sign_user ON agreement_sign(user_id, agreement_type, version);
```

## 2. API 设计

### 2.1 POST /api/orders/formal

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "coach_id": "uuid",
    "standard_package_id": "uuid | null",
    "custom_hours": 12,
    "agreement_versions": {
      "user_notice": "v3",
      "health": "v1",
      "disclaimer": "v1"
    }
  }
  ```
- **Response 201**: `{ order_id, amount, expire_at }`
- **Response 400**: `{ code: AGREEMENT_REQUIRED | COACH_CONFLICT | COACH_UNAVAILABLE | INVALID_HOURS }`

### 2.2 GET /api/agreements/status

- **鉴权**：必须登录
- **Response 200**: `{ user_notice: { required_version, signed_version }, health, disclaimer }`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `order` | 无 → 待支付 | 提交订单 |
| `user` | （本 US 不触发，支付成功后 US-025 触发） | — |

## 4. 缓存

（以写为主，无特殊缓存）

## 5. 性能

| 指标 | 目标 |
|------|------|
| 下单 P99 | < 300ms |

## 6. 安全

- 登录鉴权
- 回调签名验证（US-025）
- 同教练 active 套餐唯一性校验（防止越权购买）
- 协议版本防篡改（后端以 DB 当前版本为准）

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录/注册资料 |
| US-011 | 依赖 | 教练审核 |
| US-019 | 依赖 | 套餐浏览 |
| US-045 | 依赖 | 标准/自定义套餐配置 |
| US-021 | 被依赖 | 我的套餐 |
| US-025 | 被依赖 | 订单支付 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 标准套餐下单 | `test_formal_order_standard_package` |
| 自定义课时下单 | `test_formal_order_custom_hours` |
| 未同意协议 | `test_formal_order_agreement_required` |
| 教练冲突 | `test_formal_order_coach_conflict` |
| 教练不可用 | `test_formal_order_coach_unavailable` |
| 幂等 | `test_formal_order_idempotent` |
