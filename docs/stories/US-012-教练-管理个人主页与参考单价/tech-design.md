# US-012 教练管理个人主页与参考单价 — 技术设计

> **状态**：初稿　|　**最后更新**：2026-07-30

---

## 1. 数据模型

### 1.1 涉及的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `coach` | 修改 | bio、reference_price、teaching_years、phone、wechat_qr_url |
| `coach_certificate` | 新增/修改/删除 | 证书图片 |
| `coach_update_log` | 新增 | 变更历史 |

### 1.2 字段定义

**coach 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `coach_id` | BIGINT | PK | 教练 ID |
| `bio` | TEXT | 可空 | 个人简介 |
| `teaching_years` | INT | 非空 | 任教年限 |
| `phone` | VARCHAR(16) | 可空 | 手机号 |
| `wechat_qr_url` | VARCHAR(512) | 可空 | 微信二维码 |
| `reference_price` | DECIMAL(10,2) | 非空 | 参考单价 |
| `price_changed_at` | DATETIME | 可空 | 上次改价时间 |
| `price_change_count_today` | INT | 默认 0 | 今日改价次数 |

**coach_update_log 表**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `log_id` | BIGINT | PK | ID |
| `coach_id` | BIGINT | FK | 教练 ID |
| `field_name` | VARCHAR(32) | 非空 | 变更字段 |
| `old_value` | TEXT | 可空 | 旧值 |
| `new_value` | TEXT | 可空 | 新值 |
| `created_at` | DATETIME | 默认 CURRENT_TIMESTAMP | 时间 |

---

## 2. API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/coach/profile/detail` | POST | 获取个人主页 |
| `/api/coach/profile/update` | POST | 更新个人主页 |
| `/api/coach/reference-price/update` | POST | 更新参考单价 |
| `/api/common/file/upload` | POST | 上传图片 |

### 2.1 POST /api/coach/profile/update

- **请求体**：`{ "name", "gender", "age", "email", "wechat_qr_url", "portrait_url", "teaching_years", "teaching_strokes", "bio" }`（允许部分字段）
- **响应体**：`{ "code": 0, "data": { ...更新后的 coach 字段 } }`
- **错误码**：
  - `INVALID_IMAGE_FORMAT`（400406）
  - `IMAGE_TOO_LARGE`（400405）
  - `SENSITIVE_CONTENT`（400407）
  - `COACH_STATUS_NOT_ALLOWED`（403）

### 2.2 POST /api/coach/reference-price/update

- **请求体**：
  ```json
  {
    "reference_price": 350
  }
  ```
- **响应体**：
  ```json
  {
    "code": 0,
    "data": {
      "reference_price": 350,
      "price_changed_at": "2026-07-30T12:00:00Z"
    }
  }
  ```
- **错误码**：`INVALID_REFERENCE_PRICE` (400601), `PRICE_CHANGE_LIMIT` (400602)

---

## 3. 状态机

- 不改变 coach.status，仅更新资料字段

---

## 4. 缓存策略

| 缓存 | 键 | TTL | 说明 |
|------|----|-----|------|
| 教练详情 | `coach:{coach_id}` | 立即失效 | 更新后刷新 |
| 教练列表 | `coach:list:*` | 立即失效 | 价格变化影响列表展示 |

---

## 5. 性能与安全

### 5.1 性能

- 更新接口 P99 < 300ms
- 详情查询 P99 < 100ms

### 5.2 安全

- 仅 status=1 的教练可操作
- 参考单价后端校验范围
- 敏感词过滤个人简介
- 记录变更日志

---

## 6. 跨 US 依赖

- 依赖 US-011 审核通过
- 影响 US-017/US-020 套餐定价
- 与 US-038 分享主页相关
