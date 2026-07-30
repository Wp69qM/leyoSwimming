# US-013 教练管理实时状态 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 修改 | realtime_status、status_override_flag |
| `coach_status_log` | 新增 | 状态变更历史 |
| `booking` | 读取 | 判断课程时间 |

### 1.2 字段定义

**coach 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `coach_id` | BIGINT | PK | 教练 ID |
| `realtime_status` | TINYINT | 默认 1 | 1=空闲中, 2=上课中, 3=休息中, 4=已下班, 5=请假中 |
| `status_override_flag` | TINYINT | 默认 0 | 0=自动, 1=手动覆盖 |
| `override_until` | DATETIME | 可空 | 手动覆盖有效期 |

**coach_status_log 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `from_status` | TINYINT | 非空 | 变更前状态 |
| `to_status` | TINYINT | 非空 | 变更后状态 |
| `source` | VARCHAR(16) | 非空 | manual/auto |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 时间 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/coach/realtime-status` | GET | 获取当前状态 |
| `/api/coach/realtime-status` | PUT | 手动更新状态 |
| `/api/admin/coach/realtime-status` | GET | 管理员查看 |

### 2.1 PUT /api/coach/realtime-status

- **请求体**：
  ```json
  {
    "realtime_status": 1
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "realtime_status": 1,
      "status_override_flag": 1
    }
  }
  ```
- **错误码**：`INVALID_STATUS` (400701), `LEAVE_REQUIRED` (400702)

---

## 3. 状态机

```
任意实时状态 ──[手动切换/自动切换]──→ 任意实时状态
```

- 手动设置后 status_override_flag=1
- 请假中状态由请假审批同步，优先级最高

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 教练实时状态 | `coach:realtime:{coach_id}` | 5 分钟 | 学员端展示 |
| 状态广播 | WebSocket channel | 实时 | 状态变更推送 |

---

## 5. 性能与安全

### 5.1 性能

- 状态更新 P99 < 200ms
- 自动切换任务每分钟执行一次

### 5.2 安全

- 仅 status=1 的教练可操作
- 请假中状态不可手动修改
- 记录状态变更日志

---

## 6. 跨 US 依赖

- 依赖 US-011 审核通过
- 与 US-036 教练请假关联
- 影响 US-029 学员端状态展示
