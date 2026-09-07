"""normalizers 共享模块测试（US-067 Task 1）。

normalize_stroke / normalize_gender 从 recommendation_tools.py 原样迁移
（既有行为回归），normalize_package_mode 从 query_packages 闭包提取。
LangChain 链路与 MCP 链路共用，防止两协议层行为漂移。
"""

import pytest

from app.tools.normalizers import normalize_gender, normalize_package_mode, normalize_stroke


@pytest.mark.parametrize(
    "input_stroke,expected",
    [
        ("自由泳", "freestyle"),
        ("FREESTYLE", "freestyle"),
        ("蛙泳", "breaststroke"),
        ("breaststroke", "breaststroke"),
        ("仰泳", "backstroke"),
        ("蝶泳", "butterfly"),
        (None, None),
        ("", None),
        ("unknown-stroke", None),
    ],
)
def test_normalize_stroke(input_stroke, expected):
    assert normalize_stroke(input_stroke) == expected


@pytest.mark.parametrize(
    "input_gender,expected",
    [
        ("女", "female"),
        ("female", "female"),
        ("f", "female"),
        ("男", "male"),
        ("male", "male"),
        ("m", "male"),
        (None, None),
        ("", None),
        ("unknown", None),
    ],
)
def test_normalize_gender(input_gender, expected):
    assert normalize_gender(input_gender) == expected


@pytest.mark.parametrize(
    "input_mode,expected",
    [
        ("体验", "experience"),
        ("体验课", "experience"),
        ("experience", "experience"),
        ("EXPERIENCE", "experience"),
        ("标准", "standard"),
        ("标准套餐", "standard"),
        ("standard", "standard"),
        ("自定义", "custom"),
        ("custom", "custom"),
        (None, None),
        ("", None),
        ("unknown", None),
    ],
)
def test_normalize_package_mode(input_mode, expected):
    assert normalize_package_mode(input_mode) == expected
