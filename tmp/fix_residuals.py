#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对 C- page-spec 进行第三次修补：修正前两次脚本未覆盖的样式残留与断句问题。
"""
import re
from pathlib import Path

ROOT = Path("d:/AI Agent/leyoSwimming/docs/figma/page-spec")
FILES = sorted(ROOT.glob("C-*.md"))

# 精确替换规则（按顺序应用）
REPLACEMENTS = [
    # 形状/轮廓
    (re.compile(r"绘制\s*（\s*outline\s*）\s*游泳"), "绘制游泳"),
    (re.compile(r"绘制\s*\(\s*outline\s*\)\s*游泳"), "绘制游泳"),

    # 视觉/动效/样式描述
    (re.compile(r"与成功态视觉一致"), "与成功态一致"),
    (re.compile(r"品牌元素保留但视觉上弱化"), "品牌元素保留"),
    (re.compile(r"需保持视觉连贯"), "需保持一致"),
    (re.compile(r"视觉风格一致"), "一致"),
    (re.compile(r"以淡出动效离开并跳转目标页"), "跳转目标页"),
    (re.compile(r"登录按钮保持成功态\s*[\d\.]+\s*ms\s*后，"), "登录按钮保持成功态后，"),
    (re.compile(r"登录按钮保持成功态\s*[\d\.]+\s*ms\s*后"), "登录按钮保持成功态后"),

    # 颜色/主题残留
    (re.compile(r"页面整体符合「\s*\+\s*活力橙\s*」"), "页面整体符合品牌规范"),
    (re.compile(r"活力橙"), ""),

    # 排班管理页残留
    (re.compile(r"是周，是周\+时间段的二维网格"), "上方是周历，下方是周+时间段的二维网格"),
    (re.compile(r"每个\s*，选中态\s*白字"), "每个日期，选中态"),
    (re.compile(r"未选中日期小蓝点"), "未选中日期显示今天标识"),
    (re.compile(r"每个\s*，按状态着色"), "每个时段格按状态区分"),
    (re.compile(r"可约\s*/\s*已预约\s*白字\s*/\s*已请假\s*/\s*过去"), "可约 / 已预约 / 已请假 / 过去"),
    (re.compile(r"操作按钮\s*\|\s*删除/\s*取消\s*/\s*保存"), "操作按钮 | 删除 / 取消 / 保存"),

    # 通用样式残留词（在四态/检查清单等上下文中）
    (re.compile(r"白字"), ""),
    (re.compile(r"小蓝点"), "标识"),
    (re.compile(r"按状态着色"), "按状态区分"),
    (re.compile(r"使用不同\s*$", re.MULTILINE), "需区分显示"),
    (re.compile(r"使用不同\s*（"), "需区分显示（"),
]

# 删除无意义检查清单项
CHECKLIST_REMOVALS = [
    re.compile(r"^\s*- \[ \] 页面整体符合[^\n]*$", re.MULTILINE),
]


def process(content: str) -> str:
    for pat, repl in REPLACEMENTS:
        content = pat.sub(repl, content)
    for pat in CHECKLIST_REMOVALS:
        content = pat.sub("", content)
    # 清理连续空行
    content = re.sub(r"\n{3,}", "\n\n", content)
    return content


def main():
    for path in FILES:
        if path.name in ("A-TEMPLATE.md", "TEMPLATE.md"):
            continue
        content = path.read_text(encoding="utf-8")
        new_content = process(content)
        if new_content != content:
            path.write_text(new_content, encoding="utf-8")
            print(f"fixed: {path.name}")
        else:
            print(f"skip: {path.name}")


if __name__ == "__main__":
    main()
