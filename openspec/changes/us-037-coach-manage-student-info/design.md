## Context

本变更在教练端引入「我的学员」能力。教练可查看与自己存在 booking/package 关联的学员完整资料（US-005 自主档案 + 教练视角切片），并维护教练视角的信息切片与沟通备注。核心约束：US-005 字段在教练端只读，教练切片与学员本人资料解耦。未成年人信息统一由学员在 US-005 中维护，教练端仅只读展示，不在 `coach_student_profile` 中保存监护人字段。学员详情页还需展示与该教练关联的 package 实例卡片，点击后跳转至 US-021 教练视角套餐使用详情页。

## Goals / Non-Goals

**Goals:**
- 提供关联学员列表（展示 US-005 头像、姓名、性别、年龄、是否未成年人）
- 提供学员完整资料详情：US-005 自主档案（只读）+ 教练视角切片
- 支持教练更新切片字段与沟通备注
- 提供学员详情页「关联套餐」卡片区，展示 package 名称、模式、有效期、状态标签、剩余课时
- 点击套餐卡片跳转至 US-021 教练视角套餐使用详情页
- 保证数据隔离与敏感信息安全
- 记录审计日志

**Non-Goals:**
- 修改 `user` 表中学员自主维护的 US-005 基础资料
- 维护未成年人及监护人信息（统一由 US-005 负责）
- 实现套餐详情页内容（由 US-021 教练视角统一承接）
- 家长独立账号体系

## Decisions

1. **独立 `coach_student_profile` 表**
   - 理由：避免教练视角覆盖学员本人资料，支持不同教练对同一学员保存不同切片
   - 替代方案：扩展 `user` 表加 `coach_notes` 字段 —— rejected，无法隔离多教练

2. **`coach_student_profile` 不含未成年人及监护人字段**
   - 理由：未成年人信息统一由学员在 US-005 中自主维护，教练端只读展示；若放入教练切片会造成数据源不一致与合规风险
   - 影响：教练切片字段仅包含 learning_strokes、swim_level、basics、notes

3. **US-005 字段在教练端只读**
   - 理由：头像、姓名、手机号、年龄、性别、游泳基础、是否未成年人等属于学员自主档案，应由学员在「我的 → 个人资料」中维护
   - 实现：GET 详情接口联查 `user` 表并返回 `user_profile`；PUT 接口忽略或拒绝 US-005 字段

4. **关联关系校验基于 `package` + `booking`**
   - 理由：只要学员购买过该教练套餐或存在预约，即认为存在教学关联
   - 替代方案：单独维护 `coach_student` 关联表 —— 当前数据已足够，MVP 不新增关联表

5. **套餐卡片仅展示剩余课时**
   - 理由：用户反馈列表卡片信息密度过高，剩余课时是最核心的教练关注信息
   - 影响：卡片字段不包含已用/总课时，详情页（US-021）再展示完整统计

6. **套餐详情页由 US-021 教练视角统一承接**
   - 理由：避免 US-037 与 US-021 分别实现两套详情页，减少重复开发与视觉不一致
   - 影响：US-037 仅提供入口与列表卡片；US-021 负责详情页内容及教练视角适配

7. **缓存列表与详情，更新时失效**
   - 理由：我的学员列表访问频率中等，缓存可降低 DB 压力
   - TTL 5 分钟，权衡一致性与性能

## API Design

> 统一使用 POST，URL 按 `/api/{module}/{resource}/{action}`，参数通过 JSON body 传递，字段使用小驼峰。

### POST /api/coach/student/list

- 鉴权：教练 JWT，`coach.status = 1`
- Request：
  ```json
  { "tab": "active" }
  ```
- Response 200：
  ```json
  {
    "students": [
      {
        "studentUserId": 10001,
        "avatarUrl": "https://cdn.example.com/avatar.jpg",
        "name": "张小明",
        "gender": "male",
        "age": 25,
        "isMinor": true,
        "updatedAt": "2026-07-30T10:00:00Z"
      }
    ]
  }
  ```
- 错误码：`AUTH_FORBIDDEN`（403）

### POST /api/coach/student/detail

- 鉴权：教练 JWT
- Request：
  ```json
  { "studentId": 10001 }
  ```
- Response 200：
  ```json
  {
    "studentUserId": 10001,
    "userProfile": {
      "avatarUrl": "https://cdn.example.com/avatar.jpg",
      "name": "张 swimmer",
      "phoneMasked": "138****8000",
      "age": 25,
      "gender": "male",
      "hasSwimBasis": true,
      "swimStrokes": "蛙泳/自由泳",
      "swimYears": "3年",
      "personalDesc": "想提高自由泳",
      "isMinor": true
    },
    "coachSlice": {
      "learningStrokes": "自由泳",
      "swimLevel": 2,
      "basics": "怕水，需循序渐进",
      "notes": "学员水性较好，可加快进度"
    }
  }
  ```
- 错误码：`NOT_ASSOCIATED_STUDENT`（403）

### POST /api/coach/student/update

- 鉴权：教练 JWT
- Request：
  ```json
  {
    "studentId": 10001,
    "learningStrokes": "自由泳",
    "swimLevel": 2,
    "basics": "怕水，需循序渐进",
    "notes": "学员水性较好，可加快进度",
    "idempotencyKey": "coach:1:student:10001:ts:1753879200000"
  }
  ```
- Response 200：`{ "message": "保存成功" }`
- 错误码：
  - `NOT_ASSOCIATED_STUDENT`（403）
  - `READONLY_USER_PROFILE`（400）
  - `IDEMPOTENCY_DUPLICATE`（409）

### POST /api/coach/student/package/list

- 鉴权：教练 JWT
- Request：
  ```json
  { "studentId": 10001 }
  ```
- Response 200：
  ```json
  {
    "packages": [
      {
        "packageId": 20001,
        "packageName": "10 节私教课",
        "packageMode": "standard",
        "status": "active",
        "statusLabel": "使用中",
        "validStart": "2026-07-01",
        "validEnd": "2026-09-29",
        "remainingHours": 4
      }
    ]
  }
  ```
- 错误码：`NOT_ASSOCIATED_STUDENT`（403）

## Risks / Trade-offs

- **[Risk]** 教练误标记未成年人导致监护人信息缺失 → **Mitigation**: 未成年人信息由 US-005 维护，教练端仅只读展示，前端无编辑入口
- **[Risk]** 并发保存产生重复审计日志 → **Mitigation**: idempotency_key 去重
- **[Risk]** 教练通过接口绕过前端修改 US-005 字段 → **Mitigation**: PUT 接口白名单只接受教练切片字段
- **[Risk]** 套餐列表与 US-021/US-050 数据不一致 → **Mitigation**: 写入方统一失效 `coach:student:packages:*` 缓存

## Migration Plan

1. 执行 Flyway migration 创建 `coach_student_profile` 表与唯一索引（不含 guardian 字段）
2. 部署后端接口与教练端页面
3. 对历史 booking/package 关联自动生成空切片（可选，MVP 可懒加载）
4. 回滚：删除表并回退代码

## Open Questions

- 是否需要教练首次查看学员时自动初始化空切片？建议 MVP 采用懒加载，查询不存在时返回默认值并在保存时创建。
