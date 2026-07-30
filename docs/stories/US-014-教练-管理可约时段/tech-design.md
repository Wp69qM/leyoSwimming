# US-014 教练管理可约时段 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `schedule_slot` | 新增/修改/删除 | 教练可约时段主表 |
| `booking` | 读取 | 判断时段是否已被预约 |
| `coach_schedule_log` | 新增 | 排班变更审计日志 |
| `venue_closure` | 读取 | 闭馆日期，复制上周排班时跳过 |

### 1.2 字段定义

**schedule_slot 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `slot_id` | BIGINT | PK | 时段 ID |
| `coach_id` | BIGINT | FK → coach | 教练 ID |
| `start_time` | DATETIME | 非空 | 时段开始时间 |
| `end_time` | DATETIME | 非空 | 时段结束时间 |
| `status` | VARCHAR(16) | 默认 'available' | available / booked / closed / hidden |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | 默认 CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**唯一索引**：`UNIQUE KEY uk_coach_start_time (coach_id, start_time)`

**coach_schedule_log 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | 日志 ID |
| `coach_id` | BIGINT | FK → coach | 教练 ID |
| `slot_id` | BIGINT | 可空 | 相关时段 ID |
| `action` | VARCHAR(32) | 非空 | create / update / delete / copy |
| `from_value` | JSON | 可空 | 变更前内容 |
| `to_value` | JSON | 可空 | 变更后内容 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 时间 |

---

## 2. API 设计

### 2.1 接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/coach/schedule-slots` | GET | 获取可约时段列表 |
| `/api/coach/schedule-slots` | POST | 批量添加可约时段 |
| `/api/coach/schedule-slots/{id}` | PUT | 修改可约时段 |
| `/api/coach/schedule-slots/{id}` | DELETE | 删除可约时段 |
| `/api/coach/schedule-slots/copy-last-week` | POST | 复制上周排班 |

### 2.2 详细定义

**POST /api/coach/schedule-slots**

- **请求体**：
  ```json
  {
    "slots": [
      {
        "start_time": "2026-07-31 09:00:00",
        "end_time": "2026-07-31 10:00:00"
      }
    ]
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "created": 1,
      "conflicts": []
    }
  }
  ```
- **错误码**：
  - `SLOT_TIME_CONFLICT` (400201)
  - `PAST_TIME_NOT_ALLOWED` (400202)

**DELETE /api/coach/schedule-slots/{id}**

- **错误码**：
  - `SLOT_HAS_BOOKING` (400203)

---

## 3. 状态机

### 3.1 时段状态机

```
无 ──[教练发布]──→ available ──[学员预约成功]──→ booked
  │                    │
  │                    └──[教练关闭]──→ closed
  │                    │
  │                    └──[管理员隐藏]──→ hidden
```

- 本 US 触发：`无 → available`
- 后续 US-016：系统自动生成未来时段
- 后续 US-018/US-029：学员预约使 `available → booked`

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 教练可约时段缓存 | `coach:slots:{coach_id}:{date}` | 5 分钟 | 学员端展示用 |
| 排班变更后主动失效 | — | — | 发布/修改/删除后立即清除 |

---

## 5. 性能与安全

### 5.1 性能

- 批量添加接口 P99 < 500ms（≤ 50 个时段）
- 学员端可约时段查询 P99 < 200ms

### 5.2 安全

- 仅 `coach.status = 1` 可调用教练端排班接口
- 删除/修改操作校验教练所有权
- 已预约时段禁止删除，避免学员权益受损
- 操作记录审计日志

---

## 6. 跨 US 依赖

- 依赖 US-011 教练通过入驻审核
- 支撑 US-016 系统自动释放下周可约时段
- 支撑 US-018 / US-029 学员预约课程
- 支撑 US-023 学员候补与关注时段
