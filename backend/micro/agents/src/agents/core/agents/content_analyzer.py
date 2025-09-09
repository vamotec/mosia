"""Content analysis AI engine using OpenAI/Anthropic APIs."""

import asyncio
import json
from datetime import datetime, timezone
from typing import Dict, List

import anthropic
import openai
from anthropic.types import MessageParam
from openai.types.chat import ChatCompletionMessageParam

from ..models.analysis import (
    ContentInput,
    ContentAnalysisResult,
    KeywordResult,
    SentimentResult, Sentiment
)
from ...config.logging import get_logger


class ContentAnalyzer:
    """API-based content analysis engine."""
    
    def __init__(self, openai_api_key: str = None, anthropic_api_key: str = None):
        self.logger = get_logger(__name__)
        self.openai_client = openai.AsyncOpenAI(api_key=openai_api_key) if openai_api_key else None
        self.anthropic_client = anthropic.AsyncAnthropic(api_key=anthropic_api_key) if anthropic_api_key else None
        
    async def initialize(self) -> None:
        """Initialize API clients."""
        try:
            if self.openai_client:
                self.logger.info("OpenAI client initialized")
            if self.anthropic_client:
                self.logger.info("Anthropic client initialized")
            self.logger.info("Content analyzer initialized successfully")
        except Exception as e:
            self.logger.error("Failed to initialize content analyzer", error=str(e))
            raise
    
    async def analyze_content(self, input_data: ContentInput) -> ContentAnalysisResult:
        """Perform comprehensive content analysis using AI APIs."""
        start_time = asyncio.get_event_loop().time()
        
        try:
            # Use OpenAI for structured analysis
            analysis_data = await self._get_ai_analysis(input_data.content)
            
            # Parse AI response
            keywords = self._parse_keywords(analysis_data.get("keywords", []))
            sentiment = self._parse_sentiment(analysis_data.get("sentiment", {}))
            topics = analysis_data.get("topics", [])
            summary = analysis_data.get("summary", "")
            
            # Calculate business logic insights
            insights = await self._calculate_business_insights(
                input_data.content, keywords, sentiment, topics, input_data
            )
            
            # Calculate overall confidence
            confidence = self._calculate_confidence(keywords, sentiment, topics)
            
            # processing_time = int((asyncio.get_event_loop().time() - start_time) * 1000)
            analysis_id = f"analysis_{input_data.user_id}_{int(start_time)}"
            
            return ContentAnalysisResult(
                analysis_id=analysis_id,
                keywords=keywords,
                sentiment=sentiment,
                topics=topics,
                summary=summary,
                insights=insights,
                analyzed_at=datetime.now(timezone.utc),
                confidence=confidence
            )
            
        except Exception as e:
            self.logger.error("Content analysis failed", error=str(e))
            raise
    
    async def _get_ai_analysis(self, content: str) -> Dict:
        """Get structured analysis from AI API."""
        prompt = f"""
        Analyze the following content and return a JSON response with this structure:
        {{
            "keywords": [
                {{"text": "keyword", "score": 0.9, "category": "CONCEPT|PERSON|ORG|EVENT|PRODUCT"}}
            ],
            "sentiment": {{
                "sentiment": "positive|negative|neutral",
                "score": -1.0 to 1.0,
                "confidence": 0.0 to 1.0,
                "emotions": {{"joy": 0.8, "anger": 0.1, "neutral": 0.1}}
            }},
            "topics": ["topic1", "topic2", "topic3"],
            "summary": "Brief summary of the content"
        }}
        
        Content to analyze: {content}
        """
        
        try:
            if self.openai_client:
                messages = [
                    ChatCompletionMessageParam(role="user", content=prompt)
                ]
                response = await self.openai_client.chat.completions.create(
                    model="gpt-3.5-turbo",
                    messages=messages,
                    temperature=0.3,
                    max_tokens=1000
                )
                
                content_str = response.choices[0].message.content
                return json.loads(content_str)
                
        except Exception as e:
            self.logger.warning(f"OpenAI analysis failed: {e}")
            
        try:
            if self.anthropic_client:
                messages = [
                    MessageParam(role="user", content=prompt)
                ]
                response = await self.anthropic_client.messages.create(
                    model="claude-3-haiku-20240307",
                    max_tokens=1000,
                    temperature=0.3,
                    messages=messages
                )
                
                content_str = response.content[0].text
                return json.loads(content_str)
                
        except Exception as e:
            self.logger.warning(f"Anthropic analysis failed: {e}")
        
        # Fallback to basic analysis
        return await self._fallback_analysis(content)

    @staticmethod
    async def _fallback_analysis(content: str) -> Dict:
        """Basic fallback analysis without ML models."""
        words = content.lower().split()
        
        # Simple keyword extraction based on word frequency
        word_freq = {}
        for word in words:
            if len(word) > 3 and word.isalpha():
                word_freq[word] = word_freq.get(word, 0) + 1
        
        keywords = [
            {"text": word, "score": min(freq / len(words) * 10, 1.0), "category": "CONCEPT"}
            for word, freq in sorted(word_freq.items(), key=lambda x: x[1], reverse=True)[:10]
        ]
        
        # Simple sentiment analysis
        positive_words = {"good", "great", "excellent", "amazing", "wonderful", "fantastic", "love", "like", "best"}
        negative_words = {"bad", "terrible", "awful", "horrible", "disappointing", "frustrating", "hate", "dislike", "worst"}
        
        pos_count = sum(1 for word in words if word in positive_words)
        neg_count = sum(1 for word in words if word in negative_words)
        
        if pos_count > neg_count:
            # sentiment = {"sentiment": "positive", "score": 0.6, "confidence": 0.7}
            sentiment: Sentiment = {
                "sentiment": "positive",
                "score": 0.6,
                "confidence": 0.7,
                "emotions": {
                    "joy": 0.6,
                    "anger": 0.0,
                    "neutral": 0.4
                }
            }
        elif neg_count > pos_count:
            # sentiment = {"sentiment": "negative", "score": -0.6, "confidence": 0.7}
            sentiment: Sentiment = {
                "sentiment": "negative",
                "score": -0.6,
                "confidence": 0.7,
                "emotions": {
                    "joy": 0.0,
                    "anger": 0.6,
                    "neutral": 0.4
                }
            }
        else:
            # sentiment = {"sentiment": "neutral", "score": 0.0, "confidence": 0.5}
            sentiment: Sentiment = {
                "sentiment": "neutral",
                "score": 0.0,
                "confidence": 0.5,
                "emotions": {
                    "joy": 0.0,
                    "anger": 0.0,
                    "neutral": 1.0
                }
            }
        
        # sentiment["emotions"] = {
        #     "joy": max(0, sentiment["score"]),
        #     "anger": max(0, -sentiment["score"]),
        #     "neutral": 1 - abs(sentiment["score"])
        # }
        
        return {
            "keywords": keywords,
            "sentiment": sentiment,
            "topics": list(word_freq.keys())[:5],
            "summary": content[:200] + "..." if len(content) > 200 else content
        }

    @staticmethod
    def _parse_keywords(keywords_data: List[Dict]) -> List[KeywordResult]:
        """Parse keywords from AI response."""
        return [
            KeywordResult(
                text=kw.get("text", ""),
                score=kw.get("score", 0.5),
                category=kw.get("category", "CONCEPT")
            )
            for kw in keywords_data[:20]
        ]

    @staticmethod
    def _parse_sentiment(sentiment_data: Dict) -> SentimentResult:
        """Parse sentiment from AI response."""
        return SentimentResult(
            sentiment=sentiment_data.get("sentiment", "neutral"),
            score=sentiment_data.get("score", 0.0),
            confidence=sentiment_data.get("confidence", 0.5),
            emotions=sentiment_data.get("emotions", {"neutral": 1.0})
        )

    @staticmethod
    async def _calculate_business_insights(
        content: str, 
        keywords: List[KeywordResult], 
        sentiment: SentimentResult,
        topics: List[str],
        input_data: ContentInput
    ) -> Dict[str, str]:
        """Calculate business-specific insights for collaborative workspace."""
        insights = {}
        
        # Content characteristics
        word_count = len(content.split())
        insights["word_count"] = str(word_count)
        insights["reading_time"] = str(max(1, word_count // 200))  # minutes
        insights["content_complexity"] = "high" if word_count > 1000 else "medium" if word_count > 300 else "low"
        
        # Collaboration potential
        collaboration_keywords = [kw for kw in keywords if kw.category in ["PERSON", "ORG", "EVENT"]]
        insights["collaboration_potential"] = "high" if len(collaboration_keywords) >= 3 else "medium" if len(collaboration_keywords) >= 1 else "low"
        
        # Content type classification for workspace
        content_lower = content.lower()
        if any(word in content_lower for word in ["meeting", "agenda", "action", "task", "deadline"]):
            insights["content_type"] = "meeting_content"
        elif any(word in content_lower for word in ["project", "plan", "milestone", "deliverable"]):
            insights["content_type"] = "project_planning"
        elif any(word in content_lower for word in ["idea", "brainstorm", "concept", "innovation"]):
            insights["content_type"] = "creative_content"
        elif any(word in content_lower for word in ["report", "summary", "analysis", "findings"]):
            insights["content_type"] = "analytical_content"
        else:
            insights["content_type"] = "general_content"
        
        # Workspace context enhancement
        if input_data.workspace_id:
            insights["workspace_relevance"] = "high"  # Can be enhanced with workspace-specific business logic
        
        # Priority and urgency detection
        urgency_keywords = ["urgent", "asap", "immediately", "critical", "deadline", "emergency"]
        if any(word in content_lower for word in urgency_keywords):
            insights["urgency_level"] = "high"
        else:
            insights["urgency_level"] = "normal"
        
        # Actionability
        action_keywords = ["todo", "task", "action", "complete", "finish", "deliver", "implement"]
        if any(word in content_lower for word in action_keywords):
            insights["actionability"] = "actionable"
        else:
            insights["actionability"] = "informational"
        
        # Knowledge extraction potential
        if topics and len(keywords) > 5:
            insights["knowledge_value"] = "high"
        elif topics or len(keywords) > 2:
            insights["knowledge_value"] = "medium"
        else:
            insights["knowledge_value"] = "low"
        
        # Emotional tone for team dynamics
        insights["emotional_tone"] = sentiment.sentiment
        insights["team_sentiment_impact"] = "positive" if sentiment.score > 0.3 else "negative" if sentiment.score < -0.3 else "neutral"
        
        return insights

    @staticmethod
    def _calculate_confidence(
        keywords: List[KeywordResult], 
        sentiment: SentimentResult,
        topics: List[str]
    ) -> float:
        """Calculate overall analysis confidence."""
        confidence_factors = []
        
        # Keyword confidence
        if keywords:
            avg_keyword_score = sum(kw.score for kw in keywords) / len(keywords)
            confidence_factors.append(avg_keyword_score)
        
        # Sentiment confidence
        confidence_factors.append(sentiment.confidence)
        
        # Topic confidence
        topic_confidence = min(len(topics) / 5.0, 1.0)
        confidence_factors.append(topic_confidence)
        
        return sum(confidence_factors) / len(confidence_factors) if confidence_factors else 0.5