"""Data models for content analysis."""

from typing import Dict, List, Optional, TypedDict
from datetime import datetime
from pydantic import BaseModel, Field


class ContentInput(BaseModel):
    """Input model for content analysis."""
    
    user_id: str
    workspace_id: str
    content: str
    content_type: str = Field(..., pattern="^(text|markdown|document|code)$")
    metadata: Dict[str, str] = Field(default_factory=dict)


class KeywordResult(BaseModel):
    """Individual keyword extraction result."""
    
    text: str
    score: float = Field(..., ge=0.0, le=1.0)
    category: Optional[str] = None


class SentimentResult(BaseModel):
    """Sentiment analysis result."""
    
    sentiment: str = Field(..., pattern="^(positive|negative|neutral)$")
    score: float = Field(..., ge=-1.0, le=1.0)
    confidence: float = Field(..., ge=0.0, le=1.0)
    emotions: Dict[str, float] = Field(default_factory=dict)


class ContentAnalysisResult(BaseModel):
    """Complete content analysis result."""
    
    analysis_id: str
    keywords: List[KeywordResult]
    sentiment: SentimentResult
    topics: List[str]
    summary: str
    insights: Dict[str, str] = Field(default_factory=dict)
    analyzed_at: datetime
    confidence: float = Field(..., ge=0.0, le=1.0)


class AnalysisMetrics(BaseModel):
    """Metrics for analysis operations."""
    
    processing_time_ms: int
    token_count: int
    model_used: str
    cache_hit: bool = False

class Emotions(TypedDict):
    joy: float
    anger: float
    neutral: float

class Sentiment(TypedDict):
    sentiment: str
    score: float
    confidence: float
    emotions: Emotions