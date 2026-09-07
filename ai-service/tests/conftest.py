import os

# pytest-cov 的 trace 会干扰 pydantic 延迟加载 root_model（RootModel.__class_getitem__
# 访问 sys.modules['pydantic.root_model'] 时报 KeyError），预导入规避。
import pydantic.root_model  # noqa: F401

import pytest

# 在应用导入前设置内部接口鉴权 Token，确保测试请求可以通过中间件校验。
# 长度需满足启动时 >= 32 位的校验。
# 必须强制赋值而非 setdefault：pytest-cov 启动时 coverage 会提前导入 app 包，
# 触发 config.py 模块级 load_dotenv() 将本地 .env 的真实 token 写入进程环境，
# 早于本 conftest 执行；setdefault 会保留真实值导致测试请求 401。
# 强制赋值后进程环境变量优先于 env_file（pydantic-settings 优先级），与
# tests/mcp/helpers.py 硬编码的 MCP_TOKEN 保持一致。
os.environ["INTERNAL_API_TOKEN"] = "test-internal-token-for-ai-service-123"

# MCP Server 独立鉴权 Token（US-066），长度需满足启动时 >= 32 位的校验。
os.environ["MCP_API_TOKEN"] = "test-mcp-token-for-ai-service-45678"

# 测试环境使用较低的限流阈值，便于验证限流行为。
# 同样强制赋值：本地 .env 的真实限流配置会在 pytest-cov 提前触发
# load_dotenv() 时注入进程环境，setdefault 会保留真实值导致
# test_rate_limit 的阈值断言（429 触发点）随 .env 漂移而失败。
os.environ["RATE_LIMIT_IP_PER_MINUTE"] = "3"
os.environ["RATE_LIMIT_USER_PER_MINUTE"] = "2"


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
