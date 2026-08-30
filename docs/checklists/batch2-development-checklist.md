# 第二批次开发检查清单

> **文档定位**：每次进入第二批次开发前读取并生成 `TodoWrite` 的检查清单，避免上下文丢失。  
> **配套文档**：
> - [docs/tech/dev-plan-batch2.md](../tech/dev-plan-batch2.md)
> - [docs/figma/第二批次页面梳理.md](../figma/第二批次页面梳理.md)
> - [docs/tech/api-convention.md](../tech/api-convention.md)

---

## 1. 批次范围

覆盖 12 个 US：

| 端 | US |
|----|----|
| 用户端小程序 | US-001 / US-017 / US-019 / US-020 / US-021 / US-025 / US-026 / US-027 |
| 教练端小程序 | US-037 |
| Web 管理后台 | US-043 / US-045 / US-046 / US-028 |

---

## 2. 前置检查（每次开发前必须完成）

- [ ] 本地 MySQL / Redis 可连接（密码 `leyo1234`）
- [ ] 后端可 `./mvnw spring-boot:run`
- [ ] 三个前端项目可本地运行
- [ ] 当前 US 的 user-story.md / tech-design.md / test-plan.md 已读取
- [ ] 当前 US 涉及 page-spec 的 Calicat file_id / node-id 已确认

---

## 3. 批次任务清单

### 任务 1：API 路径规范修正

- [ ] 读取 [api-convention.md](../tech/api-convention.md)
- [ ] 扫描第二批次 12 个 US 的 `user-story.md` §7.2 API 影响表
- [ ] 扫描第二批次 12 个 US 的 `tech-design.md` §4 API 设计
- [ ] 扫描第二批次 12 个 US 的 `test-plan.md` 中引用的接口路径
- [ ] 统一修正为 `POST /api/{module}/{singular-resource}/{action}`
- [ ] 移除 URL 路径参数，ID 改为 JSON body 中的 lowerCamelCase 字段
- [ ] 同步更新 OpenSpec change 中相关 API 路径

### 任务 2：三件套一致性检查

- [ ] user-story §4 业务流程 ↔ tech-design 接口/数据模型/状态机
- [ ] user-story §6 验收标准 ↔ test-plan 测试用例
- [ ] user-story §7.1 数据表影响 ↔ tech-design §3 数据模型
- [ ] user-story §7.3 状态机影响 ↔ tech-design §5 状态机
- [ ] 错误码在三份文档中命名与含义一致
- [ ] API 请求/响应字段命名一致

### 任务 3：OpenSpec 变更产物检查

- [ ] 每个 US 对应 `openspec/changes/us-NNN-.../` 目录存在
- [ ] 四件套齐全：`proposal.md`、`design.md`、`specs/.../spec.md`、`tasks.md`
- [ ] proposal / design / spec / tasks 与 docs/stories 三件套一致
- [ ] 运行 `openspec validate --change <name>` 通过

### 任务 4：开发实施

按依赖顺序执行：

- [ ] **G5：后台套餐模板**（US-045）→ 套餐模板管理页/编辑弹窗
- [ ] **G1：购买链路**（US-017 / US-019 / US-020 / US-025）→ 套餐列表/详情/订单确认/支付
- [ ] **G2：我的资产**（US-021 / US-026）→ 我的套餐/订单列表与详情
- [ ] **G3：退款链路**（US-027 / US-028 / US-046）→ 申请退款/订单管理/退款审批
- [ ] **G4：教练学员**（US-037）→ 学员列表/详情/教练视角套餐详情

### 任务 5：提交前检查

- [ ] 代码通过 `code-reviewer` Agent
- [ ] 安全敏感代码通过 `security-reviewer` Agent
- [ ] 前端页面通过 `visual-review` Agent
- [ ] 全端 lint / test / build 通过
- [ ] 集成测试覆盖率 ≥ 80%

---

## 4. 批次结束强制复盘

- [ ] 本批次所有页面均已通过 `visual-review` Agent（无 HIGH / Block 问题）
- [ ] 已运行 `..\scripts\update-batch-lessons.ps1 -Batch batch2 -StartDate <start> -EndDate <end>`
- [ ] 已生成 `tmp/batch-retrospective-input-batch2.md`
- [ ] 已调用 `batch-retrospective` Agent 读取复盘输入文件
- [ ] `docs/figma/batch1-lessons-learned.md` 已追加新增问题与检查项
- [ ] `docs/figma/第二批次页面梳理.md` §7 已记录「复盘完成」

---

## 5. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-15 | AI | 创建第二批次开发可复用检查清单 |
