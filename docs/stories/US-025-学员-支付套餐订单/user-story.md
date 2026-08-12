# US-025 学员支付套餐订单

> **状态**：[REVIEW]（评审中）
> **优先级**：[MVP]
> **估时**：1.5 人天
> **作者**：PM　|　**最后更新**：2026-08-12
> **配套文档**：Figma：[待设计填写]　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-025 |
| **标题** | 学员支付套餐订单 |
| **角色（Actor）** | 学员/注册用户（主）、系统（辅）|
| **业务价值（Why）** | 完成正价套餐的支付闭环，将待支付订单转化为有效课时包，使用户获得学员身份 |
| **优先级** | [MVP] |
| **估时** | 1.5 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

---

## 2. 触发条件

- **触发方**：学员/注册用户
- **触发动作**：在支付页确认支付并调起微信/支付宝支付
- **触发时机**：订单创建后（US-020）用户主动支付时

---

## 3. 前置条件

- [x] 用户已登录（依赖 US-004 / US-005）
- [x] 存在待支付订单（依赖 US-020，order.status = 待支付）
- [x] 订单未超过 24 小时支付有效期
- [x] 用户已同意《用户须知》、健康承诺书、免责协议（§6.9）

---

## 4. 业务流程

### 4.1 主路径

1. 用户进入订单支付页，系统展示订单金额、支付方式（微信/支付宝）
2. 用户选择支付方式并点击「确认支付」
3. 系统校验订单状态为待支付且未过期
4. 系统调用微信支付/支付宝统一下单，生成预支付参数
5. 前端调起对应支付 SDK，用户完成支付
6. 第三方支付异步回调系统支付接口
7. 系统幂等处理回调：order.status → 已支付，paid_at 写入时间戳
8. 系统使用订单中已快照的模板字段创建 package，package.status → active（不实时查询 package_template）
9. 系统触发身份重算：注册用户 → 学员
10. 系统发送支付成功通知

### 4.2 异常分支

- **分支 1**：用户取消支付或支付失败 → order.status 保持待支付，提示"支付失败，请重试"
- **分支 2**：订单超过 24 小时未支付 → 定时任务将 order.status → 已取消，不可再支付
- **分支 3**：重复支付回调 → 幂等键去重，返回已成功，不重复创建 package
- **分支 4**：支付成功但创建 package 失败 → 事务回滚或进入补偿队列，通知管理员

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 购买套餐生成订单，支持微信支付、支付宝支付 | [§5.3.4](../../prd/prd.md) |
| 2 | 订单状态机：待支付 → 已支付 | [§6.2](../../prd/prd.md) |
| 3 | 套餐状态机：支付成功后 package.status → active | [§4.2](../../prd/prd.md) |
| 4 | 用户身份状态机：支付成功 + active package → 学员 | [§3.2](../../prd/prd.md) |
| 5 | 支付链路具备重试与对账机制 | [§12.2](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：微信支付成功

```gherkin
Given 用户已登录且存在待支付订单 order.status = 待支付，amount = 1800 元
And   订单中已快照模板字段：package_name = "蛙泳基础 10 节", package_mode = "standard", coach_id = "C001", coach_name = "王教练", teaching_type = "1v1", stroke_ids = ["breaststroke"], total_hours = 10, duration_minutes = 60, valid_days = 90, original_price = 2000, paid_amount = 1800, refund_enabled = true, refund_ratio = 0.8, refund_valid_days = 30
And   订单未超过 24h 有效期
When  用户选择微信支付并完成支付
Then  系统收到支付成功回调
And   order.status = 已支付，paid_at 写入时间戳
And   package.status = active，available = 10
And   package 字段与订单快照一致：package_mode = "standard", coach_id = "C001", teaching_type = "1v1", total_hours = 10, duration_minutes = 60, valid_days = 90, paid_amount = 1800
And   用户身份升级为学员
And   系统发送支付成功微信订阅消息
```

### 6.2 场景 2：支付宝支付成功

```gherkin
Given 用户已登录且存在待支付订单 order.status = 待支付，amount = 2400 元
And   订单中已快照模板字段：package_name = "自由泳进阶 12 节", package_mode = "standard", coach_id = "C002", coach_name = "李教练", teaching_type = "1v2", stroke_ids = ["freestyle"], total_hours = 12, duration_minutes = 60, valid_days = 120, original_price = 2600, paid_amount = 2400, refund_enabled = true, refund_ratio = 0.7, refund_valid_days = 30
And   订单未超过 24h 有效期
When  用户选择支付宝支付并完成支付
Then  系统收到支付成功回调
And   order.status = 已支付
And   package.status = active
And   package 字段与订单快照一致：package_mode = "standard", coach_id = "C002", teaching_type = "1v2", total_hours = 12, duration_minutes = 60, valid_days = 120, paid_amount = 2400
And   用户身份升级为学员
```

### 6.3 场景 3：用户取消支付

```gherkin
Given 用户已登录且存在待支付订单
When  用户在支付 SDK 中点击取消
Then  系统记录支付失败流水 payment.status = 失败
And   order.status 保持待支付
And   前端提示"支付已取消，可重新发起"
```

### 6.4 场景 4：订单已超时

```gherkin
Given 待支付订单已创建 24 小时 01 分
When  用户尝试支付该订单
Then  系统返回 HTTP 400，错误码 ORDER_EXPIRED
And   order.status = 已取消
And   前端提示"订单已过期，请重新下单"
```

### 6.5 场景 5：重复支付回调

```gherkin
Given 订单已支付成功且 package.status = active
And   package 字段与订单快照一致
When  第三方支付再次发送同一笔支付成功回调
Then  系统幂等处理，返回 HTTP 200
And   不重复创建 package
And   order.status 仍 = 已支付
And   package 字段仍与订单快照一致
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `order` | 修改 | status → 已支付，paid_at 回填；order 中已保存模板快照字段，用于后续 package 创建 |
| 2 | `payment` | 新增 | 支付流水，含 channel_trade_no |
| 3 | `package` | 新增 | 支付成功后创建 active 课时包，字段全部取自 order 快照，不实时查询 package_template |
| 4 | `user` | 读取/触发 | 身份重算为学员 |
| 5 | `agreement_sign` | 读取 | 校验协议已签署 |
| 6 | `package_template` | 读取 | 下单时校验 template.status = active；支付回调阶段不再依赖 template 当前状态 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/orders/{order_id}/pay` | POST | 新增 | 创建支付流水并返回预支付参数 |
| 2 | `/api/payments/callback/wechat` | POST | 新增 | 微信支付异步回调 |
| 3 | `/api/payments/callback/alipay` | POST | 新增 | 支付宝异步回调 |
| 4 | `/api/orders/{order_id}` | GET | 读取 | 查询订单支付状态 |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `order` | 待支付 → 已支付 | 支付回调成功 | 幂等处理 |
| 2 | `package` | 无 → active | order 支付成功后创建 | 含 total_hours/available |
| 3 | `user` | 注册用户 → 学员 | 存在 active package | 异步身份重算 |

---

## 8. 边界场景

### 8.1 边界场景 1：支付回调网络超时

- **触发条件**：第三方支付回调到达系统时网络超时
- **预期行为**：第三方会重试；系统幂等键保证不重复处理；同时提供主动对账接口
- **用户可见反馈**：支付页轮询订单状态，成功后跳转

### 8.2 边界场景 2：支付成功但 package 创建失败

- **触发条件**：DB 事务提交异常
- **预期行为**：事务回滚，标记支付流水为待对账，进入补偿队列通知管理员
- **用户可见反馈**：显示"支付状态处理中，请勿重复支付"

### 8.3 边界场景 3：并发支付请求

- **触发条件**：用户快速点击两次支付按钮
- **预期行为**：幂等键保证仅生成一笔 payment 记录，仅调起一次支付
- **用户可见反馈**：正常调起支付 SDK

### 8.4 边界场景 4：支付期间模板被修改/下架

- **触发条件**：用户下单后、支付回调完成前，管理员修改了 package_template（如下架、改价、调整课时数）
- **预期行为**：支付成功后创建 package 仍使用订单中已快照的模板字段，不受 package_template 后续变更影响；package_template 当前状态不阻塞已下单订单的支付回调
- **用户可见反馈**：用户支付成功后，套餐内容、价格、有效期与下单时一致

### 8.5 边界场景 5：支付超时与候补转正并发竞争

- **触发条件**：订单 24h 超时取消定时任务执行的同时，该订单对应教练的候补学员触发转正（US-023）
- **预期行为**：库存释放（超时取消）与候补转正必须竞争同一分布式锁（`inventory:{coach_id}`），串行执行；避免超时释放的库存被重复分配给候补或原订单
- **用户可见反馈**：候补学员看到准确的库存状态，不会出现「已取消订单占用名额」或「超卖」

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）
- [x] US-005（用户补充注册资料）
- [x] US-020（学员购买正价套餐）

### 9.2 后续 US（依赖本故事）

- [ ] US-021（学员查看我的套餐）
- [ ] US-026（学员查看订单列表与详情）
- [ ] US-027（学员申请退款）
- [ ] US-029（学员预约正价课程）

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 依赖 US-020 的待支付订单，但支付逻辑可独立交付
- [x] **N**egotiable（可协商）- 支付方式、支付页 UI 可协商
- [x] **V**aluable（有价值）- 完成交易闭环，是核心营收路径
- [x] **E**stimable（可估算）- 1.5 人天明确
- [x] **S**mall（足够小）- 仅覆盖支付到 package 激活
- [x] **T**estable（可测试）- 5 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符
- [x] 错误码明确（ORDER_EXPIRED）

### 11.2 业务规则

- [x] 引用 §5.3.4 / §6.2 / §4.2 / §3.2 / §12.2
- [x] 与订单/套餐状态机一致
- [x] 与身份状态机一致

### 11.3 验收标准

- [x] 2 正常 + 3 异常 GWT
- [x] 每个 Then 含具体数值/状态码/DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] 链接明确

---

## 12. 备注

- **幂等键**：`idempotency_key = order_id:pay:{timestamp}:{nonce}`，按订单维度去重
- **事务边界**：支付流水状态更新 + order 状态更新 + package 创建 在同一事务
- **快照字段**：package 创建必须仅读取 order 中已保存的快照字段（package_name, package_mode, coach_id, coach_name, teaching_type, stroke_ids, total_hours, duration_minutes, valid_days, original_price, paid_amount, refund_enabled, refund_ratio, refund_valid_days），禁止回查 package_template
- **性能要求**：支付回调处理 P99 < 500ms，预支付参数获取 P99 < 200ms
- **对账机制**：每日凌晨与第三方支付渠道对账，差异进入异常队列

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 / node-id | 状态 |
|---|------|---------------|------|
| 1 | 订单支付页 Figma file URL | 🔲 待设计填写 | 🔲 |
| 2 | 支付成功页 Figma file URL | 🔲 待设计填写 | 🔲 |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **订单支付页** | — | 🔲 | 🔲 | — | |
| **支付成功页** | — | 🔲 | 🔲 | 🔲 | |

---

## 14. 页面级设计决策

### 14.1 支付方式选择

- 默认选中上次使用的支付方式（首次默认微信）
- 仅展示已配置的支付方式（微信/支付宝）

### 14.2 支付倒计时

- 支付页顶部展示订单剩余有效期倒计时（24h 制）
- 倒计时结束时自动置灰支付按钮并提示"订单已过期"

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
| v1.1 | 2026-07-31 | PM | P1 修复：§8 增加支付超时与候补转正并发竞争边界场景，明确使用同一分布式锁 |
| v1.2 | 2026-08-01 | PM | §13.1 四态标记统一为 🔲，删除样式描述，添加四态要求说明 |
| v1.3 | 2026-08-12 | PM | 适配 US-045：package 创建使用订单快照字段；§6 Gherkin 补充快照字段断言；§8 增加支付期间模板被修改/下架边界场景；§7.1/§12 更新数据表影响与备注 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
