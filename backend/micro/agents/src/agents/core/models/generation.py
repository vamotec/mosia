"""Data models for content generation."""

from typing import Dict, List, Optional
from datetime import datetime
from pydantic import BaseModel, Field
from enum import Enum


class ContentType(str, Enum):
    """Types of content that can be generated."""
    TEXT = "text"
    MARKDOWN = "markdown"
    CODE = "code"
    EMAIL = "email"
    SUMMARY = "summary"


class GenerationStyle(str, Enum):
    """Content generation styles."""
    FORMAL = "formal"
    CASUAL = "casual"
    TECHNICAL = "technical"
    CREATIVE = "creative"


class GenerationOptions(BaseModel):
    """Options for content generation."""
    
    max_length: int = Field(default=1000, ge=1, le=10000)
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    style: GenerationStyle = GenerationStyle.FORMAL
    language: str = Field(default="en")
    include_citations: bool = Field(default=False)


class ContentGenerationInput(BaseModel):
    """Input for content generation."""
    
    user_id: str
    workspace_id: str
    prompt: str
    content_type: ContentType
    parameters: Dict[str, str] = Field(default_factory=dict)
    options: GenerationOptions = Field(default_factory=GenerationOptions)


class GeneratedContent(BaseModel):
    """Generated content result."""
    
    generated_content: str
    generation_id: str
    metadata: Dict[str, str] = Field(default_factory=dict)
    generated_at: datetime
    word_count: int
    estimated_reading_time: int  # in minutes


class SummarizationInput(BaseModel):
    """Input for text summarization."""
    
    content: str
    max_length: int = Field(default=500, ge=50, le=2000)
    summary_type: str = Field(default="paragraph", pattern="^(bullet_points|paragraph|abstract)$")


class SummarizationResult(BaseModel):
    """Text summarization result."""
    
    summary: str
    key_points: List[str]
    metadata: Dict[str, str] = Field(default_factory=dict)
    compression_ratio: float  # original_length / summary_length
    reading_time_saved: int  # in minutes


class GenerationMetrics(BaseModel):
    """Metrics for generation operations."""
    
    processing_time_ms: int
    input_tokens: int
    output_tokens: int
    model_used: str
    cost_estimate: Optional[float] = None