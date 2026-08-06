# US-054 教练手机号验证码登录 — 技术设计

> **状态**：待填充（本 US 进入 TDD 实现阶段前由开发补充）
> **对应 user-story**：[./user-story.md](./user-story.md)

---

## 1. 概述

待补充：本节概述本 US 的技术目标、涉及范围、关键设计决策。

## 2. 数据模型

待补充：`coach` 表、`coach_session` 表、`sms_code` 表、`coach_login_log` 表的字段、索引、约束。

## 3. API 设计

待补充：
- `POST /api/v1/auth/coach/sms/code` — 发送登录验证码
- `POST /api/v1/auth/coach/login/phone` — 教练手机号验证码登录

## 4. 状态机

待补充：`coach.status` 在本 US 中的读取逻辑与分流规则。

## 5. 安全与性能

待补充：验证码限流、手机号加密存储、JWT 签发与刷新、幂等性、接口响应时间目标。

## 6. 跨 US 依赖

- US-051：复用教练端登录页、会话管理、`coach` 表设计
- US-009：复用隐私协议与用户须知校验逻辑
