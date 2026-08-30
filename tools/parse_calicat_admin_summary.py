import json
import os
import re
import sys

INPUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "tmp", "calicat_admin_check")
OUTPUT_FILE = os.path.join(INPUT_DIR, "summary.md")


def load_design(path):
    with open(path, "r", encoding="utf-8") as f:
        raw = json.load(f)
    text = raw["content"][0]["text"]
    return json.loads(text)


def walk(node, depth=0, texts=None, frames=None):
    if texts is None:
        texts = []
    if frames is None:
        frames = []
    name = node.get("name", "")
    node_type = node.get("type", "")
    if node_type in ("paragraph", "rectangle"):
        content = node.get("content", "")
        if content and isinstance(content, str) and content.strip():
            texts.append({
                "depth": depth,
                "parent": name,
                "type": node_type,
                "content": content.strip(),
                "fontSize": node.get("fontSize"),
                "fontFill": node.get("fontFill"),
                "width": node.get("width"),
                "height": node.get("height"),
            })
    elif node_type == "frame":
        frames.append({
            "depth": depth,
            "name": name,
            "width": node.get("width"),
            "height": node.get("height"),
            "layout": node.get("layout"),
            "padding": node.get("padding"),
        })
    for child in node.get("children", []):
        walk(child, depth + 1, texts, frames)
    return texts, frames


def summarize_frame(name, layer_data):
    texts, frames = walk(layer_data)
    lines = []
    lines.append(f"## {name}\n")
    lines.append(f"- 尺寸: {layer_data.get('width')} x {layer_data.get('height')}")
    lines.append(f"- 布局: {layer_data.get('layout')}, align: {layer_data.get('alignItems')}, justify: {layer_data.get('justifyContent')}")
    fills = layer_data.get("fills")
    if fills:
        lines.append(f"- 背景: {json.dumps(fills, ensure_ascii=False)[:200]}")
    lines.append("\n### 文本内容（按出现顺序）\n")
    for t in texts:
        extra = []
        if t["fontSize"]:
            extra.append(f"fontSize={t['fontSize']}")
        if t["fontFill"]:
            extra.append(f"fill={t['fontFill']}")
        if t["width"]:
            extra.append(f"w={t['width']}")
        extra_str = ", ".join(extra)
        lines.append(f"- {'  ' * t['depth']}[{t['parent']}]: {t['content']}" + (f"  ({extra_str})" if extra_str else ""))
    lines.append("\n### 主要 Frame 容器\n")
    for f in frames[:40]:
        lines.append(f"- {'  ' * f['depth']}{f['name']} {f['width']}x{f['height']} layout={f['layout']} padding={f['padding']}")
    lines.append("\n---\n")
    return "\n".join(lines)


def main():
    files = sorted([f for f in os.listdir(INPUT_DIR) if f.endswith("_design.json")])
    out_lines = ["# Calicat Admin First Batch Design Summary\n"]
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
