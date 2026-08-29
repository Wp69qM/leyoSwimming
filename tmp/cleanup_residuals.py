#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对 C- page-spec 进行二次修补：修正脚本清理后残留的断句、URL、函数调用等问题。
"""
import re
from pathlib import Path

ROOT = Path("d:/AI Agent/leyoSwimming/docs/figma/page-spec")
FILES = sorted(ROOT.glob("C-*.md"))

REPLACEMENTS = [
    # URL 协议补回
    (re.compile(r"https:www\.calicat\.cn"), "https://www.calicat.cn"),
    # 函数调用补回
    (re.compile(r"wx\.login(?!\(\))"), "wx.login()"),
    (re.compile(r"wx\.openSetting(?!\(\))"), "wx.openSetting()"),
    # 断句修复
    (re.compile(r"slogan 较长时允许两行，行\.4，第二行度同第一行"), "slogan 较长时允许两行"),
    (re.compile(r"未选中时 ，选中时"), "未选中时显示未勾选框，选中时显示已勾选框"),
    (re.compile(r"类型徽标或，展示 package\.status 映射后的中文标签"), "展示 package.status 映射后的中文标签"),
    (re.compile(r"卡片展示，\"剩余 \{available\} 课时\""), '"剩余 {available} 课时"'),
    (re.compile(r"剩余课时，\"共 \{total_hours\} 课时\""), '"共 {total_hours} 课时"'),
    (re.compile(r"\| 状态栏 \| 与 NavBar \|"), "| 状态栏 | — |"),
    (re.compile(r"\| 滚动区域 \| 状态栏 \+ NavBar至屏幕 \|"), "| 滚动区域 | — |"),
    (re.compile(r"列表展示最近 3 个时段，每项"), "列表展示最近 3 个时段"),
    (re.compile(r"\| 错误提示区 \| 登录区 \|"), "| 错误提示区 | — |"),
    (re.compile(r"获取验证码按钮可点击时填充，倒计时状态禁用"), "获取验证码按钮倒计时状态禁用"),
]

# 删除纯样式行
ROW_REMOVALS = [
    re.compile(r"\n\| 品牌色使用 \|[^|]*\|\n"),
]

# 删除无意义的检查清单项（按行）
CHECKLIST_REMOVALS = [
    re.compile(r"^\s*- \[ \] 登录按钮使用\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 微信一键登录按钮使用\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 微信一键登录入口使用文字按钮\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 分享卡片使用 \+ \+\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 头图使用\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 头图使用 深水\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] NavBar 使用 ，标题\"套餐使用详情\"，有返回按钮\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 套餐主信息卡：体验课用系，正价课用系\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 页面\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 页面整体符合品牌\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 页面所有文字确保在\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 登录区与页面形成明确卡片感\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 所有元素实现（禁止手动估算 x/y）\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 页面整体风格符合「 \+ 活力橙」主题\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 所有元素严格引用 Token\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 预览头图使用 深水\s*$", re.MULTILINE),
    re.compile(r"^\s*- \[ \] 预览头图使用\s*$", re.MULTILINE),
]


def process(content: str) -> str:
    for pat, repl in REPLACEMENTS:
        content = pat.sub(repl, content)
    for pat in ROW_REMOVALS:
        content = pat.sub("\n", content)
    for pat in CHECKLIST_REMOVALS:
        content = pat.sub("", content)
    # 清理因删除检查清单项而产生的连续空行
    content = re.sub(r"\n{3,}", "\n\n", content)
    return content


def main():
    for path in FILES:
        if path.name in ("A-TEMPLATE.md", "TEMPLATE.md"):
            continue
        content = path.read_text(encoding="utf-8")
        new_content = process(content)
        path.write_text(new_content, encoding="utf-8")
        print(f"patched: {path.name}")


if __name__ == "__main__":
    main()
