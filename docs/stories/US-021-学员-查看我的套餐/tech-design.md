# Tech Design: US-021 学员查看我的套餐

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `package` | 读 | 用户套餐 |
| `coach` | 读 | 教练姓名、状态 |
| `user` | 读 | 登录态 |

#### package

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `package_name` | 套餐名称（快照）|
| `package_mode` | 'experience'=体验 / 'standard'=正价（快照）|
| `coach_name` | 教练姓名（快照）|
| `teaching_type` | 教学类型（快照）|
| `duration_minutes` | 每节课时长（快照）|
| `valid_days` | 有效期天数（快照）|
| `paid_amount` | 实付金额（分，快照）|
| `refund_enabled` | 是否可退款（快照）|
| `refund_ratio` | 退款比例（快照）|
| `refund_valid_days` | 退款有效期天数（快照）|
| `total_hours` | 总课时 |
| `consumed_count` | 已消耗 |
| `available_count` | 剩余可用 |
| `reserved_count` | 已预约 |
| `expire_at` | 有效期 |
| `status` | active / exhausted / expired / refunded / frozen |
| `frozen_reason` | 冻结原因 |

### 1.2 索引

```sql
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_package_user_mode ON package(user_id, package_mode);
```

## 2. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 2.1 POST /api/user/package/list

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "status": "active"
  }
  ```
- **Response 200**:
  ```json
  {
    "active": { "items": [...], "summary": { "totalHours", "consumedHours", "availableHours" } },
    "exhausted": { "items": [...] },
    "expired": { "items": [...] },
    "refunded": { "items": [...] },
    "frozen": { "items": [...] }
  }
  ```

### 2.2 POST /api/coach/package/detail

- **鉴权**：教练 JWT，`coach.status = 1`
- **Request**:
  ```json
  {
    "packageId": 1
  }
  ```
- **Response 200**:
  ```json
  {
    "packageId": 1,
    "packageName": "蛙泳基础 10 节",
    "userId": 10001,
    "userName": "张小明",
    "coachName": "王教练",
    "packageMode": "standard",
    "teachingType": "1v1",
    "strokeIds": ["breaststroke"],
    "durationMinutes": 60,
    "validDays": 90,
    "paidAmount": 180000,
    "status": "active",
    "totalHours": 10,
    "availableHours": 8,
    "reservedHours": 2,
    "consumedHours": 2,
    "expireAt": "2026-10-01T00:00:00Z",
    "usageRecords": [
      { "id": 1, "lessonId": 101, "consumedHours": 1, "consumedAt": "2026-08-10T10:00:00Z" }
    ]
  }
  ```
- **Response 403**: `{ code: NOT_ASSOCIATED_STUDENT }`（package 不属于当前教练）
- **Response 404**: `{ code: PACKAGE_NOT_FOUND }`

## 3. 状态机

（只读）

## 4. 缓存

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| 小程序本地 | `my_packages` | 30s | 下拉刷新强制失效 |

## 5. 性能

| 指标 | 目标 |
|------|------|
| 列表 P99 | < 100ms |

## 6. 安全

- 登录鉴权
- 仅返回当前 user_id 的 package
- 不返回敏感 coach 联系方式

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-020 | 依赖 | 套餐购买 |
| US-022 | 被依赖 | 更换教练 |
| US-029 | 被依赖 | 预约 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 正常查看 | `test_my_packages_list_success` |
| 空状态 | `test_my_packages_empty` |
| 冻结套餐 | `test_my_packages_frozen_visible` |
