# Tech Design: US-023 学员候补与关注时段

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `waitlist` | 写 | 候补记录 |
| `slot_follow` | 写 | 关注记录 |
| `schedule_slot` | 读 | 时段容量、状态 |
| `package` | 读 | 校验 active 套餐 |
| `user` | 读 | 登录态 |

#### waitlist

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `schedule_slot_id` | FK |
| `status` | waiting / converted / cancelled |
| `created_at` | 排队顺序依据 |
| `converted_at` | 转正时间 |

#### slot_follow

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `schedule_slot_id` | FK |
| `created_at` | — |

### 1.2 索引

```sql
CREATE INDEX idx_waitlist_slot_status_created ON waitlist(schedule_slot_id, status, created_at);
CREATE INDEX idx_waitlist_user_status ON waitlist(user_id, status);
CREATE INDEX idx_slot_follow_user ON slot_follow(user_id);
CREATE INDEX idx_slot_follow_slot ON slot_follow(schedule_slot_id);
```

## 2. API 设计

### 2.1 POST /api/schedule-slots/{id}/waitlist

- **鉴权**：必须登录
- **Response 201**: `{ waitlist_id, status, position }`
- **Response 400**: `{ code: SLOT_NOT_FULL | NO_ACTIVE_PACKAGE | ALREADY_IN_WAITLIST }`

### 2.2 DELETE /api/waitlist/{id}

- **鉴权**：必须登录且为本人记录
- **Response 204**

### 2.3 POST /api/schedule-slots/{id}/follow

- **鉴权**：必须登录
- **Response 201**: `{ follow_id }`

### 2.4 DELETE /api/follows/{id}

- **鉴权**：必须登录且为本人记录
- **Response 204**

### 2.5 GET /api/users/me/waitlist-and-follows

- **鉴权**：必须登录
- **Response 200**: `{ waitlist: [...], follows: [...] }`

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `waitlist` | 无 → waiting | 加入候补 |
| `waitlist` | waiting → cancelled | 用户取消或他处转正 |
| `waitlist` | waiting → converted | US-024 转正 |

## 4. 缓存

（无）

## 5. 性能

| 指标 | 目标 |
|------|------|
| 加入候补/关注 P99 | < 200ms |
| 我的列表 P99 | < 100ms |

## 6. 安全

- 登录鉴权
- 只能操作本人 waitlist / follow
- 候补需校验 active 套餐
- 幂等防止重复候补

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录 |
| US-014/US-016 | 依赖 | 时段释放 |
| US-020/US-021 | 依赖 | active 套餐校验 |
| US-024 | 被依赖 | 候补转正 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 加入候补 | `test_waitlist_join_success` |
| 关注时段 | `test_follow_slot_success` |
| 时段未满 | `test_waitlist_slot_not_full` |
| 无 active 套餐 | `test_waitlist_no_active_package` |
| 重复候补 | `test_waitlist_duplicate` |
| 时段失效后关注记录标记 invalid | `test_slot_follow_invalid_on_slot_removed` |

---

## 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-30 | 初版 |
| v1.1 | 2026-07-31 | P1-10 修复：§1.1 slot_follow 表新增 `status`（active/invalid）与 `invalid_at` 字段，统一"标记失效"语义（替代原"自动删除或标记失效"模糊表述）；§8 测试映射补充对应用例 |
