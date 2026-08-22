import pytest

from app.tools.recommendation_tools import normalize_gender, normalize_stroke


@pytest.mark.parametrize(
    "input_stroke,expected",
    [
        ("自由泳", "freestyle"),
        ("FREESTYLE", "freestyle"),
        ("蛙泳", "breaststroke"),
        ("breaststroke", "breaststroke"),
        (None, None),
        ("", None),
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
    ],
)
def test_normalize_gender(input_gender, expected):
    assert normalize_gender(input_gender) == expected
