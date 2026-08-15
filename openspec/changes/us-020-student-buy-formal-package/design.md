# Design: US-020 学员购买正价套餐

## Overview

正价套餐购买流程：
- **标准套餐**：套餐详情页 → 选择/确认教练 → 确认订单页 → 支付页（US-025）。
- **自定义套餐**：套餐详情页 → 选择/确认教练 → 自定义配置页 → 支付页（US-025）。

- **入口 A（全局套餐列表）**：用户从首页/全部套餐进入套餐列表，点击套餐卡片进入套餐详情页；详情页底部展示横向滚动的适配教练竖向卡片（头像、姓名、评分、擅长泳姿、教龄、总学员数、参考单价），用户必须选择教练后才能点击「立即购买」。
- **入口 B（教练详情页）**：用户从教练详情页点击套餐卡片进入套餐详情页；详情页底部展示当前教练横向自适应卡片（头像、姓名、评分、擅长泳姿、教龄、总学员数、参考单价），无需再次选择。
- **标准套餐**：点击「立即购买」后进入确认订单页，展示教练横向自适应卡片、套餐信息、价格、协议勾选区；用户勾选协议并点击「确认订单」后创建待支付订单，前端跳转支付页。
- **自定义套餐**：点击「立即购买」后进入自定义配置页，展示教练横向自适应卡片（含参考单价）、课时数量（默认 4/8/12/20，支持自定义输入）、有效期（默认 30/60/90/180 天）、学习泳姿（多选）、实时价格明细、协议勾选区；用户勾选协议并点击「提交订单」后创建待支付订单，前端跳转支付页。

## Data Model

### 表

| 表 | 操作 | 关键字段 |
|----|------|---------|
| `order` | 写 | `user_id`, `coach_id`, `course_type=1`, `standard_package_id`, `custom_hours`, `amount`, `status`, `expire_at` |
| `agreement_sign` | 写 | `user_id`, `agreement_type`, `version`, `signed_at` |
| `user` | 读 | `identity`, `birth_date`, `guardian_phone` |
| `package` | 读 | `status`, `coach_id` |
| `coach` | 读 | `status`, `reference_price_per_hour` |
| `package_template` | 读 | `package_mode`, `status`, 模板快照字段；自定义套餐时读取 allowed_hours / allowed_valid_days / allowed_strokes |
| `coach_package_template`（或关联表） | 读 | 入口 A 查询适配教练 |

### 索引

```sql
CREATE INDEX idx_order_user_coach_status ON order(user_id, coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_agreement_sign_user ON agreement_sign(user_id, agreement_type, version);
```

## API Design

- `POST /api/package/detail`：套餐详情页数据源（US-019 提供，复用）；返回套餐模板详情、适配教练列表（入口 A）/ 当前教练信息（入口 B），自定义套餐教练信息含 `reference_price_per_hour`
- `POST /api/order/formal`：创建正价套餐订单（含未成年人用户信息中监护人手机号存在性校验、自定义套餐参数范围校验）
- `POST /api/agreement/status`：查询协议签署状态

## Caching

（以写为主，无特殊缓存）

## Performance Targets

| 指标 | 目标 |
|------|------|
| 下单 P99 | < 300ms |

## Security

- 登录鉴权
- 同教练 active 套餐唯一性校验
- 协议版本后端校验
- 自定义套餐课时数/有效期/泳姿范围校验
- 自定义套餐价格按课时数 × 参考单价实时计算
- 未成年人用户信息中监护人手机号存在性校验

## Cross-US Dependencies

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录/注册 |
| US-011 | 依赖 | 教练审核 |
| US-019 | 依赖 | 套餐浏览 |
| US-045 | 依赖 | 套餐配置 |
| US-021 | 被依赖 | 我的套餐 |
| US-025 | 被依赖 | 支付 |
