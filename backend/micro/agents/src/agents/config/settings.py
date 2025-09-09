"""Configuration management for the Agents service."""

import os
from pathlib import Path
from typing import Optional
from pydantic import Field
from pydantic_settings import BaseSettings


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
    db: int = Field(default=0)
    password: Optional[str] = Field(default=None)
    
    class Config:
        env_prefix = "REDIS_"


class KafkaConfig(BaseSettings):
    """Kafka configuration."""
    
    bootstrap_server: str = Field(default="localhost:9092")
    group_id: str = Field(default="financial_agents-service")
    auto_offset_reset: str = Field(default="earliest")
    
    class Config:
        env_prefix = "KAFKA_"


class AIConfig(BaseSettings):
    """AI service configuration."""
    
    openai_api_key: Optional[str] = Field(default=None)
    anthropic_api_key: Optional[str] = Field(default=None)
    model_cache_ttl: int = Field(default=3600)
    request_timeout: int = Field(default=30)
    
    class Config:
        env_prefix = "AI_"


class FeatureFlags(BaseSettings):
    """Feature flag configuration."""
    
    enable_content_analysis: bool = Field(default=True)
    enable_recommendations: bool = Field(default=True)
    enable_content_generation: bool = Field(default=True)
    enable_chat: bool = Field(default=True)
    
    class Config:
        env_prefix = "ENABLE_"


class PerformanceConfig(BaseSettings):
    """Performance and scaling configuration."""
    
    max_concurrent_requests: int = Field(default=100)
    grpc_max_workers: int = Field(default=10)
    ai_batch_size: int = Field(default=10)
    
    class Config:
        env_prefix = "MAX_" or "GRPC_" or "AI_"


class MonitoringConfig(BaseSettings):
    """Monitoring configuration."""
    
    metrics_enabled: bool = Field(default=True)
    metrics_port: int = Field(default=8080)
    health_check_interval: int = Field(default=30)
    
    class Config:
        env_prefix = "METRICS_" or "HEALTH_"


class Settings(BaseSettings):
    """Main application settings."""
    
    # Server Configuration
    agents_grpc_port: int = Field(default=50052)
    agents_host: str = Field(default="0.0.0.0")
    fetcher_grpc_port: int = Field(default=50051)
    fetcher_host: str = Field(default="0.0.0.0")
    log_level: str = Field(default="debug")

    # New: Data directory
    data_dir: Path = Field(default=Path("./data"), description="Local directory for data storage")
    
    # Service Configurations
    database: DatabaseConfig = Field(default_factory=DatabaseConfig)
    redis: RedisConfig = Field(default_factory=RedisConfig)
    kafka: KafkaConfig = Field(default_factory=KafkaConfig)
    ai: AIConfig = Field(default_factory=AIConfig)
    features: FeatureFlags = Field(default_factory=FeatureFlags)
    performance: PerformanceConfig = Field(default_factory=PerformanceConfig)
    monitoring: MonitoringConfig = Field(default_factory=MonitoringConfig)
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"


def get_settings() -> Settings:
    """Get application settings singleton."""
    return Settings()


# Global settings instance
settings = get_settings()