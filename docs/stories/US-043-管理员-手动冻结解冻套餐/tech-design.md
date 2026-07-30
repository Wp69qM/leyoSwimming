# US-043 管理员手动冻结/解冻套餐技术设计

---

## 1. 上下文

本 US 实现管理员对 package 的手动冻结与解冻。冻结会将 active package 转为 frozen 并释放 reserved 课时；解冻则恢复为 active。需要保证状态转换的原子性与幂等性。

---

## 2. 目标 / 非目标

**目标：**
- 管理员可冻结 active package 并选择原因
- 管理员可解冻 frozen package
- 冻结时自动释放 reserved 课时并取消未上课 booking
- 记录审计日志

**非目标：**
- 不实现自动冻结策略（如投诉自动冻结）
- 不实现退款执行（由 US-028 处理）
- 不修改 package 其他字段

---

## 3. 数据模型

### 3.1 修改表

#### `package`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| status | TINYINT | IDX | 0=active 1=frozen 2=exhausted 3=expired 4=refunded |
| frozen_reason | VARCHAR(32) | IDX | NULL 或 `coach_resigned`/`refund_pending`/`admin_frozen`（v3 评审 P0-1 修复：对齐 PRD §5.5.1.2 统一枚举） |
| reserved_count | INT | | |
| available_count | INT | | |
| version | INT | | 乐观锁 |

> **frozen_reason 枚举说明**（v3 评审 P0-1 修复）：原 tech-design 使用 TINYINT 整型 `0=pending_review 1=admin_manual 2=court_order` 与 PRD §3.7 `coach_resigned`、§5.5.1.2 字符串枚举不一致，现统一为 VARCHAR(32) 三值枚举：`coach_resigned`（教练离职，系统自动）/ `refund_pending`（退款处理中，US-027/US-028 触发）/ `admin_frozen`（管理员手动冻结，细分原因记录在 audit_log.remark）。

### 3.2 读取/修改表

- `booking`：冻结时取消未上课课程
- `audit_log`：记录操作

---

## 4. API 设计

### 4.1 `POST /api/admin/v1/packages/{package_id}/freeze`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：`{ "reason_detail": "投诉处理中", "version": 1 }`（frozen_reason 固定写入 `admin_frozen`，细分原因写入 audit_log.remark）
- **响应 200**：`{ "message": "套餐已冻结" }`
- **错误码**：
  - `ADMIN_PERMISSION_DENIED`（403）
  - `PACKAGE_NOT_FOUND`（404）
  - `PACKAGE_NOT_ACTIVE`（409）
  - `PACKAGE_CONCURRENTLY_UPDATED`（409）

### 4.2 `POST /api/admin/v1/packages/{package_id}/unfreeze`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：`{ "version": 1 }`
- **响应 200**：`{ "message": "套餐已解冻" }`
- **错误码**：
  - `ADMIN_PERMISSION_DENIED`（403）
  - `PACKAGE_NOT_FOUND`（404）
  - `PACKAGE_NOT_FROZEN`（409）
  - `PACKAGE_CONCURRENTLY_UPDATED`（409）

---

## 5. 状态机

### 5.1 package 状态

- `active` → `frozen`（管理员冻结）
- `frozen` → `active`（管理员解冻）

### 5.2 booking 状态

- `已预约` / `待上课` → `已取消`（冻结时，cancel_reason = 6，套餐冻结）

---

## 6. 缓存策略

- `package:{package_id}` 详情缓存：冻结/解冻后失效
- `user:packages:{user_id}` 列表缓存：冻结/解冻后失效
- `coach:packages:{coach_id}` 缓存：冻结/解冻后失效

---

## 7. 性能指标

- `POST freeze` P99 < 300ms
- `POST unfreeze` P99 < 200ms

---

## 8. 安全

- 接口校验 `MANAGE_PACKAGE` 权限
- 使用乐观锁防止并发状态变更
- 敏感操作写入 audit_log

---

## 9. 跨 US 依赖

- 依赖 US-020 产生 package
- 依赖 US-021 学员端展示状态
- 输出 frozen package 给 US-027/US-028 退款流程

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 冻结成功 | `test_admin_freeze_package_success` | 集成 |
| 解冻成功 | `test_admin_unfreeze_package_success` | 集成 |
| 无权限 | `test_admin_freeze_permission_denied` | 集成 |
| 非 active 冻结 | `test_freeze_not_active_package` | 集成 |
| 非 frozen 解冻 | `test_unfreeze_not_frozen_package` | 集成 |
| 并发冻结 | `test_concurrent_freeze_package` | 集成 |

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P0-1 修复：frozen_reason 字段类型从 TINYINT 改为 VARCHAR(32)，对齐 PRD §5.5.1.2 统一枚举（coach_resigned/refund_pending/admin_frozen）；§4.1 请求体 reason 改为 reason_detail（frozen_reason 固定 admin_frozen）；§5.2 cancel_reason 改为整型 6 |
