#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 Web 后台 Page Spec 转换为 calicat 精简版。
保留：页面基础、状态维度、元素清单、显示规则、跳转动线、四态设计、业务规则引用、AI 检查清单。
删除：冗余引言、变更日志。
"""
import os
import re
import glob

SRC_DIR = "docs/figma/page-spec"
OUT_DIR = "docs/figma/page-spec/slim-admin"

SECTIONS_TO_KEEP = [
    "1. 页面基础",
    "2. 状态维度",
    "3. 元素清单",
    "4. 显示规则总表",
    "5. 跳转动线",
    "6. 四态设计",
    "7. 业务规则引用",
    "8. AI 生成检查清单",
]


def slim_file(path: str) -> str:
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()

    # 提取标题（第一行）
    lines = text.splitlines()
    title_line = lines[0] if lines else "# 页面规格"

    # 去掉引言块（> 开头的引用段落）
    text_no_quote = re.sub(r"^>.*\n?", "", text, flags=re.MULTILINE)

    # 按二级标题分割
    sections = re.split(r"\n(?=##\s+\d+\.\s+)", text_no_quote)

    kept = [title_line]
    for sec in sections:
        sec = sec.strip()
        if not sec:
            continue
        for marker in SECTIONS_TO_KEEP:
            if sec.startswith(f"## {marker}"):
                # 移除三级标题前的空行压缩
                sec = re.sub(r"\n{3,}", "\n\n", sec)
                kept.append(sec)
                break

    # 去掉 9. 变更日志（如果存在）
    result = "\n\n".join(kept)
    result = re.sub(r"## 9\. 变更日志.*", "", result, flags=re.DOTALL)
    result = result.strip()
    return result


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    files = sorted(glob.glob(os.path.join(SRC_DIR, "A-*.md")))
    # 排除全局原则、模板、自检报告
    page_files = [
        p for p in files
        if os.path.basename(p) not in {
            "A-CROSS-BATCH-PRINCIPLES.md",
            "A-TEMPLATE.md",
        } and not os.path.basename(p).startswith("cross-page-check-admin")
    ]

    for path in page_files:
        basename = os.path.basename(path)
        out_path = os.path.join(OUT_DIR, basename)
        slim = slim_file(path)
        with open(out_path, "w", encoding="utf-8") as f:
            f.write(slim)
        print(f"[OK] {basename} -> {len(slim)} chars")

    print(f"\nDone. {len(page_files)} slim files in {OUT_DIR}")


if __name__ == "__main__":
    main()
