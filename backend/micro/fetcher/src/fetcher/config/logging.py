"""Logging configuration for the Fetcher service."""
import logging
import sys
import structlog
from typing import Any, Dict
from .settings import settings

def setup_logging() -> None:
    """Configure structured logging for the service."""

    # 先初始化标准 logging
    logging.basicConfig(
        format="%(message)s",
        stream=sys.stdout,
        level=getattr(logging, settings.log_level.upper(), logging.INFO),
    )
    
    # 抑制第三方库的冗余日志
    logging.getLogger("grpc").setLevel(logging.WARNING)

    # 配置 structlog
    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.add_log_level,
            structlog.processors.StackInfoRenderer(),
            structlog.processors.format_exc_info,
            structlog.dev.set_exc_info,
            structlog.processors.TimeStamper(fmt="iso"),
            structlog.dev.ConsoleRenderer() if settings.log_level.lower() == "debug"
            else structlog.processors.JSONRenderer(),
        ],
        wrapper_class=structlog.make_filtering_bound_logger(
            getattr(logging, settings.log_level.upper(), logging.INFO)
        ),
        logger_factory=structlog.stdlib.LoggerFactory(),
        cache_logger_on_first_use=True,
    )

def get_logger(name: str) -> structlog.BoundLogger:
    """Get a structured logger instance."""
    return structlog.get_logger(name)


def log_grpc_call(method: str, request_data: Dict[str, Any]) -> None:
    """Log gRPC method calls with structured data."""
    logger = get_logger("grpc")
    logger.info(
        "gRPC call received",
        method=method,
        request_size=len(str(request_data)),
        user_id=request_data.get("user_id"),
        workspace_id=request_data.get("workspace_id"),
    )