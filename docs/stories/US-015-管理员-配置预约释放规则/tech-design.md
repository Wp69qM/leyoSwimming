# Tech Design: US-015 管理员配置预约释放规则

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `release_rule` | 读/写 | 全局唯一配置行 |
| `admin_operation_log` | 写 | 记录配置变更日志 |

#### release_rule

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK | 固定为 1（全局唯一） |
| `release_weekday` | TINYINT | NOT NULL | 1=周一 ... 7=周日，默认 3 |
| `release_time` | TIME | NOT NULL | 默认 10:00:00 |
| `release_scope` | VARCHAR(16) | NOT NULL | NEXT_WEEK / NEXT_7_DAYS，默认 NEXT_WEEK |
| `waitlist_enabled` | BOOLEAN | NOT NULL | 默认 true |
| `waitlist_ttl_hours` | INT | NOT NULL | 候补有效期，默认 72 |
| `watch_reminder_minutes` | INT | NOT NULL | 关注提醒提前分钟数，默认 60 |
| `holiday_release_enabled` | BOOLEAN | NOT NULL | 默认 false |
| `holiday_release_offset_days` | INT | | 节假日提前/延后天数，可为负 |
| `updated_at` | DATETIME | NOT NULL | |
| `updated_by` | BIGINT | FK → admin | |

### 1.2 索引

（单表单行，无需额外索引）

## 2. API 设计

### 2.1 GET /api/admin/release-rule

- **鉴权**：需 `SCHEDULE_RELEASE_CONFIG` 权限
- **Response 200**:
  ```json
  {
    "release_weekday": 3,
    "release_time": "10:00:00",
    "release_scope": "NEXT_WEEK",
    "waitlist_enabled": true,
    "waitlist_ttl_hours": 72,
    "watch_reminder_minutes": 60,
    "holiday_release_enabled": false,
    "holiday_release_offset_days": 0
  }
  ```

### 2.2 PUT /api/admin/release-rule

- **鉴权**：需 `SCHEDULE_RELEASE_CONFIG` 权限
- **Request Body**:
  ```json
  {
    "release_weekday": 5,
    "release_time": "14:00:00",
    "release_scope": "NEXT_WEEK",
    "waitlist_enabled": true,
    "waitlist_ttl_hours": 72,
    "watch_reminder_minutes": 60,
    "holiday_release_enabled": true,
    "holiday_release_offset_days": -1
  }
  ```
- **Response 200**: 返回更新后的完整配置
- **Response 400**: `INVALID_RELEASE_TIME` / `REMINDER_TOO_LONG`
- **Response 403**: `FORBIDDEN`

## 3. 状态机

（本 US 不涉及业务实体状态机）

## 4. 缓存

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `release_rule:current` | 60s | PUT 成功后立即 DEL |
| 本地 | web-admin 表单 state | — | 页面关闭清空 |

## 5. 性能

| 指标 | 目标 |
|------|------|
| GET 响应 P99 | < 50ms |
| PUT 响应 P99 | < 100ms |

## 6. 安全

- 接口仅限管理员角色
- 所有字段做服务端校验
- 操作日志记录变更前后快照

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-016 | 被依赖 | 释放任务读取本 US 写入的规则 |
| US-003 | 被依赖 | 首页倒计时读取本 US 规则 |
| US-023 | 被依赖 | 候补/关注功能读取 waitlist_enabled 等配置 |

## 8. 测试映射

| 场景 | 测试文件 | 测试方法 |
|------|----------|----------|
| 正常配置默认释放规则 | `backend/tests/admin/release-rule.test.ts` | `test_release_rule_update_success` |
| 配置节假日提前释放 | 同上 | `test_release_rule_holiday_early_release` |
| 释放时间格式非法 | 同上 | `test_release_rule_invalid_time` |
| 关注提醒时间超过释放周期 | 同上 | `test_release_rule_reminder_too_long` |
| 无权限管理员尝试保存 | 同上 | `test_release_rule_forbidden` |

---

## 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-30 | 初版 |
| v1.1 | 2026-07-31 | P1-6 修复：§2.2 PUT 请求示例 `holiday_release_offset_days` 由 -6 改为 -1，与 user-story.md §6.2 文案"提前至本周二 10:00"对齐（周三 -1 = 本周二，-6 = 上周二） |
