## Why

热门时段容易被约满，候补与关注机制让学员仍有机会获得时段或收到提醒，提升用户体验和约课转化率。

## What Changes

- 新增 `waitlist` 表与候补加入/取消接口
- 新增 `slot_follow` 表与关注/取消关注接口
- 新增「我的候补与关注」查询接口
- 定义 waitlist 状态机：waiting / converted / cancelled

## Capabilities

### New Capabilities

- `student-waitlist-and-follow-slots`: 学员候补与关注时段

### Modified Capabilities

（无）

## Impact

- **数据表**：新增 `waitlist`、`slot_follow`
- **API**：新增 5 个端点
- **状态机**：新增 waitlist 状态机
- **依赖**：依赖 US-004/US-005 / US-014/US-016 / US-020/US-021；被 US-024 依赖
