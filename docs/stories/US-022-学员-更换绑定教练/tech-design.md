# Tech Design: US-022 学员更换绑定教练

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 修改 | 旧套餐 refunded，reserved→0 |
| `order` | 写 | 新教练待支付订单 |
| `refund_record` | 写 | 退款记录 |
| `user` | 修改 | identity 更新 |
| `coach` | 读 | 教练状态 |
| `booking` | 读 | 校验 reserved |

#### refund_record

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `package_id` | FK |
| `user_id` | FK |
| `amount` | 退款金额（分） |
| `reason` | coach_change / platform / personal |
| `status` | pending / completed |

### 1.2 索引

```sql
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_coach_status ON coach(status);
```

## 2. API 设计

### 2.1 GET /api/coaches/available-for-change

- **鉴权**：必须登录
- **Response 200**: `{ items: Coach[] }`（仅 status=1 且非当前教练）

### 2.2 POST /api/users/me/coach/change

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "new_coach_id": "uuid",
    "standard_package_id": "uuid | null",
    "custom_hours": 12,
    "agreement_versions": { ... }
  }
  ```
- **Response 200**:
  ```json
  {
    "refund_records": [{ "package_id", "amount" }],
    "new_order": { "order_id", "amount", "expire_at" }
  }
  ```
- **Response 400**: `{ code: COACH_UNAVAILABLE | SAME_COACH | NO_ACTIVE_PACKAGE | PENDING_BOOKINGS | AGREEMENT_REQUIRED }`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `package` | active → refunded | 提交更换教练 |
| `user` | 学员 → 注册用户 | 旧套餐全部 refunded |
| `order` | 无 → 待支付 | 提交更换教练 |

## 4. 缓存

（无）

## 5. 性能

| 指标 | 目标 |
|------|------|
| 更换接口 P99 | < 500ms |

## 6. 安全

- 登录鉴权
- 只能操作当前登录用户的 package
- 事务内完成退款 + 新订单 + 身份更新

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 订单创建 |
| US-021 | 依赖 | 我的套餐入口 |
| US-030 | 依赖 | 取消/改约正价课程释放 reserved |
| US-023/US-029/US-050 | 被依赖 | 后续预约/过期 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常更换 | `test_coach_change_success` |
| 自定义课时 | `test_coach_change_custom_hours` |
| 教练不可用 | `test_coach_change_unavailable` |
| 同教练 | `test_coach_change_same_coach` |
| 存在预约 | `test_coach_change_pending_bookings` |
