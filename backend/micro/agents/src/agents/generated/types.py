"""
类型定义和常量
"""

from enum import Enum
from typing import Dict, Any, List, Optional
from dataclasses import dataclass
from datetime import datetime


class SourceType(str, Enum):
    """数据源类型"""
    API = "api"
    WEB = "web"
    FILE = "file"
    STREAM = "stream"


class ProcessingType(str, Enum):
    """处理类型"""
    PARSE = "parse"
    TRANSFORM = "transform"
    VALIDATE = "validate"
    ENRICH = "enrich"


class OutputFormat(str, Enum):
    """输出格式"""
    JSON = "json"
    XML = "xml"
    CSV = "csv"
    RAW = "raw"


@dataclass
class FetchConfig:
    """获取配置"""
    timeout_seconds: int = 30
    retry_count: int = 3
    cache_enabled: bool = True
    cache_ttl: str = "3600"
    async_processing: bool = False
    output_format: OutputFormat = OutputFormat.JSON


@dataclass
class BulkConfig:
    """批量处理配置"""
    max_concurrent: int = 10
    stop_on_error: bool = False
    timeout_seconds: int = 300


# 常量定义
DEFAULT_TIMEOUT = 30
DEFAULT_RETRY_COUNT = 3
MAX_CONCURRENT_FETCHES = 50
CACHE_TTL_DEFAULT = "3600"

# 错误代码
ERROR_CODES = {
    "TIMEOUT": "请求超时",
    "NETWORK_ERROR": "网络错误", 
    "INVALID_URL": "无效的URL",
    "AUTHENTICATION_FAILED": "认证失败",
    "RATE_LIMITED": "请求过于频繁",
    "SERVER_ERROR": "服务器错误",
    "DATA_VALIDATION_FAILED": "数据验证失败",
    "UNSUPPORTED_FORMAT": "不支持的格式"
}
