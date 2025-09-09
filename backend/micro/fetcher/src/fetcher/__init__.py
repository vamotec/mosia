"""
Data Fetcher Microservice for Mosia

This service provides data fetching and processing capabilities including:
- External API integration
- Web scraping and content extraction
- File processing and validation
- Real-time data streaming
- Data enrichment and transformation
"""

__version__ = "0.1.0"
__author__ = "mofan <mofan@mosia.app>"

from .grpc.services.fetch_service import FetchService       # 假设 FetchService 定义在 service.py
from .config.settings import settings            # 假设 settings 定义在 config.py
from .config.logging import setup_logging, get_logger  # 假设日志工具在 logging.py

__all__ = [
    "FetchService",
    "settings",
    "setup_logging",
    "get_logger",
]
