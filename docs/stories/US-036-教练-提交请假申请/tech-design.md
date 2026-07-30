# 技术设计：US-036 教练提交请假申请

## Overview

本 US 为 leyoSwimming MVP 阶段功能，严格对应 PRD 章节实现。教练在教练端提交请假申请，管理员后续在 US-044 中审批。本 US 仅负责「提交」环节，不涉及审批后的预约取消逻辑。

## 1. 数据模型

### 1.1 新增/修改表

| 表名 | 操作 | 字段说明 |
|------|------|----------|
| `coach_leave` | 新增 | `leave_id` PK, `coach_id` FK, `start_time`, `end_time`, `reason`, `status` TINYINT, `created_at`, `updated_at` |
| `audit_log` | 新增记录 | 记录教练提交请假操作 |

### 1.2 字段定义

| 字段 | 类型 | 说明 |
|------|------|------|
| `leave_id` | BIGINT PK | 雪花 ID |
| `coach_id` | BIGINT FK | 关联教练 |
| `start_time` | DATETIME | 请假开始时间 |
| `end_time` | DATETIME | 请假结束时间 |
| `reason` | VARCHAR(500) | 请假原因，可选 |
| `status` | TINYINT | 0=待审批 / 1=已通过 / 2=已驳回 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

### 1.3 索引

```sql
CREATE INDEX idx_coach_leave_coach_time ON coach_leave(coach_id, start_time, end_time);
CREATE INDEX idx_coach_leave_status ON coach_leave(status);
CREATE INDEX idx_coach_leave_coach_status ON coach_leave(coach_id, status);
```

### 1.4 唯一性/约束

- 同一教练在同一时间段内只能存在一条已批准（`status = 1`）的请假记录，应用层校验重叠
- `start_time < end_time`
- `start_time >= NOW()`

## 2. API 设计

### 2.1 POST /api/v1/coach/leaves

- **鉴权**：教练 JWT，且 `coach.status = 1`（已通过）
- **请求参数**：
  ```json
  {
    "start_time": "2026-08-01T09:00:00+08:00",
    "end_time": "2026-08-01T18:00:00+08:00",
    "reason": "病假"
  }
  ```
- **成功响应**：`201 Created`
  ```json
  {
    "leave_id": 10001,
    "status": 0,
    "message": "提交成功，等待管理员审批"
  }
  ```
- **错误码**：
  | 错误码 | HTTP 状态 | 触发条件 |
  |--------|----------|----------|
  | `COACH_STATUS_INVALID` | 403 | 教练状态非已通过 |
  | `LEAVE_START_PAST` | 400 | 开始时间早于当前时间 |
  | `INVALID_TIME_RANGE` | 400 | 结束时间早于或等于开始时间 |
  | `LEAVE_TIME_CONFLICT` | 409 | 与已批准请假时间重叠 |

### 2.2 GET /api/v1/coach/leaves

- **鉴权**：教练 JWT
- **请求参数**：Query: `page`（默认 1）, `size`（默认 10，最大 50）
- **成功响应**：`200 OK`
  ```json
  {
    "items": [
      {
        "leave_id": 10001,
        "start_time": "2026-08-01T09:00:00+08:00",
        "end_time": "2026-08-01T18:00:00+08:00",
        "reason": "病假",
        "status": 0
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  }
  ```

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|----------|
| `coach_leave` | 无 → 0（待审批） | 教练提交请假申请 |

> 0→1、0→2 的转换由 US-044（管理员审批请假）处理，不在本 US 范围内。

## 4. 缓存策略

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|----------|
| Redis | `coach:{coach_id}:leaves:pending` | 60s | 请假状态变更时删除 |
| Redis | `coach:{coach_id}:leaves:list:{page}:{size}` | 60s | 请假记录变更时删除 |

## 5. 性能目标

| 指标 | 目标 |
|------|------|
| 提交接口 P99 | < 300ms |
| 列表接口 P99 | < 200ms |
| 重叠查询 DB | < 50ms |

## 6. 安全

- 所有写接口必须校验登录态与教练角色权限
- 敏感操作记录 `audit_log`
- 输入参数统一做 SQL 注入 / XSS 过滤
- 教练只能查询自己的请假列表，禁止越权
- 接口限流：同一教练 1 分钟 > 60 次提交 → 429

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-011 | 被依赖 | 教练需先通过审核 |
| US-012 | 被依赖 | 教练个人主页数据 |
| US-044 | 依赖本 US | 管理员审批请假并处理受影响预约 |

## 8. 事务边界

- `coach_leave` 插入与 `audit_log` 写入在同一事务
- 重叠检查使用数据库查询 + 应用层校验，避免并发冲突
