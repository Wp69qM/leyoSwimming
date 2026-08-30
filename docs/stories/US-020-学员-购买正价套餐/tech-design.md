# Tech Design: US-020 学员购买正价套餐

## 1. 数据模型

### 1.1 表结构

| 表名 | 操作 | 说明 |
|------|------|------|
| `order` | 写 | 待支付正价套餐订单 |
| `agreement_sign` | 写 | 协议签署记录 |
| `user` | 读 | 登录态、身份 |
| `package` | 读 | 校验同用户 active 套餐教练一致性 |
| `coach` | 读 | 教练状态、参考单价 |
| `package_template` | 读 | 标准套餐/自定义套餐模板配置 |
| `package_template_coach` | 读 | 校验标准套餐是否适配所选教练 |

#### order

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `coach_id` | FK |
| `package_mode` | 'standard'=正价套餐（自定义课时同属 standard 模式） |
| `package_template_id` | 套餐模板 ID（自定义课时为 custom 模板 ID） |
| `total_hours` | 课时数（标准套餐按模板，自定义按用户输入） |
| `amount` | 订单金额（分） |
| `status` | 待支付 / 已支付 / 已取消 / 已退款 |
| `expire_at` | 订单支付截止时间（24h） |

#### agreement_sign

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `user_id` | FK |
| `agreement_type` | user_notice / health / disclaimer |
| `version` | 签署版本号 |
| `signed_at` | 签署时间 |

### 1.2 索引

```sql
CREATE INDEX idx_order_user_coach_status ON order(user_id, coach_id, status);
CREATE INDEX idx_package_user_status ON package(user_id, status);
CREATE INDEX idx_agreement_sign_user ON agreement_sign(user_id, agreement_type, version);
```

## 2. API 设计

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### 2.1 POST /api/order/formal

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "coachId": 1,
    "packageId": 1,
    "hours": 12,
    "validDays": 60,
    "strokeIds": [1, 2],
    "agreementVersions": {
      "userNotice": "v3",
      "health": "v1",
      "disclaimer": "v1"
    }
  }
  ```
- **Response 201**: `{ orderId, amount, expireAt }`
- **Response 400**: `{ code: AGREEMENT_REQUIRED | COACH_CONFLICT | COACH_UNAVAILABLE | PACKAGE_NOT_AVAILABLE | INVALID_CUSTOM_PACKAGE_CONFIG | GUARDIAN_PHONE_REQUIRED }`

### 2.2 POST /api/agreement/status

- **鉴权**：必须登录
- **Request**: `{}`
- **Response 200**:
  ```json
  {
    "userNotice": { "requiredVersion": "v3", "signedVersion": "v3" },
    "health": { "requiredVersion": "v1", "signedVersion": null },
    "disclaimer": { "requiredVersion": "v1", "signedVersion": "v1" }
  }
  ```

### 2.3 POST /api/guardian/verify（MVP 后启用）

- **鉴权**：必须登录
- **Request**:
  ```json
  {
    "guardianPhone": "13800138000",
    "verificationCode": "123456"
  }
  ```
- **Response 200**: `{ verified: true }`
- **Response 400**: `{ code: GUARDIAN_PHONE_REQUIRED | INVALID_VERIFICATION_CODE }`

> **MVP 说明**：本接口在 MVP 阶段仅做占位，购买流程中仅校验 `guardianPhone` 已填写，不真正发送短信验证码。

### 2.4 依赖接口（其他 US 实现，本 US 引用）

- **`POST /api/package/detail`**：套餐详情页数据源，由 US-019 实现；本 US 在确认订单页复用其返回的模板快照与教练信息。
- **`POST /api/order/pay`**：调起 Mock 支付，由 US-025 实现；本 US 创建待支付订单后跳转至 US-025 支付页。

## 3. 状态机

| 实体 | 转换 | 触发条件 |
|------|------|---------|
| `order` | 无 → 待支付 | 提交订单 |
| `user` | （本 US 不触发，支付成功后 US-025 触发） | — |

## 4. 缓存

（以写为主，无特殊缓存）

## 5. 性能

| 指标 | 目标 |
|------|------|
| 下单 P99 | < 300ms |

## 6. 安全

- 登录鉴权
- 回调签名验证（US-025）
- 同教练 active 套餐唯一性校验（防止越权购买）
- 协议版本防篡改（后端以 DB 当前版本为准）

## 7. 跨 US 依赖

| US | 方向 | 说明 |
|----|------|------|
| US-004/US-005 | 依赖 | 登录/注册资料 |
| US-011 | 依赖 | 教练审核 |
| US-019 | 依赖 | 套餐浏览 |
| US-045 | 依赖 | 标准/自定义套餐配置 |
| US-021 | 被依赖 | 我的套餐 |
| US-025 | 被依赖 | 订单支付 |

## 8. 测试映射

| 场景 | 测试方法 |
|------|----------|
| 标准套餐下单 | `test_formal_order_standard_package` |
| 自定义课时下单 | `test_formal_order_custom_hours` |
| 未同意协议 | `test_formal_order_agreement_required` |
| 教练冲突 | `test_formal_order_coach_conflict` |
| 教练不可用 | `test_formal_order_coach_unavailable` |
| 幂等 | `test_formal_order_idempotent` |
| 未成年人监护人手机号未校验 | `test_formal_order_guardian_phone_required` |

---

## 变更日志

| 版本 | 日期 | 变更 |
|------|------|------|
| v1.0 | 2026-07-30 | 初版 |
| v1.1 | 2026-07-31 | P1-9 修复：§2.1 POST /api/order/formal 的 Response 400 错误码列表补充 `GUARDIAN_PHONE_REQUIRED`（原仅在 user-story.md §4.2 分支 4 与 §6.4 场景 4 出现，tech-design 漏列）；§8 测试映射同步补充对应测试方法 |
| v1.2 | 2026-08-15 | AI | 三件套一致性修复：§1.1 数据模型由 `standard_package` 更新为 `package_template` / `package_template_coach`；order 表 `course_type` 统一为 `package_mode='standard'`；§2.1 `/api/order/formal` 请求字段 `packageTemplateId`/`customHours` 统一为 `packageId`/`hours`，Response 400 错误码补充 `PACKAGE_NOT_AVAILABLE`、`INVALID_CUSTOM_PACKAGE_CONFIG` |

---