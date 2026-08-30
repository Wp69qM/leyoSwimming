# US-045 技术设计：管理员配置标准与自定义套餐

> 本文档对应 `docs/stories/US-045-.../user-story.md` 的技术实现方案。
> 角色：开发 | 最后更新：2026-07-30

---

## 0. 文档定位

本文档为设计层，回答「用什么数据模型 / API / 状态机 / 缓存实现管理员套餐配置」。具体执行步骤见 [./test-plan.md](./test-plan.md)。

---

## 1. 数据模型影响

### 1.1 新增/修改的表

| 表名 | 操作 | 说明 |
|------|------|------|
| `package_template` | 新增 | 标准套餐模板，字段见下 |
| `package_template_coach` | 新增 | 标准套餐与教练多对多关联表，字段见下 |
| `custom_package_config` | 新增 | 自定义套餐全局配置（允许课时范围 min/max、默认有效期）|
| `coach` | 读取 | 关联教练与 `reference_price` |

### 1.2 `package_template` 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | AUTO_INCREMENT | 模板 ID |
| `name` | VARCHAR(64) | UK, NOT NULL | 套餐名称 |
| `package_mode` | VARCHAR(20) | NOT NULL | 套餐模式：standard / experience |
| `teaching_type` | VARCHAR(20) | NOT NULL | 教学类型/班级规模：one_on_one / one_on_two / one_on_three |
| `stroke_ids` | JSON | — | 泳姿 ID 数组，空数组表示全部泳姿 |
| `total_hours` | INT | CHECK > 0 | 课时数 |
| `duration_minutes` | INT | CHECK > 0 | 每节课时长（分钟）|
| `valid_days` | INT | CHECK > 0 | 有效期天数 |
| `original_price` | DECIMAL(10,2) | CHECK >= 0 | 原价 |
| `price` | DECIMAL(10,2) | CHECK >= 0 | 售价 |
| `refund_enabled` | TINYINT(1) | NOT NULL | 是否支持退款 |
| `refund_ratio` | DECIMAL(3,2) | CHECK >= 0 | 退款比例（0~1）|
| `refund_valid_days` | INT | CHECK >= 0 | 退款有效期限制（天）|
| `tags` | JSON | — | 标签数组 |
| `description` | TEXT | — | 套餐描述（富文本）|
| `images` | JSON | — | 套餐展示图片 URL 数组 |
| `status` | VARCHAR(16) | NOT NULL | 上下架：inactive / active |
| `created_at` | DATETIME | — | 创建时间 |
| `updated_at` | DATETIME | — | 更新时间 |

### 1.3 `package_template_coach` 字段

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT PK | AUTO_INCREMENT | 关联 ID |
| `package_template_id` | BIGINT FK | IDX, NOT NULL | 套餐模板 ID |
| `coach_id` | BIGINT FK | IDX, NOT NULL | 教练 ID |
| `reference_price_snapshot` | DECIMAL(10,2) | CHECK >= 0 | 保存时该教练的参考单价快照 |
| `created_at` | DATETIME | — | 创建时间 |
| `updated_at` | DATETIME | — | 更新时间 |

### 1.4 索引

```sql
CREATE UNIQUE INDEX idx_package_template_name ON package_template(name);
CREATE UNIQUE INDEX idx_package_template_coach_unique ON package_template_coach(package_template_id, coach_id);
CREATE INDEX idx_package_template_coach_template ON package_template_coach(package_template_id);
CREATE INDEX idx_package_template_coach_coach ON package_template_coach(coach_id);
```

---

## 2. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 2.1 POST /api/admin/package-template/list

- **鉴权**：管理员登录 + `package:read` 权限
- **Request**：
  ```json
  {
    "page": 1,
    "pageSize": 20,
    "coachId": 1,
    "status": "active"
  }
  ```
  - `coachId` 用于筛选包含指定教练的模板
- **Response 200**：`{ items: PackageTemplate[], total, page, pageSize }`
- **Response 403**：`{ error: 'FORBIDDEN' }`

### 2.2 POST /api/admin/package-template/add

- **鉴权**：管理员登录 + `package:write` 权限
- **幂等性**：`Idempotency-Key` 请求头
- **Request**：
  ```json
  {
    "name": "标准 6 节",
    "coachIds": [1, 2],
    "totalHours": 6,
    "validDays": 90,
    "price": 108000,
    "status": "active",
    "teachingType": "one_on_one",
    "durationMinutes": 60
  }
  ```
- **业务规则**：`coachIds` 必填且至少包含 1 个教练 ID；保存时同步写入 `package_template_coach` 关联表并记录 `referencePriceSnapshot`
- **Response 201**：创建后的模板对象
- **Response 400**：`{ error: 'INVALID_PACKAGE_PARAM' }`
- **Response 409**：`{ error: 'DUPLICATE_PACKAGE_NAME' }`

### 2.3 POST /api/admin/package-template/update

- **鉴权**：管理员登录 + `package:write` 权限
- **Request**：
  ```json
  {
    "packageTemplateId": 1,
    "name": "标准 6 节",
    "coachIds": [1, 2],
    "totalHours": 6,
    "validDays": 90,
    "price": 108000,
    "status": "active",
    "teachingType": "one_on_one",
    "durationMinutes": 60
  }
  ```
- **业务规则**：`coachIds` 必填且至少包含 1 个教练 ID；保存时按新集合覆盖 `package_template_coach` 关联表
- **Response 200**：更新后的模板对象
- **Response 404**：`{ error: 'TEMPLATE_NOT_FOUND' }`

### 2.4 POST /api/admin/package-template/toggle-status

- **鉴权**：管理员登录 + `package:write` 权限
- **Request**：
  ```json
  {
    "packageTemplateId": 1
  }
  ```
- **业务规则**：在 `active` 与 `inactive` 之间切换
- **Response 200**：更新后的模板对象

### 2.5 POST /api/admin/package-template/detail

- **鉴权**：管理员登录 + `package:read` 权限
- **Request**：
  ```json
  {
    "packageTemplateId": 1
  }
  ```
- **Response 200**：模板详情对象（含 `coachIds`）
- **Response 404**：`{ error: 'TEMPLATE_NOT_FOUND' }`

### 2.6 POST /api/admin/package-template/custom-config

- **鉴权**：管理员登录 + `package:write` 权限
- **Request**：
  ```json
  {
    "minHours": 1,
    "maxHours": 50,
    "defaultValidDays": 60
  }
  ```
- **Response 200**：更新后的全局配置对象
- **Response 400**：`{ error: 'INVALID_PACKAGE_PARAM' }`

### 2.7 POST /api/admin/image/upload

- **鉴权**：管理员登录 + `package:write` 权限
- **Content-Type**：`multipart/form-data`
- **请求参数**：`files: File[]`
- **Response 200**：`{ urls: string[] }`
- **Response 400**：`{ error: 'INVALID_IMAGE' }`

---

## 3. 状态机影响

```
package_template.status:
  active ──[管理员下架]──→ inactive
  inactive ──[管理员上架]──→ active
```

- 仅影响新购订单对模板的可见性
- 不影响已生成的 `package` 记录

---

## 4. 缓存策略

| 层 | Key | TTL | 失效策略 |
|----|-----|-----|---------|
| Redis | `package:templates:active` | 300s | 模板变更时主动失效 |
| Redis | `package:template:{id}` | 300s | 模板变更时主动失效 |
| 前端 | web-admin 本地状态 | 会话级 | 列表刷新时更新 |

### 4.1 降级策略

- Redis 不可用时直接查 DB，返回 200 但记录 warning 日志

---

## 5. 性能指标

| 指标 | 目标 |
|------|------|
| 列表接口 P50 | < 150ms |
| 列表接口 P99 | < 300ms |
| 创建/更新接口 P99 | < 200ms |
| 并发 100 QPS | 无 5xx |

---

## 6. 安全 / 鉴权

- 所有接口必须登录且具备 `package:read` / `package:write` 权限
- 敏感参数 `price` 必须 ≥ 0
- 防 ID 遍历：无权限统一返回 403
- 操作日志记录管理员 ID、IP、变更前后快照

---

## 7. 跨 US 依赖

| US | 依赖方向 | 说明 |
|----|---------|------|
| US-010 / US-011 | 被依赖 | 教练数据与审核状态 |
| US-019 | 依赖本 US | 学员浏览正价套餐读取模板 |
| US-020 | 依赖本 US | 学员购买正价套餐读取模板 |

---

## 8. 异常与边界

| 场景 | 处理 |
|------|------|
| 名称重复 | 409 + DUPLICATE_PACKAGE_NAME |
| 课时数/价格非法 | 400 + INVALID_PACKAGE_PARAM |
| 适用教练为空数组 | 400 + INVALID_PACKAGE_PARAM |
| 无权限 | 403 + FORBIDDEN |
| 模板不存在 | 404 + TEMPLATE_NOT_FOUND |
| 已产生订单的模板删除 | 禁止物理删除，仅允许 inactive |
| 并发编辑冲突 | 乐观锁 version 字段，冲突返回 409 + CONCURRENT_MODIFY |

---

## 9. 实现顺序（与 test-plan 的映射）

| tech-design 章节 | 对应 test-plan Task |
|------------------|---------------------|
| §1 数据模型 | Task 1, Task 2 |
| §2 API 设计 | Task 3, Task 4, Task 5 |
| §4 缓存策略 | Task 6 |
| §6 安全 | Task 7 |

---

## 10. 上下游引用

- 上游需求：[./user-story.md](./user-story.md)
- 执行计划：[./test-plan.md](./test-plan.md)
- 全局规范：[docs/spec/tech-design/README.md](../../spec/tech-design/README.md)

---

## 11. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.1 | 2026-08-13 | Dev | 适用教练由单一 `coach_id` 改为多对多关联表 `package_template_coach`；API Body 从 `coach_id` 改为 `coach_ids: number[]`；§1、§2、§8 同步调整 |
| v1.0 | 2026-07-30 | Dev | 初版 |
