# G4 账号生命周期组（US-007 / US-039 / US-041）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 leyoSwimming 项目中完成 G4 账号生命周期组全部 3 个用户故事：用户账号注销（US-007）、教练申请离职（US-039）、管理员处理教练离职（US-041）。

**Architecture:** 后端复用 Spring Boot 现有 JWT 认证体系，用户端接口走 `/api/user/**`，教练端走 `/api/coach/**`，管理后台走 `/api/admin/**`；统一使用 `ApiResponse<T>` 响应与 `ErrorCode` 错误码。

**范围边界:**
- US-007 独立实现，不依赖 package/order/booking 等未创建表。
- US-039 / US-041 需要 `package`、`booking`、`schedule_slot` 等数据表，但第一批次未包含购课/排课 US。因此 G4 会先创建这些表的**最小可用 schema**（仅含离职流程所需字段），后续 US-020 / US-014 等可在此基础上扩展字段，不破坏已有数据。

---

## 前置依赖

- [x] G1 登录授权组已完成（US-004 / US-006 / US-051 / US-054 / US-053）
- [x] `user` / `coach` / `user_session` / `coach_session` 表已存在
- [x] 统一响应 `ApiResponse<T>`、全局异常、`ErrorCode`、JWT Filter 已就位
- [ ] 本计划会新增 `package`、`booking`、`schedule_slot` 基础表

---

## Task 1：US-007 用户账号注销

### 1.1 数据库迁移

- [ ] 创建 `V6__alter_user_add_deleted_at.sql`
  - 为 `user` 表新增 `deleted_at DATETIME DEFAULT NULL COMMENT '注销时间'`
  - 新增 `anonymous_after DATETIME DEFAULT NULL COMMENT '90 天后匿名化时间'`

### 1.2 实体更新

- [ ] 更新 `User.java`，添加 `deletedAt`、`anonymousAfter` 字段

### 1.3 错误码

- [ ] 在 `ErrorCode.java` 中新增：
  - `ACTIVE_PACKAGE_EXISTS(400201, "您还有未完成的套餐，无法注销")`
  - `PENDING_ORDER_EXISTS(400202, "您有未完成订单，请完成后注销")`
  - `ONGOING_BOOKING_EXISTS(400203, "您有未完成的课程预约，请完成后注销")`
  - `USER_ALREADY_DELETED(400204, "账号已注销")`

### 1.4 DTO

- [ ] `UserCancelCheckResponse`：`canCancel`、`checks`（noActivePackage / noPendingOrder / noOngoingBooking）
- [ ] `UserCancelResponse`：`cancelled`、`anonymousAfter`

### 1.5 服务层

- [ ] 创建 `UserAccountService.java`
  - `cancelCheck(Long userId)`：查询用户状态，检查是否有 active 套餐/未完成订单/进行中预约（当前无真实表，先用固定返回 true 或基于后续 package 表查询）
  - `cancel(Long userId)`：校验条件 → 设置 `status=1`、`deleted_at=now()`、`anonymous_after=now()+90天` → 删除 `user_session` → 记录审计日志

> **实现注意**：由于 `package` / `order` / `booking` 表在 Task 2 才创建，Task 1 的 `cancelCheck` 先用固定返回 `canCancel=true` 占位，并在代码中标注 `TODO(US-020/US-025)`。待 Task 2 完成后回填真实查询。

### 1.6 控制器

- [ ] 创建 `UserAccountController.java`
  - `POST /api/user/account/cancel-check`
  - `POST /api/user/account/cancel`

### 1.7 测试

- [ ] `UserAccountServiceTest`：正常注销、重复注销幂等、已注销用户再调用
- [ ] `UserAccountControllerIT`：两个接口的集成测试

### 1.8 前端（用户端小程序）

- [ ] 页面：`miniapp-user/src/pages/account/cancel/index.tsx`
- [ ] 调用 `/api/user/account/cancel-check` 展示 checklist
- [ ] 二次确认弹窗后调用 `/api/user/account/cancel`
- [ ] 注销成功后清除本地登录态并跳转登录页

---

## Task 2：补齐 package / booking / schedule_slot 基础表

US-039 / US-041 需要这些表，但第一批次未包含产生这些数据的 US。G4 先创建最小 schema，后续 US 在此基础上扩展。

### 2.1 迁移

- [ ] 创建 `V7__create_package_booking_schedule.sql`

```sql
-- package：用户购买的课程套餐
CREATE TABLE IF NOT EXISTS package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    total_hours INT NOT NULL COMMENT '总课时',
    consumed_count INT NOT NULL DEFAULT 0 COMMENT '已消耗课时',
    reserved_count INT NOT NULL DEFAULT 0 COMMENT '已预约占用课时',
    available_count INT NOT NULL DEFAULT 0 COMMENT '可用课时',
    price_per_hour DECIMAL(10,2) NOT NULL COMMENT '课时单价',
    paid_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/exhausted/expired/frozen/refunded',
    frozen_reason VARCHAR(32) DEFAULT NULL COMMENT 'coach_resigned / refund_pending / admin_frozen',
    pending_handover_at DATETIME DEFAULT NULL COMMENT '教练离职标记',
    expire_at DATETIME DEFAULT NULL COMMENT '套餐过期时间',
    exhausted_at DATETIME DEFAULT NULL COMMENT '课时耗尽时间',
    refunded_at DATETIME DEFAULT NULL COMMENT '退款时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_package_user_id (user_id),
    KEY idx_package_coach_id (coach_id),
    KEY idx_package_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户课程套餐表';

-- booking：课程预约
CREATE TABLE IF NOT EXISTS booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL COMMENT '套餐 ID',
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    start_time DATETIME NOT NULL COMMENT '课程开始时间',
    end_time DATETIME NOT NULL COMMENT '课程结束时间',
    status VARCHAR(20) NOT NULL DEFAULT 'booked' COMMENT 'booked/confirmed/teaching/cancelled/completed',
    cancel_reason TINYINT DEFAULT NULL COMMENT '1=学员取消,2=教练离职,3=学员旷课,4=场馆闭馆,5=教练请假,6=套餐冻结',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_booking_coach_id (coach_id),
    KEY idx_booking_user_id (user_id),
    KEY idx_booking_start_time (start_time),
    KEY idx_booking_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程预约表';

-- schedule_slot：教练可约时段
CREATE TABLE IF NOT EXISTS schedule_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    start_time DATETIME NOT NULL COMMENT '时段开始',
    end_time DATETIME NOT NULL COMMENT '时段结束',
    status VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT 'available/booked/hidden',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_schedule_slot_coach_id (coach_id),
    KEY idx_schedule_slot_start_time (start_time),
    KEY idx_schedule_slot_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练可约时段表';
```

### 2.2 实体

- [ ] 创建 `Package.java`、`Booking.java`、`ScheduleSlot.java`

### 2.3 Mapper

- [ ] 创建 `PackageMapper.java`、`BookingMapper.java`、`ScheduleSlotMapper.java`

---

## Task 3：US-039 教练申请离职

### 3.1 数据库迁移

- [ ] 创建 `V8__create_coach_resignation.sql`

```sql
CREATE TABLE IF NOT EXISTS coach_resignation_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL COMMENT '教练 ID',
    ticket_no VARCHAR(32) NOT NULL COMMENT '工单号',
    reason VARCHAR(500) DEFAULT NULL COMMENT '离职原因',
    status VARCHAR(20) NOT NULL DEFAULT 'processing' COMMENT 'processing/pending_audit/approved/rejected',
    total_packages INT NOT NULL DEFAULT 0 COMMENT '待处理 active 套餐数',
    handled_packages INT NOT NULL DEFAULT 0 COMMENT '已登记数',
    schedule_cleared TINYINT(1) NOT NULL DEFAULT 0 COMMENT '未来排班是否已清空',
    settlement_status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待结算 1=已结算',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME DEFAULT NULL COMMENT '教练提交至管理员时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_ticket_coach_id (coach_id),
    KEY idx_ticket_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练离职工单主表';

CREATE TABLE IF NOT EXISTS coach_resignation_action (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL COMMENT '工单 ID',
    package_id BIGINT NOT NULL COMMENT '套餐 ID',
    action VARCHAR(20) NOT NULL COMMENT 'refund/transfer/continue',
    target_coach_id BIGINT DEFAULT NULL COMMENT 'transfer 时新教练 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'registered' COMMENT 'registered/approved/rejected',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_action_ticket_package (ticket_id, package_id),
    KEY idx_action_ticket_id (ticket_id),
    KEY idx_action_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练离职套餐处理记录表';

CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL COMMENT '套餐 ID',
    ticket_id BIGINT DEFAULT NULL COMMENT '关联离职工单',
    refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=pending,1=approved,2=rejected,3=completed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_refund_package_id (package_id),
    KEY idx_refund_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';
```

### 3.2 实体

- [ ] 创建 `CoachResignationTicket.java`、`CoachResignationAction.java`、`RefundRecord.java`

### 3.3 错误码

- [ ] 在 `ErrorCode.java` 中新增：
  - `COACH_STATUS_NOT_ALLOWED(410002, "当前状态不可申请离职")`
  - `RESIGNATION_ALREADY_PENDING(410003, "已有进行中的离职申请")`
  - `TICKET_NOT_PROCESSING(410004, "工单状态不允许该操作")`
  - `NOT_OWN_PACKAGE(410005, "该套餐不属于当前教练")`

### 3.4 DTO

- [ ] `CoachResignationApplyRequest` / `CoachResignationApplyResponse`
- [ ] `CoachResignationDetailResponse`
- [ ] `CoachResignationPackageActionRequest` / `CoachResignationPackageActionResponse`
- [ ] `CoachResignationSubmitRequest`

### 3.5 服务层

- [ ] 创建 `CoachResignationService.java`
  - `apply(Long coachId, String reason, String idempotencyKey)`：校验 status=1 → 幂等去重 → coach.status=4 → 生成工单 → 拉取 coach 名下 active package 生成 action 占位（初始无 action，total_packages 计数）
  - `detail(Long coachId)`：返回当前 coach 的 processing/pending_audit 工单 + package 清单 + 已登记 actions
  - `registerAction(Long coachId, Long ticketId, Long packageId, String action, Long targetCoachId)`：校验 ownership → 写入/更新 action；refund 时生成 refund_record（金额 = price_per_hour × (reserved+available)）
  - `submit(Long coachId, Long ticketId)`：校验 processing → 未确认 package 默认 refund → 生成 refund_record → ticket.status=pending_audit → ticket.submitted_at=now()

### 3.6 控制器

- [ ] 创建 `CoachResignationController.java`
  - `POST /api/coach/resignation/apply`
  - `POST /api/coach/resignation/detail`
  - `POST /api/coach/resignation/package/action`
  - `POST /api/coach/resignation/submit`

### 3.7 测试

- [ ] `CoachResignationServiceTest`
- [ ] `CoachResignationControllerIT`

### 3.8 前端（教练端小程序）

- [ ] 页面：`miniapp-coach/src/pages/resignation/apply/index.tsx`
- [ ] 页面：`miniapp-coach/src/pages/resignation/processing/index.tsx`
- [ ] 页面：`miniapp-coach/src/pages/resignation/ticket/index.tsx`
- [ ] 在「我的」页面根据 `coach.status=4` 显示「查看离职申请」入口

---

## Task 4：US-041 管理员处理教练离职

### 4.1 错误码

- [ ] 在 `ErrorCode.java` 中新增：
  - `CHECKLIST_NOT_PASSED(510001, "请先完成所有检查项")`
  - `SCHEDULE_NOT_CLEARED(510002, "未来排班未清空")`
  - `TICKET_NOT_PENDING_AUDIT(510003, "工单未提交至审批队列")`
  - `TICKET_ALREADY_PROCESSED(510004, "该工单已被处理")`

### 4.2 DTO

- [ ] `AdminResignationTicketListRequest` / `AdminResignationTicketListResponse`
- [ ] `AdminResignationTicketDetailResponse`
- [ ] `AdminResignationApproveRequest` / `AdminResignationRejectRequest`

### 4.3 服务层

- [ ] 创建 `AdminCoachResignationService.java`
  - `list(String status, int page, int pageSize)`：查询工单列表
  - `detail(Long ticketId)`：工单详情 + coach 信息 + package 清单 + actions + checklist 状态
  - `approve(Long adminId, Long ticketId)`：
    1. 校验 ticket pending_audit
    2. checklist 校验：active 套餐需全部登记 action；schedule_cleared=true
    3. 按 action 类型处理 package：
       - transfer：package.coach_id = target_coach_id
       - refund：生成 refund_record，package.status=frozen，frozen_reason='coach_resigned'
       - continue：保持不变
    4. 取消未来 booking（start_time > now, status in booked/confirmed），cancel_reason=2
    5. 对应 package reserved_count 释放到 available_count
    6. 未来 schedule_slot 更新为 hidden
    7. coach.status = 3，ticket.status = approved
  - `reject(Long adminId, Long ticketId, String reason)`：ticket.status=rejected，coach.status=1

### 4.4 控制器

- [ ] 创建 `AdminCoachResignationController.java`
  - `POST /api/admin/coach/resignation-ticket/list`
  - `POST /api/admin/coach/resignation-ticket/detail`
  - `POST /api/admin/coach/resignation-ticket/approve`
  - `POST /api/admin/coach/resignation-ticket/reject`

### 4.5 权限

- [ ] 在 `AdminAuthService` 或控制器层检查管理员是否具有 `MANAGE_COACH_RESIGNATION` 权限。当前 admin 角色为字符串，MVP 先允许 `SUPER_ADMIN` 和 `COACH_MANAGER` 访问。

### 4.6 测试

- [ ] `AdminCoachResignationServiceTest`
- [ ] `AdminCoachResignationControllerIT`

### 4.7 前端（管理后台）

- [ ] 页面：`web-admin/src/views/coach/ResignationApprovalQueueView.vue`
- [ ] 页面：`web-admin/src/views/coach/ResignationTicketDetailView.vue`
- [ ] 在左侧菜单新增「教练离职审批」入口

---

## Task 5：联调与验证

### 5.1 后端验证

- [ ] `mvn test` 通过（新增测试 + 已有测试不回归）
- [ ] `mvn flyway:validate` 通过
- [ ] 启动后端，用 Postman / curl 调通 US-007 / US-039 / US-041 关键接口

### 5.2 前端验证

- [ ] 用户端注销页可正常展示 checklist、二次确认并跳转
- [ ] 教练端离职申请/处理中/工单页流程打通
- [ ] 管理后台审批队列与详情页可查询、通过、拒绝

---

## 风险与决策

| # | 风险 | 决策 |
|---|---|---|
| 1 | package/booking/schedule_slot 表尚未创建，US-039/US-041 依赖它们 | G4 先创建最小 schema，后续 US 扩展字段 |
| 2 | US-007 的注销条件校验依赖 package/order/booking | Task 1 先用固定返回占位，Task 2 后回填真实查询 |
| 3 | 管理员权限模型较简单 | MVP 仅按 admin.role 字符串检查，US-057 后统一改造 |
| 4 | 退款金额计算在多处出现 | 抽取 `RefundCalculator` 工具类统一计算 |

---

## 提交清单

- [ ] V6 迁移：user 表增加 deleted_at / anonymous_after
- [ ] V7 迁移：package / booking / schedule_slot 基础表
- [ ] V8 迁移：coach_resignation_ticket / action / refund_record
- [ ] UserAccountController / Service / Tests
- [ ] CoachResignationController / Service / Tests
- [ ] AdminCoachResignationController / Service / Tests
- [ ] 三端页面（用户端注销页、教练端离职页、管理后台审批页）
- [ ] 所有测试通过
