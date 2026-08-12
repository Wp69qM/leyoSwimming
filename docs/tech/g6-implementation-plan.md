# G6 实施计划：个人主页与退出登录

> **目标读者**：主 Agent、后端 Sub-Agent、前端 Sub-Agent、后续接手开发工程师  
> **覆盖 US**：US-012（教练管理个人主页与参考单价）、US-052（用户退出登录）、US-056（教练退出登录）  
> **定位**：G6 组完整实施蓝图，包含任务分解、API 契约、数据迁移、前端结构、测试策略与执行顺序  
> **配套文档**：
> - [dev-plan-batch1.md](./dev-plan-batch1.md)
> - [api-convention.md](./api-convention.md)
> - US-012/052/056 的 user-story.md / tech-design.md / test-plan.md
> - [frontend-development-standards.md](./frontend-development-standards.md)
> - [backend-development-standards.md](./backend-development-standards.md)

---

## 1. 范围与 US 概览

| US | 标题 | 端 | 核心交付物 | 估时 |
|---|---|---|---|---|
| US-012 | 教练管理个人主页与参考单价 | 后端 + 教练端小程序 | 教练主页查询/更新接口、参考单价接口、教练端「我的/个人主页/参考单价」页面 | 1 人天 |
| US-052 | 用户退出登录 | 后端（已有接口复用）+ 用户端小程序 | 用户端「我的」页面、退出登录交互 | 0.2 人天 |
| US-056 | 教练退出登录 | 后端 + 教练端小程序 | 教练端 `/api/coach/auth/logout`、教练端「我的」页面退出逻辑 | 0.2 人天 |

---

## 2. 设计决策

1. **个人形象照存储位置**
   - 采用 `coach_certificate` 表，`cert_type = 'PORTRAIT'`，便于与证书统一管理和排序。
   - 微信二维码继续使用 `coach.wechat_qr_url` 字段，与个人主页 tech-design 一致。

2. **参考单价独立接口**
   - 单独提供 `POST /api/coach/reference-price/update`，以便做每日修改次数限制和独立权限校验。
   - 通过 `coach.price_changed_at` 与 `coach.price_change_count_today` 实现每日最多 3 次改价。

3. **教练主页更新范围**
   - 仅更新可编辑字段：姓名、性别、年龄、邮箱、微信二维码、个人形象照、任教年限、擅长泳姿、个人简介。
   - 手机号、身份证号、身份证照片、教练资格证、健康证、总学员数、总课时数只读，不通过本接口修改。

4. **变更日志**
   - 新增 `coach_update_log` 表，记录参考单价与关键资料字段变更历史，满足审计需求。

5. **退出登录**
   - 用户端复用已有的 `POST /api/user/auth/logout` 与 `UserSessionService.logout()`。
   - 教练端新增 `POST /api/coach/auth/logout`，在 `CoachSessionService` 中新增 `logout()` 方法，逻辑与用户端对齐：按 `coach_id + refresh_token_hash` 删除当前 session。

6. **前端视觉来源**
   - 严格遵循 dev-plan-batch1.md §6 的 Calicat 优先原则：开工前拉取对应 Frame 图层数据，优先使用 Calicat Token，page-spec 仅作交互/业务规则参考。

---

## 3. 数据模型与数据库迁移

### 3.1 新增迁移文件

**文件**：`backend/src/main/resources/db/migration/V16__g6_coach_profile_and_logout.sql`

内容要点：

1. 扩展 `coach` 表（参考单价变更追踪）
   - `price_changed_at` DATETIME DEFAULT NULL COMMENT '上次改价时间'
   - `price_change_count_today` INT DEFAULT 0 COMMENT '今日改价次数'

2. 创建 `coach_update_log` 表
   - `log_id` BIGINT PK AUTO_INCREMENT
   - `coach_id` BIGINT NOT NULL
   - `field_name` VARCHAR(32) NOT NULL COMMENT '变更字段，如 reference_price/bio'
   - `old_value` TEXT
   - `new_value` TEXT
   - `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
   - 索引：`idx_coach_update_log_coach_id`

3. 为 `coach_certificate` 表确认已有索引
   - `idx_coach_certificate_coach_id_type`

> 说明：当前 `coach_session` 表没有 `revoked_at` 字段，退出登录采用与用户端一致的删除记录策略，不新增字段。

### 3.2 实体变更

| 实体 | 变更 | 说明 |
|---|---|---|
| `Coach` | 新增字段 | `priceChangedAt`、`priceChangeCountToday` |
| `CoachUpdateLog` | 新增实体 | 对应 `coach_update_log` 表 |
| `CoachCertificate` | 复用 | 新增 `PORTRAIT` 类型枚举值 |


---

## 4. API 契约

统一响应格式（已有）：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 4.1 US-012 教练主页

#### POST /api/coach/profile/detail

**说明**：获取当前登录教练个人主页完整资料。

**Response data 字段**：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 教练 ID |
| name | String | 姓名/昵称 |
| avatarUrl | String | 头像 URL |
| portraitUrl | String | 个人形象照 URL（PORTRAIT） |
| phone | String | 手机号（脱敏） |
| age | Integer | 年龄 |
| gender | String | male / female |
| email | String | 邮箱 |
| wechatQrUrl | String | 微信二维码 URL |
| idCardNoMasked | String | 脱敏身份证号 |
| idCardFrontUrl | String | 身份证正面照 |
| idCardBackUrl | String | 身份证反面照 |
| coachCertUrls | List<String> | 教练资格证图片列表 |
| healthCertUrl | String | 健康证图片 |
| totalStudents | Integer | 总学员数（只读） |
| totalHours | Integer | 总课时数（只读） |
| teachingYears | Integer | 任教年限 |
| teachingStrokes | List<String> | 擅长泳姿 |
| bio | String | 个人简介 |
| referencePrice | BigDecimal | 参考单价（元/节） |
| status | Integer | 教练状态 |
| profileCompleted | Boolean | 资料是否完善 |
| consent | Object | 协议同意状态 |

#### POST /api/coach/profile/update

**Request Body**：

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| name | String | 是 | 1-32 字符 |
| gender | String | 是 | male / female |
| age | Integer | 是 | 18-80 |
| email | String | 否 | 有效邮箱，≤128 字符 |
| wechatQrUrl | String | 否 | URL，≤512 字符 |
| portraitUrl | String | 否 | URL，≤512 字符 |
| teachingYears | Integer | 否 | 0-60 |
| teachingStrokes | List<String> | 否 | 蛙泳/自由泳/仰泳/蝶泳 |
| bio | String | 否 | 10-500 字符，敏感词过滤 |
| idempotencyKey | String | 是 | ≤64 字符 |

**错误码**：
- `COACH_STATUS_NOT_APPROVED` (410006) — 教练不处于已通过状态
- `VALIDATION_ERROR` (100005) — 字段校验失败
- `NICKNAME_SENSITIVE` (440005) — 姓名/简介含敏感词
- `INVALID_FILE_TYPE` (440006) — 图片格式非法
- `FILE_TOO_LARGE` (440007) — 图片超过 5MB

#### POST /api/coach/reference-price/update

**Request Body**：

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| referencePrice | BigDecimal | 是 | 50.00 - 2000.00，保留两位小数 |
| idempotencyKey | String | 是 | ≤64 字符 |

**Response data**：

| 字段 | 类型 | 说明 |
|---|---|---|
| referencePrice | BigDecimal | 更新后的参考单价 |
| priceChangedAt | String | ISO-8601 改价时间 |
| remainingChangesToday | Integer | 今日剩余可修改次数 |

**错误码**：
- `INVALID_REFERENCE_PRICE` — 新增错误码（建议 400601）
- `PRICE_CHANGE_LIMIT` — 新增错误码（建议 400602）
- `COACH_STATUS_NOT_APPROVED` (410006)

### 4.2 US-052 / US-056 退出登录

#### POST /api/user/auth/logout（已存在，复用）

**Request Body**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| refreshToken | String | 否 | 当前会话 refresh_token |

**Response**：`{ "code": 0, "message": "退出登录成功", "data": null }`

#### POST /api/coach/auth/logout（新增）

与上同结构，鉴权要求 JWT type = coach。


---

## 5. 后端任务分解

### US-012 后端

| 子任务 | 文件 | 操作 | 说明 |
|---|---|---|---|
| 5.1 数据迁移 | `V16__g6_coach_profile_and_logout.sql` | 创建 | 新增改价追踪字段与 `coach_update_log` 表 |
| 5.2 实体 | `Coach.java` | 修改 | 新增 `priceChangedAt`、`priceChangeCountToday` |
| 5.3 实体 | `CoachUpdateLog.java` | 创建 | 新增变更日志实体 |
| 5.4 枚举 | `CoachCertificateType.java` | 修改 | 新增 `PORTRAIT` 枚举值 |
| 5.5 Mapper | `CoachUpdateLogMapper.java` | 创建 | 变更日志数据访问 |
| 5.6 请求 DTO | `UpdateCoachProfileRequest.java` | 修改 | 扩展字段：email、wechatQrUrl、portraitUrl、teachingStrokes、bio；保持当前 phone 只读 |
| 5.7 响应 DTO | `CoachProfileResponse.java` | 修改 | 扩展字段：portraitUrl、email、wechatQrUrl、idCardNoMasked、idCardFrontUrl、idCardBackUrl、coachCertUrls、healthCertUrl、totalStudents、totalHours、teachingStrokes、bio、referencePrice、status |
| 5.8 请求 DTO | `UpdateCoachReferencePriceRequest.java` | 创建 | 参考单价更新请求 |
| 5.9 响应 DTO | `CoachReferencePriceResponse.java` | 创建 | 参考单价更新响应 |
| 5.10 Service | `CoachProfileService.java` | 修改 | 重写 `getProfile` / `updateProfile`：从 `coach_certificate` 读取 PORTRAIT 与资质图片，校验 status=1，敏感词过滤，写变更日志 |
| 5.11 Service | `CoachReferencePriceService.java` | 创建 | 参考单价更新、每日次数限制、写变更日志 |
| 5.12 Controller | `CoachProfileController.java` | 修改 | 保持 `/detail` 与 `/update`，新增 `/reference-price/update` |
| 5.13 错误码 | `ErrorCode.java` | 修改 | 新增 `INVALID_REFERENCE_PRICE`、`PRICE_CHANGE_LIMIT` |

### US-052 后端

已具备 `POST /api/user/auth/logout` 与 `UserSessionService.logout()`，**无需后端改动**。仅在前端调用。

### US-056 后端

| 子任务 | 文件 | 操作 | 说明 |
|---|---|---|---|
| 5.14 Service | `CoachSessionService.java` | 修改 | 新增 `logout(Long coachId, String refreshToken)`，按 coach_id + refresh_token_hash 删除 session |
| 5.15 Controller | `CoachAuthController.java` | 修改 | 新增 `POST /logout`，复用 `RefreshTokenRequest` 作为请求体 |

---

## 6. 前端任务分解

### US-012 教练端前端

| 子任务 | 文件 | 操作 | 说明 |
|---|---|---|---|
| 6.1 API | `miniapp-coach/src/api/profile.ts` | 修改 | 扩展 `CoachProfile` / `UpdateProfileParams` 类型；新增 `updateReferencePrice`、`getReferencePrice` |
| 6.2 页面 | `miniapp-coach/src/pages/profile/edit/index.tsx` | 创建 | 个人主页编辑页 |
| 6.3 样式 | `miniapp-coach/src/pages/profile/edit/index.scss` | 创建 | Calicat Token 样式 |
| 6.4 配置 | `miniapp-coach/src/pages/profile/edit/index.config.ts` | 创建 | 页面导航配置 |
| 6.5 页面 | `miniapp-coach/src/pages/profile/reference-price/index.tsx` | 创建 | 参考单价设置页 |
| 6.6 样式 | `miniapp-coach/src/pages/profile/reference-price/index.scss` | 创建 | Calicat Token 样式 |
| 6.7 配置 | `miniapp-coach/src/pages/profile/reference-price/index.config.ts` | 创建 | 页面导航配置 |
| 6.8 组件（可选） | `miniapp-coach/src/components/profile/PortraitUploader.tsx` | 创建 | 个人形象照上传组件（也可内联） |

### US-052 用户端前端

| 子任务 | 文件 | 操作 | 说明 |
|---|---|---|---|
| 6.9 API | `miniapp-user/src/api/auth.ts` | 修改 | `logout()` 已存在，确保调用时携带 `refreshToken` |
| 6.10 页面 | `miniapp-user/src/pages/mine/index.tsx` | 创建 | 用户个人中心页（Tab「我的」） |
| 6.11 样式 | `miniapp-user/src/pages/mine/index.scss` | 创建 | Calicat Token 样式 |
| 6.12 配置 | `miniapp-user/src/pages/mine/index.config.ts` | 创建 | 页面配置 |
| 6.13 TabBar | `miniapp-user/src/app.config.ts` | 修改 | 添加 TabBar：首页 / 教练 / 预约 / 我的 |
| 6.14 Store（可选） | `miniapp-user/src/stores/authStore.ts` | 修改 | 已具备 `logout()`，页面直接调用 |

### US-056 教练端前端

| 子任务 | 文件 | 操作 | 说明 |
|---|---|---|---|
| 6.15 API | `miniapp-coach/src/api/auth.ts` | 修改 | `logoutCoach()` 已存在，确保携带 `refreshToken` |
| 6.16 页面 | `miniapp-coach/src/pages/mine/index.tsx` | 创建 | 教练中心页（Tab「我的」） |
| 6.17 样式 | `miniapp-coach/src/pages/mine/index.scss` | 创建 | Calicat Token 样式 |
| 6.18 配置 | `miniapp-coach/src/pages/mine/index.config.ts` | 创建 | 页面配置 |
| 6.19 TabBar | `miniapp-coach/src/app.config.ts` | 修改 | 添加 TabBar：首页 / 预约 / 学员 / 我的 |
| 6.20 Store（可选） | `miniapp-coach/src/stores/authStore.ts` | 修改 | 已具备 `logout()`，页面直接调用 |


---

## 7. 前端页面/组件结构

### 7.1 用户端「我的」页面（US-052）

- **路径**：`miniapp-user/src/pages/mine/index`
- **状态**：游客 / 已登录
- **元素**：
  - 用户信息头图卡片（头像、昵称、身份状态）
  - 功能入口列表（我的套餐、我的订单、我的预约、隐私协议、设置）
  - 底部「退出登录」按钮（仅已登录显示）
- **交互**：点击退出 → `Taro.showModal` 二次确认 → 调用 `logout()` → 清除 token → 刷新本页为游客态

### 7.2 教练端「我的」页面（US-012 / US-056）

- **路径**：`miniapp-coach/src/pages/mine/index`
- **元素**：
  - 教练资料头图卡片（形象照、姓名、状态标签）
  - 状态提示卡（status=0/2/3/4 显示）
  - 功能入口列表（个人主页编辑、参考单价设置、我的收入、排班管理、请假申请、申请离职/查看离职申请、重新入驻/入驻资料）
  - 底部「退出登录」按钮
- **交互**：点击退出 → 二次确认 → 调用 `logoutCoach()` → 清除 token → `redirectTo('/pages/login/wechat/index')`

### 7.3 教练个人主页编辑页（US-012）

- **路径**：`miniapp-coach/src/pages/profile/edit/index`
- **分组**：
  1. 形象展示：形象照、姓名、任教年限、状态标签
  2. 基础信息：性别、年龄、邮箱、手机号（只读）、微信二维码
  3. 实名与资质（只读）：身份证号、身份证正反面、教练资格证、健康证
  4. 教学履历：总学员数、总课时数、擅长泳姿、个人简介
  5. 服务设置：参考单价入口
- **交互**：保存 → 调用 `updateProfile` → 返回教练中心页

### 7.4 参考单价设置页（US-012）

- **路径**：`miniapp-coach/src/pages/profile/reference-price/index`
- **元素**：说明卡片、价格输入区、今日修改次数提示、套餐价格预览、保存按钮
- **交互**：输入价格 → 实时校验 50-2000 → 保存 → 调用 `updateReferencePrice` → 返回上一页

---

## 8. 测试策略

### 8.1 后端测试

| 测试文件 | 类型 | 覆盖场景 |
|---|---|---|
| `CoachProfileServiceTest.java` | 单元 | 正常更新、状态非已通过拒绝、字段校验、敏感词过滤、资质图片读取 |
| `CoachReferencePriceServiceTest.java` | 单元 | 正常改价、超出范围、每日 3 次限制、幂等 |
| `CoachProfileControllerIT.java` | 集成 | `/detail`、`/update`、`/reference-price/update` 端到端 |
| `CoachSessionServiceTest.java` | 单元 | 教练退出删除 session、重复调用幂等 |
| `CoachAuthControllerIT.java` | 集成 | `POST /api/coach/auth/logout` 鉴权与成功路径 |

### 8.2 前端测试

| 测试文件 | 类型 | 覆盖场景 |
|---|---|---|
| `miniapp-user/src/pages/mine/index.test.tsx` | 组件 | 确认退出清除 token 并刷新游客态、取消退出保持登录态、无 token 直接刷新 |
| `miniapp-coach/src/pages/mine/index.test.tsx` | 组件 | 确认退出跳转登录页、取消退出、无 token 跳转登录页 |
| `miniapp-coach/src/pages/profile/edit/index.test.tsx` | 组件 | 表单校验、保存成功、状态异常提示 |
| `miniapp-coach/src/pages/profile/reference-price/index.test.tsx` | 组件 | 范围校验、次数提示、保存成功 |

### 8.3 E2E 测试

| 测试文件 | 场景 |
|---|---|
| `e2e/tests/coach-profile-flow.test.ts` | 教练更新主页 → 学员端教练详情页同步展示 |

### 8.4 覆盖率要求

- 后端服务层：≥ 80% 行覆盖
- 后端 Controller：≥ 80% 行覆盖
- 前端页面组件：核心交互路径覆盖


---

## 9. 执行顺序与依赖

### Phase A：数据与契约（第 1 天上午）

1. 编写 Flyway 迁移 `V16__g6_coach_profile_and_logout.sql`
2. 扩展 `Coach` 实体与新增 `CoachUpdateLog` 实体/Mapper
3. 扩展/新增 DTO（Request/Response）
4. 新增错误码 `INVALID_REFERENCE_PRICE`、`PRICE_CHANGE_LIMIT`

### Phase B：US-012 后端（第 1 天上午-下午）

5. 重构 `CoachProfileService.getProfile`：从 `coach_certificate` 聚合 PORTRAIT 与资质图片
6. 重构 `CoachProfileService.updateProfile`：字段校验、敏感词过滤、status=1 校验、写变更日志
7. 创建 `CoachReferencePriceService`：改价逻辑与每日限制
8. 扩展 `CoachProfileController`：新增 `/reference-price/update`
9. 后端单元/集成测试（TDD）

### Phase C：US-056 后端（与 Phase B 可并行）

10. 在 `CoachSessionService` 新增 `logout()`
11. 在 `CoachAuthController` 新增 `POST /logout`
12. 后端单元/集成测试

### Phase D：教练端前端（第 1-2 天）

13. 扩展 `miniapp-coach/src/api/profile.ts`
14. 创建「个人主页编辑页」
15. 创建「参考单价设置页」
16. 创建「教练中心页（我的）」与 TabBar
17. 集成退出登录逻辑（US-056）
18. 前端组件测试

### Phase E：用户端前端（第 1-2 天，与 D 并行）

19. 创建「个人中心页（我的）」与 TabBar
20. 集成退出登录逻辑（US-052）
21. 前端组件测试

### Phase F：联调与验收

22. 后端/前端联调
23. E2E 测试
24. Code Review（code-reviewer + security-reviewer）
25. 视觉还原检查（visual-review，按 Calicat）

---

## 10. 关键依赖与阻塞点

| 依赖 | 说明 |
|---|---|
| US-011 | 教练必须已通过审核，才能进入个人主页编辑 |
| US-051 / US-054 | 教练登录态机制必须先完成，US-056 才能测试 |
| US-004 / US-006 | 用户登录态机制必须先完成，US-052 才能测试 |
| 图片上传 | `POST /api/common/file/upload` 已存在，可直接复用 |
| Calicat 图层 | 页面开发前需拉取 C-个人主页编辑页、C-参考单价设置页、C-教练中心页、U-个人中心页图层数据 |

