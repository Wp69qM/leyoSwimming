# US-010 教练提交入驻资料 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-08-05

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 新增/修改 | 教练主表，含微信登录信息、实名资质、教学履历、服务设置与审核状态 |
| `coach_certificate` | 新增/修改 | 证书图片，按 `cert_type` 区分类型 |
| `coach_audit_log` | 新增 | 审核日志，记录 submit/approve/reject 动作（本 US 触发 submit）|

### 1.2 字段定义

**coach 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `coach_id` | BIGINT | PK | 教练 ID |
| `openid` | VARCHAR(64) | UK | 微信登录 openid |
| `union_id` | VARCHAR(64) | UK | 微信 unionid |
| `phone` | VARCHAR(16) | UK | 微信解密手机号，脱敏展示 |
| `name` | VARCHAR(32) | 非空 | 姓名/昵称，1-32 字符 |
| `avatar_url` | VARCHAR(512) | 可空 | 微信头像 URL，80×80 圆形展示 |
| `id_card_no` | VARCHAR(64) | 非空 | 18 位中国大陆身份证号，后端 AES 加密存储 |
| `teaching_years` | INT | 非空 | 任教年限，0-60 |
| `total_students` | INT | 非空 | 总学员数，0-99999 |
| `total_hours` | INT | 非空 | 总课时数，0-99999 |
| `teaching_strokes` | JSON / VARCHAR(128) | 可空 | 擅长泳姿，多选：蛙泳/自由泳/仰泳/蝶泳 |
| `bio` | VARCHAR(500) | 非空 | 个人简介，10-500 字符 |
| `reference_price` | DECIMAL(10,2) | 非空 | 参考单价（元/节），50-2000 |
| `status` | TINYINT | 默认 -1 | -1=未提交入驻资料，0=待审核，1=已通过，2=已驳回，3=已离职，4=申请离职中 |
| `submitted_at` | DATETIME | 可空 | 正式提交审核时间；NULL 表示草稿 |
| `approved_at` | DATETIME | 可空 | 审核通过时间 |
| `rejection_reason` | VARCHAR(255) | 可空 | 驳回原因；重新提交时清空 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | DATETIME | 默认 CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**coach_certificate 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `cert_id` | BIGINT | PK | 证书 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `cert_type` | ENUM | 非空 | ID_CARD_FRONT / ID_CARD_BACK / COACH_CERT / HEALTH_CERT / PORTRAIT / OTHER |
| `image_url` | VARCHAR(512) | 非空 | 图片 URL |
| `sort_order` | INT | 默认 0 | 同类型证书排序 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

**coach_audit_log 表（保持不变）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | 日志 ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `action` | ENUM | 非空 | submit / approve / reject |
| `operator_id` | BIGINT | 可空 | 操作人 ID（系统触发为 NULL）|
| `remark` | VARCHAR(255) | 可空 | 备注（如驳回原因）|
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 创建时间 |

### 1.3 索引

```sql
-- 微信 openid/unionid/phone 唯一索引
CREATE UNIQUE INDEX idx_coach_openid ON coach(openid);
CREATE UNIQUE INDEX idx_coach_union_id ON coach(union_id);
CREATE UNIQUE INDEX idx_coach_phone ON coach(phone);

-- 待审核/已通过/已驳回记录的唯一性，用于防重复提交
CREATE UNIQUE INDEX idx_coach_active_application ON coach(openid) WHERE status IN (-1, 0, 1, 2);

-- 证书查询索引
CREATE INDEX idx_coach_certificate_coach_id_type ON coach_certificate(coach_id, cert_type);
```

---

## 2. API 设计

### 2.1 接口清单

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/coach/application` | POST | 提交入驻资料 |
| `/api/coach/application/draft` | PUT | 保存草稿 |
| `/api/coach/application` | GET | 查询当前入驻资料（完整字段 + 证书列表）|
| `/api/upload/image` | POST | 通用图片上传 |

### 2.2 POST /api/coach/application

- **鉴权**：教练端登录态（需 US-051 登录态）
- **请求体**：
  ```json
  {
    "name": "张教练",
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
      "status": 0,
      "submitted_at": "2026-08-05T14:30:00+08:00"
    }
  }
  ```
- **错误码**：
  - `COACH_APPLICATION_PENDING`（400401）：已存在待审核/已通过记录，禁止重复提交
  - `INVALID_REFERENCE_PRICE`（400402）：参考单价超出 50-2000 范围
  - `MISSING_REQUIRED_FIELDS`（400403）：必填字段或必填资质缺失
  - `INVALID_ID_CARD`（400404）：身份证号格式不合法
  - `IMAGE_TOO_LARGE`（400405）：单张图片超过 5MB
  - `INVALID_IMAGE_FORMAT`（400406）：图片格式非 JPG/PNG

### 2.3 PUT /api/coach/application/draft

- **鉴权**：教练端登录态
- **请求体**：同 POST /api/coach/application（允许部分字段）
- **响应体 200**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "status": 0,
      "submitted_at": null
    }
  }
  ```
- **说明**：
  - 保存草稿与提交审核均写入 `status = 0`，不新增独立草稿态。
  - `submitted_at = NULL` 表示草稿，US-011 审核列表必须过滤 `submitted_at IS NOT NULL` 的记录。
  - `status = -1` 的教练首次保存草稿后，`status` 变为 0 但 `submitted_at` 为空。
  - `status = 2` 的教练保存草稿或重新提交时，`status` 重置为 0，并清空 `rejection_reason`。

### 2.4 GET /api/coach/application

- **鉴权**：教练端登录态
- **响应体 200**：
  ```json
  {
    "code": 0,
    "data": {
      "coach_id": 20001,
      "name": "张教练",
      "avatar_url": "https://cdn.example.com/avatar.png",
      "phone": "138****8888",
      "id_card_no": "110101********1234",
      "teaching_years": 5,
      "total_students": 100,
      "total_hours": 500,
      "teaching_strokes": ["蛙泳", "自由泳"],
      "bio": "专业游泳教练，擅长少儿与成人教学。",
      "reference_price": 300.00,
      "status": 0,
      "submitted_at": "2026-08-05T14:30:00+08:00",
      "rejection_reason": null,
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
- **说明**：返回手机号与身份证号均为脱敏展示；完整图片 URL 用于等待审核页查看。

### 2.5 POST /api/upload/image

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

```
-1 未提交 ──[保存草稿]──→ 0 待审核（submitted_at = NULL）
-1 未提交 ──[提交审核]──→ 0 待审核（submitted_at = now）
  2 已驳回 ──[重新提交]──→ 0 待审核（submitted_at = now，rejection_reason = NULL）
```

- 本 US 触发初始转换（-1 → 0）与驳回后重新提交转换（2 → 0）。
- 后续 US-011 触发：0 → 1（通过）或 0 → 2（驳回）。

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
| v2.0 | 2026-08-05 | 按字段设计补全 coach/coach_certificate 全部字段；新增身份证、图片、参考单价等错误码；明确 GET 接口返回完整资料；新增 `cert_type` 枚举与状态机 -1 初始态 |
