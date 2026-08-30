# US-043 管理员-查看与管理用户套餐技术设计

---

## 1. 上下文

本 US 实现管理员对用户购买后的 `package` 实例进行管理，覆盖：

- 查看套餐实例列表（按状态、模式、到期时间、关键词筛选）
- 查看套餐实例详情（购买时快照、购买时间、到期时间、课时消耗、冻结原因、关联订单、上课记录、操作日志）
- 手动冻结 active package 并选择原因
- 手动解冻 frozen package
- 手动延期 active 或 expired package
- 从套餐管理发起退款，创建退款订单并冻结 package

后续审批/驳回/金额调整在订单管理页（US-046）完成。需要保证状态转换的原子性、幂等性与乐观锁并发控制。

---

## 2. 目标 / 非目标

**目标：**

- 管理员可查询套餐实例列表与详情
- 管理员可冻结 active package 并选择原因，冻结时释放 reserved 课时并取消未上课 booking
- 管理员可解冻 frozen package，清除 frozen_reason
- 管理员可延期 active 或 expired 且剩余课时 > 0 的 package
- 管理员可从套餐管理发起退款，生成退款订单与 refund_record，并将 package 标记为 frozen/refund_pending
- 所有状态变更写入 audit_log

**非目标：**

- 不实现自动冻结策略（如投诉自动冻结）
- 不实现退款审批/驳回/金额调整/原路退回（由 US-046 处理）
- 不修改 package_template（模板配置属于 US-045）
- 不恢复因冻结而取消的 booking

---

## 3. 数据模型

### 3.1 读取/修改表

#### `package`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| id | BIGINT | PK | |
| user_id | BIGINT | IDX | |
| coach_id | BIGINT | IDX | |
| package_mode | VARCHAR(20) | IDX | standard / experience |
| total_hours | INT | | 购买时总课时 |
| consumed_count | INT | | 已消耗课时 |
| reserved_count | INT | | 已预约课时 |
| available_count | INT | | 剩余可用课时 |
| paid_amount | DECIMAL(10,2) | | 实付金额 |
| original_price | DECIMAL(10,2) | | 购买时原价 |
| refund_enabled | TINYINT(1) | | 是否可退款 |
| refund_ratio | DECIMAL(3,2) | | 退款比例 |
| refund_valid_days | INT | | 退款有效天数 |
| status | VARCHAR(32) | IDX | active / frozen / exhausted / expired / refunded |
| frozen_reason | VARCHAR(32) | IDX | NULL / coach_resigned / refund_pending / admin_frozen |
| expire_at | DATETIME | IDX | |
| extend_reason | VARCHAR(200) | | 管理员手动延期原因，最多 200 字 |
| exhausted_at | DATETIME | | |
| refunded_at | DATETIME | | |
| version | INT | | 乐观锁 |

> **frozen_reason 枚举说明**：VARCHAR(32) 三值枚举：`coach_resigned`（教练离职，系统自动）/ `refund_pending`（退款处理中，US-027/US-028/US-043 触发）/ `admin_frozen`（管理员手动冻结，细分原因记录在 audit_log.remark）。

#### `order`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| id | BIGINT | PK | |
| order_no | VARCHAR(32) | UK | 业务单号 |
| type | VARCHAR(20) | IDX | purchase / refund |
| status | VARCHAR(32) | IDX | pending_payment / paid / cancelled / refund_pending / refund_processing / refunded / rejected / dispute_processing |
| user_id | BIGINT | IDX | |
| coach_id | BIGINT | IDX | |
| package_id | BIGINT | IDX | |
| purchase_order_id | BIGINT | IDX | 退款订单指向原购买订单 |
| original_amount | DECIMAL(10,2) | | |
| discount_amount | DECIMAL(10,2) | | |
| paid_amount | DECIMAL(10,2) | | 实付/退款金额 |
| payment_method | VARCHAR(20) | | wechat / alipay |
| channel_trade_no | VARCHAR(64) | | 第三方流水号 |
| reason | VARCHAR(500) | | 退款申请原因 |
| rejected_reason | VARCHAR(500) | | 驳回原因 |
| approved_by | BIGINT | | 处理管理员 ID |
| approved_at | DATETIME | | 处理时间 |
| refunded_at | DATETIME | | 退款成功时间 |

#### `refund_record`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| id | BIGINT | PK | |
| package_id | BIGINT | IDX | |
| order_id | BIGINT | IDX | 关联退款订单 |
| refund_amount | DECIMAL(10,2) | | 退款金额 |
| reason | VARCHAR(500) | | 退款原因 |
| status | TINYINT | | 待审批 / 已通过 / 已驳回等 |

### 3.2 读取/修改表

- `booking`：冻结时取消未上课课程
- `audit_log`：记录操作

---

## 4. API 设计

> 统一使用 POST，URL 按 `/api/admin/package/{action}`，参数通过 JSON body 传递。

### 4.1 `POST /api/admin/package/list`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：
  ```json
  {
    "status": "active",
    "packageMode": "standard",
    "keyword": "套餐编号/用户/教练",
    "expireAtStart": "2026-08-01",
    "expireAtEnd": "2026-09-01",
    "page": 1,
    "pageSize": 20
  }
  ```
- **响应 200**：分页列表

### 4.2 `POST /api/admin/package/detail`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：`{ "packageId": 1 }`
- **响应 200**：套餐详情（含购买时快照、购买时间、到期时间、课时消耗、冻结原因、关联订单、上课记录、操作日志）

### 4.3 `POST /api/admin/package/freeze`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：`{ "packageId": 1, "reasonDetail": "投诉处理中", "version": 1 }`
- **响应 200**：`{ "message": "套餐已冻结" }`
- **错误码**：
  - `ADMIN_PERMISSION_DENIED`（403）
  - `PACKAGE_NOT_FOUND`（404）
  - `PACKAGE_NOT_ACTIVE`（409）
  - `PACKAGE_CONCURRENTLY_UPDATED`（409）

### 4.4 `POST /api/admin/package/unfreeze`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：`{ "packageId": 1, "version": 1 }`
- **响应 200**：`{ "message": "套餐已解冻" }`
- **错误码**：
  - `ADMIN_PERMISSION_DENIED`（403）
  - `PACKAGE_NOT_FOUND`（404）
  - `PACKAGE_NOT_FROZEN`（409）
  - `PACKAGE_CONCURRENTLY_UPDATED`（409）

### 4.5 `POST /api/admin/package/extend`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：`{ "packageId": 1, "newExpireAt": "2026-09-01T23:59:59", "reason": "学员出差一个月", "version": 1 }`
- **响应 200**：`{ "message": "套餐已延期" }`
- **业务规则**：
  - `reason` 必填，长度 1~200 字符
  - `newExpireAt` 必须晚于当前时间
- **错误码**：
  - `ADMIN_PERMISSION_DENIED`（403）
  - `PACKAGE_NOT_FOUND`（404）
  - `INVALID_EXTENSION_REASON`（400）
  - `PACKAGE_NOT_EXTENDABLE`（409）
  - `PACKAGE_CONCURRENTLY_UPDATED`（409）

### 4.6 `POST /api/admin/package/refund`

- **鉴权**：管理员 JWT，`MANAGE_PACKAGE`
- **请求体**：`{ "packageId": 1, "reason": "协商退款", "refundAmount": 1200.00, "adjustReason": "协商一致", "version": 1 }`
  - `refundAmount`：管理员填写的退款金额，可选，默认使用系统计算金额；可大于系统计算金额，但不能小于 0
  - `adjustReason`：金额调整原因，当 `refundAmount` 与系统计算金额不一致时必填
- **响应 200**：`{ "message": "退款订单已生成，请前往订单管理审批", "orderNo": "R202608010001" }`
- **错误码**：
  - `FORBIDDEN`（403）
  - `PACKAGE_NOT_FOUND`（404）
  - `PACKAGE_NOT_REFUNDABLE`（409）
  - `REFUND_AMOUNT_INVALID`（400）
  - `REFUND_PENDING_EXISTS`（409）
  - `PACKAGE_CONCURRENTLY_UPDATED`（409）

---

## 5. 状态机

### 5.1 package 状态

| 转换 | 触发条件 |
|------|---------|
| active → frozen | 管理员冻结 / 管理员发起退款 / 教练离职 / 退款审批中 |
| frozen → active | 管理员解冻 / 退款订单被驳回 |
| active → expired | 系统定时任务（US-050） |
| active → exhausted | 可用课时与预约课时均为 0（US-050） |
| expired → active | 管理员延期 |
| frozen → refunded | 退款审批通过且渠道回调成功 |

### 5.2 booking 状态

| 转换 | 触发条件 |
|------|---------|
| 已预约 / 待上课 → 已取消 | 冻结时，cancel_reason = 6（套餐冻结） |

---

## 6. 缓存策略

- `package:{package_id}` 详情缓存：冻结/解冻/延期/发起退款后失效
- `user:packages:{user_id}` 列表缓存：状态变更后失效
- `coach:packages:{coach_id}` 缓存：状态变更后失效
- 列表页采用分页缓存，筛选条件作为 key 的一部分

---

## 7. 性能指标

- `POST /api/admin/package/list` P99 < 300ms
- `POST /api/admin/package/detail` P99 < 200ms
- `POST /api/admin/package/freeze` P99 < 300ms
- `POST /api/admin/package/unfreeze` P99 < 200ms
- `POST /api/admin/package/extend` P99 < 200ms
- `POST /api/admin/package/refund` P99 < 300ms

---

## 8. 安全

- 接口校验 `MANAGE_PACKAGE` 权限
- 使用乐观锁防止并发状态变更
- 敏感操作写入 audit_log
- 退款金额计算与修改仅允许在订单管理页（US-046）进行

---

## 9. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-020 | 本 US 依赖 | 产生 package 记录 |
| US-021 | 本 US 依赖 | 学员端展示 package 状态 |
| US-045 | 本 US 依赖 | package 快照字段来源 |
| US-053 | 本 US 依赖 | 后台管理系统登录认证入口 |
| US-027 / US-028 | 双向 | 本 US 发起退款生成的订单由 US-028 处理；US-027 学员端退款也会进入同一流程 |
| US-046 | 双向 | 本 US 发起退款生成的订单在 US-046 中审批/驳回 |
| US-050 | 本 US 依赖 | 自动状态流转（active → expired / exhausted） |

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 查看套餐列表 | `test_admin_package_list_success` | 集成 |
| 查看套餐详情 | `test_admin_package_detail_success` | 集成 |
| 冻结成功 | `test_admin_freeze_package_success` | 集成 |
| 解冻成功 | `test_admin_unfreeze_package_success` | 集成 |
| 延期成功 | `test_admin_extend_package_success` | 集成 |
| 发起退款成功 | `test_admin_request_refund_success` | 集成 |
| 无权限 | `test_admin_package_permission_denied` | 集成 |
| 非 active 冻结 | `test_freeze_not_active_package` | 集成 |
| 非 frozen 解冻 | `test_unfreeze_not_frozen_package` | 集成 |
| 不可延期 | `test_extend_not_extendable_package` | 集成 |
| 不可退款 | `test_refund_not_refundable_package` | 集成 |
| 重复发起退款 | `test_refund_pending_exists` | 集成 |
| 并发冻结 | `test_concurrent_freeze_package` | 集成 |
| 冻结时取消未上课 booking | `test_freeze_cancels_future_bookings` | 集成 |
| 退款驳回后套餐恢复 active | `test_reject_refund_restores_package` | 集成 |

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：仅覆盖冻结/解冻 |
| v1.1 | 2026-07-31 | Dev | v3 评审 P0-1 修复：frozen_reason 字段类型从 TINYINT 改为 VARCHAR(32)，对齐 PRD §5.5.1.2 统一枚举 |
| v2.0 | 2026-08-13 | Dev | 扩展为完整套餐管理：新增列表/详情/延期/发起退款 API；更新状态机、缓存、测试映射、跨 US 依赖 |
| v2.1 | 2026-08-13 | Dev | 延期 API 新增 `reason` 字段；`package` 表新增 `extend_reason` 字段；新增错误码 `INVALID_EXTENSION_REASON` |
