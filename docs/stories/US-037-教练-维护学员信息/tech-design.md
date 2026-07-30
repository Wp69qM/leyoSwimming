# US-037 教练维护学员信息（含未成年人）技术设计

---

## 1. 上下文

本 US 为教练端提供「我的学员」功能：教练可查看并维护与自己存在 booking/package 关联的学员信息切片，包括未成年人标识与监护人联系方式。数据模型需要与学员本人资料（`user` 表）解耦，避免教练视角覆盖学员自主信息。

---

## 2. 目标 / 非目标

**目标：**
- 教练能查看关联学员列表与详情
- 教练能更新学员信息切片，支持未成年人及监护人字段
- 系统强制校验未成年人必须填写监护人手机号
- 仅允许维护与当前教练存在关联的学员

**非目标：**
- 不修改 `user` 表中学员自行维护的基础资料
- 不实现监护人短信发送逻辑（在本 US 仅保存字段，短信在 US-032/ US-041 中触发）
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
| is_minor | BOOLEAN | | 是否未成年人 |
| guardian_name | VARCHAR(64) | | 监护人姓名 |
| guardian_phone | VARCHAR(64) | | 监护人手机号，AES 加密 |
| created_at | DATETIME | | |
| updated_at | DATETIME | | |

**唯一索引**：`UNIQUE INDEX idx_coach_student (coach_id, student_user_id)`

### 3.2 读取表

- `user`：读取学员昵称、手机号（用于列表展示）
- `package` / `booking`：用于判断教练与学员是否存在关联
- `audit_log`：写入操作日志

---

## 4. API 设计

### 4.1 `GET /api/coach/v1/students`

- **鉴权**：教练 JWT，`coach.status = 1`
- **功能**：获取与当前教练存在关联的学员列表
- **响应 200**：
  ```json
  {
    "students": [
      {
        "student_user_id": 10001,
        "nickname": "张小明",
        "phone_masked": "138****8000",
        "is_minor": true,
        "updated_at": "2026-07-30T10:00:00Z"
      }
    ]
  }
  ```
- **错误码**：`AUTH_FORBIDDEN`（403）

### 4.2 `GET /api/coach/v1/students/{student_user_id}/profile`

- **鉴权**：教练 JWT
- **功能**：获取某学员的信息切片
- **响应 200**：
  ```json
  {
    "student_user_id": 10001,
    "nickname": "张小明",
    "learning_strokes": "自由泳",
    "swim_level": 2,
    "basics": "怕水，需循序渐进",
    "notes": "",
    "is_minor": true,
    "guardian_name": "王芳",
    "guardian_phone_masked": "138****8000"
  }
  ```
- **错误码**：`NOT_ASSOCIATED_STUDENT`（403）

### 4.3 `PUT /api/coach/v1/students/{student_user_id}/profile`

- **鉴权**：教练 JWT
- **请求体**：
  ```json
  {
    "learning_strokes": "自由泳",
    "swim_level": 2,
    "basics": "怕水，需循序渐进",
    "notes": "",
    "is_minor": true,
    "guardian_name": "王芳",
    "guardian_phone": "13800138000",
    "idempotency_key": "coach:1:student:10001:ts:1753879200000"
  }
  ```
- **响应 200**：`{ "message": "保存成功" }`
- **错误码**：
  - `GUARDIAN_PHONE_REQUIRED`（400）
  - `INVALID_PHONE`（400）
  - `NOT_ASSOCIATED_STUDENT`（403）
  - `IDEMPOTENCY_DUPLICATE`（409）

---

## 5. 状态机

本 US 不涉及业务状态机转换。

---

## 6. 缓存策略

| Key | 类型 | TTL | 失效策略 |
|-----|------|-----|---------|
| `coach:students:{coach_id}` | 列表 JSON | 5 分钟 | 更新 profile 时删除 |
| `coach:student:profile:{coach_id}:{student_user_id}` | 详情 JSON | 5 分钟 | 更新时删除 |

---

## 7. 性能指标

- `GET /api/coach/v1/students` P99 < 200ms（含缓存命中）
- `GET /api/coach/v1/students/{id}/profile` P99 < 150ms
- `PUT /api/coach/v1/students/{id}/profile` P99 < 300ms
- 列表接口支持 coach_id 索引，单页默认 20 条

---

## 8. 安全

- 接口必须校验 JWT 中的 coach_id 与请求中隐含的 coach_id 一致
- guardian_phone 采用 AES-256 加密存储，返回前端时脱敏
- `notes` / `basics` 字段入库前进行 HTML 转义，防止 XSS
- 操作写入 `audit_log`，记录 before/after JSON 与 IP

---

## 9. 跨 US 依赖

- 依赖 US-012 完成后教练才能登录教练端并具备 status=1
- 依赖 US-005 用户基础资料存在
- 本 US 产生 `coach_student_profile` 数据，供 US-041 管理员处理教练离职时参考学员清单与处理结果

---

## 10. 接口测试映射

| 场景 | 测试方法 | 层级 |
|------|---------|------|
| 正常更新成人学员 | `test_update_adult_student_profile_success` | 集成 |
| 正常更新未成年人 | `test_update_minor_student_profile_success` | 集成 |
| 未成年人缺监护人 | `test_update_minor_missing_guardian` | 单元/集成 |
| 手机号格式非法 | `test_update_invalid_guardian_phone` | 单元 |
| 非关联学员 | `test_update_non_associated_student_forbidden` | 集成 |
| 并发保存幂等 | `test_update_profile_idempotent` | 集成 |
| XSS 转义 | `test_notes_xss_escaped` | 单元 |
