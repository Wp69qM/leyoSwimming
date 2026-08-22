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
        return FakeResponse({"data": [{"name": "王教练"}]})

    real_client.client.post = fake_post
    result = await real_client.query_coaches(stroke="freestyle")
    assert result == [{"name": "王教练"}]


@pytest.mark.asyncio
async def test_query_packages_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse({"data": [{"name": "自由泳课"}]})

    real_client.client.post = fake_post
    result = await real_client.query_packages(package_mode="standard")
    assert result == [{"name": "自由泳课"}]


@pytest.mark.asyncio
async def test_get_user_profile_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse({"data": {"age": 30}})

    real_client.client.post = fake_post
    result = await real_client.get_user_profile("u_123")
    assert result == {"age": 30}


@pytest.mark.asyncio
async def test_get_user_packages_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse({"data": []})

    real_client.client.post = fake_post
    result = await real_client.get_user_packages("u_123")
    assert result == []


@pytest.mark.asyncio
async def test_get_hot_recommendations_real_path(real_client):
    async def fake_post(path, json):
        return FakeResponse({"data": [{"type": "coach"}]})

    real_client.client.post = fake_post
    result = await real_client.get_hot_recommendations()
    assert result == [{"type": "coach"}]


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
