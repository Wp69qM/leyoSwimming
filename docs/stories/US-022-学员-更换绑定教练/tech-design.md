# Tech Design: US-022 学员更换绑定教练

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 修改 | 第一步冻结旧套餐；第二步正式退款；超时回滚恢复原始状态 |
| `order` | 写 | 第二步创建新教练待支付订单 |
| `refund_record` | 写 | 第一步创建 pending_change 记录；第二步更新为 approved；超时取消/删除 |
| `user` | 修改 | 第一步 identity 学员→注册用户；第二步支付成功或超时回滚后重算 identity |
| `coach` | 读 | 校验新教练状态 |
| `booking` | 读 | 第一步校验无 reserved 预约 |

### 1.2 关键字段

**package 表（本 US 相关字段）**

| 字段 | 说明 |
|------|------|
| `status` | active / frozen / refunded / exhausted / expired |
| `frozen_reason` | 第一步置为 `refund_pending`（复用 PRD §5.5.1.2 枚举，禁止新增） |
| `original_status` | 第一步记录变更前状态（active/exhausted/expired），用于超时回滚 |

**refund_record 表（本 US 专用）**

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `package_id` | FK |
| `user_id` | FK |
| `amount` | 退款金额（分） |
| `status` | pending_change / approved / cancelled |
| `original_package_status` | 第一步记录的 package 原始状态 |
| `reason` | coach_change（换教练）/ platform / personal |

> 注：换教练专用 `status = pending_change`，与 US-027 的待审批退款在业务上隔离。

### 1.3 索引

```sql
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_booking_user_status ON booking(user_id, status);
CREATE INDEX idx_coach_status ON coach(status);
CREATE INDEX idx_refund_record_user_status ON refund_record(user_id, status);
```

---

## 2. API 设计

### 2.1 GET /api/coaches/available-for-change

- **鉴权**：必须登录
- **Response 200**: `{ items: Coach[] }`（仅 status=1 且非当前绑定教练）

### 2.2 POST /api/users/me/coach/unsubscribe — 第一步

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "idempotency_key": "{user_id}:{package_id}:unsubscribe"
  }
  ```
- **Response 200**:
  ```json
  {
    "refund_records": [
      {
        "refund_record_id": 1001,
        "package_id": 2001,
        "amount": 56000,
        "original_package_status": "active",
        "valid_before": "2026-08-01T10:00:00Z"
      }
    ]
  }
  ```
- **Response 400**: `{ code: NO_ACTIVE_PACKAGE | PENDING_BOOKINGS }`

### 2.3 POST /api/users/me/coach/purchase — 第二步

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "new_coach_id": "uuid",
    "standard_package_id": "uuid | null",
    "custom_hours": 12,
    "agreement_versions": { ... },
    "idempotency_key": "{user_id}:{new_coach_id}:purchase"
  }
  ```
- **Response 201**:
  ```json
  {
    "refund_records": [{ "refund_record_id", "package_id", "amount" }],
    "new_order": { "order_id", "amount", "expire_at" }
  }
  ```
- **Response 400**: `{ code: COACH_UNAVAILABLE | SAME_COACH | ACTIVE_PACKAGE_EXISTS | COACH_CHANGE_EXPIRED | AGREEMENT_REQUIRED }`

---

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `package` | active → frozen | 第一步冻结旧套餐 |
| `package` | frozen → refunded | 第二步购买校验通过 |
| `package` | frozen → original_status | 超时未发起第二步 |
| `refund_record` | 无 → pending_change | 第一步 |
| `refund_record` | pending_change → approved | 第二步成功 |
| `refund_record` | pending_change → cancelled | 超时回滚 |
| `user` | 学员 → 注册用户 | 第一步完成（旧套餐全部 frozen） |
| `order` | 无 → 待支付 | 第二步购买 |

---

## 4. 缓存

- package 缓存：`package:{package_id}`，状态变更后删除
- 用户身份缓存：`user:{user_id}`，第一步/第二步后删除
- 可换教练列表：`coaches:available-for-change:{user_id}`，TTL 60s

---

## 5. 性能

| 指标 | 目标 |
|------|------|
| 退订接口 P99 | < 500ms |
| 购新接口 P99 | < 500ms |

---

## 6. 安全

- 登录鉴权
- 只能操作当前登录用户的 package
- 事务内完成第一步（冻结 + pending_change 记录 + 身份回退）
- 第二步校验 24 小时有效期与 pending_change 记录

---

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 订单创建逻辑复用 |
| US-021 | 依赖 | 我的套餐入口 |
| US-030 | 依赖 | 取消预约释放 reserved |
| US-023/US-029/US-050 | 被依赖 | 后续预约/过期 |

---

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 第一步正常冻结 | `test_coach_unsubscribe_success` |
| 第二步正常购新 | `test_coach_purchase_success` |
| 存在预约拒绝第一步 | `test_coach_unsubscribe_pending_bookings` |
| 未退订直接购新 | `test_coach_purchase_active_package_exists` |
| 新教练不可用 | `test_coach_purchase_unavailable` |
| 超时回滚 | `test_coach_change_expired_rollback` |

---

## 9. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版（单端点 change） |
| v1.1 | 2026-07-31 | Dev | P0 修复：按 user-story v1.4 拆分为 unsubscribe / purchase 两个端点；状态机增加 pending_change / frozen / 超时回滚 |
