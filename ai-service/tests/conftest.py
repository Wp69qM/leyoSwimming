import os

import pytest

# 在应用导入前设置内部接口鉴权 Token，确保测试请求可以通过中间件校验。
# 长度需满足启动时 >= 32 位的校验。
os.environ.setdefault("INTERNAL_API_TOKEN", "test-internal-token-for-ai-service-123")

# MCP Server 独立鉴权 Token（US-066），长度需满足启动时 >= 32 位的校验。
os.environ.setdefault("MCP_API_TOKEN", "test-mcp-token-for-ai-service-45678")

# 测试环境使用较低的限流阈值，便于验证限流行为。
os.environ.setdefault("RATE_LIMIT_IP_PER_MINUTE", "3")
os.environ.setdefault("RATE_LIMIT_USER_PER_MINUTE", "2")


def pytest_configure(config):
    config.addinivalue_line(
        "markers",
        "rate_limit: mark tests that exercise the real rate limiting middleware",
    )


@pytest.fixture(autouse=True)
def disable_rate_limit_for_non_rate_limit_tests(request, monkeypatch):
    """默认关闭限流中间件对 Redis 的依赖，避免无 Redis 环境导致测试失败。

    标记了 @pytest.mark.rate_limit 的测试会保留真实限流逻辑。
    """
    if request.node.get_closest_marker("rate_limit"):
        return

    from app.middleware.rate_limit import RateLimitMiddleware

    async def _noop_is_limited(self, key: str, limit: int) -> bool:
        return False

    monkeypatch.setattr(RateLimitMiddleware, "_is_limited", _noop_is_limited)
