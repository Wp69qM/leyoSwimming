# Tech Design: US-024 学员候补自动转正

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `waitlist` | 写 | 候补记录状态更新 |
| `booking` | 写 | 自动创建预约记录 |
| `package` | 写 | 可用课时扣减与预占 |
| `schedule_slot` | 读/写 | 校验时段状态、更新已约数量 |
| `slot_follow` | 读 | 查询关注用户 |
| `notification` | 写 | 转正与提醒通知 |
| `user` | 读 | 用户身份校验 |

#### waitlist

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `schedule_slot_id` | FK |
| `status` | waiting / converted / cancelled |
| `created_at` | 排队顺序依据 |
| `converted_at` | 转正时间 |

#### booking

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `schedule_slot_id` | FK |
| `package_id` | FK（扣减课时的来源套餐） |
| `status` | 已预约 |
| `source` | waitlist（标记为自动转正） |
| `created_at` | — |

### 1.2 索引

```sql
CREATE INDEX idx_waitlist_slot_status_created ON waitlist(schedule_slot_id, status, created_at);
CREATE INDEX idx_waitlist_user_status ON waitlist(user_id, status);
CREATE INDEX idx_slot_follow_slot ON slot_follow(schedule_slot_id);
CREATE INDEX idx_booking_slot_status ON booking(schedule_slot_id, status);
```

## 2. API 设计

本 US 为事件驱动，无新增用户端 API。内部由 `booking.cancel` 事件触发。

### 2.1 内部事件

- **事件名**：`BookingCancelledEvent`
- **Payload**：`{ schedule_slot_id, cancelled_booking_id, cancelled_at }`
- **消费方**：`WaitlistPromotionService.promote(slot_id)`

### 2.2 内部服务接口

```ts
interface WaitlistPromotionService {
  // 消费取消事件，尝试为指定 slot 转正候补
  promote(schedule_slot_id: string): Promise<PromotionResult>;
}
```

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `waitlist` | waiting → converted | 自动转正成功 |
| `waitlist` | waiting → cancelled | 该用户其他候补在别处转正 |
| `package` | available -1, reserved +1 | 自动预占课时 |
| `booking` | 无 → 已预约 | 自动创建预约 |
| `schedule_slot` | booked_count +1 | 成功转正占用名额 |

## 4. 缓存

（无）

## 5. 性能

| 指标 | 目标 |
|------|------|
| 候补转正处理 P99 | < 300ms |
| 单 slot 关注提醒发送 P99 | < 100ms |

## 6. 安全

- 仅内部事件消费触发，不暴露外部端点
- 事务控制保证名额不超卖
- 同一 `waitlist.id` 仅允许一次转正（幂等键）
- 转正时仅操作本人 package

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-014/US-016 | 依赖 | 时段释放 |
| US-020/US-021 | 依赖 | active 套餐 |
| US-023 | 依赖 | 候补/关注记录 |
| US-030 | 依赖 | 取消事件触发释放 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 取消后首位候补转正 | `test_waitlist_promote_success` |
| 多人候补仅首位转正 | `test_waitlist_promote_first_only` |
| 首位无可用课时跳过 | `test_waitlist_promote_skip_no_quota` |
| 首位已取消跳过 | `test_waitlist_promote_skip_cancelled` |
| 无候补仅关注提醒 | `test_waitlist_promote_notify_followers` |
