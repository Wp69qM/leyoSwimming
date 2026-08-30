# US-010 教练提交入驻资料 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-08-05

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 新增/修改 | 教练生效资料与生命周期状态主表；pending 期间生效资料不变 |
| `coach_application` | 新增 | 入驻/重新入驻/编辑申请快照表，保存每次提交/草稿的完整资料副本 |
| `coach_certificate_application` | 新增 | 申请快照关联证书图片 |
| `coach_certificate` | 只读 | 已生效证书（仅审核通过后才写入）|
| `coach_audit_log` | 新增 | 状态变更事件日志，记录 submit/approve/reject/draft_save |

### 1.2 字段定义

**coach 表（生效资料 + 生命周期）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `coach_id` | BIGINT | PK | 教练 ID |
| `openid` | VARCHAR(64) | UK | 微信登录 openid |
| `union_id` | VARCHAR(64) | UK | 微信 unionid |
| `phone` | VARCHAR(16) | UK | 手机号，登录后写入 |
| `name` | VARCHAR(32) | 可空 | 姓名/昵称，1-32 字符；审核通过后写入 |
| `gender` | TINYINT | 可空 | 性别：1=男 / 2=女 |
| `age` | INT | 可空 | 年龄，18-80 |
| `email` | VARCHAR(128) | 可空 | 邮箱 |
| `wechat_qr_url` | VARCHAR(512) | 可空 | 微信二维码图片 URL |
| `id_card_no` | VARCHAR(64) | 可空 | 18 位中国大陆身份证号，AES 加密 |
| `teaching_years` | INT | 可空 | 任教年限，0-60 |
| `total_students` | INT | 可空 | 总学员数，0-99999 |
| `total_hours` | INT | 可空 | 总课时数，0-99999 |
| `teaching_strokes` | JSON / VARCHAR(128) | 可空 | 擅长泳姿 |
| `bio` | VARCHAR(500) | 可空 | 个人简介，10-500 字符 |
| `reference_price` | DECIMAL(10,2) | 可空 | 参考单价（元/节），50-2000 |
| `status` | TINYINT | 默认 -1 | -1=未提交，0=待审核，1=已通过，2=已驳回，3=已离职，4=申请离职中 |
| `submitted_at` | DATETIME | 可空 | 当前 pending application 的提交时间 |
| `approved_at` | DATETIME | 可空 | 最近一次审核通过时间 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | 默认 CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**coach_application 表（申请快照）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `application_id` | BIGINT | PK | 申请快照 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `status` | ENUM | 非空 | draft / pending / approved / rejected |
| `previous_coach_status` | TINYINT | 可空 | 提交前 coach.status（-1/2/3）；draft 时为空 |
| `name` | VARCHAR(32) | 可空 | 姓名/昵称快照 |
| `gender` | TINYINT | 可空 | 性别快照 |
| `age` | INT | 可空 | 年龄快照 |
| `email` | VARCHAR(128) | 可空 | 邮箱快照 |
| `wechat_qr_url` | VARCHAR(512) | 可空 | 微信二维码快照 |
| `id_card_no` | VARCHAR(64) | 可空 | 身份证号快照，AES 加密 |
| `teaching_years` | INT | 可空 | 任教年限快照 |
| `total_students` | INT | 可空 | 总学员数快照 |
| `total_hours` | INT | 可空 | 总课时数快照 |
| `teaching_strokes` | JSON / VARCHAR(128) | 可空 | 擅长泳姿快照 |
| `bio` | VARCHAR(500) | 可空 | 个人简介快照 |
| `reference_price` | DECIMAL(10,2) | 可空 | 参考单价快照 |
| `submitted_at` | DATETIME | 可空 | 正式提交时间；draft 时为 NULL |
| `approved_at` | DATETIME | 可空 | 审核通过时间 |
| `approved_by` | BIGINT | 可空 | 审核管理员 ID |
| `rejection_reason` | VARCHAR(512) | 可空 | 驳回原因；rejected 时写入 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | 默认 CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**coach_certificate_application 表（快照证书）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `cert_id` | BIGINT | PK | 证书 ID |
| `application_id` | BIGINT | FK | 申请快照 ID |
| `cert_type` | ENUM | 非空 | ID_CARD_FRONT / ID_CARD_BACK / COACH_CERT / HEALTH_CERT / PORTRAIT / OTHER |
| `image_url` | VARCHAR(512) | 非空 | 图片 URL |
| `sort_order` | INT | 默认 0 | 同类型证书排序 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

**coach_certificate 表（已生效证书）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `cert_id` | BIGINT | PK | 证书 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `cert_type` | ENUM | 非空 | 证书类型 |
| `image_url` | VARCHAR(512) | 非空 | 图片 URL |
| `sort_order` | INT | 默认 0 | 排序 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

**coach_audit_log 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | 日志 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `application_id` | BIGINT | FK | 关联申请快照 ID；draft_save 可为 NULL |
| `admin_id` | BIGINT | FK | 管理员 ID；教练自身动作为 NULL |
| `action` | ENUM | 非空 | submit / approve / reject / draft_save |
| `from_status` | TINYINT | 非空 | 变更前 coach.status |
| `to_status` | TINYINT | 非空 | 变更后 coach.status |
| `reason` | VARCHAR(512) | 可空 | 原因 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

### 1.3 索引

```sql
-- 微信 openid/unionid/phone 唯一索引
CREATE UNIQUE INDEX idx_coach_openid ON coach(openid);
CREATE UNIQUE INDEX idx_coach_union_id ON coach(union_id);
CREATE UNIQUE INDEX idx_coach_phone ON coach(phone);

-- 每个教练只能有一条待审核申请
CREATE UNIQUE INDEX idx_coach_pending_application ON coach_application(coach_id) WHERE status = 'pending';

-- 查询教练最新申请/草稿
CREATE INDEX idx_coach_application_coach_status ON coach_application(coach_id, status, created_at DESC);

-- 证书查询索引
CREATE INDEX idx_coach_certificate_application_id_type ON coach_certificate_application(application_id, cert_type);
CREATE INDEX idx_coach_certificate_coach_id_type ON coach_certificate(coach_id, cert_type);
```

---

## 2. API 设计

### 2.1 接口清单

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/coach/application/submit` | POST | 提交入驻资料 |
| `/api/coach/application/save-draft` | POST | 保存草稿 |
| `/api/coach/application/detail` | POST | 查询当前入驻资料（完整字段 + 证书列表）|
| `/api/common/file/upload` | POST | 通用图片上传 |

### 2.2 POST /api/coach/application/submit

- **鉴权**：教练端登录态（需 US-051/US-054 登录态）
- **请求体**：
  ```json
  {
    "name": "张教练",
    "gender": 1,
    "age": 30,
    "email": "coach@example.com",
    "wechat_qr_url": "https://cdn.example.com/wechat-qr.png",
    "id_card_no": "110101199001011234",
    "teaching_years": 5,
    "total_students": 100,
    "total_hours": 500,
    "teaching_strokes": ["蛙泳", "自由泳"],
    "bio": "专业游泳教练，擅长少儿与成人教学。",
    "reference_price": 300.00,
    "certificates": [
      { "cert_type": "ID_CARD_FRONT", "image_url": "https://cdn.example.com/id-front.png" },
      { "cert_type": "ID_CARD_BACK", "image_url": "https://cdn.example.com/id-back.png" },
      { "cert_type": "COACH_CERT", "image_url": "https://cdn.example.com/coach-cert-1.png" },
      { "cert_type": "COACH_CERT", "image_url": "https://cdn.example.com/coach-cert-2.png" },
      { "cert_type": "HEALTH_CERT", "image_url": "https://cdn.example.com/health.png" },
      { "cert_type": "PORTRAIT", "image_url": "https://cdn.example.com/portrait.png" }
    ]
  }
  ```
- **响应体 200**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "application_id": 10001,
      "status": 0,
      "submitted_at": "2026-08-05T14:30:00+08:00"
    }
  }
  ```
- **错误码**：
  - `COACH_APPLICATION_PENDING`（400401）：已存在 pending 状态的 coach_application，禁止重复提交
  - `INVALID_REFERENCE_PRICE`（400402）：参考单价超出 50-2000 范围
  - `MISSING_REQUIRED_FIELDS`（400403）：必填字段或必填资质缺失
  - `INVALID_ID_CARD`（400404）：身份证号格式不合法
  - `IMAGE_TOO_LARGE`（400405）：单张图片超过 5MB
  - `INVALID_IMAGE_FORMAT`（400406）：图片格式非 JPG/PNG

- **说明**：
  - 校验通过后创建/复用 `coach_application` 快照，`status = pending`，记录 `previous_coach_status` 与 `submitted_at`。
  - 更新 `coach.status = 0`、`coach.submitted_at = now`；coach 表生效资料此时**不更新**，等待 US-011 审核通过后再覆盖。
  - 写入 `coach_audit_log`：`action='submit'`、`from_status=previous_coach_status`、`to_status=0`。

### 2.3 POST /api/coach/application/save-draft

- **鉴权**：教练端登录态
- **请求体**：同 POST /api/coach/application/submit（允许部分字段）
- **响应体 200**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "application_id": 10001,
      "status": "draft",
      "submitted_at": null
    }
  }
  ```
- **说明**：
  - 保存草稿仅创建或更新 `coach_application` 快照，`status = draft`，不修改 `coach.status`。
  - `submitted_at = NULL` 表示草稿；US-011 审核列表仅查询 `coach_application.status = pending` 的记录。
  - `coach.status = -1/2/3` 的教练保存草稿时，coach.status 均保持不变；草稿数据仅写入 coach_application 快照。
  - `coach.status = 3` 的教练重新入驻时复用原 coach 记录，历史数据不回滚、不隔离。

### 2.4 POST /api/coach/application/detail

- **鉴权**：教练端登录态
- **响应体 200**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "application_id": 10001,
      "name": "张教练",
      "phone": "13800138000",
      "gender": 1,
      "age": 30,
      "email": "coach@example.com",
      "wechat_qr_url": "https://cdn.example.com/wechat-qr.png",
      "id_card_no": "110101********1234",
      "teaching_years": 5,
      "total_students": 100,
      "total_hours": 500,
      "teaching_strokes": ["蛙泳", "自由泳"],
      "bio": "专业游泳教练，擅长少儿与成人教学。",
      "reference_price": 300.00,
      "status": 0,
      "submitted_at": "2026-08-05T14:30:00+08:00",
      "entry_type": "first",
      "prompt_message": null,
      "certificates": [
        { "cert_id": 1, "cert_type": "ID_CARD_FRONT", "image_url": "..." },
        { "cert_id": 2, "cert_type": "ID_CARD_BACK", "image_url": "..." },
        { "cert_id": 3, "cert_type": "COACH_CERT", "image_url": "..." },
        { "cert_id": 4, "cert_type": "HEALTH_CERT", "image_url": "..." },
        { "cert_id": 5, "cert_type": "PORTRAIT", "image_url": "..." }
      ]
    }
  }
  ```
- **响应 404**：`NO_APPLICATION`（尚未创建 coach 记录）
- **说明**：
  - 返回数据来自**最新 coach_application 快照**（draft/pending/rejected）；审核通过后 coach 表生效资料由 US-011 覆盖写入。
  - 身份证号脱敏展示；完整图片 URL 用于等待审核页查看。
  - `entry_type` 枚举：`draft`（status=draft）、`first`（status=pending 且 previous_coach_status=-1）、`rejected`（coach.status=2，最新 application 为 rejected）、`reapply`（coach.status=3，最新 application 为 rejected 或 pending previous_coach_status=3）。
  - `prompt_message`：coach.status=2 时返回最新 rejected application 的 `rejection_reason`；coach.status=3 时返回固定重新入驻说明文案；其他状态返回 `null`。

### 2.5 POST /api/common/file/upload

- **鉴权**：教练端登录态
- **请求体**：`multipart/form-data`，字段名 `file`
- **约束**：文件大小 ≤ 5MB；格式 JPG/PNG（通过 magic number + 后缀校验）
- **响应体 200**：
  ```json
  {
    "code": 0,
    "data": {
      "url": "https://cdn.example.com/upload/xxx.png"
    }
  }
  ```
- **错误码**：
  - `IMAGE_TOO_LARGE`（400405）
  - `INVALID_IMAGE_FORMAT`（400406）

---

## 3. 状态机

### 3.1 coach.status（生命周期状态）

```
-1 未提交 ──[保存草稿]──→ -1 未提交（coach_application.status = draft）
-1 未提交 ──[提交审核]──→ 0 待审核（coach_application.status = pending, previous_coach_status=-1）
  2 已驳回 ──[保存草稿]──→ 2 已驳回（coach_application.status = draft）
  2 已驳回 ──[重新提交]──→ 0 待审核（coach_application.status = pending, previous_coach_status=2）
  3 已离职 ──[保存草稿]──→ 3 已离职（coach_application.status = draft）
  3 已离职 ──[重新入驻提交]──→ 0 待审核（coach_application.status = pending, previous_coach_status=3）

  0 待审核 ──[管理员通过]──→ 1 已通过（coach_application 快照覆盖 coach 生效资料）
  0 待审核 ──[管理员驳回]──→ previous_coach_status
            ├── previous_coach_status=-1 → 2 已驳回
            ├── previous_coach_status=2  → 2 已驳回
            └── previous_coach_status=3  → 3 已离职
```

### 3.2 coach_application.status（快照状态）

```
draft 草稿 ──[提交审核]──→ pending 待审核
pending 待审核 ──[管理员通过]──→ approved 已通过
pending 待审核 ──[管理员驳回]──→ rejected 已驳回
```

- 本 US 触发 coach.status 的初始转换（-1 → 0）、驳回后重新提交转换（2 → 0）与已离职重新入驻转换（3 → 0）。保存草稿不触发 coach.status 转换。
- US-011 触发审核结果：coach.status 0 → 1（通过）或 0 → previous_coach_status（驳回）；coach_application.status pending → approved/rejected。
- pending 期间 coach 表生效资料保持不变；审核通过后将 coach_application 快照字段覆盖写入 coach 表及 coach_certificate 表。
- `status = 3` 的教练重新入驻时复用原 coach 记录，历史数据不回滚、不隔离。

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 教练申请状态 | `coach:application:{openid}` | 5 分钟 | 减少等待审核页重复查询 |
| 上传图片临时链接 | `upload:temp:{file_key}` | 1 小时 | 图片上传后暂存，提交成功后失效 |

---

## 5. 性能与安全

### 5.1 性能

- 提交接口 P99 < 500ms（不含图片上传，图片上传单独接口）
- 状态查询 P99 < 100ms
- 图片上传 P99 < 2s
- 并发 50 QPS P99 < 1s

### 5.2 安全

- 所有接口必须教练端登录鉴权
- `id_card_no` 后端 AES 加密存储；返回给前端时脱敏展示
- 图片上传校验格式（JPG/PNG）与大小（≤5MB）
- 防重复提交（数据库唯一索引 + 幂等键 `coach_apply:{openid}:{timestamp}`）
- 参考单价、任教年限、总学员数、总课时数等敏感/计费字段必须后端二次校验
- 防止 XSS/SQL 注入：使用参数化查询，图片 URL 做白名单校验

---

## 6. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-009 | 依赖 | 教练需先同意隐私协议与用户须知 |
| US-051 | 依赖 | 教练端微信授权登录态 |
| US-011 | 被依赖 | 产生待审核数据供管理员审核 |
| US-012 / US-013 / US-014 | 被依赖 | 审核通过后解锁教练端功能 |
| US-040 | 被依赖 | 已离职教练重新入驻复用本 US 接口与页面 |

---

## 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-30 | 初版 |
| v1.1 | 2026-07-31 | 新增 `submitted_at` 字段，区分草稿与已提交 |
| v2.1 | 2026-08-05 | 已离职教练（status=3）重新入驻与 US-010 合并；POST /api/coach/application/detail 响应新增 `entry_type` 与 `prompt_message`；状态机增加 3 → 0 转换；明确历史数据不复用隔离 |
| v2.0 | 2026-08-05 | 按字段设计补全 coach/coach_certificate 全部字段；新增身份证、图片、参考单价等错误码；明确 GET 接口返回完整资料；新增 `cert_type` 枚举与状态机 -1 初始态 |
