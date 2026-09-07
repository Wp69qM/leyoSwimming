"""MCP 配置项默认值测试（US-066 Task 1 / US-067 Task 2）。"""

from app.config import Settings


def test_mcp_api_token_defaults_to_empty(monkeypatch):
    monkeypatch.delenv("MCP_API_TOKEN", raising=False)
    settings = Settings(_env_file=None)
    assert settings.mcp_api_token == ""


def test_mcp_top_k_max_defaults_to_10(monkeypatch):
    monkeypatch.delenv("MCP_TOP_K_MAX", raising=False)
    settings = Settings(_env_file=None)
    assert settings.mcp_top_k_max == 10


def test_mcp_limit_max_defaults_to_20(monkeypatch):
    monkeypatch.delenv("MCP_LIMIT_MAX", raising=False)
    settings = Settings(_env_file=None)
    assert settings.mcp_limit_max == 20


def test_mcp_api_token_loaded_from_env(monkeypatch):
    monkeypatch.setenv("MCP_API_TOKEN", "a" * 32)
    settings = Settings(_env_file=None)
    assert settings.mcp_api_token == "a" * 32
