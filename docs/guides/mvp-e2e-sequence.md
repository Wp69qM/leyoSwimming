# MVP 端到端时序图（E2E Sequence Diagrams）

> **文档用途**：用 Mermaid 时序图可视化 MVP 核心流程中「学员 / 教练 / 管理员 / 系统」四方的交互，帮助判断 50 个 US 在逻辑上是否能顺畅串接。
> **最后更新**：2026-07-30

---

## 1. 什么是 E2E 时序图？

E2E（End-to-End）时序图是一种**按时间顺序展示多方角色/系统如何交互**的逻辑梳理图。

它回答的问题：
- 谁（Actor）在什么时机触发了什么动作？
- 系统/后台如何处理？
- 数据/状态如何流转？
- 异常分支在哪里出现？

它不是 UI 设计图，也不是 API 详细文档，而是**业务逻辑层面的流程骨架**。

---

## 2. 图例说明

```mermaid
sequenceDiagram
    participant U as 学员/游客
    participant C as 教练
    participant A as 管理员
    participant S as System
    participant DB as Database
```

- **实线箭头 `->>`**：同步调用/请求
- **虚线箭头 `-->>`**：返回/回调
- **矩形块 `rect`**：同一流程内的子步骤
- **alt/opt/loop**：分支、可选、循环

---

## 3. 主线 1：游客注册 → 购买体验课 → 首次上课

覆盖 US：US-001 ~ US-005, US-017, US-018, US-029 ~ US-033

```mermaid
sequenceDiagram
    autonumber
    actor G as 游客
    participant MP as 用户小程序
    participant S as System
    participant DB as Database
    actor C as 教练

    G->>MP: 打开小程序（US-001）
    MP->>S: 获取教练列表、套餐、公告
    S-->>MP: 返回首页数据

    rect rgb(230, 245, 255)
        Note over G,DB: 注册登录（US-004 / US-005 / US-006）
        G->>MP: 点击微信授权
        MP->>S: 微信 OAuth Code
        S->>DB: 查询/创建用户账号
        S-->>MP: 返回 token + 是否首次登录
        alt 首次登录
            MP->>G: 请求补充手机号
            G->>MP: 提交手机号 + 验证码
            MP->>S: 绑定手机号
            S->>DB: 更新 user.phone
        end
    end

    rect rgb(255, 245, 230)
        Note over G,DB: 购买体验课（US-017）
        G->>MP: 浏览套餐列表/详情
        MP->>S: 查询体验课套餐
        S-->>MP: 返回套餐信息
        G->>MP: 下单购买体验课
        MP->>S: 创建订单
        S->>DB: 写入 order (status=pending)
        S-->>MP: 返回支付参数
        G->>MP: 完成微信支付
        MP->>S: 支付回调
        S->>DB: 更新 order=success, 创建 package (trial, 1课时, 30天有效)
        S-->>MP: 购买成功
    end

    rect rgb(230, 255, 230)
        Note over G,C: 预约上课（US-018 / US-029 / US-031）
        G->>MP: 进入预约页
        MP->>S: 查询可用时段（US-014 / US-016）
        S->>DB: 读取 coach_slot 表
        S-->>MP: 返回可约时段
        G->>MP: 选择时段并确认预约
        MP->>S: 创建 booking
        S->>DB: 扣减 package.remaining_hours, 锁定 slot
        S-->>MP: 预约成功
        Note right of MP: 预约成功页（US-014）
    end

    rect rgb(255, 235, 235)
        Note over G,C: 上课确认（US-032 / US-033）
        G->>MP: 到店扫码签到
        MP->>S: 更新 booking.status=checked_in
        S->>DB: 记录签到时间
        C->>MP: 课后确认上课
        MP->>S: 教练确认扣课时
        S->>DB: booking.status=completed, package 已使用课时 +1
        S-->>C: 确认成功
        S-->>G: 上课完成通知
    end
```

### 逻辑检查点

| 检查项 | 状态 |
|--------|------|
| 游客未登录也能浏览教练/套餐 | ✅ US-001~US-003 |
| 购买体验课前必须完成注册 | ✅ US-004 / US-005 |
| 体验课有效期 30 天 | ✅ US-017 |
| 预约时校验套餐剩余课时 | ✅ US-018 / US-029 |
| 上课记录需教练确认 | ✅ US-033 |

---

## 4. 主线 2：教练入驻 → 排班 → 可约时段释放

覆盖 US：US-010 ~ US-016, US-036

```mermaid
sequenceDiagram
    autonumber
    actor C as 教练
    participant CP as 教练小程序
    participant A as 管理员
    participant AP as 管理后台
    participant S as System
    participant DB as Database

    rect rgb(230, 245, 255)
        Note over C,DB: 教练入驻（US-010 / US-011）
        C->>CP: 提交入驻资料
        CP->>S: 上传资质、身份证、银行卡
        S->>DB: 创建 coach (status=0 pending)
        S-->>CP: 提交成功
        A->>AP: 查看待审核教练
        AP->>S: 查询 coach.status=0
        S-->>AP: 返回列表
        A->>AP: 审核通过/拒绝
        AP->>S: 更新 coach.status=1/2
        S->>DB: 写入审核结果
        S-->>C: 推送审核结果通知
    end

    rect rgb(255, 245, 230)
        Note over C,DB: 教练配置主页与时段（US-012 / US-013 / US-014）
        C->>CP: 编辑个人主页、参考单价
        CP->>S: 更新 coach.profile
        C->>CP: 设置可约时段模板
        CP->>S: 保存 coach_schedule
        S->>DB: 写入 coach_schedule
    end

    rect rgb(230, 255, 230)
        Note over C,DB: 系统自动释放下周时段（US-015 / US-016）
        loop 每周三 10:00
            S->>S: 触发释放任务
            S->>DB: 读取 coach_schedule + release_rule
            S->>DB: 批量生成 coach_slot (next week)
        end
    end

    rect rgb(255, 235, 235)
        Note over C,A: 教练请假（US-036 / US-044）
        C->>CP: 提交请假申请
        CP->>S: 创建 leave_request
        S->>DB: 锁定受影响 slot, 通知已预约学员
        A->>AP: 审批请假
        AP->>S: 更新 leave_request.status
        alt 审批通过
            S->>DB: 释放 slot, 触发候补转正（US-024）或退款
        else 审批拒绝
            S->>DB: 恢复 slot 可约状态
        end
    end
```

### 逻辑检查点

| 检查项 | 状态 |
|--------|------|
| 教练状态机 0→1 需管理员审核 | ✅ US-011 |
| 已入驻教练才能管理主页/排班 | ✅ US-012~US-014 |
| 时段释放由系统定时触发 | ✅ US-016 |
| 请假需管理员审批并处理已有预约 | ✅ US-036 / US-044 |

---

## 5. 主线 3：正价套餐购买 → 退款

覆盖 US：US-019 ~ US-022, US-025 ~ US-028

```mermaid
sequenceDiagram
    autonumber
    actor U as 学员
    participant MP as 用户小程序
    participant A as 管理员
    participant AP as 管理后台
    participant S as System
    participant DB as Database
    participant Pay as 支付渠道

    rect rgb(230, 245, 255)
        Note over U,DB: 购买正价套餐（US-019 / US-020 / US-025）
        U->>MP: 浏览正价套餐列表
        MP->>S: 查询 standard/custom packages
        S-->>MP: 返回套餐
        U->>MP: 选择套餐 + 绑定教练
        MP->>S: 创建订单
        S->>DB: 写入 order (status=pending)
        U->>MP: 完成支付
        MP->>S: 支付回调
        S->>Pay: 确认收款
        S->>DB: order=success, 创建 package (active)
        S-->>MP: 购买成功
    end

    rect rgb(255, 245, 230)
        Note over U,DB: 更换绑定教练（US-022）
        U->>MP: 申请更换教练
        MP->>S: 校验剩余课时、新教练可接
        S->>DB: 更新 package.coach_id
        S-->>MP: 更换成功
    end

    rect rgb(255, 235, 235)
        Note over U,A: 退款（US-027 / US-028）
        U->>MP: 申请退款
        MP->>S: 创建 refund_request
        S->>DB: 计算 refund_amount = unit_price × remaining_hours
        S-->>MP: 显示预计退款金额
        A->>AP: 审核退款
        AP->>S: 审批通过
        S->>DB: package.status=refunded, order.status=refunded
        S->>Pay: 原路退回
        Pay-->>U: 退款到账
    end
```

### 逻辑检查点

| 检查项 | 状态 |
|--------|------|
| 正价套餐绑定教练 | ✅ US-020 |
| 更换教练需校验 | ✅ US-022 |
| 退款按剩余课时计算 | ✅ US-028 |
| 退款原路退回 | ✅ US-028 |

---

## 6. 主线 4：教练离职 → 管理员处理 → 系统自动退款

覆盖 US：US-039 ~ US-041

```mermaid
sequenceDiagram
    autonumber
    actor C as 教练
    participant CP as 教练小程序
    actor A as 管理员
    participant AP as 管理后台
    participant S as System
    participant DB as Database
    actor U as 学员

    C->>CP: 提交离职申请（US-039）
    CP->>S: 创建 resignation_request
    S->>DB: coach.status=4 (resignation in progress)

    A->>AP: 查看离职申请（US-041）
    AP->>S: 查询 coach.status=4
    S-->>AP: 返回列表

    A->>AP: 审批离职
    AP->>S: 更新 coach.status=3 (resigned)
    S->>DB: 查询该教练名下 active packages

    alt 存在 active package
        S->>DB: package.status=pending_resolution
        S-->>U: 通知学员选择：转教练 或 退款
        U->>MP: 选择处理方式
        MP->>S: 提交选择
        alt 转教练
            S->>DB: 更新 package.coach_id, status=active
        else 退款
            S->>DB: package.status=refunded
            S->>Pay: 原路退回 remaining_hours × unit_price
        end
    else 无 active package
        S->>DB: 仅更新 coach.status=3
    end

    S-->>C: 离职处理完成通知
```

### 逻辑检查点

| 检查项 | 状态 |
|--------|------|
| 教练状态机 1→4→3 | ✅ US-039 / US-041 |
| 离职教练对用户隐藏 | ✅ US-041 |
| 非学员主动离职 100% 退款 | ✅ US-041 |
| 学员可选择转教练或退款 | ✅ US-041 |

---

## 7. 主线 5：候补与自动转正

覆盖 US：US-023, US-024

```mermaid
sequenceDiagram
    autonumber
    actor U1 as 学员A
    actor U2 as 学员B
    participant MP as 用户小程序
    participant S as System
    participant DB as Database

    U1->>MP: 预约已满员时段
    MP->>S: 尝试创建 booking
    S->>DB: slot.remaining=0
    S-->>MP: 提示可加入候补
    U1->>MP: 确认加入候补队列
    MP->>S: 创建 waitlist_entry (U1, priority=1)
    S->>DB: 写入 waitlist

    U2->>MP: 关注该时段（US-023）
    MP->>S: 创建 follow 记录
    S->>DB: 写入 follow

    alt 有人取消预约
        U1->>MP: 取消预约（US-030）
        MP->>S: 释放 slot
        S->>DB: slot.remaining=1
        S->>DB: 查询 waitlist, 取 priority 最高者
        S->>DB: 自动创建 booking for U2
        S-->>U2: 候补转正通知（US-024）
    else 教练释放额外时段
        S->>DB: slot.remaining +1
        S->>DB: 触发候补转正
    end
```

### 逻辑检查点

| 检查项 | 状态 |
|--------|------|
| 满员时段可候补 | ✅ US-023 |
| 用户可关注时段 | ✅ US-023 |
| 取消/释放时按优先级自动转正 | ✅ US-024 |
| 转正后通知用户 | ✅ US-024 |

---

## 8. 主线 6：套餐过期与课时耗尽

覆盖 US：US-050

```mermaid
sequenceDiagram
    autonumber
    participant S as System
    participant DB as Database
    actor U as 学员

    loop 每日定时任务
        S->>DB: 扫描 package.status=active
        alt 当前日期 > package.expire_at
            S->>DB: package.status=expired
            S-->>U: 推送过期通知
        else package.remaining_hours = 0
            S->>DB: package.status=exhausted
            S-->>U: 推送课时耗尽通知
        end
    end
```

### 逻辑检查点

| 检查项 | 状态 |
|--------|------|
| 过期状态自动转换 | ✅ US-050 |
| 课时耗尽状态自动转换 | ✅ US-050 |
| 状态转换后通知用户 | ✅ US-050 |

---

## 9. MVP 全局逻辑通顺性总结

### 9.1 主线是否完整

| 主线 | 完整性 |
|------|--------|
| 游客 → 注册 → 体验课 → 上课 | ✅ 完整 |
| 教练入驻 → 排班 → 释放时段 | ✅ 完整 |
| 正价套餐 → 预约 → 退款 | ✅ 完整 |
| 教练离职 → 处理 → 退款/转教练 | ✅ 完整 |
| 候补 → 转正 | ✅ 完整 |
| 套餐过期/耗尽 | ✅ 完整 |

### 9.2 关键依赖关系

```
US-004/005/006 登录注册 → US-017 体验课 → US-018 预约
US-010 入驻 → US-011 审核 → US-012/013/014 主页/排班 → US-016 自动释放
US-020 购买 → US-021 查看 → US-029 预约 → US-032 签到 → US-033 确认
US-023 候补 → US-024 自动转正
US-039 离职 → US-041 处理 → 转教练 / 退款
```

### 9.3 潜在风险点（建议重点 review）

1. **体验课与正价套餐的预约页是否共用？** US-018 和 US-029 可能共享同一预约页，需确认字段差异。
2. **教练请假时已预约学员的处理**：US-036 和 US-044 中，请假审批通过后是转教练还是退款？需与 US-041 保持一致。
3. **候补与关注的优先级**：US-023 中候补和关注的区别是否足够清晰？转正时是否优先候补再通知关注用户？
4. **管理后台权限粒度**：US-042 ~ US-049 中管理员是否统一为超级管理员，还是分角色？

---

## 10. 如何使用本文档

1. **PM 评审时**：对照时序图检查 `user-story.md` 中的流程是否覆盖图中每一步。
2. **设计师出图时**：把每条主线映射为 Figma 的多个页面 frame。
3. **开发联调时**：用时序图作为接口联调的参考主线。
4. **测试用例设计时**：每条时序图的主路径 + 每个 alt/loop 分支都可转化为测试用例。
