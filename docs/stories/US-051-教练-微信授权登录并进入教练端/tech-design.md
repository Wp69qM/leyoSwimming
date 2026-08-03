# US-051 教练微信授权登录并进入教练端 — 技术设计文档

> **状态**：待开发填写
> **对应用户故事**：[./user-story.md](./user-story.md)
> **最后更新**：2026-08-03

---

## 1. 概述

本文档承接 [user-story.md](./user-story.md)，聚焦教练端微信授权登录的技术实现：
- 如何复用/扩展用户端微信 OAuth 接口以支持 `app_type=coach`
- 如何根据 `coach.status` 计算 `redirect_page`
- 登录成功后如何前置校验 US-009 隐私协议同意状态
- 新增 `/api/v1/coach/me/status` 查询接口的设计
- JWT 签发、session 管理、幂等、安全细节

> **隐私协议前置**：教练端首次登录成功后，前端需调用 US-009 的隐私协议状态接口（`/api/user/privacy/status`），未同意当前生效版本时跳转隐私协议页，同意后再按 `redirect_page` 进入目标页面。

---

## 2. 数据模型

### 2.1 表/字段变更

| # | 表名 | 操作 | 字段/说明 |
|---|------|------|----------|
| 1 | `user` | 读取/新增 | 按 `union_id` 查询；首次登录时插入，字段同 US-004 |
| 2 | `coach` | 读取 | 按 `union_id` 查询 `status`、`rejection_reason` |
| 3 | `user_session` | 新增 | 登录成功后写入会话 |

### 2.2 索引

- `user(union_id)` 唯一索引（已存在）
- `coach(user_id)` 或 `coach(union_id)` 索引（需确认现有设计）

---

## 3. API 设计

### 3.1 改造接口：`POST /api/v1/auth/wechat-login`

#### 请求参数

```json
{
  "code": "string",
  "app_type": "coach"  // 新增，枚举：user | coach，默认 user
}
```

#### 响应字段（新增/变更）

```json
{
  "access_token": "string",
  "refresh_token": "string",
  "is_new_user": false,
  "profile_completed": false,
  "coach_status": null,        // null | 0 | 1 | 2 | 3 | 4
  "redirect_page": "coach_onboarding"  // 见 user-story §4.1
}
```

### 3.2 新增接口：`GET /api/v1/coach/me/status`

#### 响应字段

```json
{
  "coach_status": 1,
  "rejection_reason": null,
  "redirect_page": "coach_home"
}
```

---

## 4. 状态机映射

| coach.status | redirect_page | 目标页面 |
|-------------|---------------|---------|
| null（无记录） | coach_onboarding | 入驻资料页（US-010） |
| 0 | coach_pending | 等待审核页 |
| 1 | coach_home | 教练首页 |
| 2 | coach_rejected | 重新提交入驻页（US-040） |
| 3 | coach_resigned | 重新入驻页（US-040） |
| 4 | coach_resigning | 离职处理中页 |

---

## 5. 安全与性能

- `session_key` 不入响应
- `app_type` 白名单校验
- `code` 幂等 5 分钟
- P99 < 1500ms

---

## 6. 待补充

- 字段级校验规则
- 详细序列图
- 测试桩/mock 策略
- 错误码完整列表
