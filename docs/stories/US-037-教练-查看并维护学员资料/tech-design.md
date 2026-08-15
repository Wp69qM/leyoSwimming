# US-037 教练查看并维护学员资料技术设计

---

## 1. 上下文

本 US 为教练端提供「我的学员」功能：教练可查看与自己存在 booking/package 关联的学员完整资料（含 US-005 学员自主档案），并维护教练视角的信息切片（学习特征、沟通备注）。数据模型需要与学员本人资料（`user` 表）解耦：US-005 字段在教练端只读，教练修改仅写入 `coach_student_profile` 表，避免覆盖学员自主信息。未成年人信息统一由学员在 US-005 中维护，教练端仅只读展示，不在 `coach_student_profile` 中保存监护人字段。

---

## 2. 目标 / 非目标

**目标：**
- 教练能查看关联学员列表（展示 US-005 头像、姓名、性别、年龄、是否未成年人）
- 教练能查看学员完整资料：US-005 自主档案（只读）+ 教练视角切片
- 教练能更新学员信息切片（学习泳姿、游泳等级、基础情况、沟通备注）
- 教练能查看与该学员关联的所有 package 实例卡片（套餐名称、模式、有效期、状态标签、剩余课时）
- 点击套餐卡片跳转至 US-021 教练视角套餐使用详情页
- 仅允许维护与当前教练存在关联的学员

**非目标：**
- 不修改 `user` 表中学员自行维护的 US-005 基础资料
- 不维护未成年人及监护人信息（统一由 US-005 负责）
- 不实现套餐详情页内容（由 US-021 教练视角统一承接）
- 不实现家长独立账号体系

---

## 3. 数据模型

### 3.1 新增表

#### `coach_student_profile`

| 字段 | 类型 | 索引 | 备注 |
|------|------|------|------|
| profile_id | BIGINT PK | | 雪花 ID |
| coach_id | BIGINT FK | IDX | 关联 coach |
| student_user_id | BIGINT FK | IDX | 关联 user |
| learning_strokes | VARCHAR(64) | | 学习泳姿，如"自由泳/蛙泳" |
| swim_level | TINYINT | | 0=零基础 1=入门 2=进阶 3=高级 |
| basics | VARCHAR(500) | | 基础情况描述 |
| notes | VARCHAR(1000) | | 沟通备注 |
| created_at | DATETIME | | |
| updated_at | DATETIME | | |

**唯一索引**：`UNIQUE INDEX idx_coach_student (coach_id, student_user_id)`

### 3.2 读取表

- `user`：读取学员昵称、手机号、年龄、性别、游泳基础等（用于列表与详情展示）
- `package` / `booking`：用于判断教练与学员是否存在关联，并查询关联套餐列表
- `audit_log`：写入操作日志

---

## 4. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 4.1 POST /api/coach/student/list

- **鉴权**：教练 JWT，`coach.status = 1`
- **功能**：获取与当前教练存在关联的学员列表
- **请求体**：
  ```json
  {
    "tab": "active"
  }
  ```
- **响应 200**：
  ```json
  {
    "students": [
      {
        "studentUserId": 10001,
        "avatarUrl": "https://cdn.example.com/avatar.jpg",
        "name": "张小明",
        "gender": "male",
        "age": 25,
        "isMinor": true,
        "updatedAt": "2026-07-30T10:00:00Z"
      }
    ]
  }
  ```
- **错误码**：`AUTH_FORBIDDEN`（403）

### 4.2 POST /api/coach/student/detail

- **鉴权**：教练 JWT
- **功能**：获取某学员完整资料（US-005 自主档案 + 教练视角切片）
- **请求体**：
  ```json
  {
    "studentId": 10001
  }
  ```
- **响应 200**：
  ```json
  {
    "studentUserId": 10001,
    "userProfile": {
      "avatarUrl": "https://cdn.example.com/avatar.jpg",
      "name": "张 swimmer",
      "phoneMasked": "138****8000",
      "age": 25,
      "gender": "male",
      "hasSwimBasis": true,
      "swimStrokes": "蛙泳/自由泳",
      "swimYears": "3年",
      "personalDesc": "想提高自由泳",
      "isMinor": true
    },
    "coachSlice": {
      "learningStrokes": "自由泳",
      "swimLevel": 2,
      "basics": "怕水，需循序渐进",
      "notes": "学员水性较好，可加快进度"
    }
  }
  ```
- **错误码**：`NOT_ASSOCIATED_STUDENT`（403）

### 4.3 POST /api/coach/student/update

- **鉴权**：教练 JWT
- **功能**：更新教练视角切片；US-005 自主档案字段（name/phone/age/gender/avatarUrl/isMinor 等）即使传入也忽略，不修改 `user` 表
- **请求体**：
  ```json
  {
    "studentId": 10001,
    "learningStrokes": "自由泳",
    "swimLevel": 2,
    "basics": "怕水，需循序渐进",
    "notes": "学员水性较好，可加快进度",
    "idempotencyKey": "coach:1:student:10001:ts:1753879200000"
  }
  ```
- **响应 200**：`{ "message": "保存成功" }`
- **错误码**：
  - `NOT_ASSOCIATED_STUDENT`（403）
  - `READONLY_USER_PROFILE`（400）：若请求体包含 US-005 只读字段且被后端严格拒绝时使用
  - `IDEMPOTENCY_DUPLICATE`（409）

### 4.4 POST /api/coach/student/package/list

- **鉴权**：教练 JWT
- **功能**：获取该学员与当前教练关联的 package 列表，用于详情页「关联套餐」卡片区
- **请求体**：
  ```json
  {
    "studentId": 10001
  }
  ```
- **业务规则**：
  - 仅返回 `package.coachId = 当前教练 coachId` 且 `package.userId = studentId` 的记录
  - 按购买时间（`createdAt`）倒序排列
  - 状态标签映射：`active` 且 `consumedCount = 0` → 未使用；`active` 且 `consumedCount > 0` → 使用中；`exhausted` → 已使用；`expired` → 已过期；`refunded` → 已退款；`frozen` → 已冻结
- **响应 200**：
  ```json
  {
    "packages": [
      {
        "packageId": 20001,
        "packageName": "10 节私教课",
        "packageMode": "standard",
        "status": "active",
        "statusLabel": "使用中",
        "validStart": "2026-07-01",
        "validEnd": "2026-09-29",
        "remainingHours": 4
      },
      {
        "packageId": 20002,
        "packageName": "新人体验课",
        "packageMode": "experience",
        "status": "exhausted",
        "statusLabel": "已使用",
        "validStart": "2026-06-01",
        "validEnd": "2026-06-30",
        "remainingHours": 0
      }
    ]
  }
  ```
- **错误码**：`NOT_ASSOCIATED_STUDENT`（403）

---

## 5. 状态机

本 US 不涉及业务状态机转换。

---

## 6. 缓存策略

| Key | 类型 | TTL | 失效策略 |
|-----|------|-----|---------|
| `coach:students:{coach_id}` | 列表 JSON | 5 分钟 | 更新 profile 时删除 |
| `coach:student:profile:{coach_id}:{student_user_id}` | 详情 JSON | 5 分钟 | 更新时删除 |
| `coach:student:packages:{coach_id}:{student_user_id}` | 套餐列表 JSON | 5 分钟 | package 状态变更时删除（由 US-021 / US-050 等写入方统一失效） |

---

## 7. 性能指标

- `POST /api/coach/student/list` P99 < 200ms（含缓存命中）
- `POST /api/coach/student/detail` P99 < 150ms
- `POST /api/coach/student/package/list` P99 < 150ms
- `POST /api/coach/student/update` P99 < 300ms
- 列表接口支持 coachId 索引，单页默认 20 条
- 套餐列表接口支持 `(coachId, userId)` 联合索引

---

## 8. 安全

- 接口必须校验 JWT 中的 coachId 与请求中隐含的 coachId 一致
- `notes` / `basics` 字段入库前进行 HTML 转义，防止 XSS
- 操作写入 `audit_log`，记录 before/after JSON 与 IP
- 更新接口必须忽略 US-005 自主档案字段，防止教练通过接口绕过前端修改学员资料
- 套餐列表接口必须校验教练与学员的关联关系，防止越权查看他人套餐

---

## 9. 跨 US 依赖

- 依赖 US-012 完成后教练才能登录教练端并具备 status=1
- 依赖 US-005 用户完善个人资料；本 US 读取 US-005 字段并在教练端只读展示
- 依赖 US-021 提供教练视角套餐使用详情页；US-037 仅提供入口与列表卡片
- 本 US 产生 `coach_student_profile` 数据，供 US-041 管理员处理教练离职时参考学员清单与处理结果

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 查看学员完整资料含 US-005 字段 | `test_get_student_profile_with_us005_fields` | 集成 |
| 正常更新成人学员并添加备注 | `test_update_adult_student_profile_with_notes` | 集成 |
| 非关联学员 | `test_get_non_associated_student_forbidden` | 集成 |
| 只读字段不可修改 | `test_us005_fields_readonly_for_coach` | 集成 |
| 并发保存幂等 | `test_update_profile_idempotent` | 集成 |
| XSS 转义 | `test_notes_xss_escaped` | 单元 |
| 查看关联套餐列表 | `test_get_student_packages_list` | 集成 |
| 套餐列表按购买时间倒序 | `test_student_packages_sorted_by_created_at_desc` | 集成 |
| 套餐列表状态标签映射 | `test_student_packages_status_labels` | 单元/集成 |
| 套餐列表越权 | `test_student_packages_non_associated_forbidden` | 集成 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-04 | PM | 初版 |
| v2.0 | 2026-08-12 | PM | 移除未成年人及监护人字段维护：未成年人信息统一由 US-005 维护，`coach_student_profile` 不再包含 `is_minor`、`guardian_name`、`guardian_phone`；PUT/GET 接口同步移除相关字段与校验 |
| v2.1 | 2026-08-12 | PM | 新增 `/api/coach/student/package/list` 接口定义，用于学员详情页「关联套餐」卡片区；明确套餐详情页由 US-021 教练视角承接；更新缓存、性能、安全与测试映射 |
