# leyoSwimming 项目每日工作日志

> **数据来源**：跨会话记忆（`c:\Users\EDY\.trae-cn\memory\projects\-d-AI-Agent-leyoSwimming`） + Git 提交日志（`git log --all -n 200`）。
> **统计周期**：2026-07-27 至 2026-08-14。
> **记录维度**：当日主要工作、关键决策、产出物、阻塞/问题、项目阶段。

---

## 项目阶段总览

| 阶段 | 时间范围 | 关键标志 | 当前状态 |
|------|---------|---------|---------|
| 阶段 1：PRD 定稿与项目初始化 | 07-27 ~ 07-29 | PRD v11 落档、项目目录结构建立 | 已完成 |
| 阶段 2：用户故事生成与业务评审 | 07-30 ~ 08-01 | 50 个 US 生成、v4 评审通过 | 已完成 |
| 阶段 3：Page Spec 与设计稿对接 | 08-02 ~ 08-08 | 用户端/教练端/管理端 page-spec 补齐、Calicat 上传 | 已完成 |
| 阶段 4：第一批次开发 | 08-09 ~ 08-13 | 登录注册 + 用户管理前后端实现 | 已完成 |
| 阶段 5：第一批次收尾 + 第二批次规划 | 08-13 ~ 08-14 | 视觉还原修复、第二批次 US/page-spec 梳理 | 进行中 |

---

## 2026-07-27（周日）

**主要工作**：
- 项目仓库初始化，提交 Initial commit。
- 上传 PRD 历史版本（readme_v1 ~ readme_v6、answer.md 等）。
- 合并落档 PRD v11（含状态机重构、实名认证删除、状态/计数对照表）。

**关键产出**：
- `readme_v11_prd_final.md` 等 PRD 文档落档。
- 项目 Git 仓库建立。

**项目阶段**：PRD 定稿初期。

---

## 2026-07-28（周一）

**主要工作**：
- 多轮讨论 PRD 中套餐与订单状态逻辑。
- 明确 `active` 套餐状态含义：非 expired、非 refunded、非 exhausted。
- 合并订单状态："退款申请"与"退款审批"合并为"退款审批"；"closed"与"canceled"合并为"canceled"。
- 明确退款审批期间套餐冻结 booking，由订单状态控制，不修改套餐状态机。
- 明确 `exhausted` 条件：`available_count=0`、`reserved_count=0`、`exhausted_at IS NOT NULL`。
- 讨论管理员手动延期、过期套餐退款、体验课购买限制等边界规则。

**关键决策**：
- 套餐与订单状态机解耦。
- active 与 expired/exhausted/refunded 互斥。
- 体验课用户最多持有 1 份 active/exhausted，过期/退款后可重购。

**项目阶段**：PRD 硬约束与状态机精化。

---

## 2026-07-29（周二）

**主要工作**：
- 分析 PRD 逻辑闭合性、开发就绪度、MVP 可行性。
- 讨论并确定 bonus 课时方案：作为独立 package（`paid_amount=0`），标准套餐耗尽后消耗。
- 讨论教练离职处理：引入 `pending_handover_at` 字段，保持现有状态机。
- 教练协商定价与 bonus 课时标记为 P2，不纳入 MVP。
- 将 SPECIFICATION.md 验收标准升级为 Gherkin + TDD 映射体系。
- 项目文档结构与内容审计，得分 53/100，识别 4 个阻塞问题。
- 重构 docs 目录，迁移 PRD v1-v10 存档，新增 US 拆分规范文档。
- v11 教练离职完整方案定稿。

**关键产出**：
- `SPECIFICATION.md` 验收标准重构。
- `docs/` 目录结构初步成型。
- 项目审计报告与改进建议。

**阻塞/问题**：
- GitHub SSL/TLS handshake 失败（网络/环境 issue）。

**项目阶段**：需求层规范化与审计。

---

## 2026-07-30（周三）

**主要工作**：
- 接入 OpenSpec CLI。
- 生成 US-001。
- 批量生成 US-001~US-004 用户故事及 OpenSpec 变更产物。
- 完成覆盖自查报告，MVP 能力覆盖 8%（2/10）。
- 输出 v2.0《leyoSwimming 用户故事业务评审报告》：47 个 US 中仅 6 个可直接 APPROVED，40 个需修改，1 个需补齐三件套；发现 28 项严重问题、8 项跨 US 数据模型不一致。

**关键决策**：
- 分阶段修复：先处理 7 个 P0 阻塞 US，解决跨 US 一致性问题，再二次评审。

**项目阶段**：用户故事生成与首轮业务评审。

---

## 2026-07-31（周四）

**主要工作**：
- 修复 P0 问题：补建 US-019、US-027 目录；将 US-033a 重命名为 US-035；补齐 US-044 三件套。
- 拆分 US-022 退订+购新；修改 US-030 标题；拆分 US-033；修复 US-017 主路径顺序；调整 US-009 位置；统一 US-007 状态码。
- 完成 v3/v4 业务评审，19 个 US approved，0 个 P0/P1 遗留。
- 讨论下一步进入 TDD 实现阶段，建议从 US-004 开始。

**关键产出**：
- `mvp-us-business-review-report-v4.md`。
- INDEX.md 更新，US 总数与依赖关系修正。

**项目阶段**：业务评审收尾，准备进入开发。

---

## 2026-08-01（周五）

**主要工作**：
- 准备 Figma 设计交付： review `docs/figma/README.md`、`figma-frame-organization.md`、`us-batch-handoff.md`。
- 在 `docs/figma/README.md` 新增§7 Figma 设计交付规范，明确必须生成可编辑图层。
- 统一 50 个 US 的§13.1 四态标记；清理 23 个占位 URL；补齐 US-020/US-049 决策章节。
- 测试 MarkdownToFigma agent，确认其为只读 guideline 生成器。
- 讨论 Figma 直接绘图方案（cursor-talk-to-figma-mcp）与 DocumentToDesign 替代方案。

**关键产出**：
- Figma 设计规范文档更新。
- 50 个 US 准备好进入 Figma 设计交付。

**项目阶段**：设计交付规范与 handoff 准备。

---

## 2026-08-02（周六）

**主要工作**：
- 反馈 DocumentToFigma 生成原型质量不佳（首页、教练详情页、排班页布局问题）。
- 批量生成教练端 21 个 page-spec + 2 个全局文档（`CROSS-BATCH-PRINCIPLES-COACH.md`、`cross-page-check-coach.md`）。
- 上传教练端 page-spec 到 Calicat（file_id: 2083742072257646592）。
- 处理 Web 管理端 page-spec 上传失败问题（Calicat 插件连接不稳定），生成精简版 admin page-spec。
- 确认管理端公告/Banner/卡片配置页需求。
- 讨论 git push 失败（GitHub 443 连接问题）。

**关键问题**：
- Calicat MCP 连接不稳定，大文件上传易超时。
- AI 直接生成 Figma 视觉稿质量不足，需人工精修。

**项目阶段**：page-spec 批量生成与 Calicat 对接。

---

## 2026-08-03（周日）

**主要工作**：
- 生成 US-051 教练端微信授权登录三件套与 OpenSpec 四件套。
- 无其他显著记录。

**项目阶段**：用户故事与 page-spec 补充。

---

## 2026-08-04（周一）

**主要工作**：
- 复核第一批次登录与注册相关文档。
- 合并 `feature_init` 分支。

**项目阶段**：第一批次文档复核。

---

## 2026-08-05（周二）

**主要工作**：
- 完成 US-054 教练手机号验证码登录三件套与 OpenSpec 四件套。
- 重构教练登录流程：后端不再返回 `redirect_page`，由前端根据 `coach_status` 跳转。
- 教练重新入驻流程调整：US-040 移除数据隔离，允许编辑历史资料。
- 数据库选型：确认使用 MySQL 8.0 Community Edition。
- 决定第一版本使用微信小程序体验版，暂不接入微信支付/开放平台对象存储。

**关键决策**：
- 教练状态驱动页面跳转。
- 重新入驻复用 US-010 入驻页面。

**项目阶段**：登录相关 US 细化与开发前准备。

---

## 2026-08-06（周三）

**主要工作**：
- 重构教练数据模型：引入 `coach_application` 快照表，分离当前在职数据与待审批快照。
- 更新 US-010/US-011/US-040 用户故事、技术设计、测试计划与 OpenSpec。
- 输出 `dev-plan-batch1.md` 第一批次开发规划。

**关键决策**：
- 教练入驻/审核/重新入驻使用 `coach_application` 快照表方案。

**项目阶段**：数据模型重构与第一批次开发规划。

---

## 2026-08-07（周四）

**主要工作**：
- 第一批次复查。
- 无详细跨会话记录。

**项目阶段**：第一批次文档复查。

---

## 2026-08-08（周五）

**主要工作**：
- 更新 US-004/006/009/007/042/057/039/051/041/055 等用户故事与 page-spec。
- 添加 US-006 手机号登录页 page-spec（`U-phone-login-page.md`）。
- 创建 OpenSpec 自动同步 hooks（`.trae/rules/openspec/hooks.md`）。
- 管理端页面操作列简化（用户管理、教练管理、管理员管理）。
- 教练端 status=4 登录后改为进入首页，「我的」页面新增「查看离职申请」入口。
- 第一批次 US 在 INDEX.md 中标记为 `[APPROVED]`。

**关键产出**：
- `.trae/rules/openspec/hooks.md`
- 第一批次 page-spec 与 US 最终对齐。

**项目阶段**：第一批次 page-spec 与 US 锁定，进入开发就绪状态。

---

## 2026-08-09（周六）

**主要工作**：
- 确认第一批次可进入开发。
- 处理 Docker Desktop 因奇安信拦截无法使用的问题，改用本地 MySQL/Redis。
- 确定前端开发规范：组件化、SCSS、公共组件库优先、响应式、导出图片、非强制 lint。
- 从 US-053 管理员账号密码登录开始第一批次开发。
- 完成 US-053 端到端验证：后端 7 个测试通过，前端登录/首页/登出联调通过。
- 配置本地 MySQL 与 Redis 密码 `leyo1234`。

**关键决策**：
- 开发期使用本地 MySQL/Redis，不依赖 Docker。
- 管理端使用 Vue 3 + Element Plus，小程序使用 Taro 4 + React 18。

**项目阶段**：工程初始化完成，第一批次首个 US 开发完成。

---

## 2026-08-10（周日）

**主要工作**：
- 完成 G4 账号生命周期组后端（US-007/US-039/US-041）。
- 修复 AdminAuthenticationFilter principal 设置 bug、DevDataInitializer admin role 大小写问题、Mockito strict matching 问题。
- 开始 G1 登录授权组开发（US-004/US-006/US-051/US-054）。
- 前端继续开发对应页面。
- 后端 72 个测试全部通过。

**项目阶段**：第一批次后端核心组开发完成，前端页面并行开发。

---

## 2026-08-11（周一）

**主要工作**：
- 提交当前代码到 `feature_init` 分支。
- 讨论下一批次开发方向：建议核心业务闭环（浏览发现 → 可约时段 → 套餐购买 → 订单管理）。
- 修复微信小程序「Page ... has not been registered yet」错误：添加 process.env 安全兜底、创建 protocol 占位页、注册页面。
- 确认后端开发标准（四层架构、DTO/VO/Entity、统一响应、MyBatis-Plus、Flyway）。
- 修复管理端教练离职审批页样式与启动页样式。
- 在 `dev-plan-batch1.md` 新增§6 Calicat 设计稿还原偏差根因分析。
- 启动后端（localhost:8080）与管理前端（localhost:3000）。

**关键问题**：
- 微信小程序开发者工具缓存/路径问题导致页面未注册错误。
- 前端页面与 Calicat 设计稿存在偏差。

**项目阶段**：第一批次代码提交与视觉还原问题暴露。

---

## 2026-08-12（周二）

**主要工作**：
- G2 组联调与代码审查：修复手机号加密固定 IV、JWT 硬编码、换绑并发、文件上传 Content-Type 校验等 HIGH/CRITICAL 问题。
- G3 开发启动。
- 修复历史集成测试失败问题（AdminAuthService selectList、@Transactional、缺少 import 等）。
- G5 修复 AdminAuthenticationFilter：限制 `/api/admin/**`、排除 `/api/admin/auth/`、实时校验管理员状态、统一 UNAUTHORIZED 返回、新增 DUPLICATE_KEY 错误码。
- G6 开发完成：US-012（教练主页/参考单价）、US-052（用户退出）、US-056（教练退出）。
- US-045 套餐模板规则更新：active 状态不可编辑、已购实例为快照、新增 `package_mode` 字段。
- US-050 套餐状态机修正：过期释放 reserved、exhausted 为终态、取消预约后状态保持规则。
- US-046/027/028 订单与退款模型重构：独立 `order` 表，退款审批并入 A-订单管理页。

**关键产出**：
- 后端 121 个测试通过。
- 第一批次主要后端与前段功能开发完成。

**项目阶段**：第一批次多组并行开发与代码审查。

---

## 2026-08-13（周三）

**主要工作**：
- 验证前端页面样式，管理端/用户端/教练端仍有与设计稿不一致之处。
- 第二批次用户故事梳理（购买套餐、确认订单、退款）。
- 第二批次套餐相关文档梳理。
- 第一批次页面 Calicat 还原验证：识别 P0 修复项（用户资料页、协议浮层、教练资料页、管理员账号页）。
- 修复教练端小程序登录、入驻、资料页、离职申请页与 Calicat 不一致问题。
- 更新 US-037：学员详情页关联套餐卡片仅展示剩余课时，套餐详情独立为 C-package-detail-page.md。
- 移除教练卡片中的性别字段。
- 修复 web-admin/miniapp-user/miniapp-coach HIGH 优先级代码审查问题。

**关键产出**：
- 第一批次视觉还原偏差修复。
- 第二批次规划初稿。

**项目阶段**：第一批次收尾修复 + 第二批次规划启动。

---

## 2026-08-14（周四）

**主要工作**：
- US-045 适用教练字段从单选改为多选，更新数据模型为 `package_template_coach` 关联表，`coach_ids: number[]`。
- 将 US-043 从「手动冻结解冻套餐」扩展为「管理员-查看与管理用户套餐」，包含列表/详情/冻结/解冻/延期/发起退款。
- A-package-detail-page.md 明确自定义套餐快照信息卡规则。
- 管理后台 page-spec 术语统一：「双日期选择器」→「日期区间选择器」。
- A-order-detail-page.md 从独立详情页改为弹窗（560px/80vh），删除状态时间轴卡与订单信息卡操作区，同步更新 7 个引用文件。
- 更新 C-student-list-page.md/C-student-detail-page.md 到 US-037。
- 更新 U-coach-detail-page.md/U-package-list-page.md/U-package-detail-page.md。
- 更新 U-order-confirm-page.md（未成年人手机号无需验证）、U-custom-package-config-page.md（先选教练后配置）、U-payment-page.md（取消支付按钮）、U-my-package-detail-page.md（套餐信息卡重命名）。
- 更新 `docs/figma/第二批次页面梳理.md`，新增批次目标、处理原则、6 项任务、页面清单。
- 生成第一批次经验手册 `docs/figma/batch1-lessons-learned.md` 与 Calicat 强制 hooks `.trae/rules/figma/calicat-mcp-hooks.md`。

**关键决策**：
- 套餐模板适用教练改为多选，购买时学员从多名教练中选择一名。
- 订单详情改为弹窗，退款审批并入订单管理页。
- 建立 Calicat 强制读取机制，避免后续批次视觉还原偏差。

**项目阶段**：第二批次需求/设计文档精化 + 第一批次经验沉淀。

---

## 跨阶段关键数据

| 指标 | 数值 | 备注 |
|------|------|------|
| 用户故事总数 | 50 | US-001 ~ US-057（含跳号） |
| 第一批次 US 数 | 19 | 用户端 6 / 教练端 8 / 管理端 5 |
| 第二批次 US 数 | 12 | 用户端 9 / 教练端 1 / 管理端 5 |
| page-spec 文件数 | ~90 | 用户端/教练端/管理端 + 全局文档 |
| 后端测试数 | 121 | 截至 08-12 全部通过 |
| Git commit 数 | ~50 | 07-27 至 08-14 |
| 已开发完成组 | G1~G6 | 登录授权、协议资料、入驻审核、账号生命周期、管理后台账号、个人主页与退出 |

---

## 高频问题与改进动作

| 问题 | 发现时间 | 改进动作 |
|------|---------|---------|
| 前端页面与 Calicat 设计稿不一致 | 08-11 | 08-14 建立 `batch1-lessons-learned.md` + `calicat-mcp-hooks.md` |
| OpenSpec 文档与 US 文档不同步 | 08-08 | 建立 `.trae/rules/openspec/hooks.md` 自动同步 |
| 手机号加密固定 IV | 08-12 | 08-12 增加随机 IV + phone_hash 唯一索引 |
| 管理员认证未实时校验状态 | 08-12 | 08-12 AdminAuthenticationFilter 增加实时状态校验 |
| Calicat MCP 上传大文件超时 | 08-02 | 08-02 生成 slim 版 page-spec，单文件上传 |

---

## 下一步建议

1. **进入第二批次开发**：按 `docs/figma/第二批次页面梳理.md` 的 6 项任务顺序执行。
2. **严格执行 Calicat 强制 hooks**：每个前端页面开工前读取设计稿、导出截图、生成 Token。
3. **补齐第二批次设计稿**：优先完成用户端套餐/订单链路、管理端套餐/订单管理页的设计稿。
4. **引入 visual-review Agent**：在 code-reviewer 之后增加视觉还原审查。
5. **处理剩余 P2 问题**：短信验证码失败次数限制、JWT 撤销、X-Forwarded-For IP 伪造等安全加固。

### [auto] 2026-08-14 补充记录

> 本小节由 scripts/update-daily-log.ps1 在 commit 后自动生成，用于保留原始 commit 与会话记录。
> 建议每日结束时由 Agent/人工基于本节内容更新上方的精炼总结。

- 提交 [6af46c7] docs: 第二批次页面梳理及设计稿设计
- 会话 [6a7855515e4d5b2256e6f28e] User identified that the '适用教练' (applicable coaches) field in A-package-edit-modal.md was specified as single-select but needed to be multi-select. After confirming the change, the assistant updated multiple documents: A-package-edit-modal.md (changed to multi-select with 'at least 1 selection' rule
- 会话 [6a7b21d06286d1392896fe0c] User decided to create a new user story US-043 for package management instead of merging with existing US-046 or US-045. US-043 was renamed from '管理员-手动冻结解冻套餐' to '管理员-查看与管理用户套餐', with its folder and related INDEX.md, US-053 dependency list, us-batch-handoff.md, and tmp_us_extract.json updated accor
- 会话 [6a7b21d06286d1392896fe0c] User updated the A-package-detail-page.md documentation to clarify package detail page display rules. First, corrected the '教学类型' field in §3.2 to '班级规模：一对一 / 一对二 / 一对三'. Then, added specifications for custom package snapshot information cards in §3.2, including not displaying '套餐模式' Tag, '查看当前模板' l
- 会话 [6a7855515e4d5b2256e6f28e] 用户对项目文档中的术语和内容提出修改要求。首先，将 docs/figma/page-spec/ 目录下所有管理后台 page-spec 文件中的「双日期选择器」统一替换为「日期区间选择器」，涉及 A-coach-audit-queue-page.md 等 9 个文件。接着，删除 A-order-detail-page.md 中的「状态时间轴卡」及其在 §2 状态维度、§6 四态设计等 6 处相关引用。然后，删除该文件订单信息卡中的「操作区」一行，明确操作按钮统一保留在底部固定操作栏。最后，用户要求重新组织 A-order-detail-page.md 的信息展示，将其从独立详情页改为弹窗（A-
- 会话 [6a7e7776be0e117af9f4faa6] User updated multiple page-spec documents for the leyoSwimming project. First, C-student-list-page.md was updated to US-037, adding active/historical student tabs, revising card fields (removing course type, adding age/gender/minor flag), and removing appointment-related entries. C-student-detail-pa
- 会话 [6a7e7776be0e117af9f4faa6] 用户确认已购套餐底部教练信息区的展示内容应与已选教练自适应卡保持一致，并支持点击跳转教练详情页。已更新 `U-package-detail-page.md`：§3.8.2 已购套餐底部教练信息区新增评分、专攻泳姿、右侧箭头字段；增加点击跳转 `U-教练详情页`（携带 `coach_id`）的交互行为；§5 跳转动线矩阵新增对应行；§8 AI 检查清单同步更新；版本号提升至 v1.3，changelog 增加相关条目并补充归属 US-021。
- 会话 [6a7e7776be0e117af9f4faa6] User specified that minor phone numbers do not require verification in the MVP phase, leading to updates in U-order-confirm-page.md: adding notes in §3.6, modifying guardian phone field to input-only without verification, removing verification code line, updating submit button disabled conditions, a
