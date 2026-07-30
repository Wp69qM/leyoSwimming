## Why

满员时段有人取消时，若依赖人工刷新抢约，体验差且转化率低。系统按候补顺序自动转正并通知，可最大化利用时段资源。

## What Changes

- 新增候补自动转正内部服务 `WaitlistPromotionService`
- 新增 `BookingCancelledEvent` 事件消费，触发候补转正
- 新增关注用户释放提醒通知
- 新增同一用户多候补自动取消规则
- 新增幂等控制，防止同一候补重复转正

## Capabilities

### New Capabilities

- `waitlist-auto-promote`: 学员候补自动转正

### Modified Capabilities

（无）

## Impact

- **数据表**：新增 `booking` 记录，修改 `waitlist` / `package` / `schedule_slot`
- **API**：无新增用户端 API，仅内部事件消费
- **状态机**：新增 waitlist waiting → converted、waitlist waiting → cancelled 转换
- **依赖**：依赖 US-023 / US-030；被 US-029 依赖
