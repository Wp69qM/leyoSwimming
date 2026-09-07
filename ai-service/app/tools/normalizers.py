"""参数归一化共享模块（US-067）。

LangChain 工具链路与 MCP 工具链路共用的单一事实来源，
防止两协议层行为漂移。函数均幂等：无法识别的输入返回 None（静默降级）。
"""

from app.utils.logger import get_logger

logger = get_logger(__name__)

STROKE_MAP = {
    "自由泳": "freestyle",
    "蛙泳": "breaststroke",
    "仰泳": "backstroke",
    "蝶泳": "butterfly",
}

STROKE_ENS = {"freestyle", "breaststroke", "backstroke", "butterfly"}


def normalize_stroke(stroke: str | None) -> str | None:
    if not stroke:
        return None
    lower = stroke.lower()
    for cn, en in STROKE_MAP.items():
        if cn in stroke or lower == en:
            return en
    logger.warning("unknown_stroke_input", stroke=stroke)
    return None


def normalize_gender(gender: str | None) -> str | None:
    if not gender:
        return None
    g = gender.lower()
    if g in ("女", "female", "f"):
        return "female"
    if g in ("男", "male", "m"):
        return "male"
    logger.warning("unknown_gender_input", gender=gender)
    return None


def normalize_package_mode(package_mode: str | None) -> str | None:
    if not package_mode:
        return None
    mode = package_mode.lower()
    if "体验" in package_mode or mode == "experience":
        return "experience"
    if "标准" in package_mode or mode == "standard":
        return "standard"
    if "自定义" in package_mode or mode == "custom":
        return "custom"
    logger.warning("unknown_package_mode_input", package_mode=package_mode)
    return None
