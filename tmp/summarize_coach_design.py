import json
import os
import sys

INPUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "tmp", "calicat_coach")
OUTPUT_FILE = os.path.join(INPUT_DIR, "summary.md")


def load_design(path):
    with open(path, "r", encoding="utf-8") as f:
        raw = json.load(f)
    text = raw["content"][0]["text"]
    return json.loads(text)


def hex_from_rgba(rgba):
    if not rgba:
        return None
    if isinstance(rgba, list):
        rgba = rgba[0] if rgba else None
    if not isinstance(rgba, str):
        return str(rgba)
    rgba = rgba.strip()
    if rgba.startswith("rgba("):
        try:
            inner = rgba[5:-1]
            parts = [p.strip() for p in inner.split(",")]
            r, g, b = int(float(parts[0])), int(float(parts[1])), int(float(parts[2]))
            a = float(parts[3]) if len(parts) > 3 else 1.0
            if a >= 1:
                return f"#{r:02X}{g:02X}{b:02X}"
            return f"#{r:02X}{g:02X}{b:02X}{int(a*255):02X}"
        except Exception:
            return rgba
    return rgba


def walk(node, depth=0, texts=None, frames=None, svgs=None):
    if texts is None:
        texts = []
    if frames is None:
        frames = []
    if svgs is None:
        svgs = []
    name = node.get("name", "")
    node_type = node.get("type", "")
    if node_type in ("rectangle", "paragraph"):
        content = node.get("content", "")
        if content and isinstance(content, str) and content.strip():
            texts.append({
                "depth": depth,
                "parent": name,
                "type": node_type,
                "content": content.strip(),
                "fontSize": node.get("fontSize"),
                "fontFill": hex_from_rgba(node.get("fontFill")),
                "fontWeight": node.get("fontWeight"),
                "width": node.get("width"),
                "height": node.get("height"),
                "x": node.get("x"),
                "y": node.get("y"),
            })
    elif node_type == "frame":
        fills = node.get("fills")
        bg = None
        if fills:
            if isinstance(fills, list):
                bg = fills[0]
            else:
                bg = fills
        frames.append({
            "depth": depth,
            "name": name,
            "width": node.get("width"),
            "height": node.get("height"),
            "x": node.get("x"),
            "y": node.get("y"),
            "layout": node.get("layout"),
            "padding": node.get("padding"),
            "cornerRadius": node.get("cornerRadius"),
            "background": bg,
            "stroke": node.get("stroke"),
        })
    elif node_type == "svg":
        svgs.append({
            "depth": depth,
            "name": name,
            "width": node.get("width"),
            "height": node.get("height"),
            "fills": node.get("fills"),
            "stroke": node.get("stroke"),
        })
    for child in node.get("children", []):
        walk(child, depth + 1, texts, frames, svgs)
    return texts, frames, svgs


def summarize_frame(name, layer_data):
    texts, frames, svgs = walk(layer_data)
    lines = []
    lines.append(f"## {name}\n")
    lines.append(f"- 尺寸: {layer_data.get('width')} x {layer_data.get('height')}")
    lines.append(f"- 布局: {layer_data.get('layout')}, align: {layer_data.get('alignItems')}, justify: {layer_data.get('justifyContent')}")
    fills = layer_data.get("fills")
    if fills:
        lines.append(f"- 背景: {json.dumps(fills, ensure_ascii=False)[:300]}")
    lines.append(f"- 子节点数: {len(layer_data.get('children', []))}")

    lines.append("\n### 文本内容\n")
    for t in texts:
        extra = []
        if t["fontSize"]:
            extra.append(f"fontSize={t['fontSize']}px")
        if t["fontFill"]:
            extra.append(f"fill={t['fontFill']}")
        if t["width"]:
            extra.append(f"w={t['width']}")
        extra_str = ", ".join(extra)
        lines.append(f"- {'  ' * t['depth']}[{t['parent']}]: {t['content']}" + (f"  ({extra_str})" if extra_str else ""))

    lines.append("\n### 主要 Frame 容器\n")
    for f in frames[:80]:
        bg = ""
        if f["background"]:
            bg_short = json.dumps(f["background"], ensure_ascii=False)[:120]
            bg = f" bg={bg_short}"
        lines.append(f"- {'  ' * f['depth']}{f['name']} {f['width']}x{f['height']} corner={f['cornerRadius']} padding={f['padding']}{bg}")

    lines.append("\n### SVG/图标\n")
    for s in svgs[:30]:
        lines.append(f"- {'  ' * s['depth']}{s['name']} {s['width']}x{s['height']} fills={hex_from_rgba(s['fills'])} stroke={s['stroke']}")

    lines.append("\n---\n")
    return "\n".join(lines)


def main():
    files = sorted([f for f in os.listdir(INPUT_DIR) if f.endswith("_design.json")])
    out_lines = ["# Calicat Coach First Batch Design Summary\n"]
    for fname in files:
        path = os.path.join(INPUT_DIR, fname)
        try:
            design = load_design(path)
            layer_data = design["result"][0]["layer_data"]
            frame_name = fname.replace("_design.json", "")
            out_lines.append(summarize_frame(frame_name, layer_data))
        except Exception as e:
            out_lines.append(f"## {fname}\n\nERROR: {e}\n\n---\n")
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("\n".join(out_lines))
    print(f"Summary written to {OUTPUT_FILE}", file=sys.stderr)


if __name__ == "__main__":
    main()
