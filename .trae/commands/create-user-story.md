---
name: "Create User Story"
description: "按 docs/spec 规范创建用户故事三件套并注册 INDEX.md（/create-user-story）"
---

按 `docs/spec/` 规范创建一个新用户故事（US），产出三件套（user-story.md / tech-design.md / test-plan.md）并注册到 INDEX.md。

**Input**: `/create-user-story` 后的参数为 US 描述（角色 + 动作 + 目标），如「学员单次预约教练时段」。

**Steps**

1. **无输入时询问**

   Ask the user (open-ended):
   > "要创建什么用户故事？请用「角色 + 动作 + 目标」描述，如：学员单次预约教练时段。"

   不要在没有理解用户意图前继续。

2. **读取规范（强制，先于任何写入）**

   依次 Read：
   - `docs/spec/user-story/SPECIFICATION.md`
   - `docs/spec/user-story/TEMPLATE.md`
   - `docs/spec/user-story/EXAMPLE.md`
   - `docs/spec/user-story/INDEX.md`

   并按 `.trae/rules/spec/spec-routing.md` Step 2 输出「Spec 开工检查清单」，全部勾选后才能写入。

3. **分配编号与目录名**

   - 从 INDEX.md §1 找出当前最大编号，新编号 = max + 1；**编号一经分配不重用**（已删除/CANCELLED 的编号不回收）
   - 目录名：`docs/stories/US-XXX-[角色]-[动作]/`（与 INDEX 既有命名惯例一致）
   - 标题必须符合「[角色] + [动作] + [目标]」（如 ✅「学员单次预约教练时段」/ ❌「预约功能开发」）

4. **确认拆分合理性**

   按 SPECIFICATION §3 INVEST / §5 拆分粒度自检：
   - 估时 0.5-3 人天；超出 → 建议拆分并与用户确认后再继续
   - 单一职责；跨页面 / 跨状态机 → 拆分为多个 US
   - 确定复杂度等级 L1（0.5d）/ L2（1-1.5d）/ L3（2d+）→ 决定 §6 Gherkin 场景数量（3 / 5 / 7）

5. **创建三件套**

   - **user-story.md**：复制 `TEMPLATE.md` 起步，替换全部 `{{占位符}}`，按 15 章节填写：
     - §5 业务规则引用必须指向 `docs/prd/prd.md` 真实章节（先 Read 验证章节存在，禁止凭记忆引用）
     - §6 Gherkin 场景数量符合 L1/L2/L3 分级（至少 1 正常 + 2 异常）
     - §7 数据/API/状态机影响与 §5 引用的 PRD 规则一致
     - §9 依赖关系与 INDEX §4 一致，无循环依赖
     - §10 INVEST ≥ 5/6，§11 完整性检查全部勾选
     - 状态置 `[DRAFT]`
   - **tech-design.md**：按 `docs/spec/tech-design/README.md` 的 11 个必填章节建占位骨架，标注「待开发填写」
   - **test-plan.md**：按 `docs/spec/test-plan/README.md` 的章节结构建占位骨架，标注「待开发+QA 填写」

6. **更新 INDEX.md**

   - §1 US 清单表追加一行：编号 / 标题 / 所属能力 / 优先级 / 估时 / 前置 US / `[DRAFT]`
   - §2 能力覆盖映射：若属于新能力或现有映射需变更，同步补充
   - §3 状态机触达映射：若本 US 触发状态转换，补充到对应状态机行
   - §4 依赖关系总览：插入新依赖边
   - §6 变更日志追加一行（注明新增 US 编号与标题）

7. **OpenSpec 同步**

   按 `.trae/rules/openspec/hooks.md` 流程：运行 `openspec list --json` 查找 `us-xxx-` 前缀的 active change：
   - 找到唯一匹配 → 同步该 change 的 artifacts
   - 未找到 → 提示用户「未找到对应 OpenSpec change，是否需要创建」，不静默跳过

8. **自检并汇报**

   按 SPECIFICATION §12 检查清单核对本次产出（全量覆盖性检查留待批次评审），输出：
   - 新建文件列表（含路径）
   - 编号、复杂度等级（L1/L2/L3）、场景数、估时
   - INVEST 自检结果
   - INDEX.md 更新摘要
   - OpenSpec 同步结果

**Guardrails**

- 禁止自创 user-story.md 结构，必须从 TEMPLATE.md 复制起步
- 禁止复用已删除 / CANCELLED 的编号
- 禁止跳过 PRD 章节验证直接写 §5 引用
- 新 US 状态只能是 `[DRAFT]`；推进到 `[REVIEW]` / `[APPROVED]` 必须走 `us-business-reviewer` 评审
- 若识别出该需求实为技术任务（如「搭建 CI」），停止并提示用户：这不是用户故事，应作为技术任务独立跟踪
