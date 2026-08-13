import json, os, re
from pathlib import Path

OUTPUT_DIR = Path(__file__).parent / 'calicat_coach'

def load_design(name):
    path = OUTPUT_DIR / f"{name}_design.json"
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    text = data['content'][0]['text']
    return json.loads(text)['result']

def walk(node, depth=0):
    if isinstance(node, list):
        for child in node:
            yield from walk(child, depth)
        return
    if not isinstance(node, dict):
        return
    yield (depth, node)
    children = node.get('children') or node.get('layer_data', {}).get('children')
    if children:
        for child in children:
            yield from walk(child, depth+1)

def extract_texts(node):
    texts = []
    for depth, n in walk(node):
        content = n.get('content')
        if content and isinstance(content, str) and not content.startswith('<svg'):
            # text node
            name = n.get('name', '')
            texts.append((name, content.strip()))
    return texts

def extract_styles(node):
    styles = []
    for depth, n in walk(node):
        if n.get('type') in ('frame','rectangle','text','paragraph'):
            rec = {
                'name': n.get('name'),
                'type': n.get('type'),
                'width': n.get('width'),
                'height': n.get('height'),
                'fills': n.get('fills'),
                'fontSize': n.get('fontSize'),
                'fontFamily': n.get('fontFamily'),
                'fontFill': n.get('fontFill'),
                'cornerRadius': n.get('cornerRadius'),
                'effects': n.get('effects'),
                'stroke': n.get('stroke'),
                'padding': n.get('padding'),
            }
            styles.append(rec)
    return styles

def main():
    pages = [
        'C-wechat-auth-page',
        'C-phone-login-page',
        'C-coach-onboarding-page',
        'C-coach-onboarding-success-page',
        'C-coach-pending-page',
        'C-coach-center-page',
        'C-profile-edit-page',
        'C-reference-price-page',
        'C-resignation-page',
        'C-resignation-ticket-page',
        'C-coach-resigning-page',
    ]
    for p in pages:
        try:
            result = load_design(p)
            root = result[0] if result else {}
            print(f"\n=== {p} ===")
            print("Root name:", root.get('name') or root.get('layer_data',{}).get('name'))
            layer = root.get('layer_data', root)
            print("Root type:", layer.get('type'), "w:", layer.get('width'), "h:", layer.get('height'))
            texts = extract_texts(layer)
            print("Texts:")
            for name, text in texts:
                print(f"  [{name}] {text}")
            # Find main background/page container fills
            for depth, n in walk(layer):
                if n.get('type') == 'frame' and n.get('name') in ('页面容器','Page','画板','主容器', '主页面'):
                    print("Page container fills:", n.get('fills'))
                    break
        except Exception as e:
            print(f"{p}: ERROR {e}")

if __name__ == '__main__':
    main()
