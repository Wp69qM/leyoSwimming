import os, re, json, glob

root = r'c:\AI\AI测试项目\leyoSwimming\docs\stories'
items = []

def read_section(text, start_marker, end_markers):
    """Extract a section starting with start_marker until one of end_markers or end of text."""
    idx = text.find(start_marker)
    if idx == -1:
        return None
    start = idx + len(start_marker)
    end = len(text)
    for em in end_markers:
        nxt = text.find(em, start)
        if nxt != -1 and nxt < end:
            end = nxt
    return text[start:end].strip()

def extract_gherkin(text):
    """Extract all gherkin scenario blocks with their titles."""
    blocks = []
    pattern = r'###\s*6\.\d+\s*场景.*?\n(.*?)(?=###\s*6\.\d+|##\s*7\.|##\s*7\b|<!---|$)'
    # Better: find section 6, then split by ### 6.x
    sec6 = read_section(text, '## 6.', ['## 7.', '## 7 ', '<!---'])
    if not sec6:
        sec6 = read_section(text, '## 6.', ['## 7.', '<!---'])
    if not sec6:
        return blocks
    # Split by scenario headers
    parts = re.split(r'\n###\s*6\.\d+\s*', sec6)
    for p in parts:
        p = p.strip()
        if not p:
            continue
        # First line is scenario title
        lines = p.splitlines()
        title = lines[0].strip() if lines else ''
        body = '\n'.join(lines[1:]).strip()
        if '```gherkin' in body:
            blocks.append({'title': title, 'body': body})
    return blocks

def extract_boundary(text):
    """Extract boundary scene items."""
    sec8 = read_section(text, '## 8.', ['## 9.', '<!---'])
    if not sec8:
        return []
    # Look for numbered or ### subsections
    items_b = []
    # Pattern: ### 8.x or | # | scene | handling |
    if '|' in sec8 and '场景' in sec8:
        # table format
        rows = [r.strip() for r in sec8.splitlines() if r.strip().startswith('|') and r.strip() != '|' and not r.strip().startswith('|---')]
        for r in rows:
            cells = [c.strip() for c in r.split('|')]
            cells = [c for c in cells if c]
            if len(cells) >= 3 and cells[0].isdigit():
                items_b.append({'idx': cells[0], 'scene': cells[1], 'handling': cells[2]})
    else:
        # subsection format
        subs = re.split(r'\n###\s*8\.\d+\s*', sec8)
        for s in subs:
            s = s.strip()
            if not s:
                continue
            lines = s.splitlines()
            title = lines[0].strip() if lines else ''
            body = '\n'.join(lines[1:]).strip()
            items_b.append({'title': title, 'body': body})
    return items_b

def section_exists(text, marker):
    return text.find(marker) != -1

def extract_rules(text):
    sec5 = read_section(text, '## 5.', ['## 6.', '<!---'])
    if not sec5:
        return None
    return sec5

def extract_deps(text):
    pre = read_section(text, '### 9.1', ['### 9.2', '<!---'])
    post = read_section(text, '### 9.2', ['## 10.', '<!---'])
    return {'pre': pre, 'post': post}

def extract_design_decisions(text):
    sec14 = read_section(text, '## 14.', ['## 15.', '<!---'])
    return sec14

for d in sorted(os.listdir(root)):
    if not d.startswith('US-'): continue
    path = os.path.join(root, d, 'user-story.md')
    if not os.path.exists(path): continue
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    # basic
    m = re.search(r'\*\*状态\*\*\s*[:：]\s*\[([^\]]+)\]', text)
    status = m.group(1) if m else ''
    m = re.search(r'^# (US-\d+\s+.+)$', text, re.M)
    title = m.group(1).strip() if m else d
    m = re.search(r'\*\*角色（Actor）\*\*\s*[:|]\s*(.+)', text)
    actor = m.group(1).strip() if m else ''
    m = re.search(r'\*\*业务价值（Why）\*\*\s*[:|]\s*(.+)', text)
    why = m.group(1).strip() if m else ''
    m = re.search(r'\*\*优先级\*\*\s*[:|]\s*(\[MVP\])', text)
    priority = m.group(1) if m else ''
    m = re.search(r'\*\*估时\*\*\s*[:|]\s*([0-9.]+)\s*人天', text)
    estimate = m.group(1) if m else ''
    # sections
    gherkin = extract_gherkin(text)
    boundary = extract_boundary(text)
    rules = extract_rules(text)
    deps = extract_deps(text)
    design = extract_design_decisions(text)
    # preconditions
    pre_sec = read_section(text, '## 3.', ['## 4.', '<!---'])
    preconditions = pre_sec
    # flow
    flow_sec = read_section(text, '## 4.', ['## 5.', '<!---'])
    # figma placeholders
    figma_link_sec = read_section(text, '## 13.', ['## 14.', '<!---'])
    figma_has_placeholder = '待设计填写' in (figma_link_sec or '') or '待补充' in (figma_link_sec or '')
    figma_all_empty = bool(figma_link_sec and all(x in figma_link_sec for x in ['待设计填写', '🔲']))
    items.append({
        'dir': d,
        'title': title,
        'status': status,
        'actor': actor,
        'why': why,
        'priority': priority,
        'estimate': estimate,
        'scenarios_count': len(gherkin),
        'boundary_count': len(boundary),
        'gherkin': gherkin,
        'boundary': boundary,
        'rules': rules,
        'preconditions': preconditions,
        'flow': flow_sec,
        'dependencies': deps,
        'design_decisions': design,
        'figma_has_placeholder': figma_has_placeholder,
        'sections_present': {
            '1_basic': section_exists(text, '## 1.'),
            '2_trigger': section_exists(text, '## 2.'),
            '3_preconditions': section_exists(text, '## 3.'),
            '4_flow': section_exists(text, '## 4.'),
            '5_rules': section_exists(text, '## 5.'),
            '6_gherkin': section_exists(text, '## 6.'),
            '7_data': section_exists(text, '## 7.'),
            '8_boundary': section_exists(text, '## 8.'),
            '9_deps': section_exists(text, '## 9.'),
            '10_invest': section_exists(text, '## 10.'),
            '11_complete': section_exists(text, '## 11.'),
            '12_notes': section_exists(text, '## 12.'),
            '13_figma': section_exists(text, '## 13.'),
            '14_design': section_exists(text, '## 14.'),
            '15_review': section_exists(text, '## 15.'),
        }
    })

out = r'c:\AI\AI测试项目\leyoSwimming\tmp_business_review_data.json'
with open(out, 'w', encoding='utf-8') as f:
    json.dump(items, f, ensure_ascii=False, indent=2)
print('written', out, 'count', len(items))
