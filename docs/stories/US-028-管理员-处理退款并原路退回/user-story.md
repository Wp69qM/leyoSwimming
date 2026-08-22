# US-028 管理员处理退款并原路退回

> **状态**：[APPROVAL]（已通过）
> **优先级**：[MVP]
> **估时**：1.5 人天
> **作者**：PM　|　**最后更新**：2026-07-30
> **配套文档**：Figma：[A-订单管理页](../../figma/page-spec/A-order-management-page.md) / [A-订单详情弹窗](../../figma/page-spec/A-order-detail-page.md)　·　技术设计：[./tech-design.md](./tech-design.md)　·　测试计划：[./test-plan.md](./test-plan.md)

---

## 1. 基本信息

| 字段 | 值 |
|------|----|
| **编号** | US-028 |
| **标题** | 管理员处理退款订单并原路退回 |
| **角色（Actor）** | 管理员（主）、系统（辅）|
| **业务价值（Why）** | 在订单管理页完成退款订单的审批并触发原路退回，将退款流程闭环，保障用户资金安全与平台合规 |
| **优先级** | [MVP] |
| **估时** | 1.5 人天 |
| **配套文档** | Figma / 技术设计 / 测试计划（链接见上方）|

---

## 2. 触发条件

- **触发方**：管理员
- **触发动作**：在「订单管理」页查看退款订单并点击「通过」或「驳回」
- **触发时机**：存在 order.type = 'refund' 且 status = 退款审批中 的退款订单时

---

## 3. 前置条件

- [x] 管理员已登录且具有「订单管理」权限
- [x] 存在一笔 order.type = 'refund' 且 order.status = 退款审批中 的退款订单（由 US-027 创建）
- [x] 对应 package.status = frozen（refund_pending）（由 US-027 触发，PRD §3.6 / §5.5.1.2）
- [x] 退款金额依据 package 快照字段 `paid_amount`、`total_hours`、`consumed_count`、`refund_ratio` 计算，并与 refund_record 记录一致
- [x] 原购买订单存在支付成功的 payment 记录，且包含渠道信息（微信/支付宝）

---

## 4. 业务流程

### 4.1 主路径

1. 管理员进入「订单管理」页面（US-046）
2. 系统在列表中展示退款订单（order.type = 'refund'，status = 退款审批中），可通过类型筛选
3. 管理员点击退款订单查看详情：原购买订单信息、package 购买时快照信息（套餐名称、套餐模式 `package_mode`、原价 `original_price`、实付价 `paid_amount`、退款比例 `refund_ratio`、可退金额）、package 消耗情况、可退金额计算过程
4. 管理员选择处理方式：
   - **通过退款**（两阶段）：
     - **阶段 1 受理**：系统展示可退金额作为参考；管理员可修改退款金额（修改需填写原因并记录 audit_log）；refund_record 与退款订单的 paid_amount 更新为调整后金额；order.status → 退款处理中；package.status 保持 frozen（refund_pending）；refund_transaction.status = 处理中；记录受理时间与受理管理员
     - **阶段 2 渠道回调**：调用 Mock 退款渠道；Mock 渠道回调成功 → order.status → 已退款；package.status → refunded；package.frozen_reason 清空；关联赠送 package 同步作废；refund_transaction.status = 成功；异步触发身份重算
     - **阶段 2 失败**：Mock 渠道回调失败 → order.status 回滚为 退款审批中；package.status 保持 frozen（refund_pending）；refund_transaction.status = 失败；进入重试队列并通知管理员
   - **驳回退款**：管理员填写驳回原因；order.status → 退款被拒；package.status 自动恢复为 active；package.frozen_reason 清空；系统通知学员并恢复 package 的约课能力
5. 系统发送处理结果通知（微信订阅消息 / 短信）：受理时发送"退款处理中"，渠道成功后发送"已退款"，失败时发送"退款失败，正在重试"，驳回时发送"退款申请被驳回"

### 4.2 异常分支

- **分支 1**：渠道退款接口调用失败（阶段 2 失败）→ refund_transaction.status = 失败，order.status 从「退款处理中」回滚为「退款审批中」，package.status 保持 frozen（refund_pending），package.booking_frozen 保持 true，进入重试队列并通知管理员
- **分支 2**：管理员修改后的退款金额 < 0 → 系统拒绝审批，返回错误码 `REFUND_AMOUNT_INVALID`，提示管理员核对
- **分支 3**：驳回原因为空 → 返回错误码 `REJECT_REASON_REQUIRED`
- **分支 4**：重复点击批准/驳回 → 幂等处理，返回当前最终状态
- **分支 5**：无权限人员访问 → 返回 403 FORBIDDEN

---

## 5. 业务规则引用

| # | 规则 | 章节 |
|---|------|------|
| 1 | 订单状态机：退款审批中 → 退款处理中 / 退款被拒；退款处理中 → 已退款 | [§6.2](../../prd/prd.md) |
| 2 | 退款金额依据 package 快照 `paid_amount × (total_hours - consumed_count) / total_hours × refund_ratio` 计算，不受后续 package_template 变更影响；管理员可基于业务场景调整退款金额，实际退款金额需 ≥ 0 | [§6.4.1](../../prd/prd.md)、[§6.4.2](../../prd/prd.md) |
| 4 | 管理员驳回退款后 package 自动恢复 active（PRD §3.6 / §5.5.1.2） | [§6.4](../../prd/prd.md) |
| 5 | 退款手续费（MVP 暂不收取）| [§6.4.3](../../prd/prd.md) |
| 6 | 原路退回时效 | [§6.4.4](../../prd/prd.md) |
| 7 | frozen 状态退款（100% 退）| [§6.4.5](../../prd/prd.md) |
| 8 | 退款状态期间身份判定 | [§3.6](../../prd/prd.md) |
| 9 | 管理员处理订单与退款 | [§5.5.3](../../prd/prd.md) |
| 10 | 审批详情页展示 package 购买时快照：套餐名称、套餐模式、原价、实付价、退款比例、可退金额 | [§5.5.3](../../prd/prd.md) |

---

## 6. 验收标准（业务级 Gherkin）

### 6.1 场景 1：管理员通过标准退款订单（含两阶段时序）

```gherkin
Given 管理员已登录
And   存在一笔退款订单 order.type = 'refund'，order.status = 退款审批中，amount = 1440 元
And   对应原购买订单 order.type = 'purchase'，status = 已支付
And   对应 package.status = frozen（refund_pending）
And   package 快照 package_mode = 'standard'，total_hours = 10，consumed_count = 2，paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0，refund_valid_days = 30
And   原支付渠道为微信支付
When  管理员点击「通过」
Then  阶段 1：系统校验 refund_amount = 1800 × (10-2)/10 × 1.0 = 1440 元
And   系统调用 Mock 退款渠道受理成功，创建 refund_transaction.status = 处理中
And   order.status = 退款处理中
And   package.status = frozen（refund_pending，保持）
And   学员收到"退款处理中"受理通知
And   返回 HTTP 202 Accepted
When  Mock 渠道异步回调成功
Then  阶段 2：order.status = 已退款
And   package.status = refunded
And   package.frozen_reason 清空
And   refund_transaction.status = 成功
And   学员收到"已退款"成功通知
```

### 6.2 场景 2：管理员驳回退款订单

```gherkin
Given 管理员已登录
And   存在一笔退款订单 order.type = 'refund'，order.status = 退款审批中
And   对应原购买订单 order.type = 'purchase'，status = 已支付
And   package.status = frozen（refund_pending）
And   package 快照 package_mode = 'standard'，total_hours = 10，consumed_count = 2，paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0，refund_valid_days = 30
When  管理员点击「驳回」并填写原因"资料不足"
Then  order.status = 退款被拒
And   order.rejected_reason = "资料不足"
And   package.status 自动恢复为 active
And   package.frozen_reason 清空
And   refund.status = 管理员驳回
And   学员收到驳回通知，展示原因
And   返回 HTTP 200
```

### 6.3 场景 3：Mock 退款渠道回调失败（阶段 2 失败回滚）

```gherkin
Given 管理员已登录
And   存在一笔退款订单 order.type = 'refund'，order.status = 退款审批中
And   package.status = frozen（refund_pending）
And   package 快照 package_mode = 'standard'，total_hours = 10，consumed_count = 2，paid_amount = 1800 元，refund_enabled = true，refund_ratio = 1.0，refund_valid_days = 30
And   Mock 退款渠道返回失败
When  管理员点击「通过」
Then  阶段 1：系统校验 refund_amount = 1800 × (10-2)/10 × 1.0 = 1440 元
And   系统调用 Mock 退款渠道失败
And   refund_transaction.status = 失败
And   order.status 从「退款处理中」回滚为「退款审批中」
And   package.status = frozen（refund_pending，保持）
And   系统提示管理员"退款渠道处理失败，已加入重试队列"
And   退款订单进入重试队列，5 分钟后自动重试
```

### 6.4 场景 4：重复通过退款订单

```gherkin
Given 退款订单已退款成功，order.type = 'refund'，order.status = 已退款
When  管理员再次对该退款订单点击「通过」
Then  系统返回 HTTP 200
And   不重复创建 refund_transaction
And   order.status 仍为已退款
```

### 6.5 场景 5：管理员修改退款金额后通过

```gherkin
Given 管理员已登录
And   存在一笔退款订单 order.type = 'refund'，order.status = 退款审批中
And   可退金额为 1440 元
When  管理员将退款金额修改为 1200 元并填写调整原因"协商一致"
And   点击「通过」
Then  系统返回 HTTP 202 Accepted
And   order.paid_amount = 1200 元
And   refund_record.amount = 1200 元
And   audit_log 记录金额调整：原金额 1440 元 → 1200 元，原因"协商一致"
And   order.status = 退款处理中
And   Mock 渠道回调成功后 order.status = 已退款
And   package.status = refunded
```

### 6.6 场景 6：非管理员越权访问

```gherkin
Given 普通学员已登录
When  学员请求管理员退款审批接口
Then  系统返回 HTTP 403，错误码 FORBIDDEN
And   不修改任何退款/订单状态
```

---

## 7. 数据/API/状态机影响

### 7.1 数据表影响

| # | 表名 | 操作 | 说明 |
|---|------|------|------|
| 1 | `package_template` | 修改 | 新增字段：`package_mode`（standard/experience）、`teaching_type`、`stroke_ids`、`total_hours`、`duration_minutes`、`valid_days`、`original_price`、`price`、`refund_enabled`、`refund_ratio`、`refund_valid_days`、`tags`、`description`、`images`、`status`（US-045） |
| 2 | `package` | 修改 | 购买时保存模板快照字段：`package_mode`、`total_hours`、`paid_amount`、`refund_enabled`、`refund_ratio`、`refund_valid_days` 等；管理员审批时展示与计算均基于快照，不受后续 `package_template` 变更影响 |
| 3 | `order`（退款订单）| 修改 | status → 退款处理中 / 已退款 / 退款被拒；paid_amount 在管理员修改退款金额时更新；refunded_at 在已退款时回填；rejected_reason 在驳回时回填 |
| 4 | `refund` | 修改 | status → 管理员批准 / 管理员驳回；amount 在管理员修改退款金额时更新 |
| 5 | `refund_transaction` | 新增 | 记录 Mock 退款渠道流水、状态、失败原因 |
| 6 | `package` | 修改 | 批准且渠道成功：status → refunded，frozen_reason 清空；管理员驳回：status 自动恢复 active，frozen_reason 清空；审批中/渠道失败回滚：status 保持 frozen（refund_pending） |
| 7 | `package`（赠送）| 修改 | 标准 package 退款时同步作废 |
| 8 | `audit_log` | 新增 | 记录管理员审批操作及退款金额调整 |
| 9 | `user` | 读取/触发 | 退款完成后异步重算身份 |

### 7.2 API 影响

| # | API | 方法 | 操作 | 说明 |
|---|-----|------|------|------|
| 1 | `/api/admin/order/list` | POST | 读取 | 订单列表查询（含退款订单；分页、筛选、排序；JSON body 传入 `page`、`pageSize`、`status`、`coachId`、`userId`、`startDate`、`endDate`；详见 US-046） |
| 2 | `/api/admin/order/detail` | POST | 读取 | 订单详情（含退款订单详情；JSON body 传入 `orderId`；详见 US-046） |
| 3 | `/api/admin/order/approve-refund` | POST | 新增 | 通过退款订单；JSON body 传入 `orderId`、`amount`、`remark`；支持管理员修改退款金额；触发 Mock 退款渠道 |
| 4 | `/api/admin/order/reject-refund` | POST | 新增 | 驳回退款订单；JSON body 传入 `orderId`、`reason`；package 自动恢复 active |

### 7.3 状态机影响

| # | 实体 | 转换 | 触发条件 | 说明 |
|---|------|------|---------|------|
| 1 | `order`（退款订单）| 退款审批中 → 退款处理中 | 管理员通过（阶段 1 受理）| 中间态，等待 Mock 渠道回调 |
| 2 | `order`（退款订单）| 退款处理中 → 已退款 | Mock 渠道回调成功（阶段 2 成功）| 终态 |
| 3 | `order`（退款订单）| 退款处理中 → 退款审批中 | Mock 渠道回调失败（阶段 2 失败）| 回滚，进入重试队列 |
| 4 | `order`（退款订单）| 退款审批中 → 退款被拒 | 管理员驳回 | 终态；package 自动恢复 active |
| 5 | `package` | frozen（refund_pending）→ frozen（refund_pending，保持） | 管理员通过（阶段 1 受理）/ Mock 渠道失败回滚 | 退款处理期间 status 保持冻结 |
| 6 | `package` | frozen（refund_pending）→ refunded | Mock 渠道回调成功（阶段 2 成功）| 终态；frozen_reason 清空；关联赠送 package 同步作废 |
| 7 | `package` | frozen（refund_pending）→ active | 管理员驳回 | 自动恢复，可继续约课；frozen_reason 清空 |
| 8 | `user` | 学员 → 注册用户 | 退款完成后无其他 active package | 异步重算 |

> **package 退款状态说明**：PRD §3.6 / §5.5.1.2 明确退款审批期间 package.status = frozen（refund_pending）（由 US-027 学员提交退款时触发）。Mock 渠道成功时 package.status → refunded；管理员驳回时 package.status 自动恢复 active；失败回滚时 package.status 保持 frozen（refund_pending）。

---

## 8. 边界场景

### 8.1 边界场景 1：重复审批（并发或误操作）

- **触发条件**：管理员快速点击两次「通过」或两名管理员同时操作
- **预期行为**：幂等键 / 分布式锁保证仅处理一次，第二次返回当前状态
- **用户可见反馈**：页面展示"该退款已处理，请勿重复操作"

### 8.2 边界场景 2：Mock 退款渠道回调失败/超时

- **触发条件**：Mock 退款渠道超时或返回异常
- **预期行为**：记录失败流水，order.status 从「退款处理中」回滚为「退款审批中」，package.status 保持 frozen（refund_pending），进入定时重试队列
- **用户可见反馈**：管理员后台显示"退款渠道处理中/失败"，学员端显示"退款处理中"

### 8.3 边界场景 3：管理员修改后的退款金额为负数

- **触发条件**：管理员在通过退款时将金额修改为负数
- **预期行为**：审批前校验失败，返回 REFUND_AMOUNT_INVALID，拦截并提示管理员
- **用户可见反馈**：后台弹窗"退款金额不能为负数，请核对"

---

## 9. 依赖关系

### 9.1 前置 US（本故事依赖）

- [x] US-004（用户微信授权登录）
- [x] US-005（用户补充注册资料）
- [x] US-020（学员购买正价套餐）
- [x] US-025（学员支付套餐订单）
- [x] US-027（学员申请退款）
- [x] US-046（管理员查看与处理订单）—— 提供订单管理页入口

### 9.2 后续 US（依赖本故事）

- [ ] 无

---

## 10. INVEST 自检

- [x] **I**ndependent（独立）- 依赖 US-027 的退款申请，但审批/退回逻辑可独立交付
- [x] **N**egotiable（可协商）- 审批字段、通知渠道可协商
- [x] **V**aluable（有价值）- 保障资金安全与退款合规
- [x] **E**stimable（可估算）- 1.5 人天明确
- [x] **S**mall（足够小）- 仅覆盖管理员审批与原路退回
- [x] **T**estable（可测试）- 5 个 GWT 场景可客观验证

---

## 11. 完整性检查

### 11.1 字段完整性

- [x] 15 个章节全部填写
- [x] 无"待定"/"TBD"占位符
- [x] 错误码明确

### 11.2 业务规则

- [x] 引用 §6.2 / §6.4 / §3.6 / §5.5.3
- [x] 与订单/套餐状态机一致
- [x] 退款金额公式正确

### 11.3 验收标准

- [x] 4 正常 + 2 异常 GWT
- [x] 每个 Then 含具体数值/状态码/DB 字段值
- [x] 业务规则可被验证

### 11.4 配套文档

- [x] 链接明确

---

## 12. 备注

- **幂等键**：`{admin_id}:{refund_order_id}:approve-refund` / `{admin_id}:{refund_order_id}:reject-refund`
- **快照字段**：审批详情页展示 package 购买时快照（套餐名称、套餐模式、原价、实付价、退款比例、可退金额）；退款金额计算基于快照 `paid_amount × (total_hours - consumed_count) / total_hours × refund_ratio`，不受后续 `package_template` 变更影响
- **事务边界**：refund 状态更新 + order 状态更新 + package 状态恢复（驳回时）在同一事务；Mock 渠道调用放在事务外，失败走补偿
- **驳回状态**：管理员驳回退款后 order.status = 退款被拒，package.status 自动恢复 active，frozen_reason 清空，学员可继续约课
- **金额调整**：管理员可基于业务场景调整退款金额（可大于可退金额），仅保留金额 ≥ 0 校验；修改原因与调整记录写入 audit_log
- **性能要求**：订单列表查询 P99 < 200ms，审批接口 P99 < 800ms（含 Mock 渠道调用）
- **对账机制**：每日凌晨与 Mock 渠道对账，差异进入异常队列
- **页面入口**：退款订单的列表与详情入口由 US-046「订单管理页」提供

---

## 13. Figma 链接

> Figma **设计系统规范**见 [docs/figma/README.md](../../figma/README.md)。

| # | 内容 | 链接 | 状态 |
|---|------|------|------|
| 1 | 订单管理页 page-spec | [A-order-management-page.md](../../figma/page-spec/A-order-management-page.md) | ✅ |
| 2 | 订单详情弹窗 page-spec | [A-order-detail-page.md](../../figma/page-spec/A-order-detail-page.md) | ✅ |

### 13.1 状态截图清单

> **四态要求**：每个页面必须设计空/加载/错误/成功四种状态。具体样式由设计师根据 [Figma 设计规范](../../figma/README.md) §0.5 主题与 §4 四态模板决定，不在本节指定。

| 页面 | 空状态 | 加载状态 | 错误状态 | 成功状态 | 备注 |
|------|--------|---------|---------|---------|------|
| **订单管理页** | 🔲 | 🔲 | 🔲 | 🔲 | 退款订单通过 type 筛选 |
| **订单详情弹窗** | — | 🔲 | 🔲 | 🔲 | 退款订单展示通过/驳回按钮 |

---

## 14. 页面级设计决策

### 14.1 退款金额人工调整

- 管理员可在系统计算金额基础上手动调整（实际退款金额需 ≥ 0，可退金额仅作参考）
- 调整金额与调整原因写入 audit_log
- 未调整时直接点击「通过」则使用系统计算金额

### 14.2 退款审批入口合并至订单管理

- 不再保留独立的「退款处理」菜单
- 退款订单在「订单管理」列表中通过 type 筛选，并在详情页执行「通过/驳回」

### 14.3 批量操作

- MVP 阶段仅支持单笔审批
- 列表页提供「查看详情」入口，审批按钮放在详情页，避免误操作

---

## 15. 设计评审记录

| 日期 | 评审人 | 反馈 | 处置 |
|------|--------|------|------|
| 2026-08-12 | PM/开发 | 退款审批合并至订单管理；驳回后 package 自动恢复 active；退款金额允许管理员修改 | 已更新 US-028 |

---

## 附录：变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | PM | 初版 |
| v1.1 | 2026-07-31 | PM | P1 修复：明确两阶段退款状态机时序（受理→退款处理中→渠道回调→已退款/失败回滚），消除原 §4.1/§6.1 与 §6.3 矛盾 |
| v1.2 | 2026-07-31 | PM | v3 评审 P0 修复：§7.3 明确 package frozen 转换路径（active→frozen→refunded/active），补充 frozen 中间态由 US-027 触发的说明 |
| v1.3 | 2026-07-31 | PM | P0 修复：退款审批期间 package.status 保持 active，通过 booking_frozen 冻结约课能力；§3/§4/§6/§7/§12 同步调整 |
| v1.4 | 2026-07-31 | PM | 半落地修复：对齐 PRD v11.2 §3.6（frozen(refund_pending)）。§3 前置条件改为 frozen(refund_pending)（由 US-027 触发）；§4.1 主路径改为「保持 frozen，审批通过 → refunded；审批拒绝 → active（解冻）」；§4.2/§6.1/§6.2/§6.3/§7.1/§7.3/§12 同步清理"保持 active"旧文本 |
| v1.5 | 2026-08-01 | PM | §13.1 四态标记统一为 🔲，删除样式描述，添加四态要求说明 |
| v1.6 | 2026-08-12 | PM | 适配 US-045：§3 前置条件增加快照字段计算校验；§4.1 详情页展示 package 快照信息，批准时校验快照计算金额，驳回时 package.status 保持不变；§4.2 金额不匹配文案更新；§5 增加快照展示规则；§6 Gherkin 补充快照字段断言；§7.1 更新数据表影响；§7.3 与说明调整驳回状态为保持不变；§12 补充快照字段与驳回状态说明 |
| v1.7 | 2026-08-12 | PM | 适配 US-046 订单/套餐边界重构：退款审批入口合并至订单管理页；标题改为「管理员处理退款订单并原路退回」；§2/§3/§4/§5/§6/§7/§8/§12/§13/§14 同步更新；新增驳回后 package 自动恢复 active、管理员可修改退款金额、Mock 退款渠道等规则 |
| v1.8 | 2026-08-13 | PM | 放宽退款金额限制：管理员可基于业务场景调整退款金额（可大于可退金额），仅保留金额 ≥ 0 校验；§4.1/§4.2 同步更新；OpenSpec design.md/spec.md/tasks.md 同步 |

---

<p align="right">
<sub>本文档遵循 <a href="../../spec/user-story/SPECIFICATION.md">US 拆分规范</a>　·　复制自 <a href="../../spec/user-story/TEMPLATE.md">TEMPLATE.md</a></sub>
</p>
