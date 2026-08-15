# Tech Design: US-027 学员申请退款

> **状态**：初稿　|　**最后更新**：2026-07-31

---

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `order` | 读/写 | 校验订单状态，更新为退款审批中 |
| `package` | 读/写 | 校验套餐状态，更新为 frozen(refund_pending)，释放 reserved_count → 0 |
| `refund_record` | 写 | 新增退款申请记录 |
| `booking` | 写 | 已预约 booking → 已取消（cancel_reason=1 学员取消） |
| `notification` | 写 | 通知管理员与学员 |

### 1.2 refund_record 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | — |
| `order_id` | BIGINT | FK, IDX | 关联订单 |
| `user_id` | BIGINT | FK, IDX | 学员 |
| `refund_amount` | INT | NOT NULL | 退款金额（分） |
| `reason_type` | TINYINT | NOT NULL | 1=教练原因 2=个人原因 3=平台原因 |
| `reason_detail` | VARCHAR(500) | NULL | 退款说明 |
| `status` | TINYINT | 默认 0 | 0=待审批 1=已批准 2=已驳回 3=已退款 |
| `created_at` / `updated_at` | DATETIME | — | 时间戳 |

### 1.3 索引

```sql
CREATE INDEX idx_refund_order ON refund_record(order_id, status);
CREATE INDEX idx_refund_user_status ON refund_record(user_id, status);
```

---

## 2. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 2.1 POST /api/package/refund-check

- **鉴权**：必须登录（套餐所属用户）
- **Request**:
  ```json
  {
    "packageId": 1
  }
  ```
- **Response 200**:
  ```json
  {
    "eligible": true,
    "refundAmount": 144000,
    "calculation": {
      "paidAmount": 180000,
      "totalHours": 10,
      "consumedCount": 2,
      "formula": "180000 × (10-2)/10"
    },
    "reasonCodes": [
      { "code": 1, "label": "教练原因" },
      { "code": 2, "label": "个人原因" },
      { "code": 3, "label": "平台原因" }
    ]
  }
  ```
- **Response 400**: `{ code: PACKAGE_ALREADY_REFUNDED | PACKAGE_FROZEN | PACKAGE_NOT_FOUND }`（PACKAGE_FROZEN 仅当 package.status = frozen 且 frozenReason ≠ coach_resigned）

### 2.2 POST /api/package/refund

- **鉴权**：必须登录（套餐所属用户）
- **Request**:
  ```json
  {
    "packageId": 1,
    "reasonType": 2,
    "reasonDetail": "时间冲突，无法继续学习"
  }
  ```
- **Response 201**: `{ refundOrderId: 10086, status: "refund_pending" }`
- **Response 400**: `{ code: REFUND_IN_PROGRESS | PACKAGE_ALREADY_REFUNDED | PACKAGE_FROZEN | REFUND_NOT_SUPPORTED | REFUND_EXPIRED | PACKAGE_EXHAUSTED_NOT_REFUNDABLE }`（PACKAGE_FROZEN 仅当 package.status = frozen 且 frozenReason ≠ coach_resigned）

### 2.3 业务规则

- 退款金额 = `paid_amount × (total_hours - consumed_count) / total_hours`（§6.4.2）
- package.status 必须 ∈ {active, exhausted, expired}，或 = frozen 且 frozen_reason = coach_resigned；否则拒绝
- 提交后 package.status → frozen（frozen_reason='refund_pending'，PRD §5.5.1.2），立即释放 reserved_count → 0，自动取消已预约课程（booking.status → 已取消，cancel_reason=1 学员取消，PRD §6.3.1），触发 US-024 候补转正
- 教练离职场景：保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算
- 已存在 status=待审批 的 refund_record → 拒绝（REFUND_IN_PROGRESS）

---

## 3. 状态机

```
order: 已支付 ──[学员提交退款]──→ 退款审批中
package: active/exhausted/expired ──[学员提交退款]──→ frozen(refund_pending)，reserved_count → 0
package: frozen(coach_resigned) ──[学员提交退款]──→ frozen(refund_pending)，保留 frozen_reason 历史值为 coach_resigned 用于 100% 退款计算，reserved_count → 0
booking: 已预约 ──[学员提交退款触发]──→ 已取消（cancel_reason=1 学员取消）
refund_record: 无 ──[学员提交]──→ 待审批
```

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 失效策略 |
|------|----|-----|---------|
| 退款资格检查 | `refund:check:{packageId}` | 30s | 套餐状态变更时失效 |

---

## 5. 性能

| 指标 | 目标 |
|------|------|
| 退款申请接口 P99 | < 300ms |
| 退款资格检查 P99 | < 200ms |

---

## 6. 安全

- 登录鉴权 + 套餐归属校验（仅套餐所属用户可申请）
- 幂等键：`{userId}:{packageId}:refund`
- 事务包裹：reserved 释放 + booking 取消 + package.status → frozen(refund_pending) + refund_record 创建 + order 状态更新在同一事务（PRD §3.6 / §6.3.1），任一失败回滚

---

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-025 / US-026 | 依赖 | 已支付订单 |
| US-028 | 被依赖 | 管理员处理退款 |

---

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常申请退款 | `test_submit_refund_success` |
| 套餐已退款 | `test_submit_refund_already_refunded` |
| 重复提交退款 | `test_submit_refund_duplicate` |
| 体验套餐退款金额为 0 | `test_refund_amount_zero_exhausted_trial` |
| 过期套餐有剩余课时可退 | `test_refund_expired_with_remaining` |
