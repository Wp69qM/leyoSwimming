import json
import logging
import re
import threading
from datetime import datetime
from pathlib import Path

import structlog

from app.config import get_settings


class SessionFileHandler(logging.Handler):
    """按 session_id 分文件写入的日志处理器。

    同一 session_id 的日志写入同一个文件；文件名格式为
    ``{session_id}_{首次出现时间}.log``，并统一存放在 ``logs/`` 目录下。
    无法识别 session_id 的日志会落入 ``unknown_{时间}.log``。
    """

    def __init__(self, log_dir: str):
        super().__init__()
        self.log_dir = Path(log_dir)
        self.log_dir.mkdir(parents=True, exist_ok=True)
        self._files: dict[str, tuple[Path, logging.StreamHandler]] = {}
        self._lock = threading.Lock()

    def _resolve_session_id(self, record: logging.LogRecord) -> str:
        # 1. 优先从 record 的 extra 字段取 session_id
        session_id = getattr(record, "session_id", None)
        if session_id:
            return str(session_id)

        # 2. 尝试从格式化后的 JSON 消息中解析
        try:
            data = json.loads(record.getMessage())
            session_id = data.get("session_id")
            if session_id:
                return str(session_id)
        except Exception:
            pass

        # 3. 正则兜底匹配
        msg = self.format(record)
        match = re.search(r'"session_id":\s*"([^"]+)"', msg)
        if match:
            return match.group(1)

        return "unknown"

    def _get_handler(self, session_id: str) -> logging.StreamHandler:
        with self._lock:
            if session_id in self._files:
                return self._files[session_id][1]

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            file_path = self.log_dir / f"{session_id}_{timestamp}.log"
            f = open(file_path, "a", encoding="utf-8")
            handler = logging.StreamHandler(f)
            handler.setFormatter(self.formatter)
            self._files[session_id] = (file_path, handler)
            return handler

    def emit(self, record: logging.LogRecord) -> None:
        try:
            session_id = self._resolve_session_id(record)
            handler = self._get_handler(session_id)
            handler.emit(record)
        except Exception:
            self.handleError(record)

    def close(self) -> None:
        with self._lock:
            for _, handler in self._files.values():
                handler.close()
            self._files.clear()
        super().close()


def configure_logging() -> None:
    settings = get_settings()
    level = getattr(logging, settings.log_level.upper(), logging.INFO)

    # 预创建日志目录
    Path(settings.log_dir).mkdir(parents=True, exist_ok=True)

    # 配置 structlog 将日志转发到标准库 logging，以便使用文件 handler
    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.add_log_level,
            structlog.processors.TimeStamper(fmt="iso"),
            structlog.stdlib.ExtraAdder(),
            structlog.stdlib.ProcessorFormatter.wrap_for_formatter,
        ],
        wrapper_class=structlog.make_filtering_bound_logger(level),
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        cache_logger_on_first_use=True,
    )

    root_logger = logging.getLogger()
    root_logger.setLevel(level)

    # 清除默认 handler，避免重复输出
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)

    # 控制台 handler：开发环境保持可读性
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(
        structlog.stdlib.ProcessorFormatter(
            processor=structlog.dev.ConsoleRenderer(colors=True),
            foreign_pre_chain=[
                structlog.contextvars.merge_contextvars,
                structlog.processors.add_log_level,
                structlog.processors.TimeStamper(fmt="iso"),
            ],
        )
    )
    root_logger.addHandler(console_handler)

    # 按 session_id 分文件的 handler：JSON 格式便于解析与归档
    file_handler = SessionFileHandler(log_dir=settings.log_dir)
    file_handler.setFormatter(
        structlog.stdlib.ProcessorFormatter(
            processor=structlog.processors.JSONRenderer(),
            foreign_pre_chain=[
                structlog.contextvars.merge_contextvars,
                structlog.processors.add_log_level,
                structlog.processors.TimeStamper(fmt="iso"),
            ],
        )
    )
    root_logger.addHandler(file_handler)


def get_logger(name: str):
    return structlog.get_logger(name)
