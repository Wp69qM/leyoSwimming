import os
from functools import lru_cache

from dotenv import load_dotenv
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

load_dotenv()


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = Field(default="leyo-ai-service")
    app_env: str = Field(default="development")
    host: str = Field(default="0.0.0.0")
    port: int = Field(default=8000)
    log_level: str = Field(default="INFO")
    log_dir: str = Field(default="logs")

    internal_api_token: str = Field(default="")
    java_internal_base_url: str = Field(default="http://leyo-backend:8080/api/internal/ai")

    llm_model: str = Field(default="qwen-plus")
    llm_base_url: str = Field(default="https://dashscope.aliyuncs.com/compatible-mode/v1")
    llm_api_key: str = Field(default="")
    llm_temperature: float = Field(default=0.3)
    llm_max_tokens: int = Field(default=2048)
    llm_timeout_seconds: int = Field(default=30)

    redis_host: str = Field(default="redis")
    redis_port: int = Field(default=6379)
    redis_password: str = Field(default="")
    redis_db: int = Field(default=0)

    session_message_ttl_seconds: int = Field(default=7 * 24 * 3600)
    session_max_messages: int = Field(default=20)

    rate_limit_user_per_minute: int = Field(default=30)
    rate_limit_ip_per_minute: int = Field(default=60)

    trusted_proxy_count: int = Field(default=0)

    enable_mock_data: bool = Field(default=False)

    @property
    def redis_url(self) -> str:
        auth = f":{self.redis_password}@" if self.redis_password else ""
        return f"redis://{auth}{self.redis_host}:{self.redis_port}/{self.redis_db}"


@lru_cache
def get_settings() -> Settings:
    return Settings()
