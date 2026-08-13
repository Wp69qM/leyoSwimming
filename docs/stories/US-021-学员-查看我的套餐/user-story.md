# US-021 学员查看我的套餐

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：0.5 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-021 |
| **标题** | 学员查看我的套餐 |
| **角色（Actor）** | 学员/注册用户（主）、系统（辅）|
| **业务价值（Why）** | 让用户随时掌握自己的课时、有效期与套餐状态，提升信任感 |
| **优先级** | [MVP] |
| **估时** | 0.5 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

---

## 2. 触发条件

- **触发方**：学员/注册用户
- **触发动作**：用户从底部导航「我的」进入个人中心页，点击「我的套餐」入口，进入「我的套餐」页面
- **触发时机**：主动查看

---

## 3. 前置条件

- [x] 用户已登录（依赖 US-004 / US-005）

---

## 4. 业务流程

### 4.1 主路径

1. 用户进入「我的套餐」页面
2. 系统查询用户名下所有 package 记录
3. 系统按状态分组展示：active / exhausted / expired / refunded / frozen
4. 每个卡片展示购买时快照字段：教练姓名、套餐模式标签（正价/体验课）、教学类型、每节课时长、有效期、课时数、已用/剩余课时、购买时价格、状态标签
5. 页面顶部汇总：总课时、已上课时、剩余课时
6. 套餐详情页展示完整快照字段；若原 package_template 已下架或修改，仍以 package 实例中的快照为准
7. 套餐详情页满足退款条件时（package.status ∈ {active, expired} 或 frozen 且 frozen_reason='coach_resigned'，且 refund_enabled=true，未超过 refund_valid_days），展示「申请退款」按钮；点击后跳转 US-027 发起退款订单，退款金额由 US-027 按 package 快照规则计算；exhausted 套餐不展示退款入口

### 4.2 教练视角套餐使用详情页（由 US-037 入口承接）

1. 教练在 US-037 学员详情页点击「关联套餐」卡片
2. 系统校验该 package 属于当前教练与当前学员
3. 系统跳转至 US-021 教练视角套餐使用详情页
4. 页面展示 package 购买时快照：套餐名称、套餐模式、有效期、教学类型、泳姿、每节课时长、状态标签
5. 页面展示套餐统计摘要：总课时、已用课时、剩余课时
6. 页面展示学员迷你卡，点击可返回 C-学员详情编辑页
7. 页面展示该 package 关联的 booking 使用记录列表
8. 页面不提供购买/加课入口

### 4.3 异常分支

- **分支 1**：用户无套餐 → 展示空状态并引导购买
- **分支 2**：存在 frozen 套餐 → 显示冻结原因文案（如教练离职）
- **分支 3**：教练尝试查看不属于自己或越权的 package → 返回错误 `NOT_ASSOCIATED_STUDENT`，前端提示「无权查看该套餐」

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 学员查看总课时、已上课时、剩余课时 | [§5.3.3](../../prd/prd.md) |
| 2 | 套餐状态机 active / exhausted / expired / refunded / frozen | [§3.4](../../prd/prd.md) / [§4.2](../../prd/prd.md) |
| 3 | 教练离职后 package 冻结且学员端可见 | [§3.7](../../prd/prd.md) |
| 4 | 退款触发场景：package.status ∈ {active, expired} 或 frozen 且 frozen_reason='coach_resigned'；exhausted 不可退款 | [§6.4.1](../../prd/prd.md) |
| 5 | 退款金额 = paid_amount × (total_hours - consumed_count) / total_hours × refund_ratio；教练离职 frozen 按 100% 退 | [§6.4.2](../../prd/prd.md) |
| 6 | 退款资格受 package 快照 refund_enabled、refund_valid_days 控制 | [§6.4.1](../../prd/prd.md) |
| 7 | 退款申请由 US-027 承接，点击「申请退款」跳转 US-027 | 本 US 与 US-027 衔接 |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：正常查看我的套餐

```gherkin
Given 用户已登录且名下有 2 个 active 套餐
And   套餐 A：10 节，已用 3 节，剩余 7 节，package_mode='standard'，teaching_type='private'，duration_minutes=60，valid_days=90，paid_amount=3000
And   套餐 B：6 节，已用 0 节，剩余 6 节，package_mode='standard'，teaching_type='group'，duration_minutes=90，valid_days=60，paid_amount=2400
When  用户进入「我的套餐」页面
Then  页面展示 2 个 active 套餐卡片
And   顶部汇总：总课时 16，已用 3，剩余 13
And   每个卡片展示教练姓名、套餐模式标签（正价/体验课）、教学类型、每节课时长、有效期、购买时价格、剩余课时、状态标签
And   每个卡片数据来自 package 实例快照字段，不依赖当前 package_template
And   接口返回 HTTP 200
```

### 6.2 场景 2：无套餐空状态

```gherkin
Given 用户已登录且名下无任何 package
When  用户进入「我的套餐」页面
Then  页面展示空状态插画
And   文案为"您还没有套餐，去选购吧"
And   接口返回 HTTP 200 + items = []
```

### 6.3 场景 3：存在教练离职冻结套餐

```gherkin
Given 用户已登录且名下有 1 个 active 套餐和 1 个 frozen 套餐（frozen_reason='coach_resigned'）
When  用户进入「我的套餐」页面
Then  active 套餐正常展示
And   frozen 套餐显示"教练已离职，请更换教练或申请退款"
And   接口返回 HTTP 200
```

### 6.4 场景 4：展示已耗尽套餐（exhausted）

```gherkin
Given 用户已登录且名下有 1 个 active 套餐和 1 个 exhausted 套餐（consumed_count = total_hours）
When  用户进入「我的套餐」页面
Then  active 套餐在「可用」分组正常展示
And   exhausted 套餐在「已耗尽」分组展示，卡片显示"课时已用完"标签
And   exhausted 套餐卡片不展示「预约」按钮
And   exhausted 套餐卡片不展示「申请退款」入口
And   顶部汇总仅统计 active 套餐（exhausted 不纳入）
And   接口返回 HTTP 200
```

### 6.5 场景 5：展示已过期套餐（expired，含剩余课时）

```gherkin
Given 用户已登录且名下有 1 个 expired 套餐（available_count = 3，now() > expire_at）
And   package 快照 refund_enabled = true，未超过 refund_valid_days
When  用户进入「我的套餐」页面
Then  expired 套餐在「已过期」分组展示，卡片显示"已过期"标签与过期日期
And   expired 套餐卡片提示"套餐已过期，剩余 3 节课时未使用"
And   expired 套餐卡片展示「申请退款」入口
When  用户点击「申请退款」
Then  跳转 US-027 退款申请页
And   US-027 按 package 快照计算可退金额
```

### 6.6 场景 6：active 套餐详情页展示「申请退款」入口

```gherkin
Given 用户已登录且名下有 1 个 active 套餐
And   package 快照 total_hours = 10，consumed_count = 2，paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0，未超过 refund_valid_days
When  用户进入「我的套餐」页面
And   用户点击 active 套餐卡片进入套餐详情页
Then  套餐详情页展示完整快照字段
And   套餐详情页展示「申请退款」按钮
When  用户点击「申请退款」
Then  跳转 US-027 退款申请页
And   US-027 按 1800 × (10-2)/10 × 1.0 = 1440 元 计算可退金额
```

### 6.7 场景 7：教练离职 frozen 套餐展示「申请退款」入口

```gherkin
Given 用户已登录且名下有 1 个 frozen 套餐（frozen_reason='coach_resigned'）
And   package 快照 paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0
When  用户进入「我的套餐」页面
Then  frozen 套餐在「已冻结」分组展示，卡片显示"教练已离职，请更换教练或申请退款"
And   frozen 套餐卡片展示「申请退款」入口
When  用户点击「申请退款」
Then  跳转 US-027 退款申请页
And   US-027 按 100% 全额退款计算可退金额 1800 元
```

### 6.8 场景 8：原模板已下架仍显示购买时信息

```gherkin
Given 用户已登录且名下有 1 个 active 套餐
And   该套餐购买时 package_template 的 package_name='10 节私教课'，package_mode='standard'，teaching_type='private'，duration_minutes=60，paid_amount=3000
And   该 package_template 现已被管理员修改为 package_name='12 节私教课'，status='inactive'
When  用户进入「我的套餐」页面
Then  卡片仍展示购买时的 package_name='10 节私教课'、package_mode='standard'、teaching_type='private'、duration_minutes=60、paid_amount=3000
And   接口返回 HTTP 200
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package` | 读取 | 用户名下所有套餐；展示字段全部来自购买时模板快照 |
| 2 | `coach` | 读取 | 教练姓名、状态 |
| 3 | `user` | 读取 | 登录态 |
| 4 | `package_template` | 不读取 | 学员端不依赖当前模板字段，避免模板后续变更影响历史套餐展示 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/users/me/packages` | GET | 新增 | 我的套餐列表 |
| 2 | `/api/coach/v1/packages/{package_id}` | GET | 新增 | 教练视角套餐使用详情页数据；需校验 package 属于当前教练 |

### 7.3 状态机影响

（本 US 只读，不触发状态转换）

---

## 8. 边界场景

### 8.1 边界场景 1：网络超时

- **触发条件**：列表接口超时
- **预期行为**：前端展示错误状态，支持重试
- **用户可见反馈**：错误提示 + 重试按钮

### 8.2 边界场景 2：套餐数量多

- **触发条件**：用户持有超过 20 个历史套餐
- **预期行为**：分页或按状态折叠展示
- **用户可见反馈**：默认展开 active，其他状态可展开

### 8.3 边界场景 3：多端登录数据同步

- **触发条件**：用户在另一设备购买套餐后回到本设备
- **预期行为**：下拉刷新后展示最新数据
- **用户可见反馈**：刷新成功提示

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）
- [x] US-005（用户补充注册资料）
- [x] US-020（学员购买正价套餐）

### 9.2 后续 US（依赖本故事）

- [ ] US-022（学员更换绑定教练）
- [ ] US-029（学员预约正价课程）

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 只读查询，可独立交付
- [x] **N**egotiable（可协商）- 展示字段、分组方式可协商
- [x] **V**aluable（有价值）- 用户核心信息入口
- [x] **E**stimable（可估算）- 0.5 人天明确
- [x] **S**mall（足够小）- 单一查询页面
- [x] **T**estable（可测试）- 8 个 GWT 场景

---

## 11. 完整性检查

- [x] 15 章齐全
- [x] 8 个 GWT 场景
- [x] ≥3 边界场景
- [x] PRD 引用明确

---

## 12. 备注

- **缓存**：套餐列表可本地缓存 30s，下拉刷新强制拉取
- **性能要求**：列表接口 P99 < 100ms
- **模板快照**：「我的套餐」所有展示字段必须读取 package 实例中的快照字段，不关联当前 package_template，确保模板后续修改或下架不影响已购套餐展示

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 我的套餐页 Figma file URL | 🔲 待设计填写 | 🔲 |
| 2 | 套餐卡片 Figma file URL | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **我的套餐页** | 🔲 | 🔲 | 🔲 | 🔲 | |

---

## 14. 页面级设计决策

### 14.1 状态分组展示

- active 套餐默认展开，其他状态折叠
- frozen 套餐顶部增加红色提示条

### 14.2 课时汇总规则

- 仅汇总 active 套餐的 total / consumed / available
- exhausted / expired / refunded 不纳入顶部汇总

### 14.3 教练视角套餐使用详情页

- **背景**：US-037 学员详情页需要展示关联套餐卡片，并支持查看套餐使用详情
- **选项**：
  - A. 在 US-037 内独立实现教练视角详情页
  - B. 由 US-021 统一承接教练视角套餐使用详情页
- **结论**：选择 B，由 US-021 统一承接。教练视角详情页与学员视角共用信息架构（套餐快照、统计摘要、使用记录），但入口不同、权限不同、不提供购买/加课入口
- **影响范围**：US-021 新增 `/api/coach/v1/packages/{package_id}` 接口与 `C-套餐使用详情页`；US-037 仅提供入口与卡片

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 🔲 | 🔲 | 🔲 | 🔲 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.1 | 2026-07-31 | PM | v3 评审 P0 修复：§6 新增场景 4（exhausted 展示）和场景 5（expired 含剩余课时展示+退款入口），对齐 PRD §6.4.1 expired 退款规则 |
| v1.2 | 2026-08-01 | PM | §13.1 四态标记统一为 🔲，删除样式描述，添加四态要求说明 |
| v1.3 | 2026-08-12 | PM | 适配 US-045：§4.1 明确展示 package 实例快照字段；§6 补充快照字段断言，新增场景 6；§7.1 更新数据表影响；§12 补充模板快照备注 |
| v1.4 | 2026-08-12 | PM | 适配 US-037：新增 §4.2 教练视角套餐使用详情页流程、§4.3 异常分支 3、§7.2 教练视角详情 API、§14.3 设计决策；明确套餐详情页由 US-021 教练视角统一承接 |
| v1.5 | 2026-08-13 | PM | 完善退款入口：§4.1 补充套餐详情页「申请退款」按钮及跳转 US-027 规则（支持 active / expired / frozen(coach_resigned)，exhausted 不展示）；§5 新增退款规则引用；§6 新增场景 6/7 覆盖 active / frozen(coach_resigned) 套餐退款入口，场景 4 补充 exhausted 不展示退款入口；§10/§11 更新场景数量 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
