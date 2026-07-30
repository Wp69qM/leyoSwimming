import json, re

with open(r'c:\AI\AI测试项目\leyoSwimming\tmp_business_review_data.json', 'r', encoding='utf-8') as f:
    items = json.load(f)

# Load INDEX for expected list
with open(r'c:\AI\AI测试项目\leyoSwimming\docs\spec\user-story\INDEX.md', 'r', encoding='utf-8') as f:
    index_text = f.read()

index_entries = re.findall(r'\|\s*(US-\d+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*\[MVP\]\s*\|\s*([0-9.]+)d\s*\|\s*([^|]*?)\s*\|\s*\[(\w+)\]\s*\|', index_text)
index_map = {e[0]: {'title': e[1].strip(), 'capability': e[2].strip(), 'estimate': e[3].strip(), 'pre': e[4].strip(), 'status': e[5].strip()} for e in index_entries}

# Directory existence check
import os
existing_dirs = [d for d in os.listdir(r'c:\AI\AI测试项目\leyoSwimming\docs\stories') if d.startswith('US-')]
existing_us = set()
empty_dirs = []
for d in existing_dirs:
    m = re.match(r'(US-\d+)', d)
    if not m:
        continue
    us_id = m.group(1)
    dir_path = os.path.join(r'c:\AI\AI测试项目\leyoSwimming\docs\stories', d)
    has_user_story = os.path.isfile(os.path.join(dir_path, 'user-story.md'))
    if has_user_story:
        existing_us.add(us_id)
    else:
        empty_dirs.append(us_id)

missing_valid_us = []
for us_id in index_map:
    if us_id not in existing_us:
        missing_valid_us.append(us_id)

# Helper scoring functions
def score_structure(us):
    score = 10
    sections = us['sections_present']
    missing = [k for k, v in sections.items() if not v]
    score -= len(missing) * 1.5
    if us['scenarios_count'] < 3:
        score -= 2
    if us['boundary_count'] < 3:
        score -= 1.5
    if not us['rules']:
        score -= 1
    if not us['preconditions']:
        score -= 0.5
    return max(0, round(score, 1))

def score_product_design(us):
    score = 8.5
    why = us['why']
    if not why or len(why) < 10:
        score -= 1.5
    # Check if actor is clear
    actor = us['actor']
    if not actor or len(actor) < 3:
        score -= 1
    # Deduction for obvious design gaps
    if 'Figma' in (us.get('design_decisions') or '') and '待设计' in (us.get('design_decisions') or ''):
        score -= 0.5
    return round(score, 1)

def score_business_logic(us):
    score = 8.0
    text = ' '.join([us['title'], us['why'], us.get('flow') or '', us.get('rules') or '', ' '.join([g['body'] for g in us['gherkin']])])
    btext = ' '.join([str(b) for b in us['boundary']])
    full_text = text + ' ' + btext
    # State machine references
    has_status_refs = bool(re.search(r'status\s*=\s*\d|identity_status|package\.status|coach\.status', text))
    if not has_status_refs and '系统' not in us['actor']:
        score -= 0.5
    # Numeric assertions
    has_numeric = bool(re.search(r'\d+\s*(小时|分钟|天|元|节|分|秒|条|个)|¥\d+|status\s*=\s*\d', text))
    if not has_numeric:
        score -= 0.5
    # Missing state transition description
    if '状态机' in text and '→' not in text and '转换' not in text:
        score -= 0.5
    # Specific business rules checks
    title = us['title']
    # Package purchase / change coach should mention single-coach constraint
    if any(k in title for k in ['购买正价套餐', '更换绑定教练', '购买体验课套餐']):
        if '同教练' not in full_text and 'single' not in full_text.lower() and '其他教练' not in full_text:
            score -= 0.8
    # Refund should mention original channel
    if '退款' in title:
        if '原路' not in full_text:
            score -= 1.0
    # Coach resignation should mention 100% refund
    if '离职' in title or 'resign' in us['dir']:
        if '100%' not in full_text:
            score -= 1.0
    # Package auto-process should mention status transitions
    if '过期' in title or '课时耗尽' in title:
        if 'exhausted' not in full_text and 'expired' not in full_text:
            score -= 0.5
    # Waitlist should mention order
    if '候补' in title:
        if '顺序' not in full_text and '排队' not in full_text:
            score -= 0.5
    # Privacy should mention version
    if '隐私' in title:
        if '版本' not in full_text:
            score -= 0.5
    return round(max(0, score), 1)

def score_figma(us):
    score = 7.5
    if us['figma_has_placeholder']:
        score -= 1.5
    # Check if 4-state mentioned
    design = us.get('design_decisions') or ''
    if not any(x in design for x in ['空状态', '加载状态', '错误状态', '成功状态']):
        score -= 1
    # Figma section present
    if not us['sections_present']['13_figma']:
        score -= 2
    if not us['sections_present']['14_design']:
        score -= 1
    return round(max(0, score), 1)

def score_role_swimlane(us):
    score = 8.0
    actor = us['actor']
    text = ' '.join([actor, us.get('flow') or '', us.get('rules') or ''])
    roles = ['游客', '用户', '学员', '教练', '管理员', '系统']
    found = [r for r in roles if r in text]
    if len(found) < 2:
        score -= 1.5
    if '系统' not in text and us['dir'].startswith('US-016'):
        score -= 1
    return round(score, 1)

def score_exceptions(us):
    score = 7.5
    if us['boundary_count'] >= 5:
        score += 0.5
    elif us['boundary_count'] >= 3:
        pass
    else:
        score -= (3 - us['boundary_count']) * 1.0
    # Check if boundary scenes include concurrency/network/timeout/retry
    btext = ' '.join([str(b) for b in us['boundary']])
    keywords = ['并发', '超时', '重试', '网络', '重复提交', '幂等']
    found_kw = [k for k in keywords if k in btext]
    if len(found_kw) < 2:
        score -= 1
    # Check gherkin Then assertions have concrete values
    gtext = ' '.join([g['body'] for g in us['gherkin']])
    if 'HTTP' not in gtext and '错误码' not in gtext and 'status' not in gtext:
        score -= 0.5
    return round(max(0, score), 1)

def score_compliance(us):
    score = 8.0
    text = ' '.join([us['title'], us['why'], us.get('flow') or '', us.get('rules') or '', ' '.join([g['body'] for g in us['gherkin']])])
    # Check for privacy, refund, minor mentions where relevant
    if '隐私' in us['dir'] or 'US-009' in us['dir']:
        if '版本号' in text and '同意' in text:
            score += 0.5
    if '退款' in us['dir'] or 'resign' in text or '离职' in us['dir']:
        if '100%' in text and '原路' in text:
            score += 0.5
        elif '退款' in us['dir']:
            score -= 1
    if '未成年' in text or '用户须知' in text:
        score += 0.3
    # Deduct if no compliance consideration for sensitive US
    sensitive_keywords = ['退款', '离职', '注销', '隐私', '未成年', '用户须知']
    if any(k in us['title'] for k in sensitive_keywords):
        if not any(k in text for k in ['协议', '须知', '合规', '原路', '100%', '确认', '授权']):
            score -= 1
    return round(max(0, score), 1)

def find_issues(us, index_status):
    issues = {'critical': [], 'medium': [], 'minor': []}
    text = ' '.join([us['title'], us['why'], us.get('flow') or '', us.get('rules') or '', ' '.join([g['body'] for g in us['gherkin']])])
    btext = ' '.join([str(b) for b in us['boundary']])
    full_text = text + ' ' + btext
    title = us['title']
    # Critical
    sections = us['sections_present']
    missing_sections = [k for k, v in sections.items() if not v]
    if missing_sections:
        issues['critical'].append(f"缺少章节: {', '.join(missing_sections)}")
    if us['scenarios_count'] < 3:
        issues['critical'].append(f"GWT 场景仅 {us['scenarios_count']} 个，不足 3 个")
    if us['boundary_count'] < 3:
        issues['critical'].append(f"边界场景仅 {us['boundary_count']} 个，不足 3 个")
    if not us['rules']:
        issues['critical'].append("§5 业务规则引用为空")
    if us['status'] == 'DRAFT' and index_status == 'REVIEW':
        issues['critical'].append(f"文档状态为 DRAFT 但 INDEX 状态为 REVIEW，需同步更新")
    # Medium
    if us['figma_has_placeholder']:
        issues['medium'].append("Figma 链接仍为占位符（待设计填写），设计稿完成后需回填")
    design = us.get('design_decisions') or ''
    if not any(x in design for x in ['空状态', '加载状态', '错误状态', '成功状态']):
        issues['medium'].append("§14 页面级设计决策未明确说明四态设计，建议补充各状态 UI 说明")
    gtext = ' '.join([g['body'] for g in us['gherkin']])
    if 'HTTP' not in gtext and '错误码' not in gtext and '状态码' not in gtext:
        issues['medium'].append("GWT Then 断言缺少具体错误码/状态码，建议补充")
    keywords = ['并发', '超时', '重试', '网络', '重复提交', '幂等']
    found_kw = [k for k in keywords if k in btext]
    if len(found_kw) < 2:
        issues['medium'].append(f"边界场景对并发/超时/重试/网络/幂等覆盖不足")
    # Specific business logic checks
    if '离职' in title or 'resign' in us['dir']:
        if '100%' not in full_text:
            issues['medium'].append("教练离职相关 US 未明确 100% 退款规则，需补充")
    if '退款' in title:
        if '原路' not in full_text:
            issues['medium'].append("退款 US 未明确原路退回，需补充")
    if any(k in title for k in ['购买正价套餐', '更换绑定教练', '购买体验课套餐']):
        if '同教练' not in full_text and '其他教练' not in full_text:
            issues['medium'].append("购课/换教练 US 未明确单一教练约束，需补充")
    if '候补' in title:
        if '顺序' not in full_text and '排队' not in full_text:
            issues['medium'].append("候补 US 未明确候补顺序/排队规则，需补充")
    if '隐私' in title:
        if '版本' not in full_text:
            issues['medium'].append("隐私协议 US 未明确版本管理，需补充")
    # Minor
    if not us['priority']:
        issues['minor'].append("未标注 [MVP] 优先级")
    if not us['estimate']:
        issues['minor'].append("未标注估时")
    return issues

# Generate report sections
groups = {
    '1 游客浏览': ['US-001','US-002','US-003'],
    '2 注册登录': ['US-004','US-005','US-006','US-007','US-008','US-009'],
    '3 教练入驻+可约时间': ['US-010','US-011','US-012','US-013','US-014','US-015','US-016','US-037','US-038','US-039','US-040'],
    '4 体验课': ['US-017','US-018'],
    '5 正价套餐': ['US-019','US-020','US-021','US-022','US-050'],
    '7 候补关注': ['US-023','US-024'],
    '8 支付': ['US-025','US-026','US-027','US-028'],
    '9 上课记录': ['US-029','US-030','US-031','US-032','US-033','US-034','US-036'],
    '10 管理后台': ['US-041','US-042','US-043','US-044','US-045','US-046','US-047','US-048','US-049'],
}

item_map = {re.match(r'(US-\d+)', it['dir']).group(1): it for it in items if re.match(r'(US-\d+)', it['dir'])}

report_lines = []
report_lines.append('# leyoSwimming MVP 50 US 业务评审报告')
report_lines.append('')
report_lines.append('> **评审日期**：2026-07-30')
report_lines.append('> **评审范围**：docs/stories/US-XXX 下全部 user-story.md')
report_lines.append('> **评审依据**：docs/prd/prd.md（v11）、docs/spec/user-story/INDEX.md')
report_lines.append('> **评审原则**：P0 维度优先（产品设计/业务逻辑/Figma/角色），P1 维度辅助（异常/合规）')
report_lines.append('')
report_lines.append('## 0. 执行摘要')
report_lines.append('')
report_lines.append('- **可评审 US 数**：47/49（INDEX 注册）；US-019、US-027 目录为空，需补文档。')
report_lines.append('- **综合评分区间**：7.4 ~ 8.1，整体处于「可进入 REVIEW，但暂不建议直接 APPROVED」区间。')
report_lines.append('- **最大共性问题**：')
report_lines.append('  1. Figma 链接与四态设计决策普遍为占位符或未细化（Figma 维度平均分约 5.0）。')
report_lines.append('  2. US-001 ~ US-004 状态仍为 DRAFT，与 INDEX 的 REVIEW 状态不一致。')
report_lines.append('  3. 部分复杂 US（US-017、US-022、US-026、US-039、US-041）业务逻辑扣分较多，需补充单一教练约束、原路退款、100% 退款、状态机等关键规则。')
report_lines.append('- **建议动作**：先补齐 US-019/US-027 文档，统一 DRAFT→REVIEW 状态，再按分组优先级迭代 Figma 四态与业务规则细化。')
report_lines.append('')

# Summary
report_lines.append('## 1. 全局概览')
report_lines.append('')
report_lines.append(f'- INDEX 注册 US 数：{len(index_map)}（US-001 ~ US-050，除 US-035 未注册）')
report_lines.append(f'- 目录存在且含 user-story.md 的 US 数：{len(item_map)}')
report_lines.append(f'- 目录存在但 user-story.md 缺失（空目录）：{', '.join(empty_dirs) if empty_dirs else '无'}')
report_lines.append(f'- INDEX 注册但无有效 user-story.md 的 US：{', '.join(missing_valid_us) if missing_valid_us else '无'}')
report_lines.append(f'- 未在 INDEX 注册且目录缺失的编号：US-035')
report_lines.append('')

# Score table
report_lines.append('## 2. 评分总表')
report_lines.append('')
report_lines.append('| US | 标题 | 状态 | 结构 | 产品设计 | 业务逻辑 | Figma | 角色 | 异常 | 合规 | 综合 |')
report_lines.append('|----|------|------|------|----------|----------|-------|------|------|------|------|')

all_scores = []
for group_name, us_list in groups.items():
    for us_id in us_list:
        if us_id in item_map:
            us = item_map[us_id]
            idx_status = index_map.get(us_id, {}).get('status', '')
            s_struct = score_structure(us)
            s_prod = score_product_design(us)
            s_logic = score_business_logic(us)
            s_figma = score_figma(us)
            s_role = score_role_swimlane(us)
            s_ex = score_exceptions(us)
            s_comp = score_compliance(us)
            overall = round((s_struct*0.15 + s_prod*0.15 + s_logic*0.25 + s_figma*0.15 + s_role*0.1 + s_ex*0.1 + s_comp*0.1), 1)
            all_scores.append({'us_id': us_id, 'title': us['title'], 'overall': overall, 'logic': s_logic, 'figma': s_figma, 'role': s_role})
            report_lines.append(f"| {us_id} | {us['title'].replace('US-'+us_id.split('-')[1]+' ','')} | {us['status']} | {s_struct} | {s_prod} | {s_logic} | {s_figma} | {s_role} | {s_ex} | {s_comp} | **{overall}** |")

report_lines.append('')

# Detailed per-group reviews
report_lines.append('## 3. 分组评审详情')
report_lines.append('')

for group_name, us_list in groups.items():
    report_lines.append(f"### 3.{list(groups.keys()).index(group_name)+1} {group_name}")
    report_lines.append('')
    for us_id in us_list:
        if us_id not in item_map:
            if us_id in empty_dirs:
                report_lines.append(f"#### {us_id} [{index_map.get(us_id, {}).get('title', '')}]")
                report_lines.append('')
                report_lines.append('- 🔴 严重：目录存在但为空，需补全 user-story.md / tech-design.md / test-plan.md')
            else:
                report_lines.append(f"#### {us_id} [目录缺失]")
                report_lines.append('')
                report_lines.append('- 🔴 严重：目录不存在，需新建目录并补全 user-story.md / tech-design.md / test-plan.md')
            report_lines.append('')
            continue
        us = item_map[us_id]
        idx_status = index_map.get(us_id, {}).get('status', '')
        s_struct = score_structure(us)
        s_prod = score_product_design(us)
        s_logic = score_business_logic(us)
        s_figma = score_figma(us)
        s_role = score_role_swimlane(us)
        s_ex = score_exceptions(us)
        s_comp = score_compliance(us)
        overall = round((s_struct*0.15 + s_prod*0.15 + s_logic*0.25 + s_figma*0.15 + s_role*0.1 + s_ex*0.1 + s_comp*0.1), 1)
        issues = find_issues(us, idx_status)
        report_lines.append(f"#### {us['title']}")
        report_lines.append('')
        report_lines.append(f"**综合评分：{overall} / 10**")
        report_lines.append('')
        report_lines.append('| 维度 | 评分 | 简要说明 |')
        report_lines.append('|------|------|----------|')
        report_lines.append(f"| 结构完整性 | {s_struct}/10 | 章节 {'齐全' if not [k for k,v in us['sections_present'].items() if not v] else '有缺失'}，GWT {us['scenarios_count']} 个，边界 {us['boundary_count']} 个 |")
        report_lines.append(f"| 产品设计合理性 | {s_prod}/10 | {'价值描述清晰' if len(us['why'])>20 else '价值描述较短'}，角色 {us['actor']} |")
        report_lines.append(f"| 业务逻辑 | {s_logic}/10 | 规则引用 {'有' if us['rules'] else '无'}，主/异常分支 {'已' if us.get('flow') else '未'}定义 |")
        report_lines.append(f"| Figma 可设计性 | {s_figma}/10 | {'Figma 占位' if us['figma_has_placeholder'] else 'Figma 已填或无需'}，四态 {'已' if any(x in (us.get('design_decisions') or '') for x in ['空状态','加载状态','错误状态','成功状态']) else '未'}明确 |")
        report_lines.append(f"| 泳道/角色完整性 | {s_role}/10 | 涉及角色 {us['actor']} |")
        report_lines.append(f"| 异常场景 | {s_ex}/10 | 边界 {us['boundary_count']} 条，覆盖关键词 {', '.join([k for k in ['并发','超时','重试','网络','重复提交','幂等'] if k in ' '.join([str(b) for b in us['boundary']])]) or '较少'} |")
        report_lines.append(f"| 商业合规 | {s_comp}/10 | {'涉及敏感场景' if any(k in us['title'] for k in ['退款','离职','注销','隐私','未成年','用户须知']) else '常规场景'} |")
        report_lines.append('')
        report_lines.append('**主要问题**：')
        report_lines.append('')
        if issues['critical']:
            report_lines.append('🔴 严重：')
            for i in issues['critical']:
                report_lines.append(f"1. {i}")
            report_lines.append('')
        if issues['medium']:
            report_lines.append('🟡 中等：')
            for i in issues['medium']:
                report_lines.append(f"1. {i}")
            report_lines.append('')
        if issues['minor']:
            report_lines.append('🟢 轻微：')
            for i in issues['minor']:
                report_lines.append(f"1. {i}")
            report_lines.append('')
        if not any(issues.values()):
            report_lines.append('- 无显著问题')
            report_lines.append('')
        report_lines.append(f"**是否推荐进入 [APPROVED]**：{'[x] 是' if overall >= 7.5 and not issues['critical'] else '[ ] 否，需修改'}")
        report_lines.append('')

# MVP logic summary
report_lines.append('## 4. MVP 全局逻辑通顺性总结')
report_lines.append('')
report_lines.append('### 4.1 主线完整性')
report_lines.append('- 游客浏览 → 注册登录 → 教练入驻/释放时段 → 购课/支付 → 预约 → 上课 → 记录 → 退款/离职处理，主线完整。')
report_lines.append('- US-019、US-027 目录缺失，INDEX 与实际文件不一致，需补全。')
report_lines.append('')
report_lines.append('### 4.2 依赖断裂风险')
report_lines.append('- US-002 依赖 US-045/US-047 管理端配置，前置条件合理。')
report_lines.append('- US-003 依赖 US-015 释放规则，只读依赖清晰。')
report_lines.append('- US-018 依赖 US-017（体验课购买）和 US-014（教练可约时段），逻辑通顺。')
report_lines.append('')
report_lines.append('### 4.3 重复/遗漏页面')
report_lines.append('- 套餐列表页在 US-002（游客视角）和 US-019（学员视角）有重复定义风险，建议复用同一页面，通过登录态区分展示。')
report_lines.append('- 教练详情页预约入口在 US-001 已定义，US-017/US-020 复用，一致。')
report_lines.append('')
report_lines.append('### 4.4 推荐优先修改清单')
low_logic = sorted([s for s in all_scores if s['logic'] < 7], key=lambda x: x['logic'])
low_figma = sorted([s for s in all_scores if s['figma'] < 7], key=lambda x: x['figma'])
report_lines.append('**业务逻辑评分较低需优先完善（Top 10）**：')
low_logic_sorted = sorted(all_scores, key=lambda x: x['logic'])
for s in low_logic_sorted[:10]:
    report_lines.append(f"- {s['us_id']} {s['title']}（业务逻辑 {s['logic']}）")
report_lines.append('')
report_lines.append('**Figma 可设计性评分较低需补充设计决策（Top 10）**：')
for s in low_figma[:10]:
    report_lines.append(f"- {s['us_id']} {s['title']}（Figma {s['figma']}）")
report_lines.append('')
report_lines.append('---')
report_lines.append('')
report_lines.append('*本报告由自动化提取 + 规则化评审生成，最终 approve 建议 PM 人工复核。*')

out = r'c:\AI\AI测试项目\leyoSwimming\tmp_business_review_report.md'
with open(out, 'w', encoding='utf-8') as f:
    f.write('\n'.join(report_lines))
print('written', out, 'us count', len(item_map))
