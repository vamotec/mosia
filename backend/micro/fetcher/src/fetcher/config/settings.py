"""Configuration management for the Fetcher service."""

from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings
from pathlib import Path

class DatabaseConfig(BaseSettings):
    """Database configuration."""
    
    url: str = Field(default="postgresql://mosia:ttr851217@localhost:5432/mosia_dev")
    pool_size: int = Field(default=10)
    max_connections: int = Field(default=20)
    
    class Config:
        env_prefix = "DB_"


class RedisConfig(BaseSettings):
    """Redis configuration."""
    
    host: str = Field(default="localhost")
    port: int = Field(default=6379)
    db: int = Field(default=1)
    password: Optional[str] = Field(default=None)
    
    class Config:
        env_prefix = "REDIS_"


class KafkaConfig(BaseSettings):
    """Kafka configuration."""
    
    bootstrap_server: str = Field(default="localhost:9092")
    group_id: str = Field(default="fetcher_bak-service")
    auto_offset_reset: str = Field(default="earliest")
    
    class Config:
        env_prefix = "KAFKA_"


class FetchingConfig(BaseSettings):
    """Data fetching configuration."""
    
    default_timeout: int = Field(default=30)
    max_concurrent_fetches: int = Field(default=10)
    cache_default_ttl: int = Field(default=3600)
    max_file_size_mb: int = Field(default=100)
    user_agent: str = Field(default="Mosia-Fetcher/1.0")
    
    class Config:
        env_prefix = "FETCHING_" or "DEFAULT_" or "MAX_" or "CACHE_" or "USER_"


class ExternalAPIConfig(BaseSettings):
    """External API configuration."""
    
    financial_api_key: Optional[str] = Field(default=None)
    social_api_key: Optional[str] = Field(default=None)
    news_api_key: Optional[str] = Field(default=None)
    
    class Config:
        env_prefix = "FINANCIAL_" or "SOCIAL_" or "NEWS_"


class RateLimitConfig(BaseSettings):
    """Rate limiting configuration."""
    
    requests_per_minute: int = Field(default=60)
    burst: int = Field(default=10)
    
    class Config:
        env_prefix = "RATE_LIMIT_"


class StorageConfig(BaseSettings):
    """File storage configuration."""
    
    temp_storage_path: str = Field(default="/tmp/mosia_fetcher")
    max_storage_size_gb: int = Field(default=10)
    cleanup_interval_hours: int = Field(default=24)
    
    class Config:
        env_prefix = "TEMP_" or "MAX_" or "CLEANUP_"


class ProcessingConfig(BaseSettings):
    """Data processing configuration."""
    
    batch_size: int = Field(default=100)
    parallel_workers: int = Field(default=5)
    processing_timeout: int = Field(default=300)
    
    class Config:
        env_prefix = "BATCH_" or "PARALLEL_" or "PROCESSING_"


class FeatureFlags(BaseSettings):
    """Feature flag configuration."""
    
    enable_web_scraping: bool = Field(default=True)
    enable_api_fetching: bool = Field(default=True)
    enable_file_processing: bool = Field(default=True)
    enable_streaming: bool = Field(default=True)
    enable_webhooks: bool = Field(default=True)
    
    class Config:
        env_prefix = "ENABLE_"


class MonitoringConfig(BaseSettings):
    """Monitoring configuration."""
    
    metrics_enabled: bool = Field(default=True)
    metrics_port: int = Field(default=8081)
    health_check_interval: int = Field(default=30)
    log_requests: bool = Field(default=True)
    
    class Config:
        env_prefix = "METRICS_" or "HEALTH_" or "LOG_"


class Settings(BaseSettings):
    """Main application settings."""
    
    # Server Configuration
    fetcher_grpc_port: int = Field(default=50051)
    fetcher_host: str = Field(default="0.0.0.0")
    log_level: str = Field(default="debug")

    # New: Data directory
    data_dir: Path = Field(default=Path("./data"), description="Local directory for data storage")

    # Service Configurations
    database: DatabaseConfig = Field(default_factory=DatabaseConfig)
    redis: RedisConfig = Field(default_factory=RedisConfig)
    kafka: KafkaConfig = Field(default_factory=KafkaConfig)
    fetching: FetchingConfig = Field(default_factory=FetchingConfig)
    external_apis: ExternalAPIConfig = Field(default_factory=ExternalAPIConfig)
    rate_limit: RateLimitConfig = Field(default_factory=RateLimitConfig)
    storage: StorageConfig = Field(default_factory=StorageConfig)
    processing: ProcessingConfig = Field(default_factory=ProcessingConfig)
    features: FeatureFlags = Field(default_factory=FeatureFlags)
    monitoring: MonitoringConfig = Field(default_factory=MonitoringConfig)
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


def get_settings() -> Settings:
    """Get application settings singleton."""
    return Settings()


# Global settings instance
settings = get_settings()