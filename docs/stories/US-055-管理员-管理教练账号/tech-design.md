# US-055 管理员管理教练账号 — 技术设计

> 状态：[REVIEW]
> 最后更新：2026-08-07

## 1. 需求概述

本 US 支撑管理后台「用户管理 → 教练管理」模块，提供教练账号的列表查询、详情查看、资料编辑、新建教练、取消入驻等能力。所有接口遵循 [api-convention.md](../../../docs/tech/api-convention.md) 的 RPC over HTTP 规范。

## 2. 数据模型

### 2.1 涉及表

- `coach`：教练生效资料主表
- `coach_certificate`：教练资质证书表
- `coach_application`：历史入驻申请快照表（仅读取，用于详情页展示历史）
- `coach_audit_log`：教练相关操作审计日志

### 2.2 coach 表关键字段

| 字段 | 说明 |
|------|------|
| `id` | 教练 ID |
| `phone` | 手机号，唯一 |
| `name` | 姓名/昵称 |
| `gender` | 性别 |
| `age` | 年龄 |
| `email` | 邮箱 |
| `id_card_no` | 身份证号，加密存储，唯一 |
| `wechat_qr_url` | 微信二维码图片 URL |
| `teaching_years` | 任教年限 |
| `total_students` | 总学员数 |
| `total_hours` | 总课时数 |
| `teaching_strokes` | 擅长泳姿，JSON 数组 |
| `bio` | 个人简介 |
| `reference_price` | 参考单价 |
| `status` | 0=待审核 1=已通过 2=已驳回 3=已离职 4=申请离职中 |
| `approved_at` | 审批通过/创建时间 |
| `created_at` / `updated_at` | 创建/更新时间 |

### 2.3 coach_certificate 表关键字段

| 字段 | 说明 |
|------|------|
| `coach_id` | 教练 ID |
| `cert_type` | ID_CARD_FRONT / ID_CARD_BACK / QUALIFICATION / HEALTH / PORTRAIT |
| `image_url` | 图片 URL |
| `created_at` / `updated_at` | 创建/更新时间 |

## 3. 接口设计

> 统一使用 POST，URL 按动作命名，参数放 JSON body。

### 3.1 POST /api/admin/coach/list

- **权限**：`COACH:READ`
- **请求体**：
  ```json
  {
    "page": 1,
    "pageSize": 20,
    "keyword": "",
    "status": "all",
    "realtimeStatus": "all"
  }
  ```
- **响应**：
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "total": 100,
      "list": [
        {
          "coachId": 20001,
          "name": "张教练",
          "gender": "男",
          "age": 30,
          "teachingYears": 5,
          "teachingStrokes": ["自由泳", "蛙泳"],
          "approvedAt": "2026-08-01T10:00:00+08:00",
          "status": 1,
          "realtimeStatus": "空闲中"
        }
      ]
    }
  }
  ```

### 3.2 POST /api/admin/coach/detail

- **权限**：`COACH:READ`
- **请求体**：`{ "coachId": 20001 }`
- **响应**：返回 coach 生效资料、证书列表、历史申请记录、操作审计日志

### 3.3 POST /api/admin/coach/add

- **权限**：`COACH:WRITE`
- **请求体**：完整教练入驻字段
- **业务逻辑**：
  - 校验手机号/身份证号唯一性
  - 直接写入 `coach.status = 1`，`approved_at = now`
  - 写入证书到 `coach_certificate`
  - 写入 `coach_audit_log` action='ADMIN_CREATE_COACH'

### 3.4 POST /api/admin/coach/update

- **权限**：`COACH:WRITE`
- **请求体**：`{ "coachId": 20001, "profile": { ... }, "certificates": [ ... ] }`
- **业务逻辑**：
  - 校验目标 coach 存在
  - 校验手机号/身份证号唯一性（排除自身）
  - 更新 coach 表字段
  - 全量覆盖/更新证书
  - 写入 `coach_audit_log` action='ADMIN_UPDATE_COACH_PROFILE'

### 3.5 POST /api/admin/coach/cancelEntry

- **权限**：`COACH:CANCEL_ENTRY`
- **请求体**：`{ "coachId": 20001, "reason": "主动离职" }`
- **业务逻辑**：
  - 校验 coach.status = 1
  - 更新 coach.status = 3
  - 写入 `coach_audit_log` action='ADMIN_CANCEL_COACH_ENTRY'
  - 触发 US-041 教练离职后续处理（清空可约时段、通知学员等）

## 4. 安全与审计

- 所有写操作记录 `coach_audit_log`
- 手机号、身份证号等敏感字段按需脱敏/加密
- 管理员权限通过 RBAC 校验

## 5. 错误码

| 错误码 | HTTP 状态码 | 说明 |
|--------|------------|------|
| `ADMIN_PERMISSION_DENIED` | 403 | 无权限 |
| `COACH_NOT_FOUND` | 404 | 教练不存在 |
| `PHONE_ALREADY_EXISTS` | 409 | 手机号已存在 |
| `ID_CARD_ALREADY_EXISTS` | 409 | 身份证号已存在 |
| `COACH_STATUS_NOT_APPROVED` | 400 | 教练不处于已通过状态 |
