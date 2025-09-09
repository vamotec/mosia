"""Data models for recommendation system."""

from typing import Dict, List, Optional
from datetime import datetime
from pydantic import BaseModel, Field
from enum import Enum


class RecommendationType(str, Enum):
    """Types of recommendations."""
    CONTENT = "content"
    ACTION = "action"
    CONNECTION = "connection"
    LEARNING = "learning"


class RecommendationInput(BaseModel):
    """Input for recommendation requests."""
    
    user_id: str
    workspace_id: str
    context: str
    limit: int = Field(default=10, ge=1, le=50)
    filters: List[str] = Field(default_factory=list)
    recommendation_type: Optional[RecommendationType] = None


class Recommendation(BaseModel):
    """Individual recommendation."""
    
    id: str
    type: RecommendationType
    title: str
    description: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    metadata: Dict[str, str] = Field(default_factory=dict)
    reason: Optional[str] = None


class RecommendationResponse(BaseModel):
    """Response containing recommendations."""
    
    recommendations: List[Recommendation]
    recommendation_context: str
    generated_at: datetime
    total_candidates: int
    filter_applied: List[str] = Field(default_factory=list)


class UserPreferences(BaseModel):
    """User preference model."""
    
    user_id: str
    preferences: Dict[str, str] = Field(default_factory=dict)
    interests: List[str] = Field(default_factory=list)
    updated_at: datetime
    version: int = Field(default=1)


class RecommendationMetrics(BaseModel):
    """Metrics for recommendation operations."""
    
    processing_time_ms: int
    candidate_count: int
    filtering_time_ms: int
    ranking_time_ms: int
    cache_hit_rate: float = Field(..., ge=0.0, le=1.0)