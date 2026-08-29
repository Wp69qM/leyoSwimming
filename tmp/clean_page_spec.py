#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
清理 docs/figma/page-spec/C-*.md 中的样式细节，保留业务逻辑。
"""
import re
from pathlib import Path

ROOT = Path("d:/AI Agent/leyoSwimming/docs/figma/page-spec")
FILES = sorted(ROOT.glob("C-*.md"))

# 纯样式字段：出现这些字段名时，直接删除该行
STYLE_FIELDS = {
    "尺寸", "位置", "背景", "背景/颜色", "颜色", "圆角", "字体", "阴影",
    "内边距", "外边距", "间距", "对齐", "布局方式", "安全区", "动效",
    "按压态", "左侧图标", "右侧图标", "图标", "尺寸/位置", "样式",
    "金额颜色", "状态图标", "状态插画", "左侧", "右侧", "插画占位",
    "协议名样式", "组件/尺寸", "视觉处理", "状态/尺寸", "位置/尺寸",
}

# 样式列：表头为这些时，删除整列
STYLE_COLUMNS = {
    "尺寸/位置", "样式", "背景/颜色", "背景", "颜色", "圆角", "字体",
    "阴影", "内边距", "尺寸", "位置", "图标", "尺寸/样式", "位置/尺寸",
}

# 颜色值
COLOR_RE = re.compile(r"""
    (?:\#[0-9A-Fa-f]{3,8})
    | (?:rgba?\(\s*[\d.%]+\s*,\s*[\d.%]+\s*,\s*[\d.%]+\s*(?:,\s*[\d.%]+\s*)?\))
    | (?:color-[a-z0-9-]+)
    | (?:gradient-[a-z0-9-]+)
""", re.VERBOSE)

# 尺寸/坐标
SIZE_RE = re.compile(
    r"(?:\d+\s*×\s*\d+\s*(?:px)?)"
    r"|(?:\d+\s*×\s*[^\s|，,；]+)"
    r"|(?:\d+\s*px)"
    r"|(?:x\s*=\s*-?\d+\s*(?:,\s*y\s*=\s*-?\d+)?)"
    r"|(?:y\s*=\s*-?\d+)"
    r"|(?:高\s*\d+(?:px)?)"
    r"|(?:宽\s*\d+(?:px)?)"
    r"|(?:高度\s*\d+(?:px)?)"
    r"|(?:宽度\s*\d+(?:px)?)"
    r"|(?:距.*?\d+(?:\.\d+)?(?:px|%)?)"
    r"|(?:\d+\s*%\s*屏幕高度)"
    r"|(?:固定底部)"
    r"|(?:底部\s*\d+px)"
    r"|(?:顶部\s*\d+px)"
    r"|(?:上方\s*\d+px)"
    r"|(?:下方\s*\d+px)"
    r"|(?:左侧\s*\d+px)"
    r"|(?:右侧\s*\d+px)"
    r"|(?:\d+\s*px\s*高)"
    r"|(?:\d+\s*px\s*宽)"
    r"|(?:尺寸)"
    r"|(?:高度约\s*[^|,\n]*?屏幕)"
    r"|(?:约\s*\d+\s*[^|,\n]*?屏幕)"
    r"|(?:屏幕(?:垂直|水平)约\s*[^|,\n]*?)"
    r"|(?:≥\s*[^|,\n]*?（[^）]*?）)",
    re.VERBOSE,
)

# 字体规格
FONT_RE = re.compile(r"""
    (?:\d+px\s+(?:Medium|Regular|Bold|Light|Semibold|Heavy|Italic))
    | (?:font-(?:size|weight)-[a-z0-9-]+)
    | (?:\b(?:Medium|Regular|Bold|Light|Semibold|Heavy|Italic)\b\s*(?:白色|\#[0-9A-Fa-f]+)?)
""", re.VERBOSE)

# 间距/边距
SPACING_RE = re.compile(r"""
    (?:padding\s+\d+px)
    | (?:margin\b)
    | (?:内边距\s+\d+px)
    | (?:外边距\s+\d+px)
    | (?:间距\s+\d+px)
    | (?:水平边距\s+\d+px)
    | (?:垂直间距\s+\d+px)
    | (?:按钮间距\s+\d+px)
    | (?:行高\s*[\d.]+)
    | (?:内边)
    | (?:外边)
    | (?:安全区)
""", re.VERBOSE)

# 圆角/形状
RADIUS_RE = re.compile(r"""
    (?:radius-[a-z]+(?:\s*\(\d+px\))?)
    | (?:圆角\s+\d+(?:\s+\d+){0,3})
    | (?:圆角\s*\d+(?:px)?)
    | (?:圆角)
    | (?:正圆)
    | (?:50%)
    | (?:pill)
""", re.VERBOSE | re.IGNORECASE)

# 阴影
SHADOW_RE = re.compile(r"""
    (?:shadow-[a-z]+)
    | (?:\d+\s+\d+px\s+\d+px\s+rgba?\([^)]*\))
""", re.VERBOSE)

# 背景/填充
BG_RE = re.compile(
    r"(?:白底)"
    r"|(?:背景(?:\s*[^|,\n]+?)?)(?=[,，|;]|$)"
    r"|(?:浅色)"
    r"|(?:深色)"
    r"|(?:透明)"
    r"|(?:半透明)",
    re.VERBOSE,
)

# 透明度/不透明度/百分比
OPACITY_RE = re.compile(r"""
    (?:透明度\s*[\d.]+)
    | (?:不透明度\s*[\d.]+)
    | (?:opacity\s+[\d.]+)
    | (?:\d+%\s*透明度)
    | (?:白色\s*\d+%)
    | (?:\d+%\s*白色)
    | (?:\d+%\s*不透明度)
    | (?:\b\d+%\b)
""", re.VERBOSE)

# 边框
BORDER_RE = re.compile(
    r"(?:\b边框\b)"
    r"|(?:边框\s*\d*px?\s*[^|,\n]+?)(?=[,，|;]|$)"
    r"|(?:底部\s*\d+px\s*[^|,\n]+?)(?=[,，|;]|$)"
    r"|(?:顶部\s*\d+px\s*[^|,\n]+?)(?=[,，|;]|$)"
    r"|(?:边框变红)",
    re.VERBOSE,
)

# 对齐/布局
ALIGN_RE = re.compile(r"""
    (?:居中)
    | (?:水平居中)
    | (?:垂直居中)
    | (?:Auto\s*Layout)
    | (?:横向布局)
    | (?:横向等分)
    | (?:等分)
    | (?:布局)
    | (?:flex)
    | (?:grid)
""", re.VERBOSE | re.IGNORECASE)

# 定位描述词（中文无词边界，直接匹配常见定位词）
POS_RE = re.compile(r"""
    (左侧|右侧|上方|下方|顶部|底部|固定于|位于|左上角|右上角|左下角|右下角)
""", re.VERBOSE)

# 颜色语义残留（含更多颜色与后缀）
COLOR_SEMANTIC_RE = re.compile(
    r"(?:深蓝|浅蓝|深灰|浅灰|白色|黑色|蓝色|绿色|红色|橙色|灰色|浅橙|深绿|浅绿|深红|浅红|紫色|青色|黄色|粉色|棕色|泳池蓝|微信绿|活力橙|主色|品牌色|强调色|错误色|警告色|状态色|全局状态色|禁用色|占位色|文字色|图标色|金额色|同色|撞色|深色|浅色|透明|半透明)"
    r"(?:\s*(?:线性|填充|图标|占位|色|渐变|背景|文字|蒙层|描边|边框|底|填充|系|主题))?(?=[,，;；：:\|\)\）]|\s|$)",
    re.VERBOSE,
)

# 形状/装饰
SHAPE_RE = re.compile(r"""
    (?:圆形)
    | (?:正圆)
    | (?:白边)
    | (?:pill)
    | (?:线框)
    | (?:outline)
""", re.VERBOSE | re.IGNORECASE)

# 渐变/样式关键词
STYLE_KEYWORDS_RE = re.compile(
    r"(?:渐变)"
    r"|(?:Primary)"
    r"|(?:Secondary)"
    r"|(?:主色)"
    r"|(?:错误色)"
    r"|(?:警告色)"
    r"|(?:强调色)"
    r"|(?:状态色)"
    r"|(?:全局状态色)"
    r"|(?:品牌色)"
    r"|(?:样式)"
    r"|(?:位置)"
    r"|(?:颜色)"
    r"|(?:字体)"
    r"|(?:对齐)"
    r"|(?:胶囊)"
    r"|(?:下划线)"
    r"|(?:加粗)"
    r"|(?:阴影)"
    r"|(?:安全区)"
    r"|(?:排版)"
    r"|(?:毛玻璃)"
    r"|(?:蒙层)"
    r"|(?:质感)"
    r"|(?:层次感)"
    r"|(?:视觉权重)"
    r"|(?:层级)"
    r"|(?:z-index)"
    r"|(?:悬浮)"
    r"|(?:吸顶)"
    r"|(?:固定)"
    r"|(?:粘性)"
    r"|(?:全屏)"
    r"|(?:沉浸式)"
    r"|(?:高亮)"
    r"|(?:醒目)"
    r"|(?:大字号)"
    r"|(?:小字号)"
    r"|(?:直角)"
    r"|(?:实线)"
    r"|(?:虚线)"
    r"|(?:配色)"
    r"|(?:色系)"
    r"|(?:主题)"
    r"|(?:风格)"
    r"|(?:Token)"
    r"|(?:设计稿实现布局)"
    r"|(?:深水)"
    r"|(?:浅水)"
    r"|(?:按设计稿)"
    r"|(?:同比例放大)"
    r"|(?:描边)"
    r"|(?:投影)",
    re.VERBOSE | re.IGNORECASE,
)

# 仅含明确样式值的模式，用于非表格文本/检查清单，避免破坏句子结构
EXPLICIT_STYLE_RES = [
    COLOR_RE, SIZE_RE, FONT_RE, SPACING_RE, RADIUS_RE,
    SHADOW_RE, BG_RE, OPACITY_RE, BORDER_RE,
]

ALL_STYLE_RES = EXPLICIT_STYLE_RES + [ALIGN_RE, POS_RE, COLOR_SEMANTIC_RE, SHAPE_RE, STYLE_KEYWORDS_RE]

# 检查清单中若清理后只剩这些字符，则删除该行
CHECKLIST_JUNK_RE = re.compile(r"^[\s\+，、；：:()（）\-\|\.]*$")
# 仅含数字/空格的单元格视为样式残留
NUMERIC_ONLY_RE = re.compile(r"^[\s\d]+$")


def _collapse_punctuation(text: str) -> str:
    # 保留 URL 协议中的 //：不在 : 后连续删除 /
    text = re.sub(r"(?<!:)/{2,}", "", text)
    # 清理括号内只有标点/空格的括号，但保留函数调用 ()
    text = re.sub(r"(?<![A-Za-z0-9_.])[\(\（][\s,，、/]*[\)\）](?![A-Za-z0-9_])", "", text)
    # 清理括号内的前导/尾随标点
    text = re.sub(r"(?<=[\(\（])\s*[,，;；：:、/]+\s*", "", text)
    text = re.sub(r"\s*[,，;；：:、/]+\s*(?=[\)\）])", "", text)
    # 再次清理可能变空的括号（仍保留函数调用）
    text = re.sub(r"(?<![A-Za-z0-9_.])[\(\（][\s,，、/]*[\)\）](?![A-Za-z0-9_])", "", text)
    text = re.sub(r"`\s*`", "", text)
    text = re.sub(r"[,，;；]{2,}", "，", text)
    text = re.sub(r"[,，;；]\s*[,，;；]", "，", text)
    text = re.sub(r"、\s*、+", "、", text)
    text = re.sub(r"^\s*[,，;；：:、/]\s*", "", text)
    text = re.sub(r"\s*[,，;；：:、/]\s*$", "", text)
    text = re.sub(r"\s{2,}", " ", text)
    return text.strip()


def clean_text(text: str, patterns=None) -> str:
    """清理文本中的样式标记，返回空字符串表示全部内容为样式。"""
    if not text.strip():
        return text
    patterns = patterns or ALL_STYLE_RES
    for pat in patterns:
        text = pat.sub("", text)
    text = _collapse_punctuation(text)
    return text


def clean_cell(text: str) -> str:
    """清理单个单元格中的样式标记；若清理后为空或仅标点/数字则返回 "—"。"""
    if not text.strip():
        return text
    original = text
    text = clean_text(text)
    if not text and original.strip():
        return "—"
    stripped = text.strip()
    if stripped == "":
        return "—"
    if CHECKLIST_JUNK_RE.match(stripped) or NUMERIC_ONLY_RE.match(stripped):
        return "—"
    return text


def is_style_field(field: str) -> bool:
    field = field.strip()
    return field in STYLE_FIELDS or any(f in field for f in STYLE_FIELDS)


def is_separator_row(cells):
    return all(re.match(r"^[:\-\s]*$", c) for c in cells)


def parse_table_row(line: str):
    """解析 markdown 表格行，返回 cells 列表（不含首尾空）。"""
    parts = line.split("|")
    while parts and not parts[0].strip():
        parts.pop(0)
    while parts and not parts[-1].strip():
        parts.pop()
    return [p.strip() for p in parts]


def format_table_row(cells):
    return "| " + " | ".join(cells) + " |\n"


def process_table(rows, section=None):
    """
    rows 是一个表格的所有行（包括表头和分隔线）。
    返回处理后的行列表。
    """
    if not rows:
        return rows

    parsed = [parse_table_row(r) for r in rows]
    if len(parsed) < 2:
        return rows

    header = parsed[0]

    # 检测并移除样式列
    cols_to_remove = set()
    for i, h in enumerate(header):
        if h.strip() in STYLE_COLUMNS:
            cols_to_remove.add(i)

    if cols_to_remove:
        new_parsed = []
        for cells in parsed:
            new_cells = [c for i, c in enumerate(cells) if i not in cols_to_remove]
            new_parsed.append(new_cells)
        parsed = new_parsed
        header = parsed[0]

    # 处理每一行：删除纯样式字段行，或清理单元格
    result = []
    for idx, cells in enumerate(parsed):
        if is_separator_row(cells):
            result.append(format_table_row(cells))
            continue
        if idx == 0:
            result.append(format_table_row(cells))
            continue

        field = cells[0].strip() if cells else ""
        if is_style_field(field):
            continue

        new_cells = []
        for i, c in enumerate(cells):
            if i == 0:
                new_cells.append(c)
            else:
                new_cells.append(clean_cell(c))
        result.append(format_table_row(new_cells))

    return result


def clean_section_heading(line: str) -> str:
    """移除章节标题中的 y 坐标/位置/居中 等样式标注。"""
    line = re.sub(r"\s*[（(]y\s*=[^（）()]*[）)]", "", line)
    line = re.sub(r"\s*[（(]\s*[-×\dpx\s,，]+\s*[）)]", "", line)
    return line


def clean_non_table_line(line: str, section: int | None) -> str | None:
    """对非表格行进行轻度样式清理，主要用于 §1 / §3 内的说明文字、检查清单与标题。"""
    # 跳过引用块中的版本/日期元数据
    if line.lstrip().startswith(">") and "|" in line:
        return line

    stripped = line.lstrip()
    is_heading = stripped.startswith("#")
    is_checklist = stripped.startswith("- [ ]") or stripped.startswith("- [x]")

    # 标题只移除坐标标注，保留章节名称
    if is_heading:
        return clean_section_heading(line)

    # 检查清单：应用全部样式清理，若结果无意义则删除整行
    if is_checklist:
        prefix_match = re.match(r"^(\s*- \[[ x]\]\s*)", line)
        if prefix_match:
            prefix = prefix_match.group(1)
            cleaned = clean_text(line[len(prefix):].rstrip("\n"), ALL_STYLE_RES)
            cleaned = _collapse_punctuation(cleaned)
            if cleaned == "" or CHECKLIST_JUNK_RE.match(cleaned):
                return None
            return prefix + cleaned + "\n"
        cleaned = clean_text(line.rstrip("\n"), ALL_STYLE_RES)
        if cleaned == "" or CHECKLIST_JUNK_RE.match(cleaned):
            return None
        return cleaned + "\n"

    # 纯粹的视觉/设计说明引用块（非元素清单），直接删除
    if stripped.startswith("> **视觉处理**") or stripped.startswith("> **设计重点**"):
        return None

    # §1 / §3 的说明文字仅清理明确的样式值（颜色、尺寸、字体等），保留布局/语义描述
    if section in (1, 3):
        leading = re.match(r"^(\s*[-*>]+\s*)", line)
        if leading:
            prefix = leading.group(1)
            return prefix + clean_text(line[len(prefix):].rstrip("\n"), EXPLICIT_STYLE_RES) + "\n"
        text = clean_text(line.rstrip("\n"), EXPLICIT_STYLE_RES)
        return text + "\n" if text else "\n"

    return line


def process_file(path: Path) -> str:
    content = path.read_text(encoding="utf-8")
    lines = content.splitlines(keepends=True)

    output = []
    table_buffer = []
    in_table = False
    section = None

    for line in lines:
        stripped = line.lstrip()

        # 跟踪章节号
        m = re.match(r"^##\s+(\d+)\.", line)
        if m:
            section = int(m.group(1))

        # 跳过 blockquote 中的元数据行（如 > **版本**：v1.0 | **创建日期**：...）
        if stripped.startswith(">") and "|" in line and not in_table:
            output.append(line)
            continue

        if "|" in line:
            table_buffer.append(line)
            in_table = True
        else:
            if in_table:
                output.extend(process_table(table_buffer, section))
                table_buffer = []
                in_table = False
            cleaned = clean_non_table_line(line, section)
            if cleaned is not None:
                output.append(cleaned)

    if in_table:
        output.extend(process_table(table_buffer, section))

    # 压缩连续空行（最多保留两个）
    compressed = []
    blank_count = 0
    for line in output:
        if line.strip() == "":
            blank_count += 1
            if blank_count <= 2:
                compressed.append(line)
        else:
            blank_count = 0
            compressed.append(line)
    return "".join(compressed)


def main():
    for path in FILES:
        if path.name in ("A-TEMPLATE.md", "TEMPLATE.md"):
            continue
        new_content = process_file(path)
        path.write_text(new_content, encoding="utf-8")
        print(f"cleaned: {path.name}")


if __name__ == "__main__":
    main()
