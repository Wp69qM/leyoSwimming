import pytest

from app.clients.java_client import JavaInternalClient, generate_message_id, generate_session_id
from app.config import Settings


@pytest.fixture
def real_client():
    settings = Settings(enable_mock_data=False, internal_api_token="test" * 8)
    return JavaInternalClient(settings=settings)


class FakeResponse:
    def __init__(self, data):
        self._data = data

    def raise_for_status(self):
        pass

    def json(self):
        return self._data


@pytest.mark.asyncio
async def test_query_coaches_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse(
            {
                "code": 0,
                "message": "ok",
                "data": {
                    "data": [
                        {
                            "coachHash": "c_1",
                            "coachId": 1,
                            "name": "王教练",
                            "avatarUrl": "https://example.com/avatar.jpg",
                            "gender": "female",
                            "rating": 4.9,
                            "referencePrice": 200,
                            "teachingYears": 8,
                            "teachingStrokes": ["freestyle", "breaststroke"],
                        }
                    ]
                },
            }
        )

    real_client.client.post = fake_post
    result = await real_client.query_coaches(stroke="freestyle")
    assert result == [
        {
            "coach_hash": "c_1",
            "coach_id": 1,
            "name": "王教练",
            "avatar_url": "https://example.com/avatar.jpg",
            "gender": "female",
            "rating": 4.9,
            "reference_price": 200,
            "teaching_years": 8,
            "teaching_strokes": ["freestyle", "breaststroke"],
        }
    ]


@pytest.mark.asyncio
async def test_query_packages_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse(
            {
                "code": 0,
                "message": "ok",
                "data": {
                    "data": [
                        {
                            "packageHash": "p_1",
                            "packageId": 1,
                            "packageMode": "standard",
                            "name": "自由泳课",
                            "hours": 10,
                            "price": 1800,
                            "pricePerHour": 180,
                            "validityDays": 90,
                            "classSize": "一对一",
                            "strokes": ["freestyle"],
                        }
                    ]
                },
            }
        )

    real_client.client.post = fake_post
    result = await real_client.query_packages(package_mode="standard")
    assert result == [
        {
            "package_hash": "p_1",
            "package_id": 1,
            "package_mode": "standard",
            "name": "自由泳课",
            "hours": 10,
            "price": 1800,
            "price_per_hour": 180,
            "validity_days": 90,
            "class_size": "一对一",
            "strokes": ["freestyle"],
        }
    ]


@pytest.mark.asyncio
async def test_get_user_profile_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse(
            {
                "code": 0,
                "message": "ok",
                "data": {
                    "userId": 1,
                    "userHash": "u_123",
                    "age": 30,
                    "targetStroke": "freestyle",
                    "swimmingLevel": "beginner",
                    "budget": 3000,
                    "isMinor": False,
                },
            }
        )

    real_client.client.post = fake_post
    result = await real_client.get_user_profile("u_123")
    assert result == {
        "user_id": 1,
        "user_hash": "u_123",
        "age": 30,
        "target_stroke": "freestyle",
        "swimming_level": "beginner",
        "budget": 3000,
        "is_minor": False,
    }


@pytest.mark.asyncio
async def test_get_user_packages_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse(
            {
                "code": 0,
                "message": "ok",
                "data": {
                    "data": [
                        {
                            "packageHash": "up_1",
                            "packageId": 1,
                            "name": "自由泳套餐",
                            "status": "active",
                            "hours": 10,
                            "remainingHours": 5,
                            "strokes": ["freestyle"],
                        }
                    ]
                },
            }
        )

    real_client.client.post = fake_post
    result = await real_client.get_user_packages("u_123")
    assert result == [
        {
            "package_hash": "up_1",
            "package_id": 1,
            "name": "自由泳套餐",
            "status": "active",
            "hours": 10,
            "remaining_hours": 5,
            "strokes": ["freestyle"],
        }
    ]


@pytest.mark.asyncio
async def test_get_hot_recommendations_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse(
            {
                "code": 0,
                "message": "ok",
                "data": {
                    "data": [
                        {"type": "coach", "coachHash": "c_1", "name": "王教练"},
                        {"type": "package", "packageHash": "p_1", "name": "自由泳课"},
                    ]
                },
            }
        )

    real_client.client.post = fake_post
    result = await real_client.get_hot_recommendations()
    assert result == [
        {"type": "coach", "coach_hash": "c_1", "name": "王教练"},
        {"type": "package", "package_hash": "p_1", "name": "自由泳课"},
    ]


@pytest.mark.asyncio
async def test_query_coaches_mock_mode():
    settings = Settings(enable_mock_data=True, internal_api_token="test" * 8)
    client = JavaInternalClient(settings=settings)
    result = await client.query_coaches(stroke="freestyle")
    assert len(result) > 0
    assert all("freestyle" in c["teaching_strokes"] for c in result)


@pytest.mark.asyncio
async def test_query_packages_mock_mode():
    settings = Settings(enable_mock_data=True, internal_api_token="test" * 8)
    client = JavaInternalClient(settings=settings)
    result = await client.query_packages(package_mode="experience")
    assert len(result) > 0
    assert all(p["package_mode"] == "experience" for p in result)


def test_generate_message_id_format():
    mid = generate_message_id()
    assert mid.startswith("msg_")
    assert len(mid) > 4


def test_generate_session_id_format():
    sid = generate_session_id()
    assert sid.startswith("sess_")
    assert len(sid) > 5
