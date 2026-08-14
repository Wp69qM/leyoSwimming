#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Clean style details from admin (A-) page-spec Markdown files."""

import re
from pathlib import Path

PAGE_SPEC_DIR = Path(r"D:\AI Agent\leyoSwimming\docs\figma\page-spec")
EXCLUDE = {"A-TEMPLATE.md", "TEMPLATE.md", "A-CROSS-BATCH-PRINCIPLES.md"}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

SECTION_RE = re.compile(r"^\s*##\s+(\d+)\.")


def current_section(line: str) -> int | None:
    m = SECTION_RE.match(line)
    return int(m.group(1)) if m else None


def is_table_row(line: str) -> bool:
    stripped = line.strip()
    return stripped.startswith("|") and stripped.endswith("|") and line.count("|") >= 2


def split_table_cells(line: str):
    return [p.strip() for p in line.split("|")][1:-1]


def is_separator_row(cells):
    return all(re.fullmatch(r":?-+:?", c) for c in cells if c)


def rebuild_row(cells):
    return "| " + " | ".join(cells) + " |"


# ---------------------------------------------------------------------------
# Generic token removal (safe, no Chinese business words)
# ---------------------------------------------------------------------------

def remove_colors(text: str) -> str:
    text = re.sub(r"#([0-9A-Fa-f]{3,8})\b", "", text)
    text = re.sub(r"rgba?\([^)]*\)", "", text)
    return text


def remove_design_tokens(text: str) -> str:
    return re.sub(
        r"\b(color-[a-z0-9-]+|gradient-[a-z0-9-]+|font-size-[a-z0-9-]+|"
        r"radius-[a-z0-9-]+|shadow-[a-z0-9-]+)\b",
        "",
        text,
    )


def remove_font_specs(text: str) -> str:
    return re.sub(
        r"\b\d+px\s+(Medium|Regular|Bold|Light|Semibold|Heavy|Italic)\b",
        "",
        text,
        flags=re.IGNORECASE,
    )


def remove_all_sizes(text: str) -> str:
    text = re.sub(r"\b\d+\s*×\s*\d+\s*(?:px|vh|vw|rem|em|pt)?\b", "", text)
    text = re.sub(r"\b\d+\s*\*\s*\d+\s*(?:px|vh|vw|rem|em|pt)?\b", "", text)
    text = re.sub(r"\b\d+(\.\d+)?\s*(px|vh|vw|rem|em|pt)\b", "", text)
    # Percentage dimensions inside style specs (e.g. "100%（可视区）").
    text = re.sub(r"\b\d+(\.\d+)?\s*%", "", text)
    return text


def remove_coordinates(text: str) -> str:
    return re.sub(r"\bx\s*=\s*\d+\s*,?\s*y\s*=\s*\d+\b", "", text, flags=re.IGNORECASE)


def remove_dangling_units(text: str) -> str:
    """Remove orphaned units left after numeric sizes were stripped."""
    return re.sub(r"\bpx\b", "", text)


def remove_color_words(text: str) -> str:
    """Remove standalone color adjectives (avoid breaking business terms)."""
    # Remove color words used as style descriptors, but protect common business phrases.
    protected = [
        "灰色状态",
        "红色状态",
        "绿色状态",
        "蓝色状态",
        "橙色状态",
        "颜色",
        "色彩",
        "角色",
        "特色",
        "肤色",
    ]
    placeholders = {}
    for idx, word in enumerate(protected):
        key = f"__PROTECT{idx}__"
        placeholders[key] = word
        text = text.replace(word, key)

    color_words = ["红色", "蓝色", "绿色", "橙色", "灰色", "黄色", "紫色", "黑色", "白色"]
    for cw in color_words:
        text = re.sub(rf"(?<![了有在呈为按带呈显为]){cw}(?![\u4e00-\u9fa5])", "", text)

    for key, original in placeholders.items():
        text = text.replace(key, original)
    return text


def remove_line_styles(text: str) -> str:
    """Remove line style descriptors like dashed/dotted/solid lines."""
    text = re.sub(r"虚线", "", text)
    text = re.sub(r"点线", "", text)
    # Remove "实线" only when it refers to line style, not "实现" (implement).
    text = re.sub(r"实线", "", text)
    # Keep "默认边框" as it describes a functional state (default border), not pure styling.
    return text


# ---------------------------------------------------------------------------
# Conservative Chinese style phrase removal (avoid business words)
# ---------------------------------------------------------------------------

def remove_safe_style_phrases(text: str) -> str:
    phrases = [
        r"白底(?:卡片)?",
        r"灰底",
        r"橙底",
        r"蓝底",
        r"红底",
        r"绿底",
        r"黑底",
        r"灰字",
        r"白字",
        r"红字",
        r"蓝字",
        r"绿字",
        r"橙字",
        r"黑字",
        r"顶部阴影",
        r"底部阴影",
        r"行分隔",
        r"圆角\s*[^，|,；]*",
        r"边框\s*[^，|,；]*",
        r"阴影\s*[^，|,；]*",
        r"透明度\s*[^，|,；]*",
        r"不透明度\s*[^，|,；]*",
        r"行高\s*[^，|,；]*",
        r"字重\s*[^，|,；]*",
        r"字号\s*[^，|,；]*",
        r"字体\s*[^，|,；]*",
        r"渐变[^，|,；]*",
        r"左上角",
        r"右上角",
        r"左下角",
        r"右下角",
        r"左对齐",
        r"右对齐",
        r"垂直居中",
        r"水平居中",
        r"居中对?齐?",
        r"(?<!不)居中(?![^，|,；]*)",  # avoid "不居中" if any
        r"flex\s*[^，|,；]*",
        r"grid\s*[^，|,；]*",
        r"定位\s*[^，|,；]*",
        r"等宽",
        r"等分",
        r"间距\s*[^，|,；]*",
        r"红色文字\s*，?\s*",
        r"蓝色文字\s*，?\s*",
        r"绿色文字\s*，?\s*",
        r"橙色文字\s*，?\s*",
        r"灰色文字\s*，?\s*",
        r"圆形",
        r"椭圆形",
        r"（或按设计高度）",
        r"浅\s*\+?\s*",
        r"深\s*\+?\s*",
    ]
    for p in phrases:
        text = re.sub(p, "", text)
    # Replace generic style nouns with semantic equivalents.
    text = re.sub(r"(?<![\u4e00-\u9fa5])颜色(?![\u4e00-\u9fa5])", "显示", text)
    text = re.sub(r"(?<![\u4e00-\u9fa5])样式(?![\u4e00-\u9fa5])", "显示", text)
    text = re.sub(r"(?<![\u4e00-\u9fa5])色(?![\u4e00-\u9fa5])", "", text)
    text = re.sub(r"状态色", "状态", text)
    return text


def remove_style_specs(text: str) -> str:
    """Remove common style specification fragments (numeric / positional)."""
    specs = [
        r"margin\s*[^，|,；]*",
        r"padding\s*[^，|,；]*",
        r"内边距\s*[^，|,；]*",
        r"外边距\s*[^，|,；]*",
        r"背景\s*[^，|,；]*",
        r"底色\s*[^，|,；]*",
        r"顶部固定[^，|,；]*",
        r"底部固定[^，|,；]*",
        r"左侧固定[^，|,；]*",
        r"右侧固定[^，|,；]*",
        r"距[上下左右][^，|,；]*",
        r"距[^，|,；]*",
        r"距离\s*[^，|,；]*",
        r"高\s*\d+\s*px",
        r"宽\s*\d+\s*px",
        r"宽度\s*\d+\s*px",
        r"高度\s*\d+\s*px",
        r"宽度\s*[:：]\s*",
        r"高度\s*[:：]\s*",
        r"蓝色边框",
        r"虚线边框",
        r"实线边框",
        r"点线边框",
        r"描边\s*[^，|,；]*",
        r"主色\s*[^，|,；]*",
    ]
    for p in specs:
        text = re.sub(p, "", text)
    return text


# ---------------------------------------------------------------------------
# Cleanup artifacts
# ---------------------------------------------------------------------------

def cleanup(text: str) -> str:
    text = re.sub(r"[，,]\s*[，,]", "，", text)
    text = re.sub(r"；\s*[，,]", "；", text)
    text = re.sub(r"，\s*；", "；", text)
    text = re.sub(r"\s+，", "，", text)
    text = re.sub(r"^\s*[，,；:：]\s*", "", text)
    text = re.sub(r"\s*[，,；:：]\s*$", "", text)
    text = re.sub(r"\(\s*\)", "", text)
    text = re.sub(r"（\s*）", "", text)
    text = re.sub(r"\[\s*\]", "", text)
    text = re.sub(r"`\s*`", "", text)
    text = re.sub(r"\"\s*，\s*\"", "\"", text)
    text = re.sub(r"'\s*，\s*'", "'", text)
    text = re.sub(r"\(\s+", "(", text)
    text = re.sub(r"\s+\)", ")", text)
    text = re.sub(r"（\s+", "（", text)
    text = re.sub(r"\s+）", "）", text)
    text = re.sub(r"^\s*\+\s*", "", text)
    text = re.sub(r"\s*\+\s*$", "", text)
    text = re.sub(r"\s*\+\s*，", "，", text)
    text = re.sub(r"，\s*\+\s*", "，", text)
    text = re.sub(r"\s+", " ", text)
    text = text.strip("，,；:. ")
    return text if text.strip() else "—"


# ---------------------------------------------------------------------------
# §1 page-base targeted cleaning (preserve skeleton dimensions)
# ---------------------------------------------------------------------------

SKELETON_FIELDS = {
    "设计宽度",
    "侧边栏",
    "TopBar",
    "面包屑",
    "弹窗宽度",
    "弹窗最大高度",
    "弹窗内边距",
    "顶部标题栏",
    "内容区背景",
    "内容区内边距",
    "遮罩层",
    "圆角",
}


def clean_section1_row(cells):
    if len(cells) < 2:
        return cells
    field = cells[0]

    # Only the field-value rows in §1 page base need skeleton-aware handling.
    if field not in SKELETON_FIELDS or len(cells) != 2:
        return clean_generic_row(cells)

    value = cells[1]
    if field == "侧边栏":
        value = re.sub(r"左侧固定\s*\d+\s*px\s*，\s*#[0-9A-Fa-f]+", "左侧固定 220 px", value)
        value = remove_colors(value)
    elif field == "TopBar":
        value = re.sub(r"(\d+\s*px)\s*，\s*#[0-9A-Fa-f]+\s*，\s*底部\s*\d+\s*px\s*#[0-9A-Fa-f]+", r"\1", value)
        value = re.sub(r"(\d+\s*px)\s*，\s*#[0-9A-Fa-f]+", r"\1", value)
        value = remove_colors(value)
    elif field == "面包屑":
        value = re.sub(r"^(\d+\s*px)\s*；\s*", r"\1；", value)
        value = remove_colors(value)
    elif field == "顶部标题栏":
        # Remove title-bar height but keep functional description.
        value = re.sub(r"^\s*\d+\s*px\s*[，,]\s*", "", value)
        value = re.sub(r"高\s*\d+\s*px\s*[，,]\s*", "", value)
        value = remove_colors(value)
    elif field in {"内容区背景", "内容区内边距", "遮罩层", "圆角"}:
        value = "—"
    else:
        # 设计宽度, 弹窗宽度, 弹窗最大高度, 顶部标题栏, etc.
        value = remove_colors(value)
        value = remove_design_tokens(value)
        value = remove_font_specs(value)
        value = remove_coordinates(value)
        # Remove non-skeleton style descriptors (padding, margin, background, shadows)
        value = re.sub(r"白底(?:卡片)?", "", value)
        value = re.sub(r"背景\s*[^，|,；]*", "", value)
        value = re.sub(r"顶部阴影[^，|,；]*", "", value)
        value = re.sub(r"底部\s*\d+\s*px\s*#[0-9A-Fa-f]+", "", value)
        value = re.sub(r"底部\s*1\s*px", "", value)
        value = re.sub(r"内边距\s*[^，|,；]*", "", value)
        value = re.sub(r"margin(?:-bottom|-top|-left|-right)?\s*[^，|,；]*", "", value)

    return [field, cleanup(value)]


# ---------------------------------------------------------------------------
# §3 element table cleaning
# ---------------------------------------------------------------------------

def clean_element_value(field: str, value: str) -> str:
    # Capture quoted labels for title-like fields before stripping styles.
    quoted_label = None
    if field in {"标题", "分组标题", "说明文字"}:
        m = re.search(r"[\"\"]([^\"\"]+)[\"\"]", value)
        if m:
            quoted_label = m.group(1)

    # 1. Targeted replacements BEFORE generic size/color removal.
    if field in {"角色下拉", "状态下拉", "关键词搜索", "时间范围", "高级筛选", "搜索框", "输入框", "选择器", "下拉"}:
        value = re.sub(r"宽度\s*\d+\s*px\s*[:：]\s*", "", value)
        value = re.sub(r"宽度\s*\d+\s*px\s*，\s*", "", value)

    if field.endswith("按钮"):
        value = re.sub(r"主按钮\s*#[0-9A-Fa-f]+", "主按钮", value)
        value = re.sub(r"危险按钮\s*#[0-9A-Fa-f]+", "危险按钮", value)
        value = re.sub(r"默认按钮\s*#[0-9A-Fa-f]+", "默认按钮", value)
        value = re.sub(r"红色文字\s*，?\s*", "", value)
        value = re.sub(r"蓝色文字\s*，?\s*", "", value)
        value = re.sub(r"绿色文字\s*，?\s*", "", value)

    if field == "布局":
        value = re.sub(r"单列表单\s*，\s*label\s*宽\s*\d+\s*px\s*右对齐\s*，\s*输入区宽\s*\d+\s*px", "单列表单", value)
        value = re.sub(r"只读信息表\s*，\s*\d+\s*列布局", "只读信息表", value)

    if field == "关闭按钮":
        value = re.sub(r"右上角\s*\d+\s*×\s*\d+\s*px\s*关闭\s*icon\s*(?:，\s*#[0-9A-Fa-f]+)?", "关闭 icon", value)
        value = re.sub(r"右上角\s*X\s*，?\s*", "X，", value)
        value = re.sub(r"右上角\s*", "", value)

    if field in {"头像", "个人形象照"}:
        value = re.sub(r"\d+\s*×\s*\d+\s*px\s*圆形", "", value)
        value = re.sub(r"\d+\s*\*\s*\d+\s*px?", "", value)

    # 2. Generic token removal.
    value = remove_colors(value)
    value = remove_design_tokens(value)
    value = remove_font_specs(value)
    value = remove_coordinates(value)
    value = remove_all_sizes(value)
    value = remove_dangling_units(value)
    value = remove_color_words(value)
    value = remove_line_styles(value)
    value = remove_safe_style_phrases(value)
    value = remove_style_specs(value)

    # 3. Field-specific post-processing.
    if field == "容器":
        value = re.sub(r"[，,]\s*宽度\s*$", "", value)
        value = re.sub(r"\s*宽度\s*$", "", value)
        value = cleanup(value)
        # If the result is empty or still contains style fragments, fall back to "卡片".
        if value == "—" or re.search(r"高度|底部|顶部|固定|白底|阴影|圆角|背景|margin|padding|内边距", value):
            value = "卡片"
        return value

    if field in {"表头", "行高", "行分隔", "姓名", "教练 ID", "订单号", "个人形象照", "选中态", "背景", "位置", "圆角", "阴影", "样式", "当前状态高亮", "高度", "宽度"}:
        return "—"

    if field == "布局":
        value = re.sub(r"label\s*宽[^，|,；]*", "", value)
        value = re.sub(r"输入区\s*宽[^，|,；]*", "", value)
        value = re.sub(r"每列宽度\s*=.*", "", value)
        value = re.sub(r"\d+\s*列布局", "多列布局", value)
        value = re.sub(r"\d+\s*列\s*", "多列", value)
        value = re.sub(r"等宽", "", value)
        value = re.sub(r"间距", "", value)
        value = cleanup(value)

    if field in {"标题", "分组标题", "说明文字"} and quoted_label:
        value = cleanup(value)
        if value == "—" or not value.strip():
            value = quoted_label
        return value

    # Tag color removal: strip color words after each slash-separated tag.
    value = re.sub(r"(Tag\s*：\s*[^#]+)\s*#[0-9A-Fa-f]+\s*/\s*", r"\1 / ", value)
    value = re.sub(r"/\s*[红蓝绿橙黄紫灰黑白]色", "/", value)
    value = re.sub(r"[红蓝绿橙黄紫灰黑白]色\s*/\s*", "", value)

    # Drop dangling single-character style descriptors in element descriptions.
    value = re.sub(r"^\s*宽\s*[，,、]\s*", "", value)
    value = re.sub(r"\s*[，,、]\s*宽\s*$", "", value)
    value = re.sub(r"\s*高\s*[，,、]\s*", "，", value)
    value = re.sub(r"\s*宽\s*[，,、]\s*", "，", value)
    # Remove standalone "宽度" / "高度" tokens that survive generic cleaning.
    value = re.sub(r"(?<![\u4e00-\u9fa5a-zA-Z])宽度(?![\u4e00-\u9fa5a-zA-Z])", "", value)
    value = re.sub(r"(?<![\u4e00-\u9fa5a-zA-Z])高度(?![\u4e00-\u9fa5a-zA-Z])", "", value)

    return cleanup(value)


def clean_component_cell(text: str) -> str:
    """Clean cells that describe component + size (e.g. '输入框，高，宽度')."""
    text = remove_colors(text)
    text = remove_design_tokens(text)
    text = remove_font_specs(text)
    text = remove_coordinates(text)
    text = remove_all_sizes(text)
    text = remove_dangling_units(text)
    text = remove_color_words(text)
    text = remove_line_styles(text)
    text = remove_safe_style_phrases(text)
    text = remove_style_specs(text)
    # Drop dangling width/height/size descriptors.
    text = re.sub(r"高\s*[，,、]\s*宽度\s*", "", text)
    text = re.sub(r"宽度\s*[，,、]\s*高度\s*", "", text)
    text = re.sub(r"(?<![\u4e00-\u9fa5a-zA-Z])宽度(?![\u4e00-\u9fa5a-zA-Z])", "", text)
    text = re.sub(r"(?<![\u4e00-\u9fa5a-zA-Z])高度(?![\u4e00-\u9fa5a-zA-Z])", "", text)
    text = re.sub(r"(?<![\u4e00-\u9fa5a-zA-Z])高(?![\u4e00-\u9fa5a-zA-Z])", "", text)
    text = re.sub(r"(?<![\u4e00-\u9fa5a-zA-Z])宽(?![\u4e00-\u9fa5a-zA-Z])", "", text)
    text = re.sub(r"(?<![\u4e00-\u9fa5a-zA-Z])尺寸(?![\u4e00-\u9fa5a-zA-Z])", "", text)
    return cleanup(text)


def clean_section3_row(cells):
    if len(cells) < 2:
        return cells
    # Field-value tables (字段 | 值) are the primary element tables.
    if len(cells) == 2:
        field = cells[0]
        return [field, clean_element_value(field, cells[1])]
    # Three-column tables: field | component/size | rule.
    if len(cells) == 3:
        return [cells[0], clean_component_cell(cells[1]), cleanup(cells[2])]
    # Other multi-column tables inside §3 get generic cleaning.
    return clean_generic_row(cells)


# ---------------------------------------------------------------------------
# Column tables (列名 | 宽度 | 对齐 | 说明)
# ---------------------------------------------------------------------------

def is_column_table(header_cells):
    return len(header_cells) >= 4 and "列名" in header_cells and ("宽度" in header_cells or "对齐" in header_cells)


def clean_column_table_row(cells):
    if len(cells) < 4:
        return cells
    # col1: column name (keep)
    # col2: width -> —
    # col3: alignment -> —
    # col4+: description (clean)
    new_cells = [cells[0], "—", "—"]
    desc = " | ".join(cells[3:])
    desc = remove_colors(desc)
    desc = remove_design_tokens(desc)
    desc = remove_font_specs(desc)
    desc = remove_coordinates(desc)
    desc = remove_all_sizes(desc)
    desc = remove_dangling_units(desc)
    desc = remove_color_words(desc)
    desc = remove_line_styles(desc)
    desc = remove_safe_style_phrases(desc)
    desc = remove_style_specs(desc)
    desc = re.sub(r"(Tag\s*：\s*[^#]+)\s*#[0-9A-Fa-f]+\s*/\s*", r"\1 / ", desc)
    desc = re.sub(r"/\s*[红蓝绿橙黄紫灰黑白]色", "/", desc)
    desc = re.sub(r"[红蓝绿橙黄紫灰黑白]色\s*/\s*", "", desc)
    new_cells.append(cleanup(desc))
    return new_cells


# ---------------------------------------------------------------------------
# Header row cleaning (preserve column names)
# ---------------------------------------------------------------------------

def clean_header_row(cells):
    cleaned = []
    for cell in cells:
        cell = remove_colors(cell)
        cell = remove_design_tokens(cell)
        cell = remove_font_specs(cell)
        cell = remove_coordinates(cell)
        cell = remove_all_sizes(cell)
        cell = remove_dangling_units(cell)
        cell = remove_color_words(cell)
        cell = remove_line_styles(cell)
        cell = cleanup(cell)
        cleaned.append(cell)
    return cleaned


# ---------------------------------------------------------------------------
# Generic table row cleaning
# ---------------------------------------------------------------------------

def clean_generic_row(cells):
    cleaned = []
    for cell in cells:
        cell = remove_colors(cell)
        cell = remove_design_tokens(cell)
        cell = remove_font_specs(cell)
        cell = remove_coordinates(cell)
        cell = remove_all_sizes(cell)
        cell = remove_dangling_units(cell)
        cell = remove_color_words(cell)
        cell = remove_line_styles(cell)
        cell = remove_safe_style_phrases(cell)
        cell = remove_style_specs(cell)
        cell = cleanup(cell)
        cleaned.append(cell)
    return cleaned


# ---------------------------------------------------------------------------
# Non-table line cleaning
# ---------------------------------------------------------------------------

def clean_non_table_line(line: str, section: int | None) -> str:
    # In AI checklists (section 8), keep skeleton dimensions but remove colors
    if section == 8:
        placeholders = {}

        def _protect(pattern, key):
            nonlocal line
            for m in re.finditer(pattern, line):
                placeholders[key] = m.group(0)
                line = line.replace(m.group(0), key, 1)

        # Protect canvas / layout bar / modal skeleton dimensions.
        _protect(r"\d+\s*×\s*\d+\s*主画布", "__CANVAS__")
        _protect(r"\d+\s*px\s*侧边栏", "__SIDE__")
        _protect(r"\d+\s*px\s*TopBar", "__TOP__")
        _protect(r"\d+\s*px\s*面包屑", "__CRUMB__")
        _protect(r"\d+\s*px\s*宽弹窗", "__MW__")
        _protect(r"弹窗宽度\s*\d+\s*px", "__MWIDTH__")
        _protect(r"弹窗最大高度\s*\d+\s*vh", "__MHEIGHT__")
        _protect(r"顶部标题栏\s*\d+\s*px", "__TITLEH__")

        # Remove background + padding combo first (before color removal strips the hex)
        line = re.sub(r"内容区背景\s*#[0-9A-Fa-f]+\s*，\s*内边距\s*\d+\s*px", "内容区背景与内边距", line)
        line = remove_colors(line)
        line = remove_design_tokens(line)
        line = remove_font_specs(line)
        line = remove_coordinates(line)
        line = remove_all_sizes(line)
        line = remove_dangling_units(line)
        line = remove_color_words(line)
        line = remove_line_styles(line)
        line = remove_safe_style_phrases(line)
        line = remove_style_specs(line)
        line = re.sub(r"（\s*）", "", line)
        line = re.sub(r"\(\s*\)", "", line)
        line = re.sub(r"`\s*`", "", line)

        for key, original in placeholders.items():
            line = line.replace(key, original)

        # Strip trailing punctuation/whitespace left by removed style specs.
        line = line.rstrip("，,；:. ")
        # Drop checklist items that became empty.
        if line.strip() == "- [ ]":
            return ""
    else:
        # Conservative cleanup for prose lines outside tables.
        stripped = line.strip()
        if not stripped or stripped == "—":
            return ""
        line = re.sub(r"\s*\d+\s*×\s*\d+\s*", " ", line)
        line = re.sub(r"\s*\d+\s*px\s*", " ", line)
        line = re.sub(r"\s*#[0-9A-Fa-f]+\s*", " ", line)
        line = remove_color_words(line)
        line = remove_line_styles(line)
        line = remove_style_specs(line)
        line = cleanup(line)
    line = re.sub(r"\s{2,}", " ", line)
    return line.rstrip()


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    files = sorted([p for p in PAGE_SPEC_DIR.glob("A-*.md") if p.name not in EXCLUDE])
    print(f"Found {len(files)} A- page-spec files to clean.")

    stats = []
    for path in files:
        original = path.read_text(encoding="utf-8")
        lines = original.splitlines()
        cleaned_lines = []
        section = None
        header_cells = None
        column_table = False
        prev_was_table = False

        for line in lines:
            new_section = current_section(line)
            if new_section is not None:
                section = new_section
                header_cells = None
                column_table = False

            if not is_table_row(line):
                cleaned_lines.append(clean_non_table_line(line, section))
                prev_was_table = False
                continue

            cells = split_table_cells(line)
            if is_separator_row(cells):
                cleaned_lines.append(line)
                prev_was_table = True
                continue

            # A table row after a non-table row starts a new table -> header
            if not prev_was_table:
                header_cells = cells
                column_table = is_column_table(header_cells)
                cleaned_lines.append(rebuild_row(clean_header_row(cells)))
                prev_was_table = True
                continue
            prev_was_table = True

            if section == 1:
                cleaned = clean_section1_row(cells)
            elif section == 3:
                if column_table:
                    cleaned = clean_column_table_row(cells)
                else:
                    cleaned = clean_section3_row(cells)
            else:
                cleaned = clean_generic_row(cells)

            cleaned_lines.append(rebuild_row(cleaned))

        new_content = "\n".join(cleaned_lines)
        if original.endswith("\n") and not new_content.endswith("\n"):
            new_content += "\n"
        if new_content != original:
            path.write_text(new_content, encoding="utf-8")
            stats.append((path.name, True))
        else:
            stats.append((path.name, False))

    print("\nDone. Files modified:")
    for name, changed in stats:
        print(f"  {'[M]' if changed else '[=]'} {name}")


if __name__ == "__main__":
    main()
