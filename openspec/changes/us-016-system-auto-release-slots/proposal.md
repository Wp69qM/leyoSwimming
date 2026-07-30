## Why

教练模板设置后，需要系统自动、按时、批量地生成可约时段，否则学员无法看到未来可预约时间。该任务是连接教练排班与学员预约的自动化桥梁。

## What Changes

- 新增定时任务 `release-slots-job`，按 `release_rule` 触发
- 新增 `schedule_slot` 批量生成逻辑
- 新增 `release_log` 记录执行结果与幂等
- 新增 5 分钟兜底补偿任务
- 节假日/闭馆日自动跳过对应日期

## Capabilities

### New Capabilities

- `system-auto-release-slots`: 系统按规则自动生成可约时段

### Modified Capabilities

（无）

## Impact

- **数据表**：新增 `schedule_slot`；新增 `release_log`
- **任务**：新增定时任务 + 补偿任务
- **缓存**：更新 `next_release_time`
- **依赖**：依赖 US-014 / US-015；被 US-003 / US-018 / US-029 依赖
